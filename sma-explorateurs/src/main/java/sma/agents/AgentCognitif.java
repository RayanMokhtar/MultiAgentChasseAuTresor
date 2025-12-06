package sma.agents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import sma.environnement.Carte;
import sma.environnement.Case;
import sma.environnement.Zone;
import sma.objets.Tresor;

public class AgentCognitif extends Agent {
    
    private final Queue<Message> messagesRecus = new LinkedList<>();
    private final List<Case> tresorsConnus = new ArrayList<>();
    private final Set<Case> casesAEviter = new HashSet<>();
    private Case destination = null;
    private final Random random = new Random();
    
    private final LinkedList<Case> historiqueRecent = new LinkedList<>();
    private static final int TAILLE_HISTORIQUE = 5;

    public AgentCognitif(Case positionInitiale, Carte carte) {
        super(TypologieAgent.COGNITIF, positionInitiale, carte);
    }

    public synchronized void recevoirMessage(Message message) {
        messagesRecus.add(message);
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
                    }
                }
            }
        }
        return plusProche;
    }

    private void secourirReactifSurCase() {
        List<Agent> agentsCopie = new ArrayList<>(caseActuelle.getAgents());
        for (Agent agent : agentsCopie) {
            if (agent instanceof AgentReactif && !agent.isAlive()) {
                agent.resetToQG(); // 
                System.out.println("Cognitif " + id + ": Réactif " + agent.getId() + " secouru !");
                stats.incrementerSecours();
            }
        }
    }

    @Override
    public List<Case> getCasesAdjacentes() {
        List<Case> adjacentes = super.getCasesAdjacentes();
        List<Case> filtrees = new ArrayList<>();
        for (Case c : adjacentes) {
            if (!casesAEviter.contains(c)) {
                filtrees.add(c);
            }
        }
        return filtrees;
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
        }
        return plusProche;
    }

    private int calculerDistance(Case a, Case b) {
        if (a == null || b == null || a.getZone() == null || b.getZone() == null) {
            return Integer.MAX_VALUE;
        }
        
        Zone zoneA = a.getZone();
        Zone zoneB = b.getZone();
        
        if (zoneA == zoneB) {
            return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
        }
        
        int absXa = zoneA.getZoneX() * Zone.TAILLE + a.getX();
        int absYa = zoneA.getZoneY() * Zone.TAILLE + a.getY();
        int absXb = zoneB.getZoneX() * Zone.TAILLE + b.getX();
        int absYb = zoneB.getZoneY() * Zone.TAILLE + b.getY();
        
        return Math.abs(absXa - absXb) + Math.abs(absYa - absYb);
    }

    private void explorerAleatoirement() {
        List<Case> adjacentes = getCasesAdjacentes();
        if (adjacentes.isEmpty()) {
            adjacentes = super.getCasesAdjacentes();
        }
        
        if (!adjacentes.isEmpty()) {
            Case dest = adjacentes.get(random.nextInt(adjacentes.size()));
            deplacerVers(dest);
        }
    }
}