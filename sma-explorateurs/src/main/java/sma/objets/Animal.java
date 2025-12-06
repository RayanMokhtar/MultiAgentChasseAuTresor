package sma.objets;

import sma.agents.Agent;

/**
 * Un animal dangereux. Quand un agent le touche, il subit des dégâts.
 */
public class Animal extends ObjetPassif {

    private final String espece;
    private final int degats;

    public Animal(String espece, int degats) {
        super();
        this.espece = espece;
        this.degats = degats;
    }

    public String getEspece() {
        return espece;
    }

    public int getDegats() {
        return degats;
    }

    @Override
    public void interagir(Agent agent) {
        agent.seFaireAttaquer(degats);
    }

    @Override
    public String toString() {
        return String.format("Animal[id=%d, espece=%s, degats=%d]", id, espece, degats);
    }
}
