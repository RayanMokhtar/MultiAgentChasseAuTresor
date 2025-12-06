package sma.objets;

import sma.agents.Agent;

/**
 * Un trésor à collecter.
 * Quand un agent le touche, il le collecte.
 */
public class Tresor extends ObjetPassif {
    
    private final int valeur;
    private boolean collecte;

    public Tresor(int valeur) {
        super();
        this.valeur = valeur;
        this.collecte = false;
    }

    public int getValeur() {
        return valeur;
    }

    public boolean isCollecte() {
        return collecte;
    }

    @Override
    public void interagir(Agent agent) {
        if (!collecte) {
            collecte = true;
            agent.collectTresor(this); // si agent collecte trésor , ajouter cette instance de trésor dans la liste des trésor de l'agent en question 
        }
    }

    @Override //pas obligatoire mais généré automatiquement 
    public String toString() {
        return String.format("Tresor[id=%d, valeur=%d, collecté=%b]", id, valeur, collecte);
    }
}