package sma.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import sma.environnement.Carte;
import sma.simulation.*;

/**
 * Fenêtre principale de l'application
 */
public class MainFrame extends JFrame implements Simulation.SimulationListener {
    
    private Simulation simulation;
    private CartePanel cartePanel;
    private StatsPanel statsPanel;
    private ControlPanel controlPanel;
    
    private Timer timerRafraichissement;
    private static final int FPS = 30; // Rafraîchissement 30 fois par seconde
    
    public MainFrame() {
        setTitle("🗺️ Chasse au Trésor - Système Multi-Agents | CY Cergy Paris Université - Master IISC 2 Pro");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        
        // Initialiser la simulation
        initialiserSimulation();
        
        // Initialiser l'interface
        initialiserUI();
        
        // Timer de rafraîchissement
        timerRafraichissement = new Timer(1000 / FPS, e -> rafraichir());
        timerRafraichissement.start();
        
        // Centrer la fenêtre
        pack();
        setLocationRelativeTo(null);
        
        // Gestion fermeture
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                arreterSimulation();
            }
        });
    }

    private void initialiserSimulation() {
        // Créer une nouvelle carte avec les paramètres par défaut (9 zones 3x3)
        Carte carte = new Carte();
        
        simulation = new Simulation(carte);
        simulation.ajouterListener(this);
    }

    private void initialiserUI() {
        setLayout(new BorderLayout(5, 5));
        getContentPane().setBackground(new Color(30, 30, 30));
        
        // Panel de la carte (centre)
        cartePanel = new CartePanel(simulation);
        JScrollPane scrollCarte = new JScrollPane(cartePanel);
        scrollCarte.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 2));
        scrollCarte.getViewport().setBackground(new Color(30, 30, 30));
        add(scrollCarte, BorderLayout.CENTER);
        
        // Panel des statistiques (droite)
        statsPanel = new StatsPanel(simulation);
        add(statsPanel, BorderLayout.EAST);
        
        // Panel de contrôle (bas)
        controlPanel = new ControlPanel(simulation, this);
        add(controlPanel, BorderLayout.SOUTH);
        
        // Panel d'information (haut)
        JPanel headerPanel = creerHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
    }

    private JPanel creerHeaderPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(new Color(50, 100, 50));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Titre simple
        JLabel titre = new JLabel("Chasse au Trésor - SMA");
        titre.setFont(new Font("Arial", Font.BOLD, 16));
        titre.setForeground(Color.WHITE);
        panel.add(titre);
        
        return panel;
    }

    private void rafraichir() {
        if (cartePanel != null) {
            cartePanel.rafraichir();
        }
        if (statsPanel != null && simulation.isEnCours()) {
            statsPanel.mettreAJour();
        }
    }

    public void nouvelleSimulation() {
        // Arrêter l'ancienne simulation
        if (simulation != null) {
            simulation.arreter();
            simulation.retirerListener(this);
        }
        
        // Créer une nouvelle simulation
        initialiserSimulation();
        
        // Recréer l'interface
        getContentPane().removeAll();
        initialiserUI();
        
        // Rafraîchir
        revalidate();
        repaint();
    }

    private void arreterSimulation() {
        if (timerRafraichissement != null) {
            timerRafraichissement.stop();
        }
        if (simulation != null && simulation.isEnCours()) {
            simulation.arreter();
        }
    }

    // Implémentation de SimulationListener
    @Override
    public void onUpdate() {
        // Géré par le timer
    }

    @Override
    public void onTerminee() {
        SwingUtilities.invokeLater(() -> {
            if (controlPanel != null) {
                controlPanel.onSimulationTerminee();
            }
            statsPanel.mettreAJour();
        });
    }
}
