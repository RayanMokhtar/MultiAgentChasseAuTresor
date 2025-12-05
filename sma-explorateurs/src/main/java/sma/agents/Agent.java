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
    protected int force;
    protected final AtomicBoolean enVie;
    protected final AtomicBoolean actif;
    
    protected final List<Tresor> tresorsCollectes;
    protected final Carte carte;
    protected volatile Zone zoneActuelle;
    protected Simulation simulation;
    
    // FUSIL - Item spécial
    protected volatile Fusil fusil;
    protected volatile boolean aUnFusil;
    
    // Système de blessure
    protected volatile boolean blesse;
    protected volatile Position positionBlessure;
    protected volatile long tempsBlessure;
    public static final long TEMPS_ATTENTE_BLESSURE = 10000; // 10 secondes
    
    // Statistiques thread-safe
    protected final AtomicInteger nombreCombats = new AtomicInteger(0);
    protected final AtomicInteger nombreVictoires = new AtomicInteger(0);
    protected final AtomicInteger nombreMorts = new AtomicInteger(0);
    protected final AtomicInteger distanceParcourue = new AtomicInteger(0);
    protected final AtomicInteger animauxTues = new AtomicInteger(0);
    
    // Contrôle du thread
    protected volatile boolean running = false;
    protected final Object lockMouvement = new Object();
    protected static final int DELAI_ACTION = 100; // ms entre chaque action

    public Agent(String nom, Carte carte, int pointsDeVieMax, int force) {
        this.id = compteurId.incrementAndGet();
        this.nom = nom;
        this.carte = carte;
        this.pointsDeVieMax = pointsDeVieMax;
        this.pointsDeVie = pointsDeVieMax;
        this.force = force;
        this.enVie = new AtomicBoolean(true);
        this.actif = new AtomicBoolean(true);
        this.tresorsCollectes = Collections.synchronizedList(new ArrayList<>());
        
        // Fusil
        this.fusil = null;
        this.aUnFusil = false;
        
        // Système de blessure
        this.blesse = false;
        this.positionBlessure = null;
        this.tempsBlessure = 0;
        
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
                if (blesse) {
                    // Agent blessé - attend le secours
                    gererBlessure();
                } else if (!enVie.get()) {
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
     * Gère le système de blessure :
     * - Attend 10 secondes
     * - Si un agent passe dessus, respawn sur place
     * - Sinon respawn au QG
     */
    protected void gererBlessure() {
        long tempsEcoule = System.currentTimeMillis() - tempsBlessure;
        
        if (tempsEcoule >= TEMPS_ATTENTE_BLESSURE) {
            // Temps écoulé, respawn au QG
            System.out.println("⏰ " + nom + " n'a pas été secouru, retour au QG!");
            blesse = false;
            reapparaitre();
        }
        // Sinon, continue d'attendre (un autre agent peut le secourir)
    }
    
    /**
     * Vérifie si un agent blessé est à proximité pour le secourir
     */
    public boolean peutSecourir(Agent agentBlesse) {
        if (!enVie.get() || blesse) return false;
        return position.distanceTo(agentBlesse.getPosition()) <= 20;
    }
    
    /**
     * Secourt un agent blessé - le fait respawn sur place
     */
    public void secourir(Agent agentBlesse) {
        if (agentBlesse.blesse && peutSecourir(agentBlesse)) {
            agentBlesse.respawnSurPlace();
            System.out.println("🚑 " + nom + " a secouru " + agentBlesse.getNom() + "!");
        }
    }
    
    /**
     * Respawn sur place (secouru par un autre agent)
     */
    public void respawnSurPlace() {
        synchronized (lockMouvement) {
            this.blesse = false;
            this.pointsDeVie = pointsDeVieMax;
            this.enVie.set(true);
            System.out.println("💚 " + nom + " a été secouru sur place à " + position + " et reprend l'exploration!");
        }
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
        if (!enVie.get()) return false;
        
        synchronized (lockMouvement) {
            // Clamper la position aux bords de la carte
            int x = Math.max(5, Math.min(nouvellePosition.getX(), carte.getLargeur() - 5));
            int y = Math.max(5, Math.min(nouvellePosition.getY(), carte.getHauteur() - 5));
            Position positionClampee = new Position(x, y);
            
            if (carte.positionAccessible(positionClampee)) {
                // Mise à jour de la zone
                Zone ancienneZone = zoneActuelle;
                Zone nouvelleZone = carte.getZoneAt(positionClampee);
                
                // Protection contre les zones null
                if (nouvelleZone == null) {
                    return false;
                }
                
                if (ancienneZone != nouvelleZone) {
                    if (ancienneZone != null) {
                        ancienneZone.retirerAgent(this);
                    }
                    nouvelleZone.ajouterAgent(this);
                    zoneActuelle = nouvelleZone;
                }
                
                // Calcul distance
                int dist = (int) position.distanceTo(positionClampee);
                distanceParcourue.addAndGet(dist);
                
                // Déplacement - utiliser une copie pour éviter les problèmes de référence
                this.position = new Position(positionClampee.getX(), positionClampee.getY());
                
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
     * Collecte un trésor (thread-safe, non-bloquant)
     */
    public boolean collecterTresor(Tresor tresor) {
        if (!enVie.get() || tresor == null) return false;
        
        synchronized (tresor) {
            if (tresor.collecter()) {
                tresorsCollectes.add(tresor);
                if (simulation != null) {
                    simulation.getStats().enregistrerTresorCollecte();
                }
                
                System.out.println("💰 " + nom + " a collecté un trésor !");
                return true;
            }
        }
        return false;
    }
    
    /**
     * Ramasse un fusil
     */
    public boolean ramasserFusil(Fusil f) {
        if (!enVie.get() || f == null || aUnFusil) return false;
        
        synchronized (f) {
            if (f.ramasser()) {
                this.fusil = f;
                this.aUnFusil = true;
                System.out.println("🔫 " + nom + " a ramassé un fusil!");
                return true;
            }
        }
        return false;
    }
    
    /**
     * Utilise le fusil pour tuer un animal (agents réactifs seulement)
     */
    public boolean utiliserFusil(Animal animal) {
        if (!aUnFusil || fusil == null || !fusil.aDesMunitions()) return false;
        
        if (fusil.tirer()) {
            animal.tuer();
            animauxTues.incrementAndGet();
            System.out.println("🎯 " + nom + " a tué un " + animal.getTypeAnimal().name() + " avec le fusil!");
            
            // Vérifier si le fusil est vide
            if (!fusil.aDesMunitions()) {
                System.out.println("🔫 Le fusil de " + nom + " est maintenant vide!");
            }
            return true;
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
     * Gère la mort de l'agent - devient blessé au lieu de mourir directement
     */
    public void mourir() {
        // L'agent devient blessé, pas mort directement
        blesse = true;
        positionBlessure = position.copy();
        tempsBlessure = System.currentTimeMillis();
        enVie.set(false);
        nombreMorts.incrementAndGet();
        if (simulation != null) {
            simulation.getStats().enregistrerBlessure();
        }
        // Hook pour libérer les ressources partagées (surchargé dans AgentCognitif)
        surMourir();
        System.out.println("🤕 " + nom + " est blessé à " + position + "! Attend du secours pendant 10s...");
    }
    
    /**
     * Hook appelé lors de la mort - à surcharger dans les sous-classes
     */
    protected void surMourir() {
        // Par défaut ne fait rien
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
    public boolean isEnVie() { return enVie.get(); }
    public boolean isActif() { return actif.get(); }
    public boolean isBlesse() { return blesse; }
    public boolean aUnFusil() { return aUnFusil; }
    public Fusil getFusil() { return fusil; }
    public List<Tresor> getTresorsCollectes() { return new ArrayList<>(tresorsCollectes); }
    public Zone getZoneActuelle() { return zoneActuelle; }
    public Carte getCarte() { return carte; }
    
    // Statistiques
    public int getNombreCombats() { return nombreCombats.get(); }
    public int getNombreVictoires() { return nombreVictoires.get(); }
    public int getNombreMorts() { return nombreMorts.get(); }
    public int getDistanceParcourue() { return distanceParcourue.get(); }
    public int getAnimauxTues() { return animauxTues.get(); }

    @Override
    public String toString() {
        return getTypeAgent() + " " + nom + " [PV: " + pointsDeVie + "/" + pointsDeVieMax + "] à " + position;
    }
}
