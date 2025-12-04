package sma.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import sma.simulation.*;

/**
 * Panel de contrôle de la simulation
 */
public class ControlPanel extends JPanel {
    
    private final Simulation simulation;
    private final MainFrame mainFrame;
    
    private JButton btnDemarrer;
    private JButton btnPause;
    private JButton btnArreter;
    private JButton btnReset;
    
    private JLabel lblStatus;
    
    public ControlPanel(Simulation simulation, MainFrame mainFrame) {
        this.simulation = simulation;
        this.mainFrame = mainFrame;
        
        setBackground(new Color(35, 35, 35));
        setLayout(new FlowLayout(FlowLayout.CENTER, 15, 10));
        setBorder(new EmptyBorder(5, 10, 5, 10));
        
        initialiserUI();
    }

    private void initialiserUI() {
        // Bouton Démarrer
        btnDemarrer = creerBouton("▶ Démarrer", new Color(46, 204, 113));
        btnDemarrer.addActionListener(e -> demarrerSimulation());
        add(btnDemarrer);
        
        // Bouton Pause
        btnPause = creerBouton("⏸ Pause", new Color(241, 196, 15));
        btnPause.setEnabled(false);
        btnPause.addActionListener(e -> togglePause());
        add(btnPause);
        
        // Bouton Arrêter
        btnArreter = creerBouton("⏹ Arrêter", new Color(231, 76, 60));
        btnArreter.setEnabled(false);
        btnArreter.addActionListener(e -> arreterSimulation());
        add(btnArreter);
        
        // Séparateur
        add(Box.createHorizontalStrut(20));
        
        // Bouton Reset
        btnReset = creerBouton("🔄 Nouvelle Simulation", new Color(52, 152, 219));
        btnReset.addActionListener(e -> resetSimulation());
        add(btnReset);
        
        // Séparateur
        add(Box.createHorizontalStrut(30));
        
        // Status
        lblStatus = new JLabel("⏸ En attente");
        lblStatus.setFont(new Font("Arial", Font.BOLD, 14));
        lblStatus.setForeground(Color.LIGHT_GRAY);
        add(lblStatus);
    }

    private JButton creerBouton(String texte, Color couleur) {
        JButton btn = new JButton(texte);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(couleur);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(140, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Effet hover
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBackground(couleur.brighter());
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(couleur);
            }
        });
        
        return btn;
    }

    private void demarrerSimulation() {
        simulation.demarrer();
        updateBoutons(true);
        lblStatus.setText("▶ En cours...");
        lblStatus.setForeground(new Color(46, 204, 113));
    }

    private void togglePause() {
        simulation.togglePause();
        
        if (simulation.isPause()) {
            btnPause.setText("▶ Reprendre");
            btnPause.setBackground(new Color(46, 204, 113));
            lblStatus.setText("⏸ En pause");
            lblStatus.setForeground(new Color(241, 196, 15));
        } else {
            btnPause.setText("⏸ Pause");
            btnPause.setBackground(new Color(241, 196, 15));
            lblStatus.setText("▶ En cours...");
            lblStatus.setForeground(new Color(46, 204, 113));
        }
    }

    private void arreterSimulation() {
        simulation.arreter();
        updateBoutons(false);
        lblStatus.setText("⏹ Terminée");
        lblStatus.setForeground(new Color(231, 76, 60));
    }

    private void resetSimulation() {
        if (simulation.isEnCours()) {
            simulation.arreter();
        }
        mainFrame.nouvelleSimulation();
    }

    private void updateBoutons(boolean enCours) {
        btnDemarrer.setEnabled(!enCours);
        btnPause.setEnabled(enCours);
        btnArreter.setEnabled(enCours);
    }

    public void onSimulationTerminee() {
        SwingUtilities.invokeLater(() -> {
            updateBoutons(false);
            lblStatus.setText("✅ Terminée - Tous les trésors collectés!");
            lblStatus.setForeground(new Color(46, 204, 113));
            
            // Afficher un message de fin
            JOptionPane.showMessageDialog(this,
                "🎉 Simulation terminée!\n\n" + simulation.getStats().toString(),
                "Fin de la simulation",
                JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
