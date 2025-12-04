package sma.gui;

import sma.agents.*;
import sma.simulation.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * Panel latéral affichant les statistiques et informations des agents
 */
public class StatsPanel extends JPanel {
    
    private final Simulation simulation;
    
    // Labels pour les stats globales
    private JLabel lblDuree;
    private JLabel lblIterations;
    private JLabel lblCombats;
    private JLabel lblMorts;
    private JLabel lblTresors;
    private JLabel lblScore;
    private JLabel lblZones;
    
    // Panel pour les agents
    private JPanel agentsPanel;
    
    public StatsPanel(Simulation simulation) {
        this.simulation = simulation;
        
        setPreferredSize(new Dimension(280, 600));
        setBackground(new Color(45, 45, 45));
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        initialiserUI();
    }

    private void initialiserUI() {
        // Titre
        JLabel titre = new JLabel("📊 STATISTIQUES", SwingConstants.CENTER);
        titre.setFont(new Font("Arial", Font.BOLD, 16));
        titre.setForeground(Color.WHITE);
        titre.setBorder(new EmptyBorder(0, 0, 10, 0));
        add(titre, BorderLayout.NORTH);
        
        // Panel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(45, 45, 45));
        
        // Stats globales
        JPanel globalPanel = creerPanelStats();
        mainPanel.add(globalPanel);
        
        mainPanel.add(Box.createVerticalStrut(15));
        
        // Panel agents
        JPanel agentsTitrePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        agentsTitrePanel.setBackground(new Color(45, 45, 45));
        JLabel agentsTitre = new JLabel("👥 AGENTS");
        agentsTitre.setFont(new Font("Arial", Font.BOLD, 14));
        agentsTitre.setForeground(Color.WHITE);
        agentsTitrePanel.add(agentsTitre);
        mainPanel.add(agentsTitrePanel);
        
        agentsPanel = new JPanel();
        agentsPanel.setLayout(new BoxLayout(agentsPanel, BoxLayout.Y_AXIS));
        agentsPanel.setBackground(new Color(55, 55, 55));
        
        JScrollPane scrollPane = new JScrollPane(agentsPanel);
        scrollPane.setPreferredSize(new Dimension(260, 350));
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane);
        
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel creerPanelStats() {
        JPanel panel = new JPanel(new GridLayout(7, 2, 5, 5));
        panel.setBackground(new Color(55, 55, 55));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 80)),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        panel.add(creerLabel("⏱️ Durée:", false));
        lblDuree = creerLabel("00:00", true);
        panel.add(lblDuree);
        
        panel.add(creerLabel("🔄 Itérations:", false));
        lblIterations = creerLabel("0", true);
        panel.add(lblIterations);
        
        panel.add(creerLabel("⚔️ Combats:", false));
        lblCombats = creerLabel("0", true);
        panel.add(lblCombats);
        
        panel.add(creerLabel("💀 Morts:", false));
        lblMorts = creerLabel("0", true);
        panel.add(lblMorts);
        
        panel.add(creerLabel("💰 Trésors:", false));
        lblTresors = creerLabel("0", true);
        panel.add(lblTresors);
        
        panel.add(creerLabel("🏆 Score:", false));
        lblScore = creerLabel("0", true);
        panel.add(lblScore);
        
        panel.add(creerLabel("🗺️ Zones:", false));
        lblZones = creerLabel("0", true);
        panel.add(lblZones);
        
        return panel;
    }

    private JLabel creerLabel(String texte, boolean valeur) {
        JLabel label = new JLabel(texte);
        label.setForeground(valeur ? new Color(100, 255, 100) : Color.LIGHT_GRAY);
        label.setFont(new Font("Consolas", valeur ? Font.BOLD : Font.PLAIN, 12));
        return label;
    }

    public void mettreAJour() {
        Statistiques stats = simulation.getStats();
        
        // Mettre à jour les stats globales
        lblDuree.setText(stats.getDureeFormatee());
        lblIterations.setText(String.valueOf(stats.getTotalIterations()));
        lblCombats.setText(String.valueOf(stats.getTotalCombats()));
        lblMorts.setText(String.valueOf(stats.getTotalMorts()));
        lblTresors.setText(String.valueOf(stats.getTotalTresorsCollectes()));
        lblScore.setText(String.valueOf(stats.getScoreTotal()));
        lblZones.setText(stats.getZonesExplorees() + "/" + simulation.getCarte().getNombreZones());
        
        // Mettre à jour les agents
        mettreAJourAgents();
    }

    private void mettreAJourAgents() {
        agentsPanel.removeAll();
        
        List<Agent> agents = simulation.getAgents();
        for (Agent agent : agents) {
            if (!(agent instanceof AgentCommunicant)) { // Ne pas afficher les communicants
                agentsPanel.add(creerPanelAgent(agent));
                agentsPanel.add(Box.createVerticalStrut(5));
            }
        }
        
        agentsPanel.revalidate();
        agentsPanel.repaint();
    }

    private JPanel creerPanelAgent(Agent agent) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(new Color(65, 65, 65));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(agent.getCouleur(), 2),
            new EmptyBorder(5, 8, 5, 8)
        ));
        panel.setMaximumSize(new Dimension(260, 80));
        
        // Nom et type
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setBackground(new Color(65, 65, 65));
        
        JLabel lblNom = new JLabel(agent.getNom());
        lblNom.setFont(new Font("Arial", Font.BOLD, 12));
        lblNom.setForeground(agent.getCouleur());
        headerPanel.add(lblNom);
        
        JLabel lblType = new JLabel("(" + agent.getTypeAgent() + ")");
        lblType.setFont(new Font("Arial", Font.ITALIC, 10));
        lblType.setForeground(Color.GRAY);
        headerPanel.add(lblType);
        
        // Indicateur vie/mort
        if (!agent.isEnVie()) {
            JLabel lblMort = new JLabel("💀");
            headerPanel.add(lblMort);
        }
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Stats de l'agent
        JPanel statsAgent = new JPanel(new GridLayout(2, 3, 3, 2));
        statsAgent.setBackground(new Color(65, 65, 65));
        
        // Ligne 1: PV, Énergie, Score
        JLabel lblPV = new JLabel("❤️ " + agent.getPointsDeVie() + "/" + agent.getPointsDeVieMax());
        lblPV.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblPV.setForeground(getPVColor(agent));
        statsAgent.add(lblPV);
        
        JLabel lblEnergie = new JLabel("⚡ " + agent.getEnergie() + "/" + agent.getEnergieMax());
        lblEnergie.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblEnergie.setForeground(Color.YELLOW);
        statsAgent.add(lblEnergie);
        
        JLabel lblScoreAgent = new JLabel("💰 " + agent.getScoreTotal());
        lblScoreAgent.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblScoreAgent.setForeground(new Color(255, 215, 0));
        statsAgent.add(lblScoreAgent);
        
        // Ligne 2: Combats, Victoires, Morts
        JLabel lblCombatsAgent = new JLabel("⚔️ " + agent.getNombreCombats());
        lblCombatsAgent.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblCombatsAgent.setForeground(Color.LIGHT_GRAY);
        statsAgent.add(lblCombatsAgent);
        
        JLabel lblVictoires = new JLabel("🏆 " + agent.getNombreVictoires());
        lblVictoires.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblVictoires.setForeground(new Color(100, 255, 100));
        statsAgent.add(lblVictoires);
        
        JLabel lblMortsAgent = new JLabel("💀 " + agent.getNombreMorts());
        lblMortsAgent.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblMortsAgent.setForeground(new Color(255, 100, 100));
        statsAgent.add(lblMortsAgent);
        
        panel.add(statsAgent, BorderLayout.CENTER);
        
        // Mission pour agent cognitif
        if (agent instanceof AgentCognitif) {
            AgentCognitif ac = (AgentCognitif) agent;
            JLabel lblMission = new JLabel(ac.getMissionActuelle().getDescription());
            lblMission.setFont(new Font("Arial", Font.ITALIC, 9));
            lblMission.setForeground(new Color(150, 150, 255));
            panel.add(lblMission, BorderLayout.SOUTH);
        }
        
        return panel;
    }

    private Color getPVColor(Agent agent) {
        float ratio = (float) agent.getPointsDeVie() / agent.getPointsDeVieMax();
        if (ratio > 0.6) return new Color(100, 255, 100);
        if (ratio > 0.3) return new Color(255, 165, 0);
        return new Color(255, 100, 100);
    }
}
