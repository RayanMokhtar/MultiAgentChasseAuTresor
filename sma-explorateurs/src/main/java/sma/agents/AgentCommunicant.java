package sma.agents;

import sma.environnement.*;
import sma.objets.*;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent Communicant (Sentinelle) - Agent stationnaire qui observe et partage des informations
 * 
 * RÈGLES DU TP:
 * - Stationnaire (ne bouge pas)
 * - Observe la zone assignée et collecte des informations
 * - Communique les informations aux agents qui entrent dans sa zone
 * - NE PEUT PAS secourir ou téléporter les agents
 * - Les agents cognitifs utilisent ses informations dans leurs beliefs
 */
public class AgentCommunicant extends Agent {
    
    private final Zone zoneAssignee;
    private final Map<String, Object> informationsZone;
    private final List<Message> messagesRecus;
    private final Set<Agent> agentsInformes;
    private volatile long derniereMiseAJour;
    
    private static final Color COULEUR = new Color(148, 0, 211); // Violet
    private static final long INTERVALLE_MAJ = 1000; // 1 seconde (comme dans les règles)

    /**
     * Message transmis entre agents communicants
     */
    public static class Message {
        public enum TypeMessage {
            ALERTE_DANGER("⚠️"),
            TRESOR_TROUVE("💰"),
            ZONE_SURE("✅"),
            AGENT_EN_DANGER("🆘"),
            INFO_ZONE("📍");

            private final String icone;
            TypeMessage(String icone) { this.icone = icone; }
            public String getIcone() { return icone; }
        }

        private final TypeMessage type;
        private final String contenu;
        private final Agent expediteur;
        private final Zone zone;
        private final long timestamp;

        public Message(TypeMessage type, String contenu, Agent expediteur, Zone zone) {
            this.type = type;
            this.contenu = contenu;
            this.expediteur = expediteur;
            this.zone = zone;
            this.timestamp = System.currentTimeMillis();
        }

