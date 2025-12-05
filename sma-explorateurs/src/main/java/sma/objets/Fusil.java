package sma.objets;

import java.awt.Color;

/**
 * Représente un fusil ramassable par les agents
 * Permet de tuer jusqu'à 2 animaux
 */
public class Fusil extends ObjetEnvironnement {
    
    private volatile int munitions;
    private static final int MUNITIONS_MAX = 2;
    private volatile boolean ramasse;

    public Fusil(int id, Position position) {
        super(id, position);
        this.munitions = MUNITIONS_MAX;
        this.ramasse = false;
    }

    /**
     * Tente de ramasser le fusil
     * @return true si le fusil a été ramassé avec succès
     */
    public synchronized boolean ramasser() {
        if (!ramasse && actif) {
            this.ramasse = true;
            this.actif = false;
            return true;
        }
        return false;
    }

    /**
     * Utilise une munition pour tuer un animal
     * @return true si une balle a été tirée
     */
    public synchronized boolean tirer() {
        if (munitions > 0) {
            munitions--;
            System.out.println("🔫 BANG! Munitions restantes: " + munitions);
            return true;
        }
        return false;
    }

    public synchronized int getMunitions() {
        return munitions;
    }

    public synchronized boolean aDesMunitions() {
        return munitions > 0;
    }

    public synchronized boolean isRamasse() {
        return ramasse;
    }

    @Override
    public String getType() {
        return "Fusil";
    }

    @Override
    public Color getCouleur() {
        return new Color(50, 50, 50); // Gris foncé
    }

    @Override
    public String toString() {
        return "🔫 Fusil #" + id + " (munitions: " + munitions + "/" + MUNITIONS_MAX + ") à " + position;
    }
}
