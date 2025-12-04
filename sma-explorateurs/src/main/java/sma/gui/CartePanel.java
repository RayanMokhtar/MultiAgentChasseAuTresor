package sma.gui;

import sma.environnement.*;
import sma.agents.*;
import sma.objets.*;
import sma.simulation.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;

/**
 * Panel principal d'affichage de la carte et des agents
 */
public class CartePanel extends JPanel {
    
    private final Simulation simulation;
    private final Carte carte;
    
    // Couleurs
    private static final Color COULEUR_QG = new Color(144, 238, 144);
    private static final Color COULEUR_GRILLE = new Color(100, 100, 100, 100);
    private static final Color COULEUR_TRESOR = new Color(255, 215, 0);
    private static final Color COULEUR_FOND = new Color(245, 245, 220);
    
    public CartePanel(Simulation simulation) {
        this.simulation = simulation;
        this.carte = simulation.getCarte();
        
        setPreferredSize(new Dimension(carte.getLargeur(), carte.getHauteur()));
        setBackground(COULEUR_FOND);
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Activer l'antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Dessiner les zones
        dessinerZones(g2d);
        
        // Dessiner les objets
        dessinerObjets(g2d);
        
        // Dessiner les agents
        dessinerAgents(g2d);
        
        // Dessiner la légende
        dessinerLegende(g2d);
    }

    private void dessinerZones(Graphics2D g2d) {
        for (Zone zone : carte.getZones()) {
            int x = zone.getPositionDebut().getX();
            int y = zone.getPositionDebut().getY();
            int w = carte.getTailleZoneX();
            int h = carte.getTailleZoneY();
            
            // Fond de la zone
            if (zone.estQG()) {
                g2d.setColor(COULEUR_QG);
            } else if (zone.isExploree()) {
                g2d.setColor(new Color(230, 255, 230));
            } else {
                g2d.setColor(zone.getCouleur());
            }
            g2d.fillRect(x, y, w, h);
            
            // Bordure
            g2d.setColor(COULEUR_GRILLE);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(x, y, w, h);
            
            // Numéro de zone
            g2d.setColor(new Color(100, 100, 100));
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String label = zone.estQG() ? "QG" : String.valueOf(zone.getId());
            g2d.drawString(label, x + 5, y + 12);
            
            // Indicateur de danger
            if (zone.getNiveauDanger() > 0 && !zone.estQG()) {
                g2d.setColor(new Color(255, 0, 0, 50));
                int dangerAlpha = Math.min(150, zone.getNiveauDanger() * 2);
                g2d.setColor(new Color(255, 0, 0, dangerAlpha));
                g2d.fillRect(x + 1, y + 1, w - 2, h - 2);
            }
        }
    }

    private void dessinerObjets(Graphics2D g2d) {
        for (Zone zone : carte.getZones()) {
            // Trésors
            for (Tresor tresor : zone.getTresorsNonCollectes()) {
                dessinerTresor(g2d, tresor);
            }
            
            // Animaux
            for (Animal animal : zone.getAnimaux()) {
                if (animal.isActif()) {
                    dessinerAnimal(g2d, animal);
                }
            }
            
            // Obstacles
            for (Obstacle obstacle : zone.getObstacles()) {
                dessinerObstacle(g2d, obstacle);
            }
        }
    }

    private void dessinerTresor(Graphics2D g2d, Tresor tresor) {
        Position pos = tresor.getPosition();
        int x = pos.getX();
        int y = pos.getY();
        int taille = 16;
        
        // Effet de brillance
        g2d.setColor(new Color(255, 255, 200, 100));
        g2d.fillOval(x - taille, y - taille, taille * 2, taille * 2);
        
        // Coffre au trésor
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillRoundRect(x - 8, y - 6, 16, 12, 3, 3);
        g2d.setColor(COULEUR_TRESOR);
        g2d.fillRoundRect(x - 6, y - 4, 12, 8, 2, 2);
        
        // Serrure
        g2d.setColor(new Color(139, 69, 19));
        g2d.fillOval(x - 2, y - 2, 4, 4);
        
        // Valeur
        g2d.setFont(new Font("Arial", Font.BOLD, 8));
        g2d.setColor(Color.BLACK);
        String valeur = String.valueOf(tresor.getValeur());
        g2d.drawString(valeur, x - g2d.getFontMetrics().stringWidth(valeur) / 2, y + 18);
    }

    private void dessinerAnimal(Graphics2D g2d, Animal animal) {
        Position pos = animal.getPosition();
        int x = pos.getX();
        int y = pos.getY();
        
        // Corps de l'animal
        g2d.setColor(animal.getCouleur());
        g2d.fillOval(x - 10, y - 10, 20, 20);
        
        // Bordure
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(x - 10, y - 10, 20, 20);
        
        // Emoji/Type
        g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        g2d.setColor(Color.WHITE);
        String emoji = animal.getTypeAnimal().getEmoji();
        // Fallback si emoji pas supporté
        if (!canDisplayEmoji(g2d, emoji)) {
            emoji = animal.getTypeAnimal().name().substring(0, 1);
        }
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(emoji, x - fm.stringWidth(emoji) / 2, y + 5);
        
        // Barre de vie
        int barreW = 20;
        int barreH = 3;
        int barreX = x - barreW / 2;
        int barreY = y - 15;
        float ratio = (float) animal.getPointsDeVie() / animal.getPointsDeVieMax();
        
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(barreX, barreY, barreW, barreH);
        
        Color couleurVie = ratio > 0.5 ? Color.GREEN : (ratio > 0.25 ? Color.ORANGE : Color.RED);
        g2d.setColor(couleurVie);
        g2d.fillRect(barreX, barreY, (int) (barreW * ratio), barreH);
    }

