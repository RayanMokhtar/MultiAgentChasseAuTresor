package sma.objets;

import java.awt.Color;

/**
 * Représente un animal sauvage qui peut attaquer les explorateurs
 * Thread-safe
 */
public class Animal extends ObjetEnvironnement {
    
    public enum TypeAnimal {
        LOUP("🐺", 25, new Color(128, 128, 128)),
        OURS("🐻", 40, new Color(139, 69, 19)),
        SERPENT("🐍", 15, new Color(0, 100, 0)),
        TIGRE("🐅", 35, new Color(255, 140, 0));

        private final String emoji;
        private final int forceBase;
        private final Color couleur;

        TypeAnimal(String emoji, int forceBase, Color couleur) {
            this.emoji = emoji;
            this.forceBase = forceBase;
            this.couleur = couleur;
        }

        public String getEmoji() { return emoji; }
        public int getForceBase() { return forceBase; }
        public Color getCouleur() { return couleur; }
    }

    private final TypeAnimal typeAnimal;
    private final int force;
    private volatile int pointsDeVie;
    private final int pointsDeVieMax;
    private volatile boolean agressif;

    public Animal(int id, Position position, TypeAnimal typeAnimal) {
        super(id, position);
        this.typeAnimal = typeAnimal;
        this.force = typeAnimal.getForceBase();
        this.pointsDeVieMax = force * 2;
        this.pointsDeVie = this.pointsDeVieMax;
        this.agressif = true;
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

    public synchronized boolean peutAttaquer(Position posAgent) {
        return this.actif && this.agressif && 
               this.position.distanceTo(posAgent) <= 1.5;
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
