package sma.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import sma.simulation.Simulation;

public class MainGui extends JFrame implements Runnable {
    
    private static final long serialVersionUID = 1L;
    
    private Simulation simulation;
    private Dashboard dashboard;
    private InfoPanel infoPanel;
    private JButton btnStart;
    private JButton btnStop;
    private boolean running = false;

    public MainGui() {
        super("SMA - Chasse au Trésor Multi-Agents");
        
        simulation = new Simulation();
        dashboard = new Dashboard(simulation);
        infoPanel = new InfoPanel(simulation);
        
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
        contentPane.add(dashboard, BorderLayout.CENTER);
        contentPane.add(infoPanel, BorderLayout.EAST);
        contentPane.add(controlPanel, BorderLayout.SOUTH);
        
        setSize(SimuPara.WINDOW_WIDTH, SimuPara.WINDOW_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void demarrerSimulation() {
        running = true;
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        simulation.demarrer();
        new Thread(this).start();
    }

    private void arreterSimulation() {
        running = false;
        simulation.arreter();
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
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