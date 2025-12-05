package sma.gui;

import sma.environnement.*;
import sma.agents.*;
import sma.objets.*;
import sma.simulation.*;

import javax.swing.*;
import java.awt.*;

/**
 * Panel d'affichage de la carte - Version améliorée
 */
public class CartePanel extends JPanel {
    
    private final Simulation simulation;
    private final Carte carte;
    
    // Couleurs agréables
    private static final Color COULEUR_QG = new Color(152, 251, 152); // Vert pâle
    private static final Color COULEUR_ZONE_PAIRE = new Color(255, 253, 240);
    private static final Color COULEUR_ZONE_IMPAIRE = new Color(245, 245, 235);
    private static final Color COULEUR_GRILLE_ZONE = new Color(120, 120, 120);
    private static final Color COULEUR_GRILLE_CASE = new Color(220, 220, 220);
    
    public CartePanel(Simulation simulation) {
        this.simulation = simulation;
        this.carte = simulation.getCarte();
        
        setPreferredSize(new Dimension(carte.getLargeur() + 1, carte.getHauteur() + 1));
        setBackground(Color.WHITE);
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        dessinerZones(g2d);
        dessinerGrilleCases(g2d);
        dessinerObjets(g2d);
        dessinerAgents(g2d);
        dessinerLegende(g2d);
    }

    private void dessinerZones(Graphics2D g2d) {
        int index = 0;
        for (Zone zone : carte.getZones()) {
            int x = zone.getPositionDebut().getX();
            int y = zone.getPositionDebut().getY();
            int w = carte.getTailleZoneX();
            int h = carte.getTailleZoneY();
            
            // Fond de zone avec dégradé subtil
            if (zone.estQG()) {
                GradientPaint gp = new GradientPaint(x, y, COULEUR_QG, x + w, y + h, new Color(120, 220, 120));
                g2d.setPaint(gp);
            } else {
                Color base = (index % 2 == 0) ? COULEUR_ZONE_PAIRE : COULEUR_ZONE_IMPAIRE;
                g2d.setColor(base);
            }
            g2d.fillRect(x, y, w, h);
            
            // Bordure de zone
            g2d.setColor(COULEUR_GRILLE_ZONE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(x, y, w, h);
            
            // Label de zone avec fond
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            String label = zone.estQG() ? "QG" : "Zone " + zone.getId();
            int labelWidth = g2d.getFontMetrics().stringWidth(label) + 8;
            
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.fillRoundRect(x + 3, y + 3, labelWidth, 16, 5, 5);
            g2d.setColor(zone.estQG() ? new Color(0, 100, 0) : Color.DARK_GRAY);
            g2d.drawString(label, x + 7, y + 15);
            
            index++;
        }
    }

    private void dessinerGrilleCases(Graphics2D g2d) {
        g2d.setColor(COULEUR_GRILLE_CASE);
        g2d.setStroke(new BasicStroke(0.5f));
        
        int tailleCase = Carte.TAILLE_CASE;
        
        for (int x = tailleCase; x < carte.getLargeur(); x += tailleCase) {
            g2d.drawLine(x, 0, x, carte.getHauteur());
        }
        for (int y = tailleCase; y < carte.getHauteur(); y += tailleCase) {
            g2d.drawLine(0, y, carte.getLargeur(), y);
        }
    }

    private void dessinerObjets(Graphics2D g2d) {
        int taille = Carte.TAILLE_CASE - 6;
        
        for (Zone zone : carte.getZones()) {
            // Trésors - Étoiles dorées
            for (Tresor tresor : zone.getTresorsNonCollectes()) {
                Position pos = tresor.getPosition();
                dessinerEtoile(g2d, pos.getX(), pos.getY(), taille/2, new Color(255, 215, 0), new Color(255, 180, 0));
            }
            
            // Fusils
            for (Fusil fusil : zone.getFusils()) {
                if (!fusil.isRamasse()) {
                    Position pos = fusil.getPosition();
                    dessinerFusil(g2d, pos.getX(), pos.getY());
                }
            }
            
            // Animaux
            for (Animal animal : zone.getAnimaux()) {
                if (animal.isActif()) {
                    Position pos = animal.getPosition();
                    dessinerAnimal(g2d, pos.getX(), pos.getY(), animal);
                }
            }
            
            // Obstacles
            for (Obstacle obstacle : zone.getObstacles()) {
                Position pos = obstacle.getPosition();
                dessinerObstacle(g2d, pos.getX(), pos.getY(), obstacle);
            }
        }
    }

    private void dessinerEtoile(Graphics2D g2d, int cx, int cy, int rayon, Color couleur1, Color couleur2) {
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            int r = (i % 2 == 0) ? rayon : rayon / 2;
            xPoints[i] = cx + (int)(r * Math.cos(angle));
            yPoints[i] = cy - (int)(r * Math.sin(angle));
        }
        
        GradientPaint gp = new GradientPaint(cx - rayon, cy - rayon, couleur1, cx + rayon, cy + rayon, couleur2);
        g2d.setPaint(gp);
        g2d.fillPolygon(xPoints, yPoints, 10);
        
        g2d.setColor(new Color(180, 130, 0));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawPolygon(xPoints, yPoints, 10);
    }

