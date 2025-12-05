package sma.objets;

import java.awt.Color;
import java.util.Random;
import sma.environnement.Zone;

/**
 * Représente un animal sauvage qui peut attaquer les explorateurs
 * RÈGLES:
 * - Marche au hasard dans sa zone
 * - Si détecte un agent (range 5), le chasse
 * - Si rencontre un trésor, reste autour
 * - Ne peut pas sortir de sa zone assignée
 * Thread-safe
 */
public class Animal extends ObjetEnvironnement {
    
    public enum TypeAnimal {
        LOUP("🐺", 15, new Color(128, 128, 128), 4),      // Dégâts réduits
        OURS("🐻", 25, new Color(139, 69, 19), 3),        // Dégâts réduits
        CROCODILE("🐊", 20, new Color(0, 100, 0), 3);     // Dégâts réduits

        private final String emoji;
        private final int forceBase;
        private final Color couleur;
        private final int vitesse;

        TypeAnimal(String emoji, int forceBase, Color couleur, int vitesse) {
            this.emoji = emoji;
            this.forceBase = forceBase;
            this.couleur = couleur;
            this.vitesse = vitesse;
        }

        public String getEmoji() { return emoji; }
        public int getForceBase() { return forceBase; }
        public Color getCouleur() { return couleur; }
        public int getVitesse() { return vitesse; }
    }

    private final TypeAnimal typeAnimal;
    private final int force;
    private volatile int pointsDeVie;
    private final int pointsDeVieMax;
    private volatile boolean agressif;
    private Zone zoneAssignee;
    private Position cibleActuelle;
    private final Random random;
    
    // Range de détection des agents (en cases, sera multiplié par 10 pour les pixels)
    public static final int RANGE_DETECTION = 5;

    public Animal(int id, Position position, TypeAnimal typeAnimal) {
        super(id, position);
        this.typeAnimal = typeAnimal;
        this.force = typeAnimal.getForceBase();
        this.pointsDeVieMax = force * 2;
        this.pointsDeVie = this.pointsDeVieMax;
        this.agressif = true;
        this.random = new Random();
        this.cibleActuelle = null;
    }
    
    /**
     * Définit la zone dans laquelle l'animal est confiné
     */
    public void setZoneAssignee(Zone zone) {
        this.zoneAssignee = zone;
    }
    
    public Zone getZoneAssignee() {
        return zoneAssignee;
    }
    
    /**
     * L'animal détecte un agent à portée
     */
    public boolean detecteAgent(Position posAgent) {
        if (!actif || !agressif) return false;
        double distance = position.distanceTo(posAgent);
        return distance <= RANGE_DETECTION * 10; // Conversion en pixels
    }
    
    /**
     * Définit la cible actuelle (agent à chasser)
     */
    public void setCible(Position cible) {
        this.cibleActuelle = cible;
    }
    
    /**
     * Déplace l'animal selon son comportement simplifié:
     * 1. Si un agent est détecté, le chasser
     * 2. Sinon, se déplacer aléatoirement dans la zone
     */
    public void seDeplacer() {
        if (!actif || zoneAssignee == null) return;
        
        int vitesse = typeAnimal.getVitesse();
        Position nouvellePos = null;
        
        if (cibleActuelle != null) {
            // Chasser la cible (agent détecté)
            int dx = Integer.compare(cibleActuelle.getX(), position.getX());
            int dy = Integer.compare(cibleActuelle.getY(), position.getY());
            nouvellePos = new Position(position.getX() + dx * vitesse, position.getY() + dy * vitesse);
        } else {
            // Déplacement aléatoire dans la zone
            int dx = random.nextInt(3) - 1;
            int dy = random.nextInt(3) - 1;
            nouvellePos = new Position(position.getX() + dx * vitesse, position.getY() + dy * vitesse);
        }
        
        // Vérifier que la nouvelle position est dans la zone
        if (nouvellePos != null && zoneAssignee.contientPosition(nouvellePos)) {
            this.position = nouvellePos;
        }
    }

    public synchronized int attaquer() {
        if (!actif || !agressif) return 0;
        return force;
    }

    public synchronized void recevoirDegats(int degats) {
        this.pointsDeVie -= degats;
        if (this.pointsDeVie <= 0) {
            this.actif = false;
            this.pointsDeVie = 0;
        }
    }
    
    /**
     * Tue instantanément l'animal (par un fusil)
     */
    public synchronized void tuer() {
        this.pointsDeVie = 0;
        this.actif = false;
        System.out.println("💀 " + typeAnimal.name() + " a été tué!");
    }

    public synchronized boolean peutAttaquer(Position posAgent) {
        return this.actif && this.agressif && 
               this.position.distanceTo(posAgent) <= 15; // Distance d'attaque en pixels
    }

    // Getters thread-safe
    public TypeAnimal getTypeAnimal() { return typeAnimal; }
    public int getForce() { return force; }
    public synchronized int getPointsDeVie() { return pointsDeVie; }
    public int getPointsDeVieMax() { return pointsDeVieMax; }
    public synchronized boolean isAgressif() { return agressif; }
    public synchronized void setAgressif(boolean agressif) { this.agressif = agressif; }

    @Override
    public String getType() {
        return "Animal";
    }

    @Override
    public Color getCouleur() {
        return typeAnimal.getCouleur();
    }

    @Override
    public String toString() {
        return typeAnimal.getEmoji() + " " + typeAnimal.name() + " #" + id + 
               " (force: " + force + ", PV: " + pointsDeVie + "/" + pointsDeVieMax + ") à " + position;
    }
}
