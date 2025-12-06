package sma.objets;

import sma.agents.Agent;

/**
 * Classe abstraite pour tous les objets passifs Par ailleurs remarque
 * importante, un animal pourrait éventuellement être un agent selon le design ,
 * donc TODO => trouver meilleru nom pour cette classe
 */
public abstract class ObjetPassif {

    private static int compteurId = 0;
    protected final int id;

    public ObjetPassif() {
        compteurId++;
        this.id = compteurId;
    }

    public int getId() {
        return id;
    }

    /**
     * Partie du cours R/A : définir relation ou interaction entre objet et
     * agent .. Action quand un agent entre sur la case. Chaque type d'objet
     * définit son comportement.
     */
    public abstract void interagir(Agent agent);
}
