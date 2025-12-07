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
    
    //static pour partager entre les objets les zones occupées => considérons le static comme le QG ... , les zones visitées doivent changer à chaque fois
    private static final Set<Integer> zonesOccupees = new HashSet<>();
    
    private final Set<Integer> zonesVisitees = new HashSet<>();
    private int zoneActuelleId = -1;
    
    public AgentCommunicant(Case positionInitiale, Carte carte) {
        super(TypologieAgent.COMMUNICANT, positionInitiale, carte);
        
        if (positionInitiale != null && positionInitiale.getZone() != null) {
            zoneActuelleId = positionInitiale.getZone().getId();
            zonesVisitees.add(zoneActuelleId);
            synchronized (zonesOccupees) {
                zonesOccupees.add(zoneActuelleId);
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
                
                if (objet instanceof Tresor && !((Tresor) objet).isCollecte()) {
                    envoyerAuxCognitifsDansZone(Message.TypeMessage.TRESOR_TROUVE, c, zone);
                }
                else if (objet instanceof Animal) {
                    envoyerAuxCognitifsDansZone(Message.TypeMessage.ANIMAL_DETECTE, c, zone);
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
                
                List<Agent> agentsCopie = new ArrayList<>(agentsSurCase);
                for (Agent a : agentsCopie) {
                    if (a instanceof AgentCognitif) {
                        ((AgentCognitif) a).recevoirMessage(msg);
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
        synchronized (zonesOccupees) {
            // Libérer la zone actuelle
            if (zoneActuelleId >= 0) {
                zonesOccupees.remove(zoneActuelleId);
            }
            
            // Chercher une zone non occupée par un autre communicant
            for (int zx = 0; zx < Carte.NB_ZONES_COTE; zx++) {
                for (int zy = 0; zy < Carte.NB_ZONES_COTE; zy++) {
                    if (zx == 0 && zy == 0) {
                        continue; 
                    }
                    Zone zone = carte.getZone(zx, zy);
                    if (zone == null) {
                        continue;
                        }
                    int zoneId = zone.getId();
                    
                    //vérifier si la zone n'est pas occupée et nombre de trésors restants dans la zone > = 
                    if (!zonesOccupees.contains(zoneId) && compterTresorsRestants(zone) > 0) {
                        Case caseSafe = trouverCaseSafe(zone);
                        if (caseSafe != null) {
                            caseActuelle = caseSafe;
                            zoneActuelleId = zoneId;
                            zonesVisitees.add(zoneId);
                            zonesOccupees.add(zoneId);
                            System.out.println("Communicant " + id + ": Téléporté vers Zone " + zoneId + " (libre)");
                            return;
                        }
                    }
                }
            }
            
            // Si toutes les zones libres sont vides, chercher une zone avec trésors même occupée
            for (int zx = 0; zx < Carte.NB_ZONES_COTE; zx++) {
                for (int zy = 0; zy < Carte.NB_ZONES_COTE; zy++) {
                    if (zx == 0 && zy == 0) continue;
                    
                    Zone zone = carte.getZone(zx, zy);
                    if (zone == null) continue;
                    
                    if (compterTresorsRestants(zone) > 0) {
                        Case caseSafe = trouverCaseSafe(zone);
                        if (caseSafe != null) {
                            caseActuelle = caseSafe;
                            zoneActuelleId = zone.getId();
                            zonesOccupees.add(zoneActuelleId);
                            System.out.println("Communicant " + id + ": Téléporté vers Zone " + zoneActuelleId + " (fallback)");
                            return;
                        }
                    }
                }
            }
            
            System.out.println("Communicant " + id + ": Aucune zone avec trésors disponible");
        }
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