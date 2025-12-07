package sma.concurrent;

import sma.agents.Agent;
import sma.simulation.Simulation;

public class AgentManager extends Thread {

    private final Agent agent;
    private final Simulation simulation;
    private volatile boolean running = true;
    private final long delaiEnMiliSecondes;
    private static volatile boolean simulationTerminee = false; //on peut le faire en synchronized 
    //mais notifie aux autres threads que la simulation est bien terminée 
    private static final int MULTIPLICATEUR_RESPAWN =  100;  // Facteur de pénalité

    public AgentManager(Agent agent, Simulation simulation, long delaiEnMiliSecondes) {
        this.agent = agent;
        this.simulation = simulation;
        this.delaiEnMiliSecondes = delaiEnMiliSecondes;
    }

    @Override
    public void run() {
        while (running && !simulationTerminee) {
            if (simulation.getCarte().tousTresorsCollectes()) {
                if (!simulationTerminee) {
                    simulationTerminee = true;
                    simulation.arreter();  
                }
                break;
            }
            
            if (agent.isAlive()) {
                agent.step();
                agent.getStats().incrementerIterations();
            
            } else {
                try {
                    Thread.sleep(delaiEnMiliSecondes * MULTIPLICATEUR_RESPAWN); //reset temps réapparition en théroei on est sur du 100*100 donc 10 secondes 
                } catch (InterruptedException e) {
                    System.out.println("erreur exception interrupted +> ");
                    e.printStackTrace();
                    break;
                }
                agent.resetToQG();
            }
            try {
                Thread.sleep(delaiEnMiliSecondes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stopAgent() {
        running = false;
        this.interrupt();
    }

    public Agent getAgent() {
        return agent;
    }
    
   
    public static boolean isSimulationTerminee() {
        return simulationTerminee;
    }
}