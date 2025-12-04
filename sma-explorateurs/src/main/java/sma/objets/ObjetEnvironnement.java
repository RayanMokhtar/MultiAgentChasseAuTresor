package sma.objets;

/**
 * Classe abstraite représentant un objet dans l'environnement
 * Thread-safe pour accès concurrent
 */
public abstract class ObjetEnvironnement {
    protected final int id;
    protected volatile Position position;
    protected volatile boolean actif;

    public ObjetEnvironnement(int id, Position position) {
        this.id = id;
        this.position = position;
        this.actif = true;
    }

    public int getId() {
        return id;
    }

    public synchronized Position getPosition() {
        return position;
    }

    public synchronized void setPosition(Position position) {
        this.position = position;
    }

    public synchronized boolean isActif() {
        return actif;
    }

    public synchronized void setActif(boolean actif) {
        this.actif = actif;
    }

    public abstract String getType();
    
    public abstract java.awt.Color getCouleur();
}
