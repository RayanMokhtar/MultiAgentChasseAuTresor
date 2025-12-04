package sma.agents;

import sma.environnement.*;
import sma.objets.*;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Agent Cognitif - Agent avec planification
 * Capable de :
 * - Chercher des trésors de manière planifiée
 * - Aller aider des agents en danger
 * - Exécuter un plan étape par étape
 */
public class AgentCognitif extends Agent {
    
    public enum Mission {
        CHERCHER_TRESOR("🎯 Cherche trésor"),
        ACCOMPAGNER_AGENT("🤝 Accompagne"),
        EXPLORER_ZONE("🔍 Explore"),
        RETOUR_QG("🏠 Retour QG"),
        SECOURIR_AGENT("🚑 Secours"),
        REPOS("😴 Repos"),
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
    
    private static final Color COULEUR = new Color(0, 100, 255); // Bleu

    public AgentCognitif(String nom, Carte carte) {
        super(nom, carte, 120, 35, 60);
        this.missionActuelle = Mission.AUCUNE;
        this.plan = new ConcurrentLinkedQueue<>();
        this.zonesExplorees = Collections.synchronizedSet(new HashSet<>());
        this.connaissanceTresors = Collections.synchronizedMap(new HashMap<>());
        
        // Le QG est toujours connu
        zonesExplorees.add(carte.getQG());
    }

    @Override
    public void agir() {
        if (!enVie.get()) return;

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

        // 4. Vérifier énergie et PV
        if (doitRentrerQG()) {
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
            
            // Mémoriser les trésors
            List<Tresor> tresors = zone.getTresorsNonCollectes();
            if (!tresors.isEmpty()) {
                connaissanceTresors.put(zone, new ArrayList<>(tresors));
            }
            
            System.out.println("🔍 " + nom + " explore la zone " + zone.getId());
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
        
        for (Animal animal : zone.getAnimaux()) {
            if (animal.peutAttaquer(position)) {
                // Décider: combattre ou fuir
                if (force >= animal.getForce() && pointsDeVie > animal.getForce() * 2) {
                    combattre(animal);
                    return true;
                } else if (pointsDeVie < pointsDeVieMax * 0.5) {
                    System.out.println("🏃 " + nom + " fuit le danger!");
                    definirMission(Mission.RETOUR_QG);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doitRentrerQG() {
        return energie < energieMax * 0.15 || pointsDeVie < pointsDeVieMax * 0.25;
    }

    private void choisirNouvelleMission() {
        // Si au QG avec peu de vie/énergie, se reposer
        if (zoneActuelle.estQG() && (energie < energieMax * 0.8 || pointsDeVie < pointsDeVieMax * 0.8)) {
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

    private void executerPlan() {
        switch (missionActuelle) {
            case REPOS:
                if (zoneActuelle.estQG()) {
                    reposer();
                    if (energie >= energieMax * 0.9 && pointsDeVie >= pointsDeVieMax * 0.9) {
                        missionActuelle = Mission.AUCUNE;
                    }
                }
                break;
                
            case CHERCHER_TRESOR:
                if (tresorCible != null && position.distanceTo(tresorCible.getPosition()) < 20) {
                    if (collecterTresor(tresorCible)) {
                        tresorCible = null;
                        missionActuelle = Mission.AUCUNE;
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
        if (agentASecourir != null && agentASecourir.isEnVie()) {
            // Téléporter l'agent secouru au QG
            agentASecourir.teleporterAuQG();
            System.out.println("✨ " + nom + " a téléporté " + agentASecourir.getNom() + " au QG!");
            agentASecourir = null;
            missionActuelle = Mission.AUCUNE;
        }
    }

    private void deposerTresors() {
        if (!tresorsCollectes.isEmpty()) {
            int total = getScoreTotal();
            System.out.println("📦 " + nom + " dépose " + tresorsCollectes.size() + 
                             " trésors au QG (total: " + total + " pts)");
        }
    }

    private Tresor trouverTresorConnu() {
        Tresor plusProche = null;
        double distanceMin = Double.MAX_VALUE;
        
        // D'abord vérifier dans la zone actuelle
        for (Tresor t : zoneActuelle.getTresorsNonCollectes()) {
            double dist = position.distanceTo(t.getPosition());
            if (dist < distanceMin) {
                distanceMin = dist;
                plusProche = t;
            }
        }
        
        // Puis dans les zones connues
        if (plusProche == null) {
            for (Map.Entry<Zone, List<Tresor>> entry : connaissanceTresors.entrySet()) {
                for (Tresor t : entry.getValue()) {
                    if (!t.isCollecte()) {
                        double dist = position.distanceTo(t.getPosition());
                        if (dist < distanceMin) {
                            distanceMin = dist;
                            plusProche = t;
                        }
                    }
                }
            }
        }
        
        // Finalement, chercher tous les trésors non collectés
        if (plusProche == null) {
            for (Tresor t : carte.getTousTresorsNonCollectes()) {
                double dist = position.distanceTo(t.getPosition());
                if (dist < distanceMin) {
                    distanceMin = dist;
                    plusProche = t;
                }
            }
        }
        
        return plusProche;
    }

    private Zone trouverZoneNonExploree() {
        // D'abord les zones adjacentes
        for (Zone z : carte.getZonesAdjacentes(zoneActuelle)) {
            if (!zonesExplorees.contains(z)) {
                return z;
            }
        }
        
        // Puis n'importe quelle zone non explorée
        for (Zone z : carte.getZones()) {
            if (!zonesExplorees.contains(z) && !z.estQG()) {
                return z;
            }
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
}
