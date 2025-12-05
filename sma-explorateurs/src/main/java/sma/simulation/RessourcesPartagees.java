package sma.simulation;

import java.util.concurrent.*;
import java.util.*;
import sma.agents.Agent;
import sma.environnement.Zone;
import sma.objets.Position;
import sma.objets.Tresor;

/**
 * Ressources partagées entre tous les agents (threads indépendants)
 * Permet la coordination et la communication inter-agents
 * Thread-safe avec des structures concurrentes
 */
public class RessourcesPartagees {
    
    // === DESTINATIONS DES AGENTS ===
    // Map: Agent ID -> Zone cible (pour éviter que plusieurs agents aillent au même endroit)
    private final ConcurrentHashMap<Integer, Zone> destinationsZones;
    
    // Map: Agent ID -> Position cible précise
    private final ConcurrentHashMap<Integer, Position> destinationsPositions;
    
    // === TRÉSORS RÉSERVÉS ===
    // Map: Tresor ID -> Agent ID (pour éviter que plusieurs agents visent le même trésor)
    private final ConcurrentHashMap<Integer, Integer> tresorsReserves;
    
    // === ALERTES ET SIGNAUX ===
    // File des alertes de danger (positions où des animaux ont été vus)
    private final ConcurrentLinkedQueue<AlerteDanger> alertesDanger;
    
    // File des demandes d'aide
    private final ConcurrentLinkedQueue<DemandeAide> demandesAide;
    
    // === ZONES EXPLORÉES (connaissance partagée) ===
    private final ConcurrentHashMap<Integer, Boolean> zonesExplorees;
    
    // === STATISTIQUES PARTAGÉES ===
    private final ConcurrentHashMap<Integer, InfoAgent> infosAgents;
    
    /**
     * Information sur un agent (partagée avec les autres)
     */
    public static class InfoAgent {
        public final int agentId;
        public final String nom;
        public volatile Position position;
        public volatile int pointsDeVie;
        public volatile boolean enVie;
        public volatile boolean blesse;
        public volatile String mission;
        public volatile long derniereMiseAJour;
        
        public InfoAgent(int agentId, String nom) {
            this.agentId = agentId;
            this.nom = nom;
            this.derniereMiseAJour = System.currentTimeMillis();
        }
        
        public void mettreAJour(Position pos, int pv, boolean vivant, boolean blesse, String mission) {
            this.position = pos;
            this.pointsDeVie = pv;
            this.enVie = vivant;
            this.blesse = blesse;
            this.mission = mission;
            this.derniereMiseAJour = System.currentTimeMillis();
        }
    }
    
    /**
     * Alerte de danger émise par un agent
     */
    public static class AlerteDanger {
        public final Position position;
        public final String typeAnimal;
        public final int agentEmetteur;
        public final long timestamp;
        
        public AlerteDanger(Position position, String typeAnimal, int agentEmetteur) {
            this.position = position;
            this.typeAnimal = typeAnimal;
            this.agentEmetteur = agentEmetteur;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean estRecente() {
            return System.currentTimeMillis() - timestamp < 5000; // 5 secondes
        }
    }
    
    /**
     * Demande d'aide émise par un agent
     */
    public static class DemandeAide {
        public final int agentId;
        public final Position position;
        public final String raison;
        public final long timestamp;
        public volatile boolean traitee;
        
        public DemandeAide(int agentId, Position position, String raison) {
            this.agentId = agentId;
            this.position = position;
            this.raison = raison;
            this.timestamp = System.currentTimeMillis();
            this.traitee = false;
        }
    }
    
    public RessourcesPartagees() {
        this.destinationsZones = new ConcurrentHashMap<>();
        this.destinationsPositions = new ConcurrentHashMap<>();
        this.tresorsReserves = new ConcurrentHashMap<>();
        this.alertesDanger = new ConcurrentLinkedQueue<>();
        this.demandesAide = new ConcurrentLinkedQueue<>();
        this.zonesExplorees = new ConcurrentHashMap<>();
        this.infosAgents = new ConcurrentHashMap<>();
    }
    
    // ========== GESTION DES DESTINATIONS ==========
    
    /**
     * Réserve une zone comme destination pour un agent
     * @return true si la zone a été réservée avec succès, false si déjà prise
     */
    public boolean reserverZone(int agentId, Zone zone) {
        if (zone == null) return false;
        
        // Vérifier si la zone est déjà réservée par un autre agent
        for (Map.Entry<Integer, Zone> entry : destinationsZones.entrySet()) {
            if (entry.getKey() != agentId && entry.getValue().getId() == zone.getId()) {
                return false; // Zone déjà prise
            }
        }
        
        destinationsZones.put(agentId, zone);
        return true;
    }
    
