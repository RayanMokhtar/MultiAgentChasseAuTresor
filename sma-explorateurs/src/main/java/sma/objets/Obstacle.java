package sma.objets;

import java.awt.Color;

/**
 * Représente un obstacle dans l'environnement
 */
public class Obstacle extends ObjetEnvironnement {
    
    public enum TypeObstacle {
        ROCHER("🪨", false, new Color(105, 105, 105)),
        ARBRE("🌲", true, new Color(34, 139, 34)),
        RIVIERE("🌊", true, new Color(30, 144, 255)),
        MUR("🧱", false, new Color(178, 34, 34));

        private final String emoji;
        private final boolean franchissable;
        private final Color couleur;

        TypeObstacle(String emoji, boolean franchissable, Color couleur) {
            this.emoji = emoji;
            this.franchissable = franchissable;
            this.couleur = couleur;
        }

        public String getEmoji() { return emoji; }
        public boolean isFranchissable() { return franchissable; }
        public Color getCouleur() { return couleur; }
    }

    private final TypeObstacle typeObstacle;
    private final int coutTraversee;

    public Obstacle(int id, Position position, TypeObstacle typeObstacle) {
        super(id, position);
        this.typeObstacle = typeObstacle;
        this.coutTraversee = typeObstacle.isFranchissable() ? 2 : Integer.MAX_VALUE;
    }

    public TypeObstacle getTypeObstacle() { return typeObstacle; }
    public boolean isFranchissable() { return typeObstacle.isFranchissable(); }
    public int getCoutTraversee() { return coutTraversee; }

    @Override
    public String getType() {
        return "Obstacle";
    }

    @Override
    public Color getCouleur() {
        return typeObstacle.getCouleur();
    }

    @Override
    public String toString() {
        return typeObstacle.getEmoji() + " " + typeObstacle.name() + " #" + id + 
               " à " + position + (typeObstacle.isFranchissable() ? " [franchissable]" : " [bloquant]");
    }
}
