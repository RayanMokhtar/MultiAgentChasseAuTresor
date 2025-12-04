package sma.simulation;

import java.util.*;
import java.util.concurrent.*;
import sma.agents.*;
import sma.environnement.*;

/**
 * Classe principale de simulation du système multi-agents
 * Gère le multithreading et la synchronisation
 */
public class Simulation {
    
    // Configuration par défaut
    public static final int NB_AGENTS_COGNITIFS = 3;
    public static final int NB_AGENTS_REACTIFS = 4;
    
    private final Carte carte;
    private final List<Agent> agents;
    private final List<AgentCommunicant> agentsCommunicants;
    private final Statistiques stats;
    
    private ExecutorService executorAgents;
    private ScheduledExecutorService schedulerStats;
    
    private volatile boolean enCours;
    private volatile boolean pause;
    
    private final List<SimulationListener> listeners;

    /**
     * Interface pour écouter les événements de simulation
     */
    public interface SimulationListener {
        void onUpdate();
        void onTerminee();
    }

    public Simulation() {
        this(new Carte());
    }

    public Simulation(Carte carte) {
        this.carte = carte;
        this.agents = new CopyOnWriteArrayList<>();
        this.agentsCommunicants = new CopyOnWriteArrayList<>();
        this.stats = new Statistiques();
        this.listeners = new CopyOnWriteArrayList<>();
        this.enCours = false;
        this.pause = false;
        
        initialiserAgents();
    }

    private void initialiserAgents() {
        // Créer les agents cognitifs
        String[] nomsCognitifs = {"Einstein", "Newton", "Darwin"};
        for (int i = 0; i < NB_AGENTS_COGNITIFS; i++) {
            String nom = i < nomsCognitifs.length ? nomsCognitifs[i] : "Cognitif-" + (i + 1);
            AgentCognitif agent = new AgentCognitif(nom, carte);
            agent.setSimulation(this);
            agents.add(agent);
        }
        
        // Créer les agents réactifs
        String[] nomsReactifs = {"Flash", "Bolt", "Storm", "Dash"};
        for (int i = 0; i < NB_AGENTS_REACTIFS; i++) {
            String nom = i < nomsReactifs.length ? nomsReactifs[i] : "Réactif-" + (i + 1);
            AgentReactif agent = new AgentReactif(nom, carte);
            agent.setSimulation(this);
            agents.add(agent);
        }
        
        // Créer les agents communicants (1 pour chaque 2 zones, sauf QG)
        int nbZones = carte.getNombreZones();
        int nbCommunicants = nbZones / 2;
        int compteur = 0;
        
        for (Zone zone : carte.getZones()) {
            if (!zone.estQG() && compteur < nbCommunicants) {
                String nom = "Radio-" + zone.getId();
                AgentCommunicant agent = new AgentCommunicant(nom, carte, zone);
                agent.setSimulation(this);
                agents.add(agent);
                agentsCommunicants.add(agent);
                compteur++;
            }
        }
        
        System.out.println("📊 Simulation initialisée:");
        System.out.println("   - " + NB_AGENTS_COGNITIFS + " agents cognitifs");
        System.out.println("   - " + NB_AGENTS_REACTIFS + " agents réactifs");
        System.out.println("   - " + agentsCommunicants.size() + " agents communicants");
    }

    public void demarrer() {
        if (enCours) return;
        
        enCours = true;
        pause = false;
        stats.demarrer();
        
        // Pool de threads pour les agents
        executorAgents = Executors.newFixedThreadPool(agents.size());
        
        // Démarrer tous les agents
        for (Agent agent : agents) {
            executorAgents.submit(agent);
        }
        
        // Scheduler pour les mises à jour statistiques
        schedulerStats = Executors.newSingleThreadScheduledExecutor();
        schedulerStats.scheduleAtFixedRate(() -> {
            if (!pause) {
                stats.incrementerIterations();
                mettreAJourZonesExplorees();
                notifierListeners();
                
                // Vérifier condition de fin
                if (verifierFinSimulation()) {
                    arreter();
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        
        System.out.println("🚀 Simulation démarrée!");
    }

    public void arreter() {
        if (!enCours) return;
        
        enCours = false;
        stats.terminer();
        
        // Arrêter tous les agents
        for (Agent agent : agents) {
            agent.arreter();
        }
        
        // Fermer les executors
        if (executorAgents != null) {
            executorAgents.shutdownNow();
        }
        if (schedulerStats != null) {
            schedulerStats.shutdownNow();
        }
        
        System.out.println("\n" + stats.toString());
        System.out.println("🛑 Simulation terminée!");
        
        // Notifier les listeners
        for (SimulationListener l : listeners) {
            l.onTerminee();
        }
    }

    public void togglePause() {
        pause = !pause;
        
        for (Agent agent : agents) {
            if (pause) {
                agent.suspendre();
            } else {
                agent.reprendre();
            }
        }
        
        System.out.println(pause ? "⏸️ Simulation en pause" : "▶️ Simulation reprise");
    }

    private void mettreAJourZonesExplorees() {
        int count = 0;
        for (Zone zone : carte.getZones()) {
            if (zone.isExploree()) {
                count++;
            }
        }
        stats.setZonesExplorees(count);
    }

    private boolean verifierFinSimulation() {
        // Fin si tous les trésors sont collectés
        return carte.getTousTresorsNonCollectes().isEmpty();
    }

    private void notifierListeners() {
        for (SimulationListener l : listeners) {
            l.onUpdate();
        }
    }

    public void ajouterListener(SimulationListener listener) {
        listeners.add(listener);
    }

    public void retirerListener(SimulationListener listener) {
        listeners.remove(listener);
    }

    // Getters
    public Carte getCarte() { return carte; }
    public List<Agent> getAgents() { return new ArrayList<>(agents); }
    public List<AgentCommunicant> getAgentsCommunicants() { return new ArrayList<>(agentsCommunicants); }
    public Statistiques getStats() { return stats; }
    public boolean isEnCours() { return enCours; }
    public boolean isPause() { return pause; }
}
