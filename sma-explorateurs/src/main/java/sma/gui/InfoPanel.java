package sma.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import sma.agents.Agent;
import sma.simulation.Simulation;



//inspiré du panel du code train ... 
public class InfoPanel extends JPanel {
    
    private static final long serialVersionUID = 1L;
    private final Simulation simulation;
    private final JTextArea statsArea;

    public InfoPanel(Simulation simulation) {
        this.simulation = simulation;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(250, 0));
        
        JLabel titre = new JLabel("STATISTIQUES", SwingConstants.CENTER);
        titre.setFont(new Font("Arial", Font.BOLD, 14));
        titre.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(titre, BorderLayout.NORTH);
        
        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statsArea.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JScrollPane scrollPane = new JScrollPane(statsArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void mettreAJour() {
        StringBuilder sb = new StringBuilder();
        
        int totalTresors = 0;
        int totalMorts = 0;
        
        //afficher le pannel à coté 
        for (Agent agent : simulation.getAgents()) {
            sb.append("Agent ").append(agent.getId())
              .append(" (").append(agent.getType().toString()).append(")\n");
            sb.append("  PV: ").append(agent.getPv()).append("/").append(agent.getPvMax());
            sb.append(agent.isAlive() ? " ✓" : " ✗").append("\n");
            sb.append("  Trésors: ").append(agent.getStats().getTresorsCollectes()).append("\n");
            sb.append("  Valeur: ").append(agent.getStats().getValeurTotale()).append("\n");
            sb.append("  Cases: ").append(agent.getStats().getCasesVisitees()).append("\n");
            sb.append("  Morts: ").append(agent.getStats().getNbrMorts()).append("\n");
            sb.append("\n");
            
            totalTresors += agent.getStats().getTresorsCollectes();
            totalMorts += agent.getStats().getNbrMorts();
        }
        
        sb.append("─────────────────\n");
        sb.append("TOTAL Trésors: ").append(totalTresors).append("\n");
        sb.append("TOTAL Morts: ").append(totalMorts).append("\n");
        
        statsArea.setText(sb.toString());
    }
}