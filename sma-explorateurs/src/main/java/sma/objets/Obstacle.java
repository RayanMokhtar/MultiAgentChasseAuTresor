package sma.objets;

import sma.agents.Agent;

/**
 * Un obstacle qui bloque le passage. L'agent ne peut pas entrer sur cette case.
 */
public class Obstacle extends ObjetPassif {

    private final String description;

    public Obstacle(String description) {
        super();
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void interagir(Agent agent) {
        //cette partie devra être gérée par l'agent, si détecte obstacle, avec sa range ou si caseAccessible a faire un truc comme ça .. contourner ..
    }

    @Override
    public String toString() {
        return String.format("Obstacle[id=%d, %s]", id, description);
    }
}
