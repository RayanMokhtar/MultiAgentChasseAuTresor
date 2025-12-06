package sma.environnement;

import java.util.ArrayList;
import java.util.List;

import sma.agents.Agent;
import sma.objets.ObjetPassif;
import sma.objets.Obstacle;

public class Case {
    
    private final int x;
    private final int y;
    private Zone zone;
    private ObjetPassif objet;
    private final List<Agent> agents = new ArrayList<>();  //référence ne peut jamais être modifiée masi le contenu si via les méthodes ...

    public Case(int x, int y) {
        this.x = x;
        this.y = y;
        this.objet = null;
    }

    public int getX() { 
        return x; 
    }
    public int getY() { 
        return y; 
    }
    public Zone getZone() { 
        return zone;
    }
    public ObjetPassif getObjet() { 
        return objet; 
    }
    public boolean hasObjet() { 
        return objet != null; 
    }

    public List<Agent> getAgents() { 
        return agents; 
    }

    public boolean hasAgents() {
        return !agents.isEmpty();
    }

    public int getNbAgents() { 
        return agents.size(); 
    }

    public synchronized void ajouterAgent(Agent agent) {
        if (!agents.contains(agent)) {
            agents.add(agent);
            
            if (objet != null) {
                objet.interagir(agent);
            }
        }
    }

    public synchronized void retirerAgent(Agent agent) {
        agents.remove(agent);
    }

    public void setZone(Zone zone) { 
        this.zone = zone; 
    }
    
    public void setObjet(ObjetPassif objet) { 
        this.objet = objet; 
    }
    
    public void retirerObjet() { 
        this.objet = null; 
    }

    public boolean isAccessible() {
        if (objet == null) {
            return true;
        }
        return !(objet instanceof Obstacle);
    }

    @Override
    public String toString() {
        return String.format("Case[Zone%d, (%d,%d)]", zone != null ? zone.getId() : -1, x, y);
    }
}