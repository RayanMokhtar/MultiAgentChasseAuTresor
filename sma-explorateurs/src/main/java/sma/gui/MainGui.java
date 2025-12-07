package sma.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import sma.simulation.Simulation;

public class MainGui extends JFrame implements Runnable {
    
    private static final long serialVersionUID = 1L;
    private Simulation simulation;
    private Dashboard dashboard;
    private InfoPanel infoPanel;
    private JPanel configPanel;
    private JPanel centerPanel;
    
    private JButton btnStart;
    private JButton btnStop;
    private JButton btnReset;
    private boolean running = false;

    // Spinners pour les paramètres
    private JSpinner spnReactifs;
    private JSpinner spnCognitifs;
    private JSpinner spnCommunicants;
    private JSpinner spnTresors;
    private JSpinner spnAnimaux;
    private JSpinner spnObstacles;
    private JSpinner spnDegatsMax;
    private JSpinner spnDelayMs;
    private JSpinner spnDuration;

    public MainGui() {
        super("Chasse au Trésor MultiAgents (bdi+react+comm)");
        initUI();
    }

    private void initUI() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(5, 5));
        contentPane.setBackground(new Color(45, 45, 48));

        configPanel = createConfigPanel();
        contentPane.add(configPanel, BorderLayout.WEST);

        // Panel central (message d'attente) ....
        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(30, 30, 30));
        
        JLabel waitLabel = new JLabel("Veuillez configurer les paramètres de la simulation puis cliquez sur Démarrer ! ", SwingConstants.CENTER);
        waitLabel.setForeground(Color.GRAY);
        waitLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        centerPanel.add(waitLabel, BorderLayout.CENTER);
        
        contentPane.add(centerPanel, BorderLayout.CENTER);

        // Panel de contrôle (en bas)
        contentPane.add(createControlPanel(), BorderLayout.SOUTH);

        setSize(SimuPara.WINDOW_WIDTH + 250, SimuPara.WINDOW_HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createConfigPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(45, 45, 48));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setPreferredSize(new Dimension(220, 0));

        // Titre
        JLabel title = new JLabel("Paramètres");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(0, 150, 136));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(15));

        // Section Agents
        mainPanel.add(createAgentsSection());
        mainPanel.add(Box.createVerticalStrut(10));

        // Section Objets
        mainPanel.add(createObjetsSection());
        mainPanel.add(Box.createVerticalStrut(10));

        // Section Simulation
        mainPanel.add(createSimulationSection());

        mainPanel.add(Box.createVerticalGlue());

        return mainPanel;
    }

    private JPanel createAgentsSection() {
        JPanel panel = createSection("Agents");
        
        spnReactifs = addSpinnerRow(panel, "Réactifs:", SimuPara.NB_AGENTS_REACTIFS, 0, 20);
        spnCognitifs = addSpinnerRow(panel, "Cognitifs:", SimuPara.NB_AGENTS_COGNITIFS, 0, 20);
        spnCommunicants = addSpinnerRow(panel, "Communicants:", SimuPara.NB_AGENTS_COMMUNICANTS, 0, 8);

        return panel;
    }

    private JPanel createObjetsSection() {
        JPanel panel = createSection("Objets/Zone");
        
        spnTresors = addSpinnerRow(panel, "Trésors:", SimuPara.NB_TRESORS_PAR_ZONE, 0, 10);
        spnAnimaux = addSpinnerRow(panel, "Animaux:", SimuPara.NB_ANIMAUX_PAR_ZONE, 0, 10);
        spnObstacles = addSpinnerRow(panel, "Obstacles:", SimuPara.NB_OBSTACLES_PAR_ZONE, 0, 20);
        spnDegatsMax = addSpinnerRow(panel, "Dégâts max:", SimuPara.MAX_DEGATS_ANIMAUX, 1, 50);

        return panel;
    }

    private JPanel createSimulationSection() {
        JPanel panel = createSection("Timing");
        
        spnDelayMs = addSpinnerRow(panel, "Délai (ms):", (int) SimuPara.DELAY_MS, 10, 500);
        spnDuration = addSpinnerRow(panel, "Durée (sec):", SimuPara.SIMULATION_DURATION / 1000, 10, 300);

        return panel;
    }

    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2, 5, 5));
        panel.setBackground(new Color(60, 60, 63));
        panel.setMaximumSize(new Dimension(220, 150));
        
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 150, 136), 1),
            title
        );
        border.setTitleColor(new Color(0, 150, 136));
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.setBorder(border);
        
        return panel;
    }

    private JSpinner addSpinnerRow(JPanel panel, String labelText, int value, int min, int max) {
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(label);

        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.setFont(new Font("Segoe UI", Font.BOLD, 11));
        
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(new Color(80, 80, 83));
            tf.setForeground(Color.WHITE);
            tf.setCaretColor(Color.WHITE);
        }
        
        panel.add(spinner);
        return spinner;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panel.setBackground(new Color(45, 45, 48));

        btnStart = createButton("Démarrer sim", new Color(0, 150, 136));
        btnStop = createButton("Arrêter Sim", new Color(200, 80, 80));
        btnReset = createButton("refaire Nouvelle Simulation", new Color(100, 100, 100));
        
        btnStop.setEnabled(false);
        btnReset.setEnabled(false);

        btnStart.addActionListener(e -> demarrerSimulation());
        btnStop.addActionListener(e -> arreterSimulation());
        btnReset.addActionListener(e -> resetSimulation());

        panel.add(btnStart);
        panel.add(btnStop);
        panel.add(btnReset);

        return panel;
    }

    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        //ce font a l'air bien on le garde pour l'instant :)
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void applyParameters() {
        SimuPara.NB_AGENTS_REACTIFS = (Integer) spnReactifs.getValue();
        SimuPara.NB_AGENTS_COGNITIFS = (Integer) spnCognitifs.getValue();
        SimuPara.NB_AGENTS_COMMUNICANTS = (Integer) spnCommunicants.getValue();
        SimuPara.NB_TRESORS_PAR_ZONE = (Integer) spnTresors.getValue();
        SimuPara.NB_ANIMAUX_PAR_ZONE = (Integer) spnAnimaux.getValue();
        SimuPara.NB_OBSTACLES_PAR_ZONE = (Integer) spnObstacles.getValue();
        SimuPara.MAX_DEGATS_ANIMAUX = (Integer) spnDegatsMax.getValue();
        SimuPara.DELAY_MS = (Integer) spnDelayMs.getValue();
        SimuPara.SIMULATION_DURATION = (Integer) spnDuration.getValue() * 1000;
    }

    private void setSpinnersEnabled(boolean enabled) {
        spnReactifs.setEnabled(enabled);
        spnCognitifs.setEnabled(enabled);
        spnCommunicants.setEnabled(enabled);
        spnTresors.setEnabled(enabled);
        spnAnimaux.setEnabled(enabled);
        spnObstacles.setEnabled(enabled);
        spnDegatsMax.setEnabled(enabled);
        spnDelayMs.setEnabled(enabled);
        spnDuration.setEnabled(enabled);
    }

    private void demarrerSimulation() {
        applyParameters();
        
        // Désactiver les spinners pendant la simulation
        setSpinnersEnabled(false);

        simulation = new Simulation();
        dashboard = new Dashboard(simulation);
        infoPanel = new InfoPanel(simulation);

        Container contentPane = getContentPane();
        
        BorderLayout layout = (BorderLayout) contentPane.getLayout();
        Component center = layout.getLayoutComponent(BorderLayout.CENTER);
        if (center != null) {
            contentPane.remove(center);
        }
        Component east = layout.getLayoutComponent(BorderLayout.EAST);
        if (east != null) {
            contentPane.remove(east);
        }

        // Ajouter le dashboard et l'info panel
        contentPane.add(dashboard, BorderLayout.CENTER);
        contentPane.add(infoPanel, BorderLayout.EAST);

        revalidate();
        repaint();

        running = true;
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        btnReset.setEnabled(false);
        
        simulation.demarrer();
        new Thread(this).start();
    }

    private void arreterSimulation() {
        running = false;
        simulation.arreter();
        btnStart.setEnabled(false);
        btnStop.setEnabled(false);
        btnReset.setEnabled(true);
    }

    private void resetSimulation() {
        setSpinnersEnabled(true);
        Container contentPane = getContentPane();
        BorderLayout layout = (BorderLayout) contentPane.getLayout();
        Component center = layout.getLayoutComponent(BorderLayout.CENTER);
        if (center != null) {
            contentPane.remove(center);
        }
        Component east = layout.getLayoutComponent(BorderLayout.EAST);
        if (east != null) {
            contentPane.remove(east);
        }

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(30, 30, 30));
        JLabel waitLabel = new JLabel("Configurez les paramètres puis cliquez sur Démarrer", SwingConstants.CENTER);
        waitLabel.setForeground(Color.GRAY);
        waitLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        centerPanel.add(waitLabel, BorderLayout.CENTER);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        revalidate();
        repaint();

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnReset.setEnabled(false);

        simulation = null;
        dashboard = null;
        infoPanel = null;
    }

    @Override
    public void run() {
        while (running) {
            if (dashboard != null) {
                dashboard.repaint();
            }
            if (infoPanel != null) {
                infoPanel.mettreAJour();
            }
            
            try {
                Thread.sleep(50); //équivalent 20 fps
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}