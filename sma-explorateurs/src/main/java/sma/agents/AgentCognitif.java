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
    private static final Set<Integer> ZONES_RESERVEES_COGNITIFS = new HashSet<>();
    
    private final Queue<Message> messagesRecus = new LinkedList<>();
    private final List<Case> tresorsConnus = new ArrayList<>();
    private final Set<Case> casesAEviter = new HashSet<>();
    private Case destination = null;
    private final Random random = new Random();
    
    private final LinkedList<Case> historiqueRecent = new LinkedList<>();
    private static final int TAILLE_HISTORIQUE = 5;

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
    public void step() {
        traiterMessages();
        nettoyerTresorsCollectes();
        deciderAction();
    }

    private void traiterMessages() {
        synchronized (this) {
            while (!messagesRecus.isEmpty()) {
                Message msg = messagesRecus.poll();
                
                switch (msg.getType()) {
                    case TRESOR_TROUVE:
                        if (msg.getPosition() != null && !tresorsConnus.contains(msg.getPosition())) {
                            tresorsConnus.add(msg.getPosition());
                            System.out.println("Cognitif " + id + ": Trésor reçu à " + msg.getPosition());
                        }
                        break;
                        
                    case ANIMAL_DETECTE:
                        if (msg.getPosition() != null) {
                            casesAEviter.add(msg.getPosition());
                        }
                        break;
                        
                    default:
                        break;
                }
            }
        }
    }

    private void nettoyerTresorsCollectes() {
        tresorsConnus.removeIf(c -> 
            c == null || c.getObjet() == null || 
            !(c.getObjet() instanceof Tresor) || 
            ((Tresor) c.getObjet()).isCollecte()
        );
        
        // Annuler destination si trésor collecté
        if (destination != null) {
            if (destination.getObjet() == null || 
                !(destination.getObjet() instanceof Tresor) ||
                ((Tresor) destination.getObjet()).isCollecte()) {
                destination = null;
            }
        }
    }

    private void deciderAction() {
        //priorité secourir réactif proche
        Case reactifBlesse = trouverReactifBlesseProche();
        if (reactifBlesse != null) {
            int distReactif = calculerDistance(caseActuelle, reactifBlesse);
            int distTresor = destination != null ? calculerDistance(caseActuelle, destination) : Integer.MAX_VALUE;
            if (distReactif < distTresor) {
                destination = reactifBlesse;
                System.out.println("Cognitif " + id + ": Secours réactif à " + reactifBlesse);
            }
        }
        
        // PRIORITÉ 2: Aller vers un trésor connu
        if (destination == null && !tresorsConnus.isEmpty()) {
            destination = trouverTresorLePlusProche();
            if (destination != null) {
                System.out.println("Cognitif " + id + ": Cap vers trésor à " + destination);
            }
        }
        
        // PRIORITÉ 3: Explorer
        if (destination != null) {
            allerVersDestination();
        } else {
            explorerAleatoirement();
        }
        
        // Vérifier si arrivé à destination
        if (destination != null && caseActuelle == destination) {
            // Si c'était un réactif à secourir
            secourirReactifSurCase();
            destination = null;
            historiqueRecent.clear();
        }
    }

    private Case trouverReactifBlesseProche() {
        Zone zone = caseActuelle.getZone();
        Case plusProche = null;
        int minDist = Integer.MAX_VALUE;
        
        for (int x = 0; x < Zone.TAILLE; x++) {
            for (int y = 0; y < Zone.TAILLE; y++) {
                Case c = zone.getCase(x, y);
                if (c == null) continue;
                
                // création d'un copie de la liste résout le pb de l'exConcurrentModificationException
                List<Agent> agentsCopie = new ArrayList<>(c.getAgents());
                for (Agent agent : agentsCopie) {
                    if (agent instanceof AgentReactif && !agent.isAlive()) {
                        int dist = calculerDistance(caseActuelle, c);
                        if (dist < minDist) {
                            minDist = dist;
                            plusProche = c;
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

    private void allerVersDestination() {
        if (destination == null || caseActuelle == null) return;
        
        List<Case> adjacentes = getCasesAdjacentes();
        if (adjacentes.isEmpty()) {
            adjacentes = super.getCasesAdjacentes();
            if (adjacentes.isEmpty()) return;
        }
        
        // Éviter les cases récemment visitées
        List<Case> casesNonVisitees = new ArrayList<>();
        for (Case c : adjacentes) {
            if (!historiqueRecent.contains(c)) {
                casesNonVisitees.add(c);
            }
        }
        
        List<Case> casesAPrioriser = casesNonVisitees.isEmpty() ? adjacentes : casesNonVisitees;
        
        // Trouver la meilleure case
        Case meilleure = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (Case c : casesAPrioriser) {
            int dist = calculerDistance(c, destination);
            if (dist < minDistance) {
                minDistance = dist;
                meilleure = c;
            }
        }
        
        if (meilleure == null && !adjacentes.isEmpty()) {
            meilleure = adjacentes.get(random.nextInt(adjacentes.size()));
        }
        
        if (meilleure != null) {
            historiqueRecent.addLast(caseActuelle);
            if (historiqueRecent.size() > TAILLE_HISTORIQUE) {
                historiqueRecent.removeFirst();
            }
            deplacerVers(meilleure);
        }
    }

    private Case trouverTresorLePlusProche() {
        Case plusProche = null;
        int minDist = Integer.MAX_VALUE;
        
        for (Case c : tresorsConnus) {
            if (c == null) continue;
            int dist = calculerDistance(caseActuelle, c);
            if (dist < minDist) {
                minDist = dist;
                plusProche = c;
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

    private void surveillerStagnation(Case caseAvant) {
        if (destination == null) {
            stagnation = 0;
            return;
        }

        if (caseActuelle == caseAvant) {
            stagnation++;
        } else {
            stagnation = 0;
        }

        if (stagnation >= 6) { // boucle suspecte -> reset destination
            destination = null;
            historiqueRecent.clear();
            stagnation = 0;
            libererReservation();
            cibleCommunicantZoneId = null;
        }
    }

    @Override
    public void resetToQG() {
        libererReservation();
        cibleCommunicantZoneId = null;
        super.resetToQG();
    }
}
