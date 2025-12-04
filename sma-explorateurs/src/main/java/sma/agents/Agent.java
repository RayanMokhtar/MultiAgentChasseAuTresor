package sma.agents;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import sma.environnement.*;
import sma.objets.*;
import sma.simulation.Simulation;


public abstract class Agent implements Runnable {
    
    // Identifiant unique généré automatiquement
    private static final AtomicInteger compteurId = new AtomicInteger(0);
    
    protected final int id;
    protected final String nom;
    protected volatile Position position;
    protected volatile int pointsDeVie;
    protected final int pointsDeVieMax;
    protected final int force;
    protected volatile int energie;
    protected final int energieMax;
    protected final AtomicBoolean enVie;
    protected final AtomicBoolean actif;
    
    protected final List<Tresor> tresorsCollectes;
    protected final Carte carte;
    protected volatile Zone zoneActuelle;
    protected Simulation simulation;
    
    // Statistiques thread-safe
    protected final AtomicInteger nombreCombats = new AtomicInteger(0);
    protected final AtomicInteger nombreVictoires = new AtomicInteger(0);
    protected final AtomicInteger nombreMorts = new AtomicInteger(0);
    protected final AtomicInteger distanceParcourue = new AtomicInteger(0);
    protected final AtomicInteger scoreTotal = new AtomicInteger(0);
    
    // Contrôle du thread
    protected volatile boolean running = false;
    protected final Object lockMouvement = new Object();
    protected static final int DELAI_ACTION = 100; // ms entre chaque action