    private void dessinerFusil(Graphics2D g2d, int x, int y) {
        g2d.setColor(new Color(101, 67, 33));
        g2d.fillRoundRect(x - 8, y - 2, 16, 4, 2, 2);
        g2d.setColor(new Color(60, 60, 60));
        g2d.fillRect(x + 4, y - 4, 4, 8);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(x - 8, y - 2, 16, 4, 2, 2);
    }

    private void dessinerAnimal(Graphics2D g2d, int x, int y, Animal animal) {
        Color couleur = animal.getCouleur();
        int taille = 14;
        
        // Corps - cercle avec bordure
        g2d.setColor(couleur);
        g2d.fillOval(x - taille/2, y - taille/2, taille, taille);
        g2d.setColor(couleur.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x - taille/2, y - taille/2, taille, taille);
        
        // Lettre de l'animal
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 9));
        String lettre = animal.getTypeAnimal().name().substring(0, 1);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(lettre, x - fm.stringWidth(lettre)/2, y + 3);
        
        // Indicateur de danger (triangle d'alerte)
        g2d.setColor(Color.RED);
        int[] xTri = {x, x - 4, x + 4};
        int[] yTri = {y - taille/2 - 6, y - taille/2 - 1, y - taille/2 - 1};
        g2d.fillPolygon(xTri, yTri, 3);
    }

    private void dessinerObstacle(Graphics2D g2d, int x, int y, Obstacle obstacle) {
        int taille = 12;
        g2d.setColor(obstacle.getCouleur());
        g2d.fillRoundRect(x - taille/2, y - taille/2, taille, taille, 3, 3);
        g2d.setColor(obstacle.getCouleur().darker());
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(x - taille/2, y - taille/2, taille, taille, 3, 3);
    }

    private void dessinerAgents(Graphics2D g2d) {
        int taille = Carte.TAILLE_CASE - 4;
        
        for (Agent agent : simulation.getAgents()) {
            // Dessiner les agents vivants ET les agents blessés (qui attendent du secours)
            if (!agent.isEnVie() && !agent.isBlesse()) continue;
            
            Position pos = agent.getPosition();
            int x = pos.getX();
            int y = pos.getY();
            
            // Ombre
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.fillOval(x - taille/2 + 2, y - taille/2 + 2, taille, taille);
            
            // Corps de l'agent
            Color couleur = agent.getCouleur();
            if (agent.isBlesse()) {
                couleur = new Color(150, 150, 150);
            }
            
            // Dégradé
            GradientPaint gp = new GradientPaint(
                x - taille/2, y - taille/2, couleur.brighter(),
                x + taille/2, y + taille/2, couleur.darker()
            );
            g2d.setPaint(gp);
            g2d.fillOval(x - taille/2, y - taille/2, taille, taille);
            
            // Bordure
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x - taille/2, y - taille/2, taille, taille);
            
            // Lettre du type
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            String lettre;
            if (agent instanceof AgentCognitif) {
                lettre = "C";
            } else if (agent instanceof AgentReactif) {
                lettre = "R";
            } else {
                lettre = "S";
            }
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(lettre, x - fm.stringWidth(lettre)/2, y + 4);
            
            // Indicateurs d'état
            if (agent.isBlesse()) {
                // Croix rouge
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(x + taille/2 - 2, y - taille/2 - 5, x + taille/2 + 6, y - taille/2 + 3);
                g2d.drawLine(x + taille/2 + 6, y - taille/2 - 5, x + taille/2 - 2, y - taille/2 + 3);
            }
            
            if (agent.aUnFusil()) {
                // Petit symbole fusil
                g2d.setColor(new Color(101, 67, 33));
                g2d.fillRect(x + taille/2 - 2, y + 2, 8, 3);
            }
        }
    }

    private void dessinerLegende(Graphics2D g2d) {
        int x = 8;
        int y = carte.getHauteur() - 75;
        
        // Fond semi-transparent
        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.fillRoundRect(x - 4, y - 12, 130, 70, 8, 8);
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(x - 4, y - 12, 130, 70, 8, 8);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 9));
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString("LÉGENDE", x, y);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        y += 14;
        
        // Cognitif
        g2d.setColor(new Color(0, 100, 255));
        g2d.fillOval(x, y - 8, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("C = Cognitif", x + 14, y);
        
        y += 12;
        // Réactif
        g2d.setColor(new Color(255, 100, 0));
        g2d.fillOval(x, y - 8, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("R = Réactif", x + 14, y);
        
        y += 12;
        // Sentinelle
        g2d.setColor(new Color(128, 0, 128));
        g2d.fillOval(x, y - 8, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("S = Sentinelle", x + 14, y);
        
        y += 12;
        // Trésor
        dessinerEtoile(g2d, x + 5, y - 3, 5, new Color(255, 215, 0), new Color(255, 180, 0));
        g2d.setColor(Color.BLACK);
        g2d.drawString("= Trésor", x + 14, y);
    }

    public void rafraichir() {
        repaint();
    }
}