        public TypeMessage getType() { return type; }
        public String getContenu() { return contenu; }
        public Agent getExpediteur() { return expediteur; }
        public Zone getZone() { return zone; }
        public long getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return type.getIcone() + " [" + type + "] " + contenu;
        }
    }

    public AgentCommunicant(String nom, Carte carte, Zone zoneAssignee) {
        super(nom, carte, 60, 5); // Peu de PV/force
        this.zoneAssignee = zoneAssignee;
        this.position = zoneAssignee.getCentre();
        this.zoneActuelle = zoneAssignee;
        this.informationsZone = new ConcurrentHashMap<>();
        this.messagesRecus = new CopyOnWriteArrayList<>();
        this.agentsInformes = Collections.synchronizedSet(new HashSet<>());
        this.derniereMiseAJour = 0;
        
        // S'enregistrer dans la zone
        zoneAssignee.ajouterAgent(this);
        mettreAJourInformations();
    }

    @Override
    public void agir() {
        if (!enVie.get()) return;

        // 1. Mettre à jour les informations régulièrement (toutes les 1 seconde)
        long maintenant = System.currentTimeMillis();
        if (maintenant - derniereMiseAJour > INTERVALLE_MAJ) {
            mettreAJourInformations();
            derniereMiseAJour = maintenant;
        }

        // 2. Informer les nouveaux agents dans la zone
        informerAgents();

        // 3. Traiter les messages reçus
        traiterMessages();

        // 4. Émettre des alertes si nécessaire
        verifierEtEmettre();

        // 5. Rester dans la zone (ne pas bouger - stationnaire)
        if (!zoneAssignee.contientPosition(position)) {
            position = zoneAssignee.getCentre();
        }
    }

    private void mettreAJourInformations() {
        informationsZone.clear();
        
        List<Tresor> tresors = zoneAssignee.getTresorsNonCollectes();
        List<Animal> animaux = zoneAssignee.getAnimaux();
        List<Obstacle> obstacles = zoneAssignee.getObstacles();
        
        informationsZone.put("zone_id", zoneAssignee.getId());
        informationsZone.put("tresors_disponibles", tresors.size());
        informationsZone.put("animaux_presents", animaux.size());
        informationsZone.put("obstacles", obstacles.size());
        informationsZone.put("niveau_danger", calculerNiveauDanger(animaux));
        informationsZone.put("exploree", zoneAssignee.isExploree());
        // Ne plus partager les positions exactes pour éviter la clairvoyance
    }

    private int calculerNiveauDanger(List<Animal> animaux) {
        int danger = 0;
        for (Animal animal : animaux) {
            if (animal.isActif()) {
                danger += animal.getForce();
            }
        }
        return danger;
    }

    private void informerAgents() {
        List<Agent> agentsDansZone = zoneAssignee.getAgentsPresents();
        
        for (Agent agent : agentsDansZone) {
            if (agent != this && !agentsInformes.contains(agent)) {
                transmettreInformations(agent);
                agentsInformes.add(agent);
            }
        }
        
        // Retirer les agents qui ont quitté la zone
        agentsInformes.removeIf(a -> !agentsDansZone.contains(a));
    }

    private void transmettreInformations(Agent agent) {
        int tresors = (int) informationsZone.getOrDefault("tresors_disponibles", 0);
        int animaux = (int) informationsZone.getOrDefault("animaux_presents", 0);
        int danger = (int) informationsZone.getOrDefault("niveau_danger", 0);
        
        StringBuilder info = new StringBuilder();
        info.append("📡 ").append(nom).append(" informe ").append(agent.getNom()).append(":\n");
        info.append("   Zone ").append(zoneAssignee.getId());
        info.append(" | 💰 Trésors: ").append(tresors);
        info.append(" | 🐾 Animaux: ").append(animaux);
        info.append(" | ⚠️ Danger: ").append(danger);
        
        System.out.println(info);
        
        // Pas de positions exactes pour éviter le guidage parfait
    }

    private void traiterMessages() {
        Iterator<Message> it = messagesRecus.iterator();
        while (it.hasNext()) {
            Message msg = it.next();
            
            switch (msg.getType()) {
                case AGENT_EN_DANGER:
                    // Relayer aux agents de la zone
                    for (Agent agent : zoneAssignee.getAgentsPresents()) {
                        if (agent != this && agent instanceof AgentCognitif) {
                            System.out.println("📢 " + nom + " relaie alerte à " + agent.getNom());
                        }
                    }
                    break;
                    
                case TRESOR_TROUVE:
                    mettreAJourInformations();
                    break;
                    
                default:
                    break;
            }
        }
        messagesRecus.clear();
    }

    private void verifierEtEmettre() {
        // Vérifier si un agent est en danger dans la zone
        for (Agent agent : zoneAssignee.getAgentsPresents()) {
            if (agent != this && agent.estEnDanger()) {
                diffuserMessage(new Message(
                    Message.TypeMessage.AGENT_EN_DANGER,
                    agent.getNom() + " en danger dans zone " + zoneAssignee.getId(),
                    this,
                    zoneAssignee
                ));
            }
        }
    }

    /**
     * Reçoit un message d'un autre agent
     */
    public void recevoirMessage(Message message) {
        messagesRecus.add(message);
    }

    /**
     * Diffuse un message à tous les agents communicants via la simulation
     */
    public void diffuserMessage(Message message) {
        if (simulation != null) {
            for (AgentCommunicant ac : simulation.getAgentsCommunicants()) {
                if (ac != this) {
                    ac.recevoirMessage(message);
                }
            }
        }
    }

    @Override
    public synchronized boolean deplacer(Position nouvellePosition) {
        // L'agent communicant ne bouge pas (stationnaire)
        return false;
    }

    @Override
    public String getTypeAgent() {
        return "📡 Communicant";
    }

    @Override
    public Color getCouleur() {
        return COULEUR;
    }

    public Zone getZoneAssignee() {
        return zoneAssignee;
    }

    public Map<String, Object> getInformationsZone() {
        return new HashMap<>(informationsZone);
    }

    @Override
    public String toString() {
        return super.toString() + " [Zone: " + zoneAssignee.getId() + "]";
    }
}