    public Agent(String nom, Carte carte, int pointsDeVieMax, int force, int energieMax) {
        this.id = compteurId.incrementAndGet();
        this.nom = nom;
        this.carte = carte;
        this.pointsDeVieMax = pointsDeVieMax;
        this.pointsDeVie = pointsDeVieMax;
        this.force = force;
        this.energieMax = energieMax;
        this.energie = energieMax;
        this.enVie = new AtomicBoolean(true);
        this.actif = new AtomicBoolean(true);
        this.tresorsCollectes = Collections.synchronizedList(new ArrayList<>());
        
        // Position initiale au QG
        this.position = carte.getQG().getCentre().copy();
        this.zoneActuelle = carte.getQG();
        carte.getQG().ajouterAgent(this);
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    @Override
    public void run() {
        running = true;
        System.out.println("🚀 " + nom + " démarre son exploration!");
        
        while (running && actif.get()) {
            try {
                if (!enVie.get()) {
                    // Attendre avant réapparition
                    Thread.sleep(2000);
                    reapparaitre();
                } else {
                    agir();
                }
                Thread.sleep(DELAI_ACTION);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("🛑 " + nom + " arrête son exploration.");
    }

    /**
     * Méthode principale d'action - à implémenter par les sous-classes
     */
    public abstract void agir();

    /**
     * Retourne le type d'agent
     */
    public abstract String getTypeAgent();

    /**
     * Retourne la couleur de l'agent pour l'affichage
     */
    public abstract Color getCouleur();

    /**
     * Déplace l'agent vers une nouvelle position
     */
    public synchronized boolean deplacer(Position nouvellePosition) {
        if (!enVie.get() || energie <= 0) return false;
        
        synchronized (lockMouvement) {
            if (carte.positionAccessible(nouvellePosition)) {
                // Mise à jour de la zone
                Zone ancienneZone = zoneActuelle;
                Zone nouvelleZone = carte.getZoneAt(nouvellePosition);
                
                if (ancienneZone != nouvelleZone) {
                    ancienneZone.retirerAgent(this);
                    nouvelleZone.ajouterAgent(this);
                    zoneActuelle = nouvelleZone;
                }
                
                // Calcul distance
                int dist = (int) position.distanceTo(nouvellePosition);
                distanceParcourue.addAndGet(dist);
                
                // Déplacement
                position = nouvellePosition;
                energie--;
                
                return true;
            }
        }
        return false;
    }

    /**
     * Déplace l'agent d'un pas vers une destination
     */
    public boolean deplacerVers(Position destination) {
        if (destination == null) return false;
        
        int dx = Integer.compare(destination.getX(), position.getX());
        int dy = Integer.compare(destination.getY(), position.getY());
        
        // Mouvement de plusieurs pixels à la fois
        int vitesse = 5;
        Position nouvellePos = new Position(
            position.getX() + dx * vitesse,
            position.getY() + dy * vitesse
        );
        
        return deplacer(nouvellePos);
    }

    /**
     * Collecte un trésor (thread-safe)
     */
    public boolean collecterTresor(Tresor tresor) {
        if (!enVie.get() || tresor == null) return false;
        
        synchronized (tresor) {
            if (tresor.collecter()) {
                tresorsCollectes.add(tresor);
                scoreTotal.addAndGet(tresor.getValeur());
                
                if (simulation != null) {
                    simulation.getStats().enregistrerTresorCollecte(tresor.getValeur());
                }
                
                System.out.println("💰 " + nom + " a collecté un trésor de valeur " + tresor.getValeur() + "!");
                return true;
            }
        }
        return false;
    }

    /**
     * Combat un animal (thread-safe)
     */
    public synchronized boolean combattre(Animal animal) {
        if (!enVie.get() || animal == null || !animal.isActif()) return true;
        
        nombreCombats.incrementAndGet();
        if (simulation != null) {
            simulation.getStats().enregistrerCombat();
        }
        
        System.out.println("⚔️ " + nom + " combat " + animal.getTypeAnimal().name() + "!");
        
        // Combat simplifié
        int degatsInfliges = this.force + (int)(Math.random() * 10);
        int degatsRecus = animal.attaquer();
        
        animal.recevoirDegats(degatsInfliges);
        recevoirDegats(degatsRecus);
        
        if (!animal.isActif()) {
            nombreVictoires.incrementAndGet();
            System.out.println("🏆 " + nom + " a vaincu " + animal.getTypeAnimal().name() + "!");
            return true;
        }
        
        return false;
    }

    /**
     * Reçoit des dégâts (thread-safe)
     */
    public synchronized void recevoirDegats(int degats) {
        this.pointsDeVie -= degats;
        System.out.println("💔 " + nom + " reçoit " + degats + " dégâts (PV: " + pointsDeVie + "/" + pointsDeVieMax + ")");
        
        if (this.pointsDeVie <= 0) {
            mourir();
        }
    }

    /**
     * Gère la mort de l'agent
     */
    public void mourir() {
        enVie.set(false);
        nombreMorts.incrementAndGet();
        if (simulation != null) {
            simulation.getStats().enregistrerMort();
        }
        System.out.println("💀 " + nom + " a été vaincu!");
    }

    /**
     * Réapparaît au QG
     */
    public void reapparaitre() {
        synchronized (lockMouvement) {
            Zone ancienneZone = zoneActuelle;
            if (ancienneZone != null) {
                ancienneZone.retirerAgent(this);
            }
            
            this.position = carte.getQG().getCentre().copy();
            this.zoneActuelle = carte.getQG();
            carte.getQG().ajouterAgent(this);
            this.pointsDeVie = pointsDeVieMax;
            this.energie = energieMax;
            this.enVie.set(true);
            
            System.out.println("🔄 " + nom + " réapparaît au QG!");
        }
    }

    /**
     * Téléporte l'agent au QG (pour évacuation d'urgence)
     */
    public void teleporterAuQG() {
        synchronized (lockMouvement) {
            Zone ancienneZone = zoneActuelle;
            if (ancienneZone != null) {
                ancienneZone.retirerAgent(this);
            }
            
            this.position = carte.getQG().getCentre().copy();
            this.zoneActuelle = carte.getQG();
            carte.getQG().ajouterAgent(this);
            
            System.out.println("✨ " + nom + " est téléporté au QG!");
        }
    }

    /**
     * Récupère de l'énergie et des PV au QG
     */
    public void reposer() {
        if (zoneActuelle != null && zoneActuelle.estQG()) {
            this.energie = Math.min(energie + 20, energieMax);
            this.pointsDeVie = Math.min(pointsDeVie + 30, pointsDeVieMax);
        }
    }

    /**
     * Vérifie si l'agent est en danger (PV faibles)
     */
    public boolean estEnDanger() {
        return pointsDeVie < pointsDeVieMax * 0.3;
    }

    /**
     * Vérifie si l'agent a besoin d'aide
     */
    public boolean aBesoinAide() {
        return estEnDanger() && !zoneActuelle.estQG();
    }

    // Méthodes de contrôle du thread
    public void arreter() {
        running = false;
        actif.set(false);
    }

    public void suspendre() {
        actif.set(false);
    }

    public void reprendre() {
        actif.set(true);
    }

    // Getters (thread-safe)
    public int getId() { return id; }
    public String getNom() { return nom; }
    public synchronized Position getPosition() { return position.copy(); }
    public synchronized int getPointsDeVie() { return pointsDeVie; }
    public int getPointsDeVieMax() { return pointsDeVieMax; }
    public int getForce() { return force; }
    public synchronized int getEnergie() { return energie; }
    public int getEnergieMax() { return energieMax; }
    public boolean isEnVie() { return enVie.get(); }
    public boolean isActif() { return actif.get(); }
    public List<Tresor> getTresorsCollectes() { return new ArrayList<>(tresorsCollectes); }
    public Zone getZoneActuelle() { return zoneActuelle; }
    public Carte getCarte() { return carte; }
    
    // Statistiques
    public int getNombreCombats() { return nombreCombats.get(); }
    public int getNombreVictoires() { return nombreVictoires.get(); }
    public int getNombreMorts() { return nombreMorts.get(); }
    public int getDistanceParcourue() { return distanceParcourue.get(); }
    public int getScoreTotal() { return scoreTotal.get(); }

    @Override
    public String toString() {
        return getTypeAgent() + " " + nom + " [PV: " + pointsDeVie + "/" + pointsDeVieMax + 
               ", ⚡: " + energie + "/" + energieMax + "] à " + position;
    }
}