    /**
     * Vérifie si une zone est déjà réservée par un autre agent
     */
    public boolean zoneReservee(int agentId, Zone zone) {
        if (zone == null) return false;
        
        for (Map.Entry<Integer, Zone> entry : destinationsZones.entrySet()) {
            if (entry.getKey() != agentId && entry.getValue().getId() == zone.getId()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Libère la réservation de zone d'un agent
     */
    public void libererZone(int agentId) {
        destinationsZones.remove(agentId);
    }
    
    /**
     * Obtient la zone réservée par un agent
     */
    public Zone getZoneReservee(int agentId) {
        return destinationsZones.get(agentId);
    }
    
    // ========== GESTION DES TRÉSORS ==========
    
    /**
     * Réserve un trésor pour un agent
     * @return true si le trésor a été réservé avec succès
     */
    public boolean reserverTresor(int agentId, Tresor tresor) {
        if (tresor == null) return false;
        
        Integer existant = tresorsReserves.putIfAbsent(tresor.getId(), agentId);
        return existant == null || existant == agentId;
    }
    
    /**
     * Vérifie si un trésor est réservé par un autre agent
     */
    public boolean tresorReserve(int agentId, Tresor tresor) {
        if (tresor == null) return false;
        
        Integer reservePar = tresorsReserves.get(tresor.getId());
        return reservePar != null && reservePar != agentId;
    }
    
    /**
     * Libère un trésor (quand collecté ou abandonné)
     */
    public void libererTresor(Tresor tresor) {
        if (tresor != null) {
            tresorsReserves.remove(tresor.getId());
        }
    }
    
    /**
     * Libère le trésor réservé par un agent spécifique
     */
    public void libererTresorParAgent(int agentId) {
        tresorsReserves.entrySet().removeIf(entry -> entry.getValue() == agentId);
    }
    
    // ========== ALERTES DE DANGER ==========
    
    /**
     * Émet une alerte de danger
     */
    public void emettreAlerte(Position position, String typeAnimal, int agentEmetteur) {
        alertesDanger.add(new AlerteDanger(position, typeAnimal, agentEmetteur));
        
        // Nettoyer les vieilles alertes
        alertesDanger.removeIf(a -> !a.estRecente());
    }
    
    /**
     * Récupère les alertes récentes
     */
    public List<AlerteDanger> getAlertesRecentes() {
        List<AlerteDanger> recentes = new ArrayList<>();
        for (AlerteDanger alerte : alertesDanger) {
            if (alerte.estRecente()) {
                recentes.add(alerte);
            }
        }
        return recentes;
    }
    
    // ========== DEMANDES D'AIDE ==========
    
    /**
     * Émet une demande d'aide
     */
    public void demanderAide(int agentId, Position position, String raison) {
        demandesAide.add(new DemandeAide(agentId, position, raison));
    }
    
    /**
     * Récupère une demande d'aide non traitée
     */
    public DemandeAide getDemandeAideNonTraitee() {
        for (DemandeAide demande : demandesAide) {
            if (!demande.traitee && System.currentTimeMillis() - demande.timestamp < 10000) {
                return demande;
            }
        }
        return null;
    }
    
    /**
     * Marque une demande comme traitée
     */
    public void marquerDemandeTraitee(DemandeAide demande) {
        if (demande != null) {
            demande.traitee = true;
        }
    }
    
    // ========== ZONES EXPLORÉES ==========
    
    /**
     * Marque une zone comme explorée (connaissance partagée)
     */
    public void marquerZoneExploree(Zone zone) {
        if (zone != null) {
            zonesExplorees.put(zone.getId(), true);
        }
    }
    
    /**
     * Vérifie si une zone a été explorée par l'équipe
     */
    public boolean zoneExploreeParEquipe(Zone zone) {
        if (zone == null) return false;
        return zonesExplorees.getOrDefault(zone.getId(), false);
    }
    
    // ========== INFOS AGENTS ==========
    
    /**
     * Enregistre un agent dans le système
     */
    public void enregistrerAgent(Agent agent) {
        infosAgents.put(agent.getId(), new InfoAgent(agent.getId(), agent.getNom()));
    }
    
    /**
     * Met à jour les infos d'un agent
     */
    public void mettreAJourInfoAgent(Agent agent, String mission) {
        InfoAgent info = infosAgents.get(agent.getId());
        if (info != null) {
            info.mettreAJour(
                agent.getPosition(),
                agent.getPointsDeVie(),
                agent.isEnVie(),
                agent.isBlesse(),
                mission
            );
        }
    }
    
    /**
     * Récupère les infos de tous les agents
     */
    public Collection<InfoAgent> getInfosAgents() {
        return infosAgents.values();
    }
    
    /**
     * Récupère les infos d'un agent spécifique
     */
    public InfoAgent getInfoAgent(int agentId) {
        return infosAgents.get(agentId);
    }
    
    /**
     * Nettoie toutes les réservations d'un agent (quand il meurt ou se repose)
     */
    public void nettoyerReservations(int agentId) {
        destinationsZones.remove(agentId);
        destinationsPositions.remove(agentId);
        tresorsReserves.entrySet().removeIf(e -> e.getValue() == agentId);
    }
}