    private void dessinerObstacle(Graphics2D g2d, Obstacle obstacle) {
        Position pos = obstacle.getPosition();
        int x = pos.getX();
        int y = pos.getY();
        
        g2d.setColor(obstacle.getCouleur());
        
        switch (obstacle.getTypeObstacle()) {
            case ROCHER:
                // Rocher polygonal
                int[] xPoints = {x - 8, x, x + 8, x + 5, x - 5};
                int[] yPoints = {y + 5, y - 8, y + 5, y + 8, y + 8};
                g2d.fillPolygon(xPoints, yPoints, 5);
                break;
                
            case ARBRE:
                // Tronc
                g2d.setColor(new Color(139, 69, 19));
                g2d.fillRect(x - 3, y, 6, 10);
                // Feuillage
                g2d.setColor(obstacle.getCouleur());
                g2d.fillOval(x - 10, y - 15, 20, 18);
                break;
                
            case RIVIERE:
                // Vagues
                g2d.fillRoundRect(x - 12, y - 4, 24, 8, 4, 4);
                g2d.setColor(new Color(255, 255, 255, 100));
                g2d.drawLine(x - 8, y, x - 4, y - 2);
                g2d.drawLine(x + 2, y, x + 6, y - 2);
                break;
                
            case MUR:
                // Briques
                g2d.fillRect(x - 10, y - 8, 20, 16);
                g2d.setColor(new Color(100, 20, 20));
                g2d.drawRect(x - 10, y - 8, 20, 16);
                g2d.drawLine(x, y - 8, x, y + 8);
                g2d.drawLine(x - 10, y, x + 10, y);
                break;
        }
    }

    private void dessinerAgents(Graphics2D g2d) {
        for (Agent agent : simulation.getAgents()) {
            if (!agent.isEnVie()) continue;
            
            Position pos = agent.getPosition();
            int x = pos.getX();
            int y = pos.getY();
            
            // Cercle de l'agent
            g2d.setColor(agent.getCouleur());
            g2d.fillOval(x - 12, y - 12, 24, 24);
            
            // Bordure
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(x - 12, y - 12, 24, 24);
            
            // Icône selon le type
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String icone;
            if (agent instanceof AgentCognitif) {
                icone = "C";
            } else if (agent instanceof AgentReactif) {
                icone = "R";
            } else {
                icone = "📡";
            }
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(icone, x - fm.stringWidth(icone) / 2, y + 4);
            
            // Nom de l'agent
            g2d.setFont(new Font("Arial", Font.PLAIN, 9));
            g2d.setColor(Color.BLACK);
            String nom = agent.getNom();
            g2d.drawString(nom, x - fm.stringWidth(nom) / 2 + 5, y + 22);
            
            // Barre de vie
            dessinerBarreVie(g2d, x, y - 18, agent.getPointsDeVie(), agent.getPointsDeVieMax(), 20);
            
            // Indicateur de danger
            if (agent.estEnDanger()) {
                g2d.setColor(new Color(255, 0, 0, 150));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(x - 15, y - 15, 30, 30);
            }
            
            // Mission actuelle pour agent cognitif
            if (agent instanceof AgentCognitif) {
                AgentCognitif ac = (AgentCognitif) agent;
                g2d.setFont(new Font("Arial", Font.ITALIC, 8));
                g2d.setColor(new Color(0, 0, 150));
                g2d.drawString(ac.getMissionActuelle().name(), x - 20, y + 32);
            }
        }
    }

    private void dessinerBarreVie(Graphics2D g2d, int x, int y, int vie, int vieMax, int largeur) {
        int hauteur = 4;
        float ratio = (float) vie / vieMax;
        
        // Fond
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x - largeur / 2, y, largeur, hauteur);
        
        // Vie
        Color couleur = ratio > 0.6 ? new Color(0, 200, 0) : 
                        (ratio > 0.3 ? new Color(255, 165, 0) : Color.RED);
        g2d.setColor(couleur);
        g2d.fillRect(x - largeur / 2, y, (int) (largeur * ratio), hauteur);
        
        // Bordure
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x - largeur / 2, y, largeur, hauteur);
    }

    private void dessinerLegende(Graphics2D g2d) {
        int x = 10;
        int y = carte.getHauteur() - 80;
        int espacement = 20;
        
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.fillRoundRect(x - 5, y - 15, 150, 75, 10, 10);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawRoundRect(x - 5, y - 15, 150, 75, 10, 10);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.setColor(Color.BLACK);
        g2d.drawString("LÉGENDE", x, y);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        y += espacement;
        
        // Agent Cognitif
        g2d.setColor(new Color(0, 100, 255));
        g2d.fillOval(x, y - 8, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Agent Cognitif", x + 15, y);
        
        y += espacement;
        // Agent Réactif
        g2d.setColor(new Color(255, 100, 0));
        g2d.fillOval(x, y - 8, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Agent Réactif", x + 15, y);
        
        y += espacement;
        // Trésor
        g2d.setColor(COULEUR_TRESOR);
        g2d.fillRect(x, y - 8, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Trésor", x + 15, y);
    }

    private boolean canDisplayEmoji(Graphics2D g2d, String emoji) {
        return g2d.getFont().canDisplayUpTo(emoji) == -1;
    }

    public void rafraichir() {
        repaint();
    }
}
