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
    
    // Configuration par défaut - équipe réduite pour 9 zones
    public static final int NB_AGENTS_COGNITIFS = 2;
    public static final int NB_AGENTS_REACTIFS = 3;
    
    private final Carte carte;
    private final List<Agent> agents;
    private final List<AgentCommunicant> agentsCommunicants;
    private final Statistiques stats;
    private final ConcurrentHashMap<sma.objets.Animal, Agent> leurres;
    private final RessourcesPartagees ressourcesPartagees; // Ressources partagées entre threads
    
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
        this.leurres = new ConcurrentHashMap<>();
        this.ressourcesPartagees = new RessourcesPartagees(); // Initialiser les ressources partagées
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
        
        // Créer les agents communicants (moitié du nombre de zones, sauf QG = 4 sentinelles)
        int nbZones = carte.getNombreZones();
        int nbCommunicants = (nbZones - 1) / 2; // Moitié des zones non-QG
        int compteur = 0;
        
        for (Zone zone : carte.getZones()) {
            if (!zone.estQG() && compteur < nbCommunicants) {
                String nom = "Sentinelle-" + (compteur + 1);
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
        
        // Scheduler pour les mises à jour statistiques et le comportement des animaux
        schedulerStats = Executors.newSingleThreadScheduledExecutor();
        schedulerStats.scheduleAtFixedRate(() -> {
            if (!pause) {
                stats.incrementerIterations();
                mettreAJourZonesExplorees();
                faireAgirAnimaux(); // Les animaux chassent les agents
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
    
    /**
     * Fait agir tous les animaux sur la carte
     * RÈGLES:
     * - Marche au hasard dans leur zone
     * - Si détecte un agent (range 5), le chasse
     * - Si rencontre un trésor, reste autour
     * - S'il y a 2 agents, chasse le plus proche
     * - Ne peut pas sortir de sa zone assignée
     */
    private void faireAgirAnimaux() {
        for (Zone zone : carte.getZones()) {
            if (zone.estQG()) continue; // Pas d'animaux au QG
            
            for (sma.objets.Animal animal : zone.getAnimaux()) {
                if (!animal.isActif()) continue;
                
                // Assigner la zone si pas fait
                if (animal.getZoneAssignee() == null) {
                    animal.setZoneAssignee(zone);
                }
                
                // Priorité : poursuivre un agent détecté
                Agent cibleAgent = null;
                double distMinAgent = Double.MAX_VALUE;
                for (Agent agent : zone.getAgentsPresents()) {
                    if (agent instanceof AgentCommunicant) continue; // N'attaque pas les sentinelles
                    if (!agent.isEnVie() || agent.isBlesse()) continue;
                    double dist = animal.getPosition().distanceTo(agent.getPosition());
                    if (animal.detecteAgent(agent.getPosition()) && dist < distMinAgent) {
                        distMinAgent = dist;
                        cibleAgent = agent;
                    }
                }

                if (cibleAgent != null) {
                    // Chasser l'agent détecté
                    animal.setCible(cibleAgent.getPosition());
                } else {
                    // Pas d'agent détecté, déplacement aléatoire
                    animal.setCible(null);
                }
                
                // Déplacer l'animal
                animal.seDeplacer();
                
                // Attaquer si à portée
                if (cibleAgent != null && animal.peutAttaquer(cibleAgent.getPosition())) {
                    int degats = animal.attaquer();
                    cibleAgent.recevoirDegats(degats);
                    stats.enregistrerCombat();
                    System.out.println("🐾 " + animal.getTypeAnimal().name() + 
                                     " attaque " + cibleAgent.getNom() + " (-" + degats + " PV)");
                }
            }
        }
    }

    private boolean verifierFinSimulation() {
        // Fin si tous les trésors sont collectés
        return carte.getTousTresorsNonCollectes().isEmpty();
    }

    // Coordination: un agent peut se déclarer "leurre" d'un animal gardant un trésor
    public boolean assignerLeurre(sma.objets.Animal animal, Agent agent) {
        if (animal == null || agent == null) return false;
        Agent deja = leurres.putIfAbsent(animal, agent);
        return deja == null || deja == agent;
    }

    public Agent getLeurre(sma.objets.Animal animal) {
        return animal == null ? null : leurres.get(animal);
    }

    public void libererLeurre(sma.objets.Animal animal) {
        if (animal != null) {
            leurres.remove(animal);
        }
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

    /**
     * Demande à un agent réactif disponible de se rapprocher d'un point (soutien simple)
     */
    public boolean demanderSupportReactif(sma.objets.Position cible) {
        AgentReactif choix = null;
        double distMin = Double.MAX_VALUE;
        for (Agent agent : agents) {
            if (agent instanceof AgentReactif ar && ar.isEnVie() && !ar.isBlesse()) {
                double d = ar.getPosition().distanceTo(cible);
                if (d < distMin) {
                    distMin = d;
                    choix = ar;
                }
            }
        }
        if (choix != null) {
            return choix.assignerSupport(cible);
        }
        return false;
    }

    // Getters
    public Carte getCarte() { return carte; }
    public List<Agent> getAgents() { return new ArrayList<>(agents); }
    public List<AgentCommunicant> getAgentsCommunicants() { return new ArrayList<>(agentsCommunicants); }
    public Statistiques getStats() { return stats; }
    public RessourcesPartagees getRessourcesPartagees() { return ressourcesPartagees; }
    public boolean isEnCours() { return enCours; }
    public boolean isPause() { return pause; }
}
