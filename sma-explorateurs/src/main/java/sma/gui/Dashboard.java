package sma.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import sma.agents.Agent;
import sma.environnement.Carte;
import sma.environnement.Case;
import sma.environnement.Zone;
import sma.objets.Animal;
import sma.objets.ObjetPassif;
import sma.objets.Obstacle;
import sma.objets.Tresor;
import sma.simulation.Simulation;

public class Dashboard extends JPanel {

    private static final long serialVersionUID = 1L;
    private final Simulation simulation;

    // Couleurs
    private static final Color COLOR_EMPTY = new Color(240, 240, 240);
    private static final Color COLOR_TRESOR = new Color(255, 215, 0); // Or
    private static final Color COLOR_ANIMAL = new Color(255, 100, 100); // Rouge
    private static final Color COLOR_OBSTACLE = new Color(100, 100, 100); // Gris
    private static final Color COLOR_QG = new Color(100, 200, 100); // Vert
    private static final Color COLOR_AGENT_REACTIF = Color.BLUE;
    private static final Color COLOR_AGENT_COGNITIF = Color.MAGENTA;
    private static final Color COLOR_AGENT_COMMUNICANT = Color.CYAN;

    public Dashboard(Simulation simulation) {
        this.simulation = simulation;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        dessinerCarte(g2);
        dessinerAgents(g2);
        dessinerLegende(g2);
    }

    private void dessinerCarte(Graphics2D g2) {
        Carte carte = simulation.getCarte();
        int startX = 50;
        int startY = 50;

        for (int zx = 0; zx < Carte.NB_ZONES_COTE; zx++) {
            for (int zy = 0; zy < Carte.NB_ZONES_COTE; zy++) {
                Zone zone = carte.getZone(zx, zy);
                int zonePixelX = startX + zx * (Zone.TAILLE * SimuPara.CASE_SIZE + SimuPara.ZONE_MARGIN);
                int zonePixelY = startY + zy * (Zone.TAILLE * SimuPara.CASE_SIZE + SimuPara.ZONE_MARGIN);

                // Dessiner chaque case de la zone
                for (int cx = 0; cx < Zone.TAILLE; cx++) {
                    for (int cy = 0; cy < Zone.TAILLE; cy++) {
                        Case c = zone.getCase(cx, cy);
                        int casePixelX = zonePixelX + cx * SimuPara.CASE_SIZE;
                        int casePixelY = zonePixelY + cy * SimuPara.CASE_SIZE;

                        // Couleur de la case
                        Color couleur = getCouleurCase(c, zx == 0 && zy == 0 && cx == 0 && cy == 0);
                        g2.setColor(couleur);
                        g2.fillRect(casePixelX, casePixelY, SimuPara.CASE_SIZE - 1, SimuPara.CASE_SIZE - 1);

                        // Bordure
                        g2.setColor(Color.LIGHT_GRAY);
                        g2.drawRect(casePixelX, casePixelY, SimuPara.CASE_SIZE - 1, SimuPara.CASE_SIZE - 1);
                    }
                }

                // Bordure de la zone
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(zonePixelX - 1, zonePixelY - 1,
                        Zone.TAILLE * SimuPara.CASE_SIZE + 1, Zone.TAILLE * SimuPara.CASE_SIZE + 1);

                // Numéro de zone
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString("Z" + zone.getId(), zonePixelX + 2, zonePixelY - 3);
            }
        }
    }

    private Color getCouleurCase(Case c, boolean isQG) {
        if (isQG) {
            return COLOR_QG;
        }

        if (c.hasObjet()) {
            ObjetPassif obj = c.getObjet();
            if (obj instanceof Tresor) {
                Tresor t = (Tresor) obj;
                return t.isCollecte() ? COLOR_EMPTY : COLOR_TRESOR;
            } else if (obj instanceof Animal) {
                return COLOR_ANIMAL;
            } else if (obj instanceof Obstacle) {
                return COLOR_OBSTACLE;
            }
        }

        return COLOR_EMPTY;
    }

