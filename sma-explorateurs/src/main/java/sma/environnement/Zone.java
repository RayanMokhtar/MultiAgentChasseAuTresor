package sma.environnement;

import sma.objets.*;
import sma.agents.Agent;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.awt.Color;

/**
 * Représente une zone de la carte
 * Thread-safe avec CopyOnWriteArrayList pour les listes
 */
public class Zone {
    private final int id;
    private final int ligneGrille;
    private final int colonneGrille;
    private final Position positionDebut;
    private final Position positionFin;
    private final List<ObjetEnvironnement> objets;
    private final List<Agent> agentsPresents;
    private final boolean estQG;
    private volatile boolean exploree;
    private volatile int niveauDanger;
    private final Color couleur;

    public Zone(int id, int ligne, int colonne, Position positionDebut, Position positionFin, boolean estQG) {
        this.id = id;
        this.ligneGrille = ligne;
        this.colonneGrille = colonne;
        this.positionDebut = positionDebut;
        this.positionFin = positionFin;
        this.estQG = estQG;
        this.objets = new CopyOnWriteArrayList<>();
        this.agentsPresents = new CopyOnWriteArrayList<>();
        this.exploree = estQG; // Le QG est toujours exploré
        this.niveauDanger = 0;
        
        // Couleur de zone
        if (estQG) {
            this.couleur = new Color(144, 238, 144); // Vert clair pour le QG
        } else {
            // Alternance de couleurs pour damier
            boolean pair = (ligne + colonne) % 2 == 0;
            this.couleur = pair ? new Color(245, 245, 220) : new Color(222, 184, 135);
        }
    }

    public synchronized void ajouterObjet(ObjetEnvironnement objet) {
        if (!objets.contains(objet)) {
            objets.add(objet);
            recalculerDanger();
        }
    }

    public synchronized void retirerObjet(ObjetEnvironnement objet) {
        objets.remove(objet);
        recalculerDanger();
    }

    public synchronized void ajouterAgent(Agent agent) {
        if (!agentsPresents.contains(agent)) {
            agentsPresents.add(agent);
        }
    }

    public synchronized void retirerAgent(Agent agent) {
        agentsPresents.remove(agent);
    }

    public boolean contientPosition(Position pos) {
        return pos.getX() >= positionDebut.getX() && pos.getX() <= positionFin.getX()
                && pos.getY() >= positionDebut.getY() && pos.getY() <= positionFin.getY();
    }

    public Position getCentre() {
        int centreX = (positionDebut.getX() + positionFin.getX()) / 2;
        int centreY = (positionDebut.getY() + positionFin.getY()) / 2;
        return new Position(centreX, centreY);
    }

    private void recalculerDanger() {
        int danger = 0;
        for (ObjetEnvironnement obj : objets) {
            if (obj instanceof Animal && obj.isActif()) {
                danger += ((Animal) obj).getForce();
            }
        }
        this.niveauDanger = danger;
    }

    // Getters pour les différents types d'objets
    public List<Tresor> getTresors() {
        List<Tresor> tresors = new ArrayList<>();
        for (ObjetEnvironnement obj : objets) {
            if (obj instanceof Tresor && obj.isActif()) {
                tresors.add((Tresor) obj);
            }
        }
        return tresors;
    }

    public List<Tresor> getTresorsNonCollectes() {
        List<Tresor> tresors = new ArrayList<>();
        for (ObjetEnvironnement obj : objets) {
            if (obj instanceof Tresor) {
                Tresor t = (Tresor) obj;
                if (!t.isCollecte()) {
                    tresors.add(t);
                }
            }
        }
        return tresors;
    }

    public List<Animal> getAnimaux() {
        List<Animal> animaux = new ArrayList<>();
        for (ObjetEnvironnement obj : objets) {
            if (obj instanceof Animal && obj.isActif()) {
                animaux.add((Animal) obj);
            }
        }
        return animaux;
    }

    public List<Obstacle> getObstacles() {
        List<Obstacle> obstacles = new ArrayList<>();
        for (ObjetEnvironnement obj : objets) {
            if (obj instanceof Obstacle && obj.isActif()) {
                obstacles.add((Obstacle) obj);
            }
        }
        return obstacles;
    }

    // Getters
    public int getId() { return id; }
    public int getLigneGrille() { return ligneGrille; }
    public int getColonneGrille() { return colonneGrille; }
    public Position getPositionDebut() { return positionDebut; }
    public Position getPositionFin() { return positionFin; }
    public List<ObjetEnvironnement> getObjets() { return new ArrayList<>(objets); }
    public List<Agent> getAgentsPresents() { return new ArrayList<>(agentsPresents); }
    public boolean estQG() { return estQG; }
    public boolean isExploree() { return exploree; }
    public void setExploree(boolean exploree) { this.exploree = exploree; }
    public int getNiveauDanger() { return niveauDanger; }
    public Color getCouleur() { return couleur; }

    @Override
    public String toString() {
        String status = estQG ? "🏠 QG" : (exploree ? "✓" : "?");
        return "Zone " + id + " " + status + " [" + ligneGrille + "," + colonneGrille + "]";
    }
}
