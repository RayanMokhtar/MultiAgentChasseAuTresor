package sma.agents;

import sma.environnement.*;
import sma.objets.*;
import sma.simulation.RessourcesPartagees;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Agent Cognitif - Agent avec planification BDI
 * 
 * RÈGLES DU TP:
 * - Même champ de vision que le réactif
 * - Utilise les informations de la sentinelle (dans ses beliefs)
 * - PRIORITÉ 1: Si animal, fuit comme le réactif MAIS vers le trésor le plus proche
 *   Si plus de trésor, vers la zone la plus proche
 * - Va vers le trésor le plus proche
 * - Esquive les obstacles
 * - Peut aller aider d'autres agents
 * 
 * Architecture BDI:
 * - Beliefs: Connaissances sur l'environnement (via sentinelle)
 * - Desires: Trouver trésors, aider les autres
 * - Intentions: Plan d'action en cours
 */
public class AgentCognitif extends Agent {
    
    public enum Mission {
        CHERCHER_TRESOR("🎯 Cherche trésor"),
        ACCOMPAGNER_AGENT("🤝 Accompagne"),
        EXPLORER_ZONE("🔍 Explore"),
        RETOUR_QG("🏠 Retour QG"),
        SECOURIR_AGENT("🚑 Secours"),
        REPOS("😴 Repos"),
        FUIR_VERS_TRESOR("🏃 Fuit vers trésor"),
        AUCUNE("⏸️ Attente");