    private void dessinerAgents(Graphics2D g2) {
        int startX = 50;
        int startY = 50;

        for (Agent agent : simulation.getAgents()) {
            Case c = agent.getCaseActuelle();
            if (c == null) {
                continue;
            }

            Zone zone = c.getZone();
            int zonePixelX = startX + zone.getZoneX() * (Zone.TAILLE * SimuPara.CASE_SIZE + SimuPara.ZONE_MARGIN);
            int zonePixelY = startY + zone.getZoneY() * (Zone.TAILLE * SimuPara.CASE_SIZE + SimuPara.ZONE_MARGIN);

            int agentX = zonePixelX + c.getX() * SimuPara.CASE_SIZE + SimuPara.CASE_SIZE / 2;
            int agentY = zonePixelY + c.getY() * SimuPara.CASE_SIZE + SimuPara.CASE_SIZE / 2;

            // Couleur selon le type
            Color couleur = getCouleurAgent(agent);
            g2.setColor(couleur);

            int size = SimuPara.CASE_SIZE - 2;
            g2.fillOval(agentX - size / 2, agentY - size / 2, size, size);

            // Contour
            g2.setColor(Color.BLACK);
            g2.drawOval(agentX - size / 2, agentY - size / 2, size, size);

            // ID de l'agent
            g2.setFont(new Font("Arial", Font.PLAIN, 8));
            g2.drawString(String.valueOf(agent.getId()), agentX - 2, agentY + 3);
        }
    }

    private Color getCouleurAgent(Agent agent) {
        switch (agent.getType()) {
            case REACTIF:
                return COLOR_AGENT_REACTIF;
            case COGNITIF:
                return COLOR_AGENT_COGNITIF;
            case COMMUNICANT:
                return COLOR_AGENT_COMMUNICANT;
            default:
                return Color.BLACK;
        }
    }

    private void dessinerLegende(Graphics2D g2) {
        // Calculer la position en bas à droite
        int carteWidth = Carte.NB_ZONES_COTE * (Zone.TAILLE * SimuPara.CASE_SIZE + SimuPara.ZONE_MARGIN);
        int carteHeight = carteWidth;
        int startX = 50; // même que dessinerCarte
        int startY = 50;

        // Position légende : à droite de la carte avec marge
        int legendeX = startX + carteWidth + 40;
        int legendeY = startY + carteHeight - 200; // En bas

        // Fond de la légende avec bordure arrondie
        int legendeWidth = 200;
        int legendeHeight = 220;
        g2.setColor(new Color(250, 250, 250));
        g2.fillRoundRect(legendeX - 10, legendeY - 25, legendeWidth, legendeHeight, 15, 15);
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(legendeX - 10, legendeY - 25, legendeWidth, legendeHeight, 15, 15);

        // Titre
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.setColor(Color.BLACK);
        g2.drawString("LÉGENDE", legendeX, legendeY);

        int y = legendeY + 30;
        g2.setFont(new Font("Arial", Font.PLAIN, 13));

        // Cases
        dessinerItemLegende(g2, legendeX, y, COLOR_TRESOR, "Trésor");
        y += 22;
        dessinerItemLegende(g2, legendeX, y, COLOR_ANIMAL, "Animal (danger)");
        y += 22;
        dessinerItemLegende(g2, legendeX, y, COLOR_OBSTACLE, "Obstacle");
        y += 22;
        dessinerItemLegende(g2, legendeX, y, COLOR_QG, "QG (respawn)");
        y += 30;

        // Séparateur
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(legendeX, y - 15, legendeX + 170, y - 15);

        // Agents
        dessinerItemLegende(g2, legendeX, y, COLOR_AGENT_REACTIF, "Agent Réactif");
        y += 22;
        dessinerItemLegende(g2, legendeX, y, COLOR_AGENT_COGNITIF, "Agent Cognitif");
        y += 22;
        dessinerItemLegende(g2, legendeX, y, COLOR_AGENT_COMMUNICANT, "Agent Communicant");
    }

    private void dessinerItemLegende(Graphics2D g2, int x, int y, Color color, String text) {
        // Carré avec effet légèrement arrondi
        g2.setColor(color);
        g2.fillRoundRect(x, y - 14, 16, 16, 4, 4);
        g2.setColor(Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(x, y - 14, 16, 16, 4, 4);
        g2.setColor(Color.BLACK);
        g2.drawString(text, x + 24, y);
    }
}
