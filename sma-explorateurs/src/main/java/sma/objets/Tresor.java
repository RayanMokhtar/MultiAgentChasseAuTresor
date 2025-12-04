package sma.objets;

import java.awt.Color;

/**
 * Représente un trésor à collecter
 * Thread-safe
 */
public class Tresor extends ObjetEnvironnement {
    private final int valeur;
    private volatile boolean collecte;

    public Tresor(int id, Position position, int valeur) {
        super(id, position);
        this.valeur = valeur;
        this.collecte = false;
    }

    public int getValeur() {
        return valeur;
    }

    public synchronized boolean isCollecte() {
        return collecte;
    }

    /**
     * Tente de collecter le trésor (thread-safe)
     * @return true si le trésor a été collecté avec succès, false s'il était déjà collecté
     */
    public synchronized boolean collecter() {
        if (!collecte && actif) {
            this.collecte = true;
            this.actif = false;
            return true;
        }
        return false;
    }

    @Override
    public String getType() {
        return "Trésor";
    }

    @Override
    public Color getCouleur() {
        return new Color(255, 215, 0); // Or
    }

    @Override
    public String toString() {
        return "💰 Trésor #" + id + " (valeur: " + valeur + ") à " + position + (collecte ? " [COLLECTÉ]" : "");
    }
}
