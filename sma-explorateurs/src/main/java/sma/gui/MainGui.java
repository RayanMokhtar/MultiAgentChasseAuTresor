package sma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;

import sma.simulation.Simulation;

public class MainGui extends JFrame implements Runnable {
    
    private static final long serialVersionUID = 1L;
    
    private Simulation simulation;
    private Dashboard dashboard;
    private InfoPanel infoPanel;
    private JButton btnStart;
    private JButton btnStop;
    private JPanel dashboardWrapper;
    private JPanel settingsPanel;
    private JTextField tfReactifs;
    private JTextField tfCognitifs;
    private JTextField tfCommunicants;
    private JTextField tfTresors;
    private JTextField tfAnimaux;
    private JTextField tfObstacles;
    private JTextField tfDelay;
    private boolean running = false;

    public MainGui() {
        super("SMA - Chasse au Trésor Multi-Agents");
        
        simulation = new Simulation();
        dashboard = new Dashboard(simulation);
        infoPanel = new InfoPanel(simulation);
        dashboardWrapper = new JPanel(new BorderLayout());
        dashboardWrapper.add(dashboard, BorderLayout.CENTER);

        // Panel de paramétrage simple
        settingsPanel = creerSettingsPanel();

        // Panel de contrôle
        JPanel controlPanel = new JPanel(new FlowLayout());
        btnStart = new JButton("Démarrer Simulation");
        btnStop = new JButton("Arrêter la simukation ");
        btnStop.setEnabled(false);
        
        btnStart.addActionListener(e -> demarrerSimulation()); //ajouter les déclencheruis d'évenements
        btnStop.addActionListener(e -> arreterSimulation());
        
        controlPanel.add(btnStart);
        controlPanel.add(btnStop);
        //inspiré code du train
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(dashboardWrapper, BorderLayout.CENTER);
        contentPane.add(infoPanel, BorderLayout.EAST);
        JPanel south = new JPanel(new BorderLayout());
        south.add(settingsPanel, BorderLayout.CENTER);
        south.add(controlPanel, BorderLayout.SOUTH);
        contentPane.add(south, BorderLayout.SOUTH);
        
        setSize(SimuPara.WINDOW_WIDTH, SimuPara.WINDOW_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void demarrerSimulation() {
        running = true;
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        appliquerParametres();
        simulation = new Simulation();
        // Rafraîchir les panneaux avec la nouvelle simulation
        dashboardWrapper.removeAll();
        dashboard = new Dashboard(simulation);
        dashboardWrapper.add(dashboard, BorderLayout.CENTER);
        getContentPane().remove(infoPanel);
        infoPanel = new InfoPanel(simulation);
        getContentPane().add(infoPanel, BorderLayout.EAST);
        getContentPane().revalidate();
        getContentPane().repaint();

        simulation.demarrer();
        new Thread(this).start();
    }

    private void arreterSimulation() {
        running = false;
        simulation.arreter();
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private JPanel creerSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 7, 8, 4));
        tfReactifs = new JTextField(String.valueOf(SimuPara.NB_AGENTS_REACTIFS), 4);
        tfCognitifs = new JTextField(String.valueOf(SimuPara.NB_AGENTS_COGNITIFS), 4);
        tfCommunicants = new JTextField(String.valueOf(SimuPara.NB_AGENTS_COMMUNICANTS), 4);
        tfTresors = new JTextField(String.valueOf(SimuPara.NB_TRESORS_PAR_ZONE), 4);
        tfAnimaux = new JTextField(String.valueOf(SimuPara.NB_ANIMAUX_PAR_ZONE), 4);
        tfObstacles = new JTextField(String.valueOf(SimuPara.NB_OBSTACLES_PAR_ZONE), 4);
        tfDelay = new JTextField(String.valueOf(SimuPara.DELAY_MS), 4);

        panel.add(new JLabel("Réactifs"));
        panel.add(new JLabel("Cognitifs"));
        panel.add(new JLabel("Commun."));
        panel.add(new JLabel("Trésors/zone"));
        panel.add(new JLabel("Animaux/zone"));
        panel.add(new JLabel("Obstacles/zone"));
        panel.add(new JLabel("Delay ms"));

        panel.add(tfReactifs);
        panel.add(tfCognitifs);
        panel.add(tfCommunicants);
        panel.add(tfTresors);
        panel.add(tfAnimaux);
        panel.add(tfObstacles);
        panel.add(tfDelay);
        return panel;
    }

    private void appliquerParametres() {
        SimuPara.NB_AGENTS_REACTIFS = lireEntier(tfReactifs, SimuPara.NB_AGENTS_REACTIFS);
        SimuPara.NB_AGENTS_COGNITIFS = lireEntier(tfCognitifs, SimuPara.NB_AGENTS_COGNITIFS);
        SimuPara.NB_AGENTS_COMMUNICANTS = lireEntier(tfCommunicants, SimuPara.NB_AGENTS_COMMUNICANTS);
        SimuPara.NB_TRESORS_PAR_ZONE = lireEntier(tfTresors, SimuPara.NB_TRESORS_PAR_ZONE);
        SimuPara.NB_ANIMAUX_PAR_ZONE = lireEntier(tfAnimaux, SimuPara.NB_ANIMAUX_PAR_ZONE);
        SimuPara.NB_OBSTACLES_PAR_ZONE = lireEntier(tfObstacles, SimuPara.NB_OBSTACLES_PAR_ZONE);
        SimuPara.DELAY_MS = lireEntier(tfDelay, (int) SimuPara.DELAY_MS);
    }

    private int lireEntier(JTextField tf, int defaut) {
        try {
            return Math.max(0, Integer.parseInt(tf.getText().trim()));
        } catch (NumberFormatException e) {
            tf.setText(String.valueOf(defaut));
            return defaut;
        }
    }

    @Override
    public void run() {
        while (running) {
            dashboard.repaint();
            infoPanel.mettreAJour();
            
            try {
                Thread.sleep(50); //équivalent 20 fps
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}