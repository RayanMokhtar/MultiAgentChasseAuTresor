package sma.agents;

import java.util.ArrayList;
import java.util.List;

import sma.environnement.Carte;
import sma.environnement.Case;
import sma.environnement.Zone;
import sma.objets.Tresor;

public abstract class Agent {

    private static int compteurId = 0;
    protected final int id;
    protected final TypologieAgent type;
    protected int pvMax = 100;
    protected int pv = 100;
    protected Case caseActuelle;
    protected boolean enVie = true;
    protected Carte carte; // Référence à la carte pour le passage entre zones

    protected final AgentStats stats;
    protected final List<Tresor> tresorsCollectes = new ArrayList<>(); //à vérifier

    public Agent(TypologieAgent type, Case positionInitiale, Carte carte) {
        compteurId++;
        this.id = compteurId;
        this.type = type;
        this.caseActuelle = positionInitiale;
        this.carte = carte;
        this.stats = new AgentStats();

        if (positionInitiale != null) {
            positionInitiale.ajouterAgent(this);
        }
    }

    public int getId() {
        return id;
    }

    public TypologieAgent getType() {
        return type;
    }

    public int getPv() {
        return pv;
    }

    public int getPvMax() {
        return pvMax;
    }

    public boolean isAlive() {
        return enVie;
    }

    public Case getCaseActuelle() {
        return caseActuelle;
    }

    public Zone getZoneActuelle() {
        return caseActuelle != null ? caseActuelle.getZone() : null;
    }

    public AgentStats getStats() {
        return stats;
    }

    public List<Tresor> getTresorsCollectes() {
        return tresorsCollectes;
    }

    //cases adjacentes 
    public List<Case> getCasesAdjacentes() {
        List<Case> adjacentes = new ArrayList<>();

        if (caseActuelle == null || carte == null) {
            return adjacentes;
        }

        int x = caseActuelle.getX();
        int y = caseActuelle.getY();
        Zone zone = caseActuelle.getZone();

        //pas déplacement en diagonale haut, bas, gauche, droite
        int[] horizontale = {0, 0, -1, 1};
        int[] verticale = {-1, 1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int newX = x + horizontale[i];
            int newY = y + verticale[i];

            // Dans la même zone
            if (zone.estDansLimites(newX, newY)) {
                Case c = zone.getCase(newX, newY);
                if (c != null && c.isAccessible()) {
                    adjacentes.add(c);
                }
            } //passage d'une zone à une autre => bords  avec les margins pour bien distinguer => à ajuster dans ihm
            else {
                Case caseAutreZone = getCaseZoneAdjacente(zone, x, y, horizontale[i], verticale[i]);
                if (caseAutreZone != null && caseAutreZone.isAccessible()) {
                    adjacentes.add(caseAutreZone);
                }
            }
        }

        return adjacentes;
    }

    private Case getCaseZoneAdjacente(Zone zoneActuelle, int x, int y, int dx, int dy) {
        int newZoneX = zoneActuelle.getZoneX();
        int newZoneY = zoneActuelle.getZoneY();
        int newCaseX = x;
        int newCaseY = y;
        if (dx == -1 && x == 0) {
            newZoneX--;
            newCaseX = Zone.TAILLE - 1;
        } else if (dx == 1 && x == Zone.TAILLE - 1) {
            newZoneX++;
            newCaseX = 0;
        } else if (dy == -1 && y == 0) {
            newZoneY--;
            newCaseY = Zone.TAILLE - 1;
        } else if (dy == 1 && y == Zone.TAILLE - 1) {

            newZoneY++;
            newCaseY = 0;
        } else {
            return null; // Pas au bord
        }

        // Récupérer la zone adjacente
        Zone zoneAdjacente = carte.getZone(newZoneX, newZoneY);
        if (zoneAdjacente == null) {
            return null;
        }

        return zoneAdjacente.getCase(newCaseX, newCaseY);
    }

    //changer case que si accessible (obstacle seulement poru rendre choses intéressantes )
    public boolean deplacerVers(Case destination) {
        if (destination == null || !destination.isAccessible()) {
            return false;
        }

        if (caseActuelle != null) {
            caseActuelle.retirerAgent(this);
        }

        caseActuelle = destination;
        destination.ajouterAgent(this);
        stats.incrementerCasesVisitees();

        return true;
    }

    public void seFaireAttaquer(int dmg) {
        pv -= dmg;
        stats.ajouterDegats(dmg);
        System.out.println("Agent " + id + " (" + type + "): s'est pris des dégats de" + dmg + " il reste " + pv + "/" + pvMax);
        if (pv <= 0) {
            pv = 0;
            enVie = false;
            stats.incrementerMorts();
            System.out.println(" Agent " + id + " (" + type + "): est mort à " + caseActuelle + "");

        }
    }

    public void collectTresor(Tresor t) {
        tresorsCollectes.add(t);
        stats.ajouterTresor(t.getValeur());
    }

    public void resetToQG() {
        if (caseActuelle != null) {
            caseActuelle.retirerAgent(this);
        }

        Case qg = carte.getCaseQG();
        this.caseActuelle = qg;
        qg.ajouterAgent(this);

        this.pv = pvMax;
        this.enVie = true;
        stats.incrementerRespawn();
    }

    public void resetToCaseActuelle(Case caseActuelle) {
        if (caseActuelle != null) {
            caseActuelle.retirerAgent(this);
        }

        
        this.caseActuelle = caseActuelle;
        caseActuelle.ajouterAgent(this);
        this.pv = pvMax;
        this.enVie = true;
        stats.incrementerRespawn();
    }
    public abstract void step();
}
