package sma.agents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sma.environnement.Carte;
import sma.environnement.Case;
import sma.environnement.Zone;
import sma.objets.Animal;
import sma.objets.ObjetPassif;
import sma.objets.Tresor;

public class AgentCommunicant extends Agent {
    // Ensemble partagé pour empêcher plusieurs communicants d'occuper la même zone.
    private static final Set<Integer> zonesOccupees = new HashSet<>();

    public static Set<Integer> getZonesOccupeesSnapshot() {
        synchronized (zonesOccupees) {
            return new HashSet<>(zonesOccupees);
        }
    }

    private final Set<Integer> zonesVisitees = new HashSet<>();
    private final Set<Case> tresorsDejaSignales = new HashSet<>();
    private final Set<Case> animauxDejaSignales = new HashSet<>();
    // private int maxTpAutorisees = 2;
    public AgentCommunicant(Case positionInitiale, Carte carte) {
        super(TypologieAgent.COMMUNICANT, positionInitiale, carte);
        
        if (positionInitiale != null && positionInitiale.getZone() != null) {
            zonesVisitees.add(positionInitiale.getZone().getId());
            synchronized (zonesOccupees) {
                zonesOccupees.add(positionInitiale.getZone().getId());
            }
        }
    }

    @Override
    public void seFaireAttaquer(int dmg) {
        //immuned
    }

    @Override
    public void step() {
        Zone zoneActuelle = caseActuelle.getZone();
        
        // 1. Scanner et informer les cognitifs présents dans cette zone
        scannerEtInformer(zoneActuelle);
        
        // 2. Si plus de trésors dans la zone, se téléporter ailleurs
        if (compterTresorsRestants(zoneActuelle) == 0) {
            System.out.println("Communicant " + id + ": Zone " + zoneActuelle.getId() + " vidée, téléportation...");
            teleporterVersNouvelleZone();
        }
    }

    private void scannerEtInformer(Zone zone) {
        for (int x = 0; x < Zone.TAILLE; x++) {
            for (int y = 0; y < Zone.TAILLE; y++) {
                Case c = zone.getCase(x, y);
                if (c == null) continue;
                
                ObjetPassif objet = c.getObjet();
                if (objet == null) continue;
                
                // Trésor non collecté et pas encore signalé
                if (objet instanceof Tresor && !((Tresor) objet).isCollecte() && !tresorsDejaSignales.contains(c)) {
                    envoyerAuxCognitifsDansZone(Message.TypeMessage.TRESOR_TROUVE, c, zone);
                    tresorsDejaSignales.add(c);
                }
                // Animal pas encore signalé
                else if (objet instanceof Animal && !animauxDejaSignales.contains(c)) {
                    envoyerAuxCognitifsDansZone(Message.TypeMessage.ANIMAL_DETECTE, c, zone);
                    animauxDejaSignales.add(c);
                }
            }
        }
    }

   
    private void envoyerAuxCognitifsDansZone(Message.TypeMessage type, Case position, Zone zone) {
        Message msg = new Message(this.id, type, position, zone.getId());
        
        for (int x = 0; x < Zone.TAILLE; x++) {
            for (int y = 0; y < Zone.TAILLE; y++) {
                Case caseRecupere = zone.getCase(x, y);
                if (caseRecupere == null) continue;
                
                List<Agent> agentsSurCase = caseRecupere.getAgents();
                if (agentsSurCase == null || agentsSurCase.isEmpty()) continue;
                
                //Créer une copie pour éviter cette erreur 
                List<Agent> agentsCopie = new ArrayList<>(agentsSurCase);
                for (Agent a : agentsCopie) {
                    if (a instanceof AgentCognitif) {
                        AgentCognitif cog = (AgentCognitif) a;
                        cog.recevoirMessage(msg);
                    }
                }
            }
        }
    }

    private int compterTresorsRestants(Zone zone) {
        int count = 0;
        for (int x = 0; x < Zone.TAILLE; x++) {
            for (int y = 0; y < Zone.TAILLE; y++) {
                Case c = zone.getCase(x, y);
                if (c != null && c.getObjet() instanceof Tresor) {
                    if (!((Tresor) c.getObjet()).isCollecte()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void teleporterVersNouvelleZone() {
        Zone ancienneZone = caseActuelle.getZone();
        // Chercher une zone non visitée avec des trésors
        for (int zx = 0; zx < Carte.NB_ZONES_COTE; zx++) {
            for (int zy = 0; zy < Carte.NB_ZONES_COTE; zy++) {
                if (zx == 0 && zy == 0) continue; // Skip QG
                Zone zone = carte.getZone(zx, zy);
                if (zone != null && !zonesVisitees.contains(zone.getId()) && compterTresorsRestants(zone) > 0) {
                    boolean zoneLibre;
                    synchronized (zonesOccupees) {
                        zoneLibre = !zonesOccupees.contains(zone.getId());
                        if (zoneLibre) {
                            zonesOccupees.remove(ancienneZone.getId());
                            zonesOccupees.add(zone.getId());
                        }
                    }
                    if (!zoneLibre) {
                        continue;
                    }

                    Case caseSafe = trouverCaseSafe(zone);
                    if (caseSafe != null) {
                        caseActuelle = caseSafe;
                        zonesVisitees.add(zone.getId());
                        tresorsDejaSignales.clear();
                        animauxDejaSignales.clear();
                        System.out.println("Communicant " + id + ": Téléporté vers Zone " + zone.getId() + " (nouvelle, libre)");
                        return;
                    } else {
                        // Si aucune case safe, libérer la réservation de zone.
                        synchronized (zonesOccupees) {
                            zonesOccupees.remove(zone.getId());
                            zonesOccupees.add(ancienneZone.getId());
                        }
                    }
                }
            }
        }
        
        // Toutes les zones visitées -> reset
        System.out.println("Communicant " + id + ": Toutes zones visitées, reset");
        zonesVisitees.clear();
        tresorsDejaSignales.clear();
        animauxDejaSignales.clear();
    }

    private Case trouverCaseSafe(Zone zone) {
        for (int x = 0; x < Zone.TAILLE; x++) {
            for (int y = 0; y < Zone.TAILLE; y++) {
                Case c = zone.getCase(x, y);
                if (c != null && c.isAccessible() && !(c.getObjet() instanceof Animal)) {
                    return c;
                }
            }
        }
        return null;
    }
}