package sma;

import java.awt.*;
import javax.swing.*;
import sma.gui.MainFrame;

/**
 * Classe principale de l'application
 * Système Multi-Agents - Chasse au Trésor
 * 
 * CY Cergy Paris Université - Master IISC 2 Pro
 * Mini Projet : Équipe Hybride d'Explorateurs
 * 
 * @author Équipe SMA
 * @version 1.0
 */
public class App {
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║       🗺️ SYSTÈME MULTI-AGENTS - CHASSE AU TRÉSOR 🗺️          ║");
        System.out.println("║                                                               ║");
        System.out.println("║     CY Cergy Paris Université - Master IISC 2 Pro             ║");
        System.out.println("║         Mini Projet : Équipe Hybride d'Explorateurs           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Configuration du Look and Feel
        try {
            // Utiliser le look and feel du système
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Configuration des couleurs pour les composants Swing
            UIManager.put("Panel.background", new Color(45, 45, 45));
            UIManager.put("OptionPane.background", new Color(45, 45, 45));
            UIManager.put("OptionPane.messageForeground", Color.WHITE);
            
        } catch (Exception e) {
            System.err.println("Impossible de définir le Look and Feel: " + e.getMessage());
        }
        
        // Lancer l'interface graphique sur l'EDT
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                System.out.println("🚀 Interface graphique lancée avec succès!");
            } catch (Exception e) {
                System.err.println("❌ Erreur lors du lancement de l'interface: " + e.getMessage());
                e.printStackTrace();
                
                // Afficher un message d'erreur
                JOptionPane.showMessageDialog(null, 
                    "Erreur lors du lancement de l'application:\n" + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
