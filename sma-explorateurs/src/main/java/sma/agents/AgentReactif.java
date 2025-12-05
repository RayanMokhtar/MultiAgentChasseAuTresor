package sma.agents;

import sma.environnement.*;
import sma.objets.*;

import java.awt.Color;
import java.util.*;

/**
 * Agent Réactif - Agent qui réagit selon des règles prédéfinies
 * 
 * RÈGLES DU TP:
 * - Vision de 2 cases (range)
 * - Marche au hasard
 * - Évite les obstacles (contourne par gauche ou droite)
 * - Collecte les trésors s'il tombe dessus (time.sleep(0.5))
 * - Fuit les animaux (vers la position la plus éloignée)
 * - Peut ramasser un fusil et tuer jusqu'à 2 animaux
 * - Animal ne peut pas sortir de sa zone assignée
 */
public class AgentReactif extends Agent {
    
    /**
     * Règles de comportement disponibles
     */
    public enum Regle {
        FUIR_SI_DANGER("Fuit les dangers"),
        COLLECTER_TRESOR_VISIBLE("Collecte trésors proches"),
        EXPLORER_ALEATOIRE("Exploration aléatoire"),
        RETOUR_SI_FAIBLE("Retourne au QG si blessé"),
        EVITER_OBSTACLES("Évite les obstacles"),
        RAMASSER_FUSIL("Ramasse les fusils"),
        UTILISER_FUSIL("Utilise le fusil sur les animaux"),
        SECOURIR_AGENT("Secourt les agents blessés");

