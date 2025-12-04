package sma.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Dialogue de configuration de la simulation
 */
public class ConfigDialog extends JDialog {
    
    // Paramètres de la carte
    private JSpinner spnLargeur;
    private JSpinner spnHauteur;
    private JSpinner spnZonesX;
    private JSpinner spnZonesY;
    
    // Paramètres des objets
    private JSpinner spnTresors;
    private JSpinner spnAnimaux;
    private JSpinner spnObstacles;
    
    // Paramètres des agents
    private JSpinner spnAgentsCognitifs;
    private JSpinner spnAgentsReactifs;
    
    private boolean confirme = false;

    public ConfigDialog(Frame parent) {
        super(parent, "⚙️ Configuration de la Simulation", true);
        
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(45, 45, 45));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));
        
        initialiserUI();
        
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initialiserUI() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(55, 55, 55));
        tabbedPane.setForeground(Color.WHITE);
        
        // Onglet Carte
        tabbedPane.addTab("🗺️ Carte", creerPanelCarte());
        
        // Onglet Objets
        tabbedPane.addTab("📦 Objets", creerPanelObjets());
        
        // Onglet Agents
        tabbedPane.addTab("👥 Agents", creerPanelAgents());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Boutons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(new Color(45, 45, 45));
        
        JButton btnAnnuler = new JButton("Annuler");
        btnAnnuler.addActionListener(e -> dispose());
        btnPanel.add(btnAnnuler);
        
        JButton btnConfirmer = new JButton("Confirmer");
        btnConfirmer.setBackground(new Color(46, 204, 113));
        btnConfirmer.setForeground(Color.WHITE);
        btnConfirmer.addActionListener(e -> {
            confirme = true;
            dispose();
        });
        btnPanel.add(btnConfirmer);
        
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JPanel creerPanelCarte() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(55, 55, 55));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Largeur
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(creerLabel("Largeur (pixels):"), gbc);
        gbc.gridx = 1;
        spnLargeur = creerSpinner(600, 400, 1200, 50);
        panel.add(spnLargeur, gbc);
        
        // Hauteur
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(creerLabel("Hauteur (pixels):"), gbc);
        gbc.gridx = 1;
        spnHauteur = creerSpinner(600, 400, 1200, 50);
        panel.add(spnHauteur, gbc);
        
        // Zones X
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(creerLabel("Zones horizontales:"), gbc);
        gbc.gridx = 1;
        spnZonesX = creerSpinner(5, 3, 10, 1);
        panel.add(spnZonesX, gbc);
        
        // Zones Y
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(creerLabel("Zones verticales:"), gbc);
        gbc.gridx = 1;
        spnZonesY = creerSpinner(5, 3, 10, 1);
        panel.add(spnZonesY, gbc);
        
        return panel;
    }

    private JPanel creerPanelObjets() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(55, 55, 55));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Trésors
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(creerLabel("💰 Nombre de trésors:"), gbc);
        gbc.gridx = 1;
        spnTresors = creerSpinner(15, 5, 50, 1);
        panel.add(spnTresors, gbc);
        
        // Animaux
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(creerLabel("🐾 Nombre d'animaux:"), gbc);
        gbc.gridx = 1;
        spnAnimaux = creerSpinner(10, 3, 30, 1);
        panel.add(spnAnimaux, gbc);
        
        // Obstacles
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(creerLabel("🪨 Nombre d'obstacles:"), gbc);
        gbc.gridx = 1;
        spnObstacles = creerSpinner(20, 5, 50, 1);
        panel.add(spnObstacles, gbc);
        
        return panel;
    }

    private JPanel creerPanelAgents() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(55, 55, 55));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Agents Cognitifs
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(creerLabel("🧠 Agents Cognitifs:"), gbc);
        gbc.gridx = 1;
        spnAgentsCognitifs = creerSpinner(3, 1, 10, 1);
        panel.add(spnAgentsCognitifs, gbc);
        
        // Agents Réactifs
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(creerLabel("⚡ Agents Réactifs:"), gbc);
        gbc.gridx = 1;
        spnAgentsReactifs = creerSpinner(4, 1, 10, 1);
        panel.add(spnAgentsReactifs, gbc);
        
        // Note sur les agents communicants
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        JLabel note = new JLabel("<html><i>📡 Les agents communicants sont<br>automatiquement créés (1 pour 2 zones)</i></html>");
        note.setForeground(Color.LIGHT_GRAY);
        note.setFont(new Font("Arial", Font.PLAIN, 11));
        panel.add(note, gbc);
        
        return panel;
    }

    private JLabel creerLabel(String texte) {
        JLabel label = new JLabel(texte);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        return label;
    }

    private JSpinner creerSpinner(int valeur, int min, int max, int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(valeur, min, max, step));
        spinner.setPreferredSize(new Dimension(80, 25));
        return spinner;
    }

    public boolean isConfirme() {
        return confirme;
    }

    public int getLargeur() { return (Integer) spnLargeur.getValue(); }
    public int getHauteur() { return (Integer) spnHauteur.getValue(); }
    public int getZonesX() { return (Integer) spnZonesX.getValue(); }
    public int getZonesY() { return (Integer) spnZonesY.getValue(); }
    public int getNbTresors() { return (Integer) spnTresors.getValue(); }
    public int getNbAnimaux() { return (Integer) spnAnimaux.getValue(); }
    public int getNbObstacles() { return (Integer) spnObstacles.getValue(); }
    public int getNbAgentsCognitifs() { return (Integer) spnAgentsCognitifs.getValue(); }
    public int getNbAgentsReactifs() { return (Integer) spnAgentsReactifs.getValue(); }
}
