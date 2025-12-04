package sma.agents;

import sma.environnement.*;
import sma.objets.*;

import java.awt.Color;
import java.util.*;

/**
 * Agent Réactif - Agent qui réagit selon des règles prédéfinies
 * Pas de planification, réaction immédiate aux stimuli
 */
public class AgentReactif extends Agent {
    
    /**
     * Règles de comportement disponibles
     */
    public enum Regle {
        FUIR_SI_DANGER("Fuit les dangers"),
        ATTAQUER_SI_FORT("Attaque si avantage"),
        COLLECTER_TRESOR_VISIBLE("Collecte trésors proches"),
        EXPLORER_ALEATOIRE("Exploration aléatoire"),
        SUIVRE_AUTRES_AGENTS("Suit les autres"),
        RETOUR_SI_FAIBLE("Retourne au QG si blessé"),
        EVITER_OBSTACLES("Évite les obstacles");

        private final String description;
        Regle(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    private final Set<Regle> reglesActives;
    private final Random random;
    private Position dernierePosition;
    private int compteurImmobile;
    private int directionExploration; // 0-7 pour 8 directions
    
    private static final Color COULEUR = new Color(255, 100, 0); // Orange

    public AgentReactif(String nom, Carte carte, Set<Regle> regles) {
        super(nom, carte, 90, 28, 45);
        this.reglesActives = regles != null ? new HashSet<>(regles) : getReglesParDefaut();
        this.random = new Random();
        this.dernierePosition = position.copy();
        this.compteurImmobile = 0;
        this.directionExploration = random.nextInt(8);
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
        return regles;
    }

    @Override
    public void agir() {
        if (!enVie.get()) return;

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
            Regle.FUIR_SI_DANGER,
            Regle.ATTAQUER_SI_FORT,
            Regle.COLLECTER_TRESOR_VISIBLE,
            Regle.SUIVRE_AUTRES_AGENTS,
            Regle.EVITER_OBSTACLES,
            Regle.EXPLORER_ALEATOIRE
        );
    }

    private boolean executerRegle(Regle regle) {
        switch (regle) {
            case RETOUR_SI_FAIBLE: return regleRetourSiFaible();
            case FUIR_SI_DANGER: return regleFuirSiDanger();
            case ATTAQUER_SI_FORT: return regleAttaquerSiFort();
            case COLLECTER_TRESOR_VISIBLE: return regleCollecterTresor();
            case SUIVRE_AUTRES_AGENTS: return regleSuivreAgents();
            case EXPLORER_ALEATOIRE: return regleExplorerAleatoire();
            case EVITER_OBSTACLES: return regleEviterObstacles();
            default: return false;
        }
    }

    // ========== RÈGLES ==========

    private boolean regleRetourSiFaible() {
        if (pointsDeVie < pointsDeVieMax * 0.25 || energie < energieMax * 0.15) {
            if (zoneActuelle.estQG()) {
                reposer();
                return true;
            } else {
                return seDeplacerVers(carte.getQG().getCentre());
            }
        }
        return false;
    }

    private boolean regleFuirSiDanger() {
        Zone zone = zoneActuelle;
        if (zone == null) return false;
        
        for (Animal animal : zone.getAnimaux()) {
            if (animal.isActif() && animal.peutAttaquer(position)) {
                if (animal.getForce() > this.force || pointsDeVie < pointsDeVieMax * 0.4) {
                    System.out.println("🏃 " + nom + " fuit " + animal.getTypeAnimal().name() + "!");
                    return fuir(animal.getPosition());
                }
            }
        }
        return false;
    }

    private boolean regleAttaquerSiFort() {
        Zone zone = zoneActuelle;
        if (zone == null) return false;
        
        for (Animal animal : zone.getAnimaux()) {
            if (animal.isActif() && position.distanceTo(animal.getPosition()) < 30) {
                if (this.force >= animal.getForce() && pointsDeVie > pointsDeVieMax * 0.5) {
                    // Se rapprocher pour attaquer
                    if (position.distanceTo(animal.getPosition()) > 10) {
                        return seDeplacerVers(animal.getPosition());
                    } else {
                        combattre(animal);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean regleCollecterTresor() {
        Zone zone = zoneActuelle;
        if (zone == null) return false;
        
        // Collecter si adjacent
        for (Tresor tresor : zone.getTresorsNonCollectes()) {
            if (position.distanceTo(tresor.getPosition()) <= 15) {
                if (collecterTresor(tresor)) {
                    return true;
                }
            }
        }
        
        // Se diriger vers le trésor le plus proche
        Tresor plusProche = null;
        double distanceMin = Double.MAX_VALUE;
        
        for (Tresor tresor : zone.getTresorsNonCollectes()) {
            double dist = position.distanceTo(tresor.getPosition());
            if (dist < distanceMin) {
                distanceMin = dist;
                plusProche = tresor;
            }
        }
        
        if (plusProche != null && distanceMin < 100) {
            return seDeplacerVers(plusProche.getPosition());
        }
        
        return false;
    }

    private boolean regleSuivreAgents() {
        if (simulation == null) return false;
        
        // Suivre un agent cognitif proche
        for (Agent autre : simulation.getAgents()) {
            if (autre != this && autre.isEnVie() && autre instanceof AgentCognitif) {
                if (position.distanceTo(autre.getPosition()) < 150) {
                    return seDeplacerVers(autre.getPosition());
                }
            }
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
        
        // Essayer de contourner
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

    private boolean fuir(Position danger) {
        if (danger == null) return false;
        
        int dx = Integer.compare(position.getX(), danger.getX());
        int dy = Integer.compare(position.getY(), danger.getY());
        int vitesse = 10;
        
        Position fuite = new Position(
            position.getX() + dx * vitesse,
            position.getY() + dy * vitesse
        );
        
        if (carte.positionAccessible(fuite)) {
            return deplacer(fuite);
        }
        
        // Si impossible, essayer une autre direction
        return regleExplorerAleatoire();
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
