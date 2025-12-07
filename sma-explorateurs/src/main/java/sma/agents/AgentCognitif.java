package sma.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private final Random random = new Random();

    private Case destination = null;
    private LinkedList<Case> cheminActuel = new LinkedList<>();

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

    //traiter messages communicant
    private void traiterMessages() {
        synchronized (this) {
            while (!messagesRecus.isEmpty()) {
                Message msg = messagesRecus.poll(); //défiler la fifo

                //si trésor trouvé=> dire que y a un trésor
                if (msg.getType() == Message.TypeMessage.TRESOR_TROUVE) {
                    if (msg.getPosition() != null && !tresorsConnus.contains(msg.getPosition())) {
                        tresorsConnus.add(msg.getPosition());
                        System.out.println("Cognitif " + id + ": Trésor signalé à " + msg.getPosition());
                    }
                } else if (msg.getType() == Message.TypeMessage.ANIMAL_DETECTE) {
                    if (msg.getPosition() != null) {
                        casesAEviter.add(msg.getPosition());
                        if (cheminActuel.contains(msg.getPosition())) {
                            cheminActuel.clear();
                        }
                    }
                }
            }
        }
    }

    private void nettoyerTresorsCollectes() {
        tresorsConnus.removeIf(c
                -> c == null || c.getObjet() == null
                || !(c.getObjet() instanceof Tresor)
                || ((Tresor) c.getObjet()).isCollecte()
        ); //traiter tous les cas de figure et nettoyer la liste chainées des trésor 

        if (destination != null && destination.getObjet() instanceof Tresor) {
            if (((Tresor) destination.getObjet()).isCollecte()) {
                destination = null;
                cheminActuel.clear();
            }
        }
    }

    private void deciderAction() {
        System.out.println("Cognitif " + id + "Zone " + this.getCaseActuelle().getZone() + "trésors connus = " + tresorsConnus.size()
                + ", destination = " + destination
                + ", chemin = " + cheminActuel.size());
        //priorité 1 : secourir blessé même zone
        Case agentBlesse = trouverAgentBlesseProche();
        if (agentBlesse != null && destination != agentBlesse) {
            destination = agentBlesse;
            cheminActuel = calculerCheminDijkstra(caseActuelle, destination);
            System.out.println("Cognitif " + id + ": Secours agent à " + destination);
        }

        //2 => aller vers trésor connu et pas encore collecté 
        if (agentBlesse == null && destination == null && !tresorsConnus.isEmpty()) {
            destination = trouverTresorLePlusProche();
            if (destination != null) {
                cheminActuel = calculerCheminDijkstra(caseActuelle, destination);
                System.out.println("Cognitif " + id + ": Cap vers trésor à " + destination);
            }
        }

        //explorer aléatoirement et suivre un chemin
        if (!cheminActuel.isEmpty()) {
            suivreChemin();
        } else if (destination != null) {
            //chemin actuel
            cheminActuel = calculerCheminDijkstra(caseActuelle, destination);
            if (!cheminActuel.isEmpty()) {
                suivreChemin(); // suivre le chemin 
            } else {
                explorerAleatoirement();
            }
        } else {
            explorerAleatoirement();
        }

        //arriver à destination
        if (destination != null && caseActuelle == destination) {
            secourirAgentsSurCase();
            destination = null;
            cheminActuel.clear();
        }
    }

    private void suivreChemin() {
        // fct interne qui va regarder d'abord si prochainee case du chemin est safe sinon => recalculer nouveau chemin
        if (cheminActuel.isEmpty()) {
            return;
        }

        Case prochaine = cheminActuel.peekFirst();

        if (prochaine != null && prochaine.isAccessible() && !casesAEviter.contains(prochaine)) {
            cheminActuel.pollFirst();
            deplacerVers(prochaine);
        } else {
            //obstacle => recalculer djikstra
            cheminActuel = calculerCheminDijkstra(caseActuelle, destination);
            if (!cheminActuel.isEmpty()) {
                deplacerVers(cheminActuel.pollFirst());
            }
        }
    }

    private void explorerAleatoirement() {
        // System.out.println("agent cognitif"+id+"explore aléatoirement");
        List<Case> adjacentes = getCasesAdjacentes();
        if (adjacentes.isEmpty()) {
            adjacentes = super.getCasesAdjacentes();
        }
        if (!adjacentes.isEmpty()) {
            deplacerVers(adjacentes.get(random.nextInt(adjacentes.size())));
        }
    }

    //Algo djikstra
    private LinkedList<Case> calculerCheminDijkstra(Case depart, Case arrivee) {
        if (depart == null || arrivee == null) {
            return new LinkedList<>();
        }

        Map<Case, Integer> distances = new HashMap<>();
        Map<Case, Case> parents = new HashMap<>();
        Set<Case> visites = new HashSet<>();
        List<Case> aTraiter = new ArrayList<>();

        distances.put(depart, 0);
        aTraiter.add(depart);

        while (!aTraiter.isEmpty()) {
            Case courante = null;
            int minDist = Integer.MAX_VALUE;
            for (Case c : aTraiter) {
                int d = distances.getOrDefault(c, Integer.MAX_VALUE);
                if (d < minDist) {
                    minDist = d;
                    courante = c;
                }
            }

            if (courante == null) {
                break;
            }

            aTraiter.remove(courante);
            visites.add(courante);

            if (courante == arrivee) {
                return reconstruireChemin(parents, arrivee);
            }

            int distCourante = distances.get(courante);
            for (Case voisin : getVoisinsAccessibles(courante)) {
                if (visites.contains(voisin) || casesAEviter.contains(voisin)) {
                    continue;
                }

                int nouvelleDist = distCourante + 1;
                if (nouvelleDist < distances.getOrDefault(voisin, Integer.MAX_VALUE)) {
                    distances.put(voisin, nouvelleDist);
                    parents.put(voisin, courante);
                    if (!aTraiter.contains(voisin)) {
                        aTraiter.add(voisin);
                    }
                }
            }
        }

        return new LinkedList<>();
    }

    private LinkedList<Case> reconstruireChemin(Map<Case, Case> parents, Case arrivee) {
        LinkedList<Case> chemin = new LinkedList<>();
        Case courant = arrivee;

        while (parents.containsKey(courant)) {
            chemin.addFirst(courant);
            courant = parents.get(courant);
        }
        return chemin;
    }

    private List<Case> getVoisinsAccessibles(Case c) {
        List<Case> voisins = new ArrayList<>();
        if (c == null || c.getZone() == null) {
            return voisins;
        }

        Zone zone = c.getZone();
        int x = c.getX();
        int y = c.getY();

        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];

            if (nx >= 0 && nx < Zone.TAILLE && ny >= 0 && ny < Zone.TAILLE) {
                Case voisin = zone.getCase(nx, ny);
                if (voisin != null && voisin.isAccessible()) {
                    voisins.add(voisin);
                }
            } else {
                // Entre zones
                Case voisin = getCaseEntreZones(c, d[0], d[1]);
                if (voisin != null && voisin.isAccessible()) {
                    voisins.add(voisin);
                }
            }
        }
        return voisins;
    }

    private Case getCaseEntreZones(Case c, int dx, int dy) {
        Zone zone = c.getZone();
        int zx = zone.getZoneX();
        int zy = zone.getZoneY();
        int cx = c.getX() + dx;
        int cy = c.getY() + dy;

        if (cx < 0) {
            zx--;
            cx = Zone.TAILLE - 1;
        } else if (cx >= Zone.TAILLE) {
            zx++;
            cx = 0;
        }

        if (cy < 0) {
            zy--;
            cy = Zone.TAILLE - 1;
        } else if (cy >= Zone.TAILLE) {
            zy++;
            cy = 0;
        }

        if (zx < 0 || zx >= Carte.NB_ZONES_COTE || zy < 0 || zy >= Carte.NB_ZONES_COTE) {
            return null;
        }

        Zone zoneAdj = carte.getZone(zx, zy);
        return zoneAdj != null ? zoneAdj.getCase(cx, cy) : null;
    }

    private Case trouverAgentBlesseProche() {
        if (caseActuelle == null || caseActuelle.getZone() == null) {
            return null;
        }

        Zone zone = caseActuelle.getZone();
        Case plusProche = null;
        int minDist = Integer.MAX_VALUE;

        for (int x = 0; x < Zone.TAILLE; x++) {
            for (int y = 0; y < Zone.TAILLE; y++) {
                Case c = zone.getCase(x, y);
                if (c == null) {
                    continue;
                }

                for (Agent agent : new ArrayList<>(c.getAgents())) {
                    if (agent != null && agent != this && !agent.isAlive()) {
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

    private void secourirAgentsSurCase() {
        for (Agent agent : new ArrayList<>(caseActuelle.getAgents())) {
            if (agent != null && agent != this && !agent.isAlive()) {
                agent.resetToQG();
                System.out.println("Cognitif " + id + ": " + agent.getType() + " " + agent.getId() + " secouru !");
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

    private Case trouverTresorLePlusProche() {
        Case plusProche = null;
        int minDist = Integer.MAX_VALUE;

        for (Case c : tresorsConnus) {
            if (c == null) {
                continue;
            }
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

        Zone za = a.getZone();
        Zone zb = b.getZone();

        int ax = za.getZoneX() * Zone.TAILLE + a.getX();
        int ay = za.getZoneY() * Zone.TAILLE + a.getY();
        int bx = zb.getZoneX() * Zone.TAILLE + b.getX();
        int by = zb.getZoneY() * Zone.TAILLE + b.getY();

        return Math.abs(ax - bx) + Math.abs(ay - by);
    }
}
