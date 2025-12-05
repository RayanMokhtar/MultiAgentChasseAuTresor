package sma.gui;

import sma.agents.*;
import sma.simulation.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;

/**
 * Panel simplifié des statistiques
 */
public class StatsPanel extends JPanel {
    
    private final Simulation simulation;
    
    // Labels stats
    private JLabel lblDuree;
    private JLabel lblTresors;
    private JLabel lblBlessures;
    private JLabel lblSecours;
    private JLabel lblAnimauxTues;
    
    // Panel agents
    private JPanel agentsPanel;
    
    public StatsPanel(Simulation simulation) {
        this.simulation = simulation;
        
        setPreferredSize(new Dimension(200, 500));
        setBackground(new Color(50, 50, 50));
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        
        initialiserUI();
    }

    private void initialiserUI() {
        // Titre
        JLabel titre = new JLabel("STATISTIQUES", SwingConstants.CENTER);
        titre.setFont(new Font("Arial", Font.BOLD, 14));
        titre.setForeground(Color.WHITE);
        titre.setBorder(new EmptyBorder(0, 0, 8, 0));
        add(titre, BorderLayout.NORTH);
        
        // Panel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(50, 50, 50));
        
        // Stats globales
        JPanel globalPanel = creerPanelStats();
        mainPanel.add(globalPanel);
        
        mainPanel.add(Box.createVerticalStrut(10));
        
        // Légende
        JPanel legendePanel = creerLegende();
        mainPanel.add(legendePanel);
        
        mainPanel.add(Box.createVerticalStrut(10));
        
        // Agents
        JLabel agentsTitre = new JLabel("AGENTS");
        agentsTitre.setFont(new Font("Arial", Font.BOLD, 12));
        agentsTitre.setForeground(Color.WHITE);
        agentsTitre.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(agentsTitre);
        
        mainPanel.add(Box.createVerticalStrut(5));
        
        agentsPanel = new JPanel();
        agentsPanel.setLayout(new BoxLayout(agentsPanel, BoxLayout.Y_AXIS));
        agentsPanel.setBackground(new Color(60, 60, 60));
        
        JScrollPane scrollPane = new JScrollPane(agentsPanel);
        scrollPane.setPreferredSize(new Dimension(180, 250));
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scrollPane);
        
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel creerPanelStats() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 4, 4));
        panel.setBackground(new Color(60, 60, 60));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        
        panel.add(creerLabel("Durée:", false));
        lblDuree = creerLabel("00:00", true);
        panel.add(lblDuree);
        
        panel.add(creerLabel("Trésors:", false));
        lblTresors = creerLabel("0", true);
        panel.add(lblTresors);
        
        panel.add(creerLabel("Blessures:", false));
        lblBlessures = creerLabel("0", true);
        panel.add(lblBlessures);
        
        panel.add(creerLabel("Secours:", false));
        lblSecours = creerLabel("0", true);
        panel.add(lblSecours);
        
        panel.add(creerLabel("Animaux tués:", false));
        lblAnimauxTues = creerLabel("0", true);
        panel.add(lblAnimauxTues);
        
        return panel;
    }

    private JPanel creerLegende() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 2, 2));
        panel.setBackground(new Color(60, 60, 60));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        ajouterItemLegende(panel, new Color(0, 100, 255), "C = Cognitif");
        ajouterItemLegende(panel, new Color(255, 100, 0), "R = Réactif");
        ajouterItemLegende(panel, new Color(128, 0, 128), "S = Sentinelle");
        ajouterItemLegende(panel, new Color(255, 215, 0), "Carré = Trésor");
        ajouterItemLegende(panel, Color.RED, "Triangle = Animal");
        ajouterItemLegende(panel, Color.GRAY, "Carré gris = Obstacle");
        
        return panel;
    }

    private void ajouterItemLegende(JPanel panel, Color couleur, String texte) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        item.setBackground(new Color(60, 60, 60));
        
        JPanel carre = new JPanel();
        carre.setPreferredSize(new Dimension(10, 10));
        carre.setBackground(couleur);
        item.add(carre);
        
        JLabel label = new JLabel(texte);
        label.setFont(new Font("Arial", Font.PLAIN, 10));
        label.setForeground(Color.LIGHT_GRAY);
        item.add(label);
        
        panel.add(item);
    }

    private JLabel creerLabel(String texte, boolean valeur) {
        JLabel label = new JLabel(texte);
        label.setForeground(valeur ? new Color(100, 255, 100) : Color.LIGHT_GRAY);
        label.setFont(new Font("Consolas", valeur ? Font.BOLD : Font.PLAIN, 11));
        return label;
    }

    public void mettreAJour() {
        Statistiques stats = simulation.getStats();
        
        lblDuree.setText(stats.getDureeFormatee());
        lblTresors.setText(String.valueOf(stats.getTotalTresorsCollectes()));
        lblBlessures.setText(String.valueOf(stats.getTotalBlessures()));
        lblSecours.setText(String.valueOf(stats.getTotalSecours()));
        lblAnimauxTues.setText(String.valueOf(stats.getAnimauxTues()));
        
        mettreAJourAgents();
    }

    private void mettreAJourAgents() {
        agentsPanel.removeAll();
        
        List<Agent> agents = simulation.getAgents();
        for (Agent agent : agents) {
            agentsPanel.add(creerPanelAgent(agent));
            agentsPanel.add(Box.createVerticalStrut(3));
        }
        
        agentsPanel.revalidate();
        agentsPanel.repaint();
    }

    private JPanel creerPanelAgent(Agent agent) {
        JPanel panel = new JPanel(new BorderLayout(3, 2));
        panel.setBackground(new Color(70, 70, 70));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(agent.getCouleur(), 1),
            new EmptyBorder(3, 5, 3, 5)
        ));
        panel.setMaximumSize(new Dimension(180, 45));
        
        // Nom et état
        String etat = agent.isBlesse() ? " [BLESSE]" : "";
        JLabel lblNom = new JLabel(agent.getNom() + etat);
        lblNom.setFont(new Font("Arial", Font.BOLD, 10));
        lblNom.setForeground(agent.isBlesse() ? Color.RED : agent.getCouleur());
        panel.add(lblNom, BorderLayout.NORTH);
        
        // Stats
        String info = "Trésors:" + agent.getTresorsCollectes().size();
        if (agent.aUnFusil()) {
            info += " | Fusil";
        }
        JLabel lblInfo = new JLabel(info);
        lblInfo.setFont(new Font("Consolas", Font.PLAIN, 9));
        lblInfo.setForeground(Color.LIGHT_GRAY);
        panel.add(lblInfo, BorderLayout.CENTER);
        
        return panel;
    }
}