        private final String description;
        Mission(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    private volatile Mission missionActuelle;
    private final Queue<Position> plan;
    private volatile Agent agentASecourir;
    private volatile Zone zoneCible;
    private volatile Tresor tresorCible;
    private final Set<Zone> zonesExplorees;
    private final Map<Zone, List<Tresor>> connaissanceTresors;
    
    // BDI - Beliefs (mis à jour par sentinelle)
    private Map<Position, String> beliefsDangers;

    private Position cibleEvitement;
    private long evitementExpireMs;
    private static final long EVITEMENT_COOLDOWN_MS = 2000;
    
    private static final Color COULEUR = new Color(0, 100, 255); // Bleu
    
    // Même vision que réactif
    public static final int RANGE_VISION = 3;
    public static final int VISION_PIXELS = RANGE_VISION * 20;

    public AgentCognitif(String nom, Carte carte) {
        super(nom, carte, 120, 35);
        this.missionActuelle = Mission.AUCUNE;
        this.plan = new ConcurrentLinkedQueue<>();
        this.zonesExplorees = Collections.synchronizedSet(new HashSet<>());
        this.connaissanceTresors = Collections.synchronizedMap(new HashMap<>());
        this.beliefsDangers = Collections.synchronizedMap(new HashMap<>());
        this.cibleEvitement = null;
        this.evitementExpireMs = 0;
        
        // Le QG est toujours connu
        zonesExplorees.add(carte.getQG());
    }
    
    /**
     * Met à jour les beliefs avec les informations d'une sentinelle
     */
    // Les sentinelles ne donnent plus les positions exactes des trésors
    public void recevoirInfosSentinelle(Map<Position, String> tresors, Map<Position, String> dangers) {
        // Ignorer les positions précises pour éviter la clairvoyance
        if (dangers != null) {
            beliefsDangers.putAll(dangers); // on garde l'info danger
        }
    }

    @Override
    public void agir() {
        if (!enVie.get()) return;

        if (gererEvitementActif()) return;

        // 1. Observer l'environnement
        observer();

        // 2. Vérifier si un agent a besoin d'aide (priorité haute)
        if (missionActuelle != Mission.SECOURIR_AGENT) {
            Agent agentEnDanger = trouverAgentEnDanger();
            if (agentEnDanger != null) {
                lancerMissionSecours(agentEnDanger);
            }
        }

        // 3. Gérer les dangers immédiats
        if (gererDangersImmediats()) return;

        // 4. Vérifier énergie et PV - sauf si déjà au QG
        if (doitRentrerQG() && !zoneActuelle.estQG()) {
            if (missionActuelle != Mission.RETOUR_QG) {
                definirMission(Mission.RETOUR_QG);
            }
        }

        // 5. Choisir une nouvelle mission si nécessaire
        if (missionActuelle == Mission.AUCUNE || plan.isEmpty()) {
            choisirNouvelleMission();
        }

        // 6. Exécuter le plan
        executerPlan();
    }

    private void observer() {
        Zone zone = zoneActuelle;
        if (zone != null && !zonesExplorees.contains(zone)) {
            zonesExplorees.add(zone);
            zone.setExploree(true);
            
            // Partager l'info avec les autres agents via ressources partagées
            if (simulation != null && simulation.getRessourcesPartagees() != null) {
                simulation.getRessourcesPartagees().marquerZoneExploree(zone);
            }
            
            // Mémoriser les trésors
            List<Tresor> tresors = zone.getTresorsNonCollectes();
            if (!tresors.isEmpty()) {
                connaissanceTresors.put(zone, new ArrayList<>(tresors));
            }
            
            System.out.println("🔍 " + nom + " explore la zone " + zone.getId());
        }
        
        // Mettre à jour les infos partagées sur cet agent
        if (simulation != null && simulation.getRessourcesPartagees() != null) {
            simulation.getRessourcesPartagees().mettreAJourInfoAgent(this, missionActuelle.getDescription());
        }
    }

    private Agent trouverAgentEnDanger() {
        if (simulation == null) return null;
        
        for (Agent agent : simulation.getAgents()) {
            if (agent != this && agent.aBesoinAide() && agent.isEnVie()) {
                return agent;
            }
        }
        return null;
    }

    private void lancerMissionSecours(Agent agent) {
        this.agentASecourir = agent;
        this.zoneCible = agent.getZoneActuelle();
        definirMission(Mission.SECOURIR_AGENT);
        System.out.println("🚑 " + nom + " part secourir " + agent.getNom() + "!");
    }

    private boolean gererDangersImmediats() {
        Zone zone = zoneActuelle;
        if (zone == null) return false;
        
        // Trouver l'animal le plus proche
        Animal animalDangereux = null;
        double distanceMin = Double.MAX_VALUE;
        
        for (Animal animal : zone.getAnimaux()) {
            if (animal.isActif()) {
                double distance = position.distanceTo(animal.getPosition());
                if (distance <= VISION_PIXELS && distance < distanceMin) {
                    distanceMin = distance;
                    animalDangereux = animal;
                }
            }
        }
        
        if (animalDangereux == null) return false;
        
        // PRIORITÉ: FUIR loin de l'animal d'abord!
        System.out.println("🏃 " + nom + " fuit l'animal!");
        missionActuelle = Mission.FUIR_VERS_TRESOR;
        Position cibleFuite = pointOppose(animalDangereux.getPosition(), 100);
        if (cibleFuite != null) {
            cibleEvitement = cibleFuite;
            evitementExpireMs = System.currentTimeMillis() + EVITEMENT_COOLDOWN_MS;
            return deplacerVers(cibleFuite);
        }
        return fuirDe(animalDangereux.getPosition());
    }
    
    /**
     * Fuit en direction opposée au danger
     */
    /**
     * Fuit loin d'une position de danger - logique améliorée
     */
    private boolean fuirDe(Position danger) {
        // Direction opposée à l'animal
        int dx = Integer.compare(position.getX(), danger.getX());
        int dy = Integer.compare(position.getY(), danger.getY());
        
        // Si dx et dy sont 0 (même position), choisir une direction aléatoire
        if (dx == 0 && dy == 0) {
            dx = (int)(Math.random() * 3) - 1;
            dy = (int)(Math.random() * 3) - 1;
        }
        
        int vitesse = 15; // Fuite rapide!
        
        // Essayer plusieurs directions de fuite
        Position[] fuites = {
            new Position(position.getX() + dx * vitesse, position.getY() + dy * vitesse),
            new Position(position.getX() + dx * vitesse, position.getY()),
            new Position(position.getX(), position.getY() + dy * vitesse),
            new Position(position.getX() - dy * vitesse, position.getY() + dx * vitesse), // Perpendiculaire
            new Position(position.getX() + dy * vitesse, position.getY() - dx * vitesse)  // Perpendiculaire autre sens
        };
        
        for (Position fuite : fuites) {
            if (carte.positionAccessible(fuite)) {
                return deplacer(fuite);
            }
        }
        
        return false;
    }
    
    private boolean doitRentrerQG() {
        return pointsDeVie < pointsDeVieMax * 0.25;
    }

    private void choisirNouvelleMission() {
        // Si au QG avec peu de vie/énergie, se reposer
        if (zoneActuelle.estQG() && (pointsDeVie < pointsDeVieMax * 0.8)) {
            definirMission(Mission.REPOS);
            return;
        }

        // Chercher un trésor connu
        Tresor tresorProche = trouverTresorConnu();
        if (tresorProche != null) {
            this.tresorCible = tresorProche;
            definirMission(Mission.CHERCHER_TRESOR);
            return;
        }

        // Explorer une nouvelle zone
        Zone zoneNonExploree = trouverZoneNonExploree();
        if (zoneNonExploree != null) {
            this.zoneCible = zoneNonExploree;
            definirMission(Mission.EXPLORER_ZONE);
            return;
        }

        // Par défaut: retour au QG
        definirMission(Mission.RETOUR_QG);
    }

    private void definirMission(Mission mission) {
        // Éviter le spam de logs si la mission ne change pas
        if (missionActuelle == mission) {
            return;
        }
        
        // Libérer les réservations de l'ancienne mission
        RessourcesPartagees ressources = simulation.getRessourcesPartagees();
        if (missionActuelle == Mission.EXPLORER_ZONE && mission != Mission.EXPLORER_ZONE) {
            ressources.libererZone(id);
        }
        if (missionActuelle == Mission.CHERCHER_TRESOR && mission != Mission.CHERCHER_TRESOR) {
            ressources.libererTresorParAgent(id);
        }
        
        this.missionActuelle = mission;
        this.plan.clear();
        calculerPlan();
        System.out.println("📋 " + nom + " : " + mission.getDescription());
    }

    private void calculerPlan() {
        Position destination = null;
        
        switch (missionActuelle) {
            case CHERCHER_TRESOR:
                if (tresorCible != null) {
                    destination = tresorCible.getPosition();
                }
                break;
            case EXPLORER_ZONE:
                if (zoneCible != null) {
                    destination = zoneCible.getCentre();
                }
                break;
            case SECOURIR_AGENT:
                if (agentASecourir != null) {
                    destination = agentASecourir.getPosition();
                }
                break;
            case RETOUR_QG:
            case REPOS:
                destination = carte.getQG().getCentre();
                break;
            default:
                break;
        }

        if (destination != null) {
            construireChemin(destination);
        }
    }

    private void construireChemin(Position destination) {
        plan.clear();
        Position current = position.copy();
        int maxEtapes = 200;
        int etape = 0;
        
        while (!positionsProches(current, destination) && etape < maxEtapes) {
            int dx = Integer.compare(destination.getX(), current.getX());
            int dy = Integer.compare(destination.getY(), current.getY());
            
            int pas = 10;
            Position next = new Position(current.getX() + dx * pas, current.getY() + dy * pas);
            
            if (carte.positionAccessible(next)) {
                plan.add(next);
                current = next;
            } else {
                // Contournement simple
                Position alt1 = new Position(current.getX() + dx * pas, current.getY());
                Position alt2 = new Position(current.getX(), current.getY() + dy * pas);
                
                if (carte.positionAccessible(alt1)) {
                    plan.add(alt1);
                    current = alt1;
                } else if (carte.positionAccessible(alt2)) {
                    plan.add(alt2);
                    current = alt2;
                } else {
                    break;
                }
            }
            etape++;
        }
    }

    private boolean positionsProches(Position p1, Position p2) {
        return p1.distanceTo(p2) < 15;
    }

    private Position pointOppose(Position danger, int rayon) {
        int dx = Integer.compare(position.getX(), danger.getX());
        int dy = Integer.compare(position.getY(), danger.getY());
        if (dx == 0 && dy == 0) {
            dx = 1;
        }
        Position cible = new Position(position.getX() + dx * rayon, position.getY() + dy * rayon);
        int x = Math.max(0, Math.min(cible.getX(), carte.getLargeur() - 1));
        int y = Math.max(0, Math.min(cible.getY(), carte.getHauteur() - 1));
        Position candidate = new Position(x, y);
        if (carte.positionAccessible(candidate)) {
            return candidate;
        }
        return null;
    }

    private boolean gererEvitementActif() {
        if (cibleEvitement == null) return false;
        long now = System.currentTimeMillis();
        if (now > evitementExpireMs) {
            cibleEvitement = null;
            return false;
        }
        if (position.distanceTo(cibleEvitement) > 12) {
            deplacerVers(cibleEvitement);
            return true;
        }
        cibleEvitement = null;
        return false;
    }

    private void executerPlan() {
        switch (missionActuelle) {
            case REPOS:
                if (zoneActuelle.estQG()) {
                    reposer();
                    if (pointsDeVie >= pointsDeVieMax * 0.9) {
                        missionActuelle = Mission.AUCUNE;
                    }
                }
                break;
                
            case CHERCHER_TRESOR:
                if (tresorCible != null) {
                    // Coordination: si un animal campe le trésor, organiser un leurre
                    Animal gardien = trouverGardienTresor(tresorCible);
                    if (gardien != null && gardien.isActif() && simulation != null) {
                        double distGardienTresor = gardien.getPosition().distanceTo(tresorCible.getPosition());
                        Agent decoy = simulation.getLeurre(gardien);

                        if (decoy == null || decoy == this) {
                            if (simulation.assignerLeurre(gardien, this)) {
                                if (distGardienTresor > sma.objets.Animal.RANGE_DETECTION * 10) {
                                    deplacerVers(gardien.getPosition());
                                    return;
                                }
                                Position fuite = pointLeurre(zoneActuelle, tresorCible.getPosition());
                                if (fuite != null) {
                                    deplacerVers(fuite);
                                    return;
                                }
                            }
                        } else {
                            // Un autre agent sert de leurre: attendre que le gardien s'éloigne
                            if (distGardienTresor > AgentReactif.VISION_PIXELS * 1.5) {
                                if (position.distanceTo(tresorCible.getPosition()) < 20) {
                                    if (collecterTresor(tresorCible)) {
                                        simulation.libererLeurre(gardien);
                                        tresorCible = null;
                                        missionActuelle = Mission.AUCUNE;
                                    }
                                    return;
                                }
                                avancerDansPlan();
                                return;
                            }
                            // Gardien trop proche: s'écarter
                            fuirDe(gardien.getPosition());
                            return;
                        }
                    }

                    if (position.distanceTo(tresorCible.getPosition()) < 20) {
                        if (collecterTresor(tresorCible)) {
                            tresorCible = null;
                            missionActuelle = Mission.AUCUNE;
                        }
                    } else {
                        avancerDansPlan();
                    }
                } else {
                    avancerDansPlan();
                }
                break;
                
            case SECOURIR_AGENT:
                if (agentASecourir != null) {
                    if (position.distanceTo(agentASecourir.getPosition()) < 30) {
                        effectuerSecours();
                    } else {
                        // Recalculer le chemin vers l'agent qui bouge
                        calculerPlan();
                        avancerDansPlan();
                    }
                }
                break;
                
            case RETOUR_QG:
                if (zoneActuelle.estQG()) {
                    deposerTresors();
                    missionActuelle = Mission.REPOS;
                } else {
                    avancerDansPlan();
                }
                break;
                
            default:
                avancerDansPlan();
                break;
        }
    }

    private void avancerDansPlan() {
        if (!plan.isEmpty()) {
            Position prochaine = plan.poll();
            deplacer(prochaine);
        }
    }

    private void effectuerSecours() {
        if (agentASecourir != null && agentASecourir.isBlesse()) {
            // L'agent secouru reprend vie sur place
            agentASecourir.respawnSurPlace();
            System.out.println("✨ " + nom + " a secouru " + agentASecourir.getNom() + " sur place!");
            if (simulation != null) {
                simulation.getStats().enregistrerSecours();
            }
            agentASecourir = null;
            missionActuelle = Mission.AUCUNE;
        } else if (agentASecourir != null && agentASecourir.isEnVie()) {
            // L'agent s'est déjà fait secourir ou a respawn
            System.out.println("ℹ️ " + nom + " : " + agentASecourir.getNom() + " n'a plus besoin d'aide!");
            agentASecourir = null;
            missionActuelle = Mission.AUCUNE;
        }
    }

    private void deposerTresors() {
        if (!tresorsCollectes.isEmpty()) {
            System.out.println("📦 " + nom + " dépose " + tresorsCollectes.size() + 
                             " trésors au QG");
        }
    }

    private Tresor trouverTresorConnu() {
        RessourcesPartagees ressources = (simulation != null) ? simulation.getRessourcesPartagees() : null;
        Tresor plusProche = null;
        double distanceMin = Double.MAX_VALUE;
        
        // D'abord vérifier dans la zone actuelle
        for (Tresor t : zoneActuelle.getTresorsNonCollectes()) {
            // Vérifier si ce trésor n'est pas déjà réservé par un autre agent
            boolean reserve = (ressources != null && ressources.tresorReserve(id, t));
            if (!reserve) {
                double dist = position.distanceTo(t.getPosition());
                if (dist < distanceMin) {
                    distanceMin = dist;
                    plusProche = t;
                }
            }
        }
        
        // Puis dans les zones connues
        if (plusProche == null) {
            for (Map.Entry<Zone, List<Tresor>> entry : connaissanceTresors.entrySet()) {
                for (Tresor t : entry.getValue()) {
                    if (!t.isCollecte()) {
                        boolean reserve = (ressources != null && ressources.tresorReserve(id, t));
                        if (!reserve) {
                            double dist = position.distanceTo(t.getPosition());
                            if (dist < distanceMin) {
                                distanceMin = dist;
                                plusProche = t;
                            }
                        }
                    }
                }
            }
        }
        
        // Réserver le trésor choisi
        if (plusProche != null && ressources != null) {
            ressources.reserverTresor(id, plusProche);
        }
        
        return plusProche;
    }

    private Zone trouverZoneNonExploree() {
        RessourcesPartagees ressources = (simulation != null) ? simulation.getRessourcesPartagees() : null;
        List<Zone> zonesDisponibles = new ArrayList<>();
        
        // Collecter les zones adjacentes non explorées ET non réservées par d'autres agents
        for (Zone z : carte.getZonesAdjacentes(zoneActuelle)) {
            if (!zonesExplorees.contains(z)) {
                // Vérifier aussi si l'équipe a déjà exploré cette zone
                boolean exploreeParEquipe = (ressources != null && ressources.zoneExploreeParEquipe(z));
                // Vérifier si un autre agent a déjà réservé cette zone
                boolean reservee = (ressources != null && ressources.zoneReservee(id, z));
                
                if (!exploreeParEquipe && !reservee) {
                    zonesDisponibles.add(z);
                }
            }
        }
        
        // Si on a des zones adjacentes disponibles, en choisir une
        if (!zonesDisponibles.isEmpty()) {
            // Utiliser l'ID pour diversifier les choix entre agents
            int index = id % zonesDisponibles.size();
            Zone choix = zonesDisponibles.get(index);
            
            // Réserver cette zone
            if (ressources != null) {
                ressources.reserverZone(id, choix);
                System.out.println("📌 " + nom + " réserve la zone " + choix.getId());
            }
            return choix;
        }
        
        // Sinon chercher n'importe quelle zone non explorée et non réservée
        zonesDisponibles.clear();
        for (Zone z : carte.getZones()) {
            if (!zonesExplorees.contains(z) && !z.estQG()) {
                boolean exploreeParEquipe = (ressources != null && ressources.zoneExploreeParEquipe(z));
                boolean reservee = (ressources != null && ressources.zoneReservee(id, z));
                
                if (!exploreeParEquipe && !reservee) {
                    zonesDisponibles.add(z);
                }
            }
        }
        
        if (!zonesDisponibles.isEmpty()) {
            int index = id % zonesDisponibles.size();
            Zone choix = zonesDisponibles.get(index);
            
            if (ressources != null) {
                ressources.reserverZone(id, choix);
                System.out.println("📌 " + nom + " réserve la zone " + choix.getId());
            }
            return choix;
        }
        
        return null;
    }

    @Override
    public String getTypeAgent() {
        return "🧠 Cognitif";
    }

    @Override
    public Color getCouleur() {
        return COULEUR;
    }

    public Mission getMissionActuelle() {
        return missionActuelle;
    }

    public Set<Zone> getZonesExplorees() {
        return new HashSet<>(zonesExplorees);
    }

    // ========== AIDE LEURRE ==========    

    private Animal trouverGardienTresor(Tresor tresor) {
        Zone zone = zoneActuelle;
        if (zone == null || tresor == null) return null;
        for (Animal animal : zone.getAnimaux()) {
            if (animal.isActif() && animal.getPosition().distanceTo(tresor.getPosition()) <= Animal.RANGE_DETECTION * 12) {
                return animal;
            }
        }
        return null;
    }

    private Position pointLeurre(Zone zone, Position reference) {
        if (zone == null || reference == null) return null;
        Position[] coins = {
            new Position(zone.getPositionDebut().getX() + 5, zone.getPositionDebut().getY() + 5),
            new Position(zone.getPositionDebut().getX() + 5, zone.getPositionFin().getY() - 5),
            new Position(zone.getPositionFin().getX() - 5, zone.getPositionDebut().getY() + 5),
            new Position(zone.getPositionFin().getX() - 5, zone.getPositionFin().getY() - 5)
        };

        Position meilleur = null;
        double dMax = -1;
        for (Position p : coins) {
            double d = p.distanceTo(reference);
            if (d > dMax && carte.positionAccessible(p)) {
                dMax = d;
                meilleur = p;
            }
        }
        return meilleur != null ? meilleur : zone.getCentre();
    }
    
    @Override
    protected void surMourir() {
        // Libérer toutes les ressources partagées
        RessourcesPartagees ressources = simulation.getRessourcesPartagees();
        ressources.libererZone(id);
        ressources.libererTresorParAgent(id);
        System.out.println("🔓 " + nom + " libère ses réservations");
    }
}