        private final String description;
        Regle(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    private final Set<Regle> reglesActives;
    private final Random random;
    private Position dernierePosition;
    private int compteurImmobile;
    private int directionExploration; // 0-7 pour 8 directions
    private Position objectifEvitement;
    private long evitementExpireMs;
    private Position cibleSupport;
    private long supportExpireMs;
    private static final long SUPPORT_TTL_MS = 3000;
    private static final long EVITEMENT_COOLDOWN_MS = 1500;
    
    // Vision de 3 cases (en pixels: 3 * tailleCase = 60 pixels)
    public static final int RANGE_VISION = 3;
    public static final int VISION_PIXELS = RANGE_VISION * 20;
    
    private static final Color COULEUR = new Color(255, 100, 0); // Orange

    public AgentReactif(String nom, Carte carte, Set<Regle> regles) {
        super(nom, carte, 90, 28);
        this.reglesActives = regles != null ? new HashSet<>(regles) : getReglesParDefaut();
        this.random = new Random();
        this.dernierePosition = position.copy();
        this.compteurImmobile = 0;
        this.directionExploration = random.nextInt(8);
        this.objectifEvitement = null;
        this.evitementExpireMs = 0;
        this.cibleSupport = null;
        this.supportExpireMs = 0;
    }

    public AgentReactif(String nom, Carte carte) {
        this(nom, carte, null);
    }

    private Set<Regle> getReglesParDefaut() {
        Set<Regle> regles = new HashSet<>();
        regles.add(Regle.FUIR_SI_DANGER);
        regles.add(Regle.COLLECTER_TRESOR_VISIBLE);
        regles.add(Regle.EXPLORER_ALEATOIRE);
        regles.add(Regle.RETOUR_SI_FAIBLE);
        regles.add(Regle.EVITER_OBSTACLES);
        regles.add(Regle.RAMASSER_FUSIL);
        regles.add(Regle.UTILISER_FUSIL);
        regles.add(Regle.SECOURIR_AGENT);
        return regles;
    }

    @Override
    public void agir() {
        if (!enVie.get()) return;

        if (gererSupportActif()) return;

        // Exécuter les règles par ordre de priorité
        for (Regle regle : getPrioriteRegles()) {
            if (reglesActives.contains(regle)) {
                if (executerRegle(regle)) {
                    return; // Une seule action par tour
                }
            }
        }
    }

    private List<Regle> getPrioriteRegles() {
        return Arrays.asList(
            Regle.RETOUR_SI_FAIBLE,
            Regle.SECOURIR_AGENT,
            Regle.UTILISER_FUSIL,      // Priorité haute si on a un fusil
            Regle.FUIR_SI_DANGER,
            Regle.RAMASSER_FUSIL,
            Regle.COLLECTER_TRESOR_VISIBLE,
            Regle.EVITER_OBSTACLES,
            Regle.EXPLORER_ALEATOIRE
        );
    }

    private boolean executerRegle(Regle regle) {
        switch (regle) {
            case RETOUR_SI_FAIBLE: return regleRetourSiFaible();
            case SECOURIR_AGENT: return regleSecourirAgent();
            case UTILISER_FUSIL: return regleUtiliserFusil();
            case FUIR_SI_DANGER: return regleFuirSiDanger();
            case RAMASSER_FUSIL: return regleRamasserFusil();
            case COLLECTER_TRESOR_VISIBLE: return regleCollecterTresor();
            case EXPLORER_ALEATOIRE: return regleExplorerAleatoire();
            case EVITER_OBSTACLES: return regleEviterObstacles();
            default: return false;
        }
    }

    // ========== RÈGLES ==========

    private boolean regleRetourSiFaible() {
        if (pointsDeVie < pointsDeVieMax * 0.25) {
            if (zoneActuelle.estQG()) {
                reposer();
                return true;
            } else {
                return seDeplacerVers(carte.getQG().getCentre());
            }
        }
        return false;
    }
    
    /**
     * Règle: Secourir un agent blessé à proximité
     */
    private boolean regleSecourirAgent() {
        if (simulation == null) return false;
        
        for (Agent agent : simulation.getAgents()) {
            if (agent != this && agent.isBlesse() && peutSecourir(agent)) {
                secourir(agent);
                if (simulation != null) {
                    simulation.getStats().enregistrerSecours();
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * Règle: Utiliser le fusil si on en a un et qu'un animal est à portée
     */
    private boolean regleUtiliserFusil() {
        if (!aUnFusil || fusil == null || !fusil.aDesMunitions()) return false;
        
        Zone zone = zoneActuelle;
        if (zone == null) return false;
        
        for (Animal animal : zone.getAnimaux()) {
            if (animal.isActif() && position.distanceTo(animal.getPosition()) <= VISION_PIXELS) {
                // Utiliser le fusil!
                if (utiliserFusil(animal)) {
                    if (simulation != null) {
                        simulation.getStats().enregistrerAnimalTue();
                    }
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Règle: Ramasser un fusil visible
     */
    private boolean regleRamasserFusil() {
        if (aUnFusil) return false; // On a déjà un fusil
        
        Zone zone = zoneActuelle;
        if (zone == null) return false;
        
        for (Fusil f : zone.getFusils()) {
            if (!f.isRamasse() && position.distanceTo(f.getPosition()) <= 15) {
                return ramasserFusil(f);
            }
        }
        return false;
    }

    /**
     * Règle: Fuit les animaux vers la position la plus éloignée
     */
    private boolean regleFuirSiDanger() {
        Zone zone = zoneActuelle;
        if (zone == null) return false;

        long now = System.currentTimeMillis();

        // Si un évitement est en cours et aucun animal immédiat, continuer vers le point sûr
        if (objectifEvitement != null && now < evitementExpireMs) {
            if (position.distanceTo(objectifEvitement) > Carte.TAILLE_CASE) {
                return deplacerVers(objectifEvitement);
            }
            objectifEvitement = null; // Atteint ou proche
        }
        
        // Trouver l'animal le plus proche
        Animal animalProche = null;
        double distanceMin = Double.MAX_VALUE;
        
        for (Animal animal : zone.getAnimaux()) {
            if (animal.isActif()) {
                double distance = position.distanceTo(animal.getPosition());
                if (distance <= VISION_PIXELS && distance < distanceMin) {
                    distanceMin = distance;
                    animalProche = animal;
                }
            }
        }
        
        if (animalProche != null) {
            System.out.println("🏃 " + nom + " fuit " + animalProche.getTypeAnimal().name() + "!");
            Position cibleFuite = pointFuiteLointaine(animalProche.getPosition(), 80);
            if (cibleFuite != null) {
                objectifEvitement = cibleFuite;
                evitementExpireMs = now + EVITEMENT_COOLDOWN_MS;
                if (deplacerVers(cibleFuite)) {
                    return true;
                }
            }
            return fuirVersPositionLaPlusEloignee(animalProche.getPosition());
        }
        return false;
    }

    private Position pointFuiteLointaine(Position danger, int rayon) {
        int dx = Integer.compare(position.getX(), danger.getX());
        int dy = Integer.compare(position.getY(), danger.getY());
        if (dx == 0 && dy == 0) {
            dx = 1;
        }
        Position cible = new Position(position.getX() + dx * rayon, position.getY() + dy * rayon);
        // Clamp dans la carte
        int x = Math.max(0, Math.min(cible.getX(), carte.getLargeur() - 1));
        int y = Math.max(0, Math.min(cible.getY(), carte.getHauteur() - 1));
        Position candidate = new Position(x, y);
        if (carte.positionAccessible(candidate)) {
            return candidate;
        }
        return null;
    }
    
    /**
     * Fuit vers la position la plus éloignée de l'animal
     * Avec plusieurs options de contournement
     */
    private boolean fuirVersPositionLaPlusEloignee(Position posAnimal) {
        // Direction opposée à l'animal
        int dx = Integer.compare(position.getX(), posAnimal.getX());
        int dy = Integer.compare(position.getY(), posAnimal.getY());
        
        // Si même position, choisir une direction aléatoire
        if (dx == 0 && dy == 0) {
            dx = random.nextInt(3) - 1;
            dy = random.nextInt(3) - 1;
            if (dx == 0 && dy == 0) dx = 1;
        }
        
        int vitesse = 12; // Fuite rapide
        
        // Essayer plusieurs directions de fuite
        Position[] fuites = {
            new Position(position.getX() + dx * vitesse, position.getY() + dy * vitesse),
            new Position(position.getX() + dx * vitesse, position.getY()),
            new Position(position.getX(), position.getY() + dy * vitesse),
            new Position(position.getX() - dy * vitesse, position.getY() + dx * vitesse),
            new Position(position.getX() + dy * vitesse, position.getY() - dx * vitesse)
        };
        
        for (Position fuite : fuites) {
            if (carte.positionAccessible(fuite)) {
                return deplacer(fuite);
            }
        }
        
        // Dernier recours: n'importe quelle direction
        return regleExplorerAleatoire();
    }

    private boolean regleCollecterTresor() {
        Zone zone = zoneActuelle;
        if (zone == null) return false;

        Tresor meilleur = null;
        double distMin = Double.MAX_VALUE;

        // Trouver le trésor le plus proche dans la zone
        for (Tresor tresor : zone.getTresorsNonCollectes()) {
            double d = position.distanceTo(tresor.getPosition());
            if (d < distMin) {
                distMin = d;
                meilleur = tresor;
            }
        }

        if (meilleur == null) return false;

        // Si très proche, le ramasser
        if (distMin <= Carte.TAILLE_CASE) {
            return collecterTresor(meilleur);
        }

        // Si dans le champ de vision, se diriger vers lui
        if (distMin <= VISION_PIXELS * 1.5) {
            return seDeplacerVers(meilleur.getPosition());
        }

        return false;
    }

    private boolean regleExplorerAleatoire() {
        // Vérifier si bloqué
        if (position.equals(dernierePosition)) {
            compteurImmobile++;
            if (compteurImmobile > 5) {
                // Changer de direction
                directionExploration = random.nextInt(8);
                compteurImmobile = 0;
            }
        } else {
            compteurImmobile = 0;
        }
        dernierePosition = position.copy();

        // Directions: N, NE, E, SE, S, SW, W, NW
        int[][] directions = {
            {0, -1}, {1, -1}, {1, 0}, {1, 1},
            {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
        };
        
        int vitesse = 8;
        int[] dir = directions[directionExploration];
        Position nouvellePos = new Position(
            position.getX() + dir[0] * vitesse,
            position.getY() + dir[1] * vitesse
        );

        // Vérifier les bords et changer de direction si nécessaire
        if (!carte.positionValide(nouvellePos)) {
            directionExploration = (directionExploration + 4) % 8; // Demi-tour
            dir = directions[directionExploration];
            nouvellePos = new Position(
                position.getX() + dir[0] * vitesse,
                position.getY() + dir[1] * vitesse
            );
        }

        if (carte.positionAccessible(nouvellePos)) {
            if (deplacer(nouvellePos)) {
                // Changer légèrement de direction parfois
                if (random.nextInt(10) < 2) {
                    directionExploration = (directionExploration + random.nextInt(3) - 1 + 8) % 8;
                }
                return true;
            }
        }
        
        // Si bloqué, essayer une autre direction
        directionExploration = random.nextInt(8);
        return false;
    }

    private boolean regleEviterObstacles() {
        // Géré dans positionAccessible
        return false;
    }

    private boolean gererSupportActif() {
        if (cibleSupport == null) return false;
        long now = System.currentTimeMillis();
        if (now > supportExpireMs) {
            cibleSupport = null;
            return false;
        }
        if (position.distanceTo(cibleSupport) > Carte.TAILLE_CASE) {
            return seDeplacerVers(cibleSupport);
        }
        cibleSupport = null;
        return false;
    }

    public boolean assignerSupport(Position cible) {
        if (!enVie.get() || isBlesse()) return false;
        this.cibleSupport = cible.copy();
        this.supportExpireMs = System.currentTimeMillis() + SUPPORT_TTL_MS;
        return true;
    }

    // ========== UTILITAIRES ==========

    private boolean seDeplacerVers(Position cible) {
        if (cible == null) return false;
        
        int dx = Integer.compare(cible.getX(), position.getX());
        int dy = Integer.compare(cible.getY(), position.getY());
        int vitesse = 6;
        
        Position nouvellePos = new Position(
            position.getX() + dx * vitesse,
            position.getY() + dy * vitesse
        );
        
        if (carte.positionAccessible(nouvellePos)) {
            return deplacer(nouvellePos);
        }
        
        // Essayer de contourner (gauche ou droite comme dans les règles)
        Position alt1 = new Position(position.getX() + dx * vitesse, position.getY());
        Position alt2 = new Position(position.getX(), position.getY() + dy * vitesse);
        
        if (carte.positionAccessible(alt1)) {
            return deplacer(alt1);
        }
        if (carte.positionAccessible(alt2)) {
            return deplacer(alt2);
        }
        
        return false;
    }

    public void ajouterRegle(Regle regle) {
        reglesActives.add(regle);
    }

    public void retirerRegle(Regle regle) {
        reglesActives.remove(regle);
    }

    @Override
    public String getTypeAgent() {
        return "⚡ Réactif";
    }

    @Override
    public Color getCouleur() {
        return COULEUR;
    }

    public Set<Regle> getReglesActives() {
        return new HashSet<>(reglesActives);
    }
}
