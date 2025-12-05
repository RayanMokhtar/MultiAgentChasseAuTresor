package sma.environnement;

import sma.objets.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Représente la carte de l'environnement d'exploration
 * Thread-safe avec verrous lecture/écriture
 */
public class Carte {
    
    // Paramètres par défaut (modifiables)
    // 9 zones (3x3), chaque zone = 10x10 cases, chaque case = 20 pixels
    public static final int TAILLE_CASE = 20; // Pixels par case
    public static final int CASES_PAR_ZONE = 10; // 10x10 cases par zone
    public static final int ZONES_X_DEFAUT = 3;
    public static final int ZONES_Y_DEFAUT = 3;
    public static final int LARGEUR_DEFAUT = ZONES_X_DEFAUT * CASES_PAR_ZONE * TAILLE_CASE; // 600
    public static final int HAUTEUR_DEFAUT = ZONES_Y_DEFAUT * CASES_PAR_ZONE * TAILLE_CASE; // 600
    public static final int NB_TRESORS_DEFAUT = 10;
    public static final int NB_ANIMAUX_DEFAUT = 8; // Plus d'animaux pour plus de danger
    public static final int NB_OBSTACLES_DEFAUT = 8;
    
    private final int largeur;
    private final int hauteur;
    private final int nbZonesX;
    private final int nbZonesY;
    private final int tailleZoneX;
    private final int tailleZoneY;
    
    private final Zone[][] grille;
    private final List<Zone> zones;
    private Zone qg;
    
    private final Map<Integer, ObjetEnvironnement> objetsParId;
    private final ReentrantReadWriteLock lock;

    private static record AnimalPlacement(Position position, Animal.TypeAnimal type) {}
    private static record ObstaclePlacement(Position position, Obstacle.TypeObstacle type) {}

    // Configuration fixe et équilibrée (3x3 zones) : objets dispersés
    private static final List<Position> TRESORS_BASE = List.of(
        new Position(60, 60),     // QG pour un départ rapide
        new Position(80, 240),    // Zone (0,1)
        new Position(80, 440),    // Zone (0,2)
        new Position(240, 80),    // Zone (1,0)
        new Position(240, 260),   // Zone (1,1)
        new Position(240, 460),   // Zone (1,2)
        new Position(420, 80),    // Zone (2,0)
        new Position(420, 260),   // Zone (2,1)
        new Position(420, 460),   // Zone (2,2)
        new Position(300, 320)    // Bonus au centre
    );

    private static final List<AnimalPlacement> ANIMAUX_BASE = List.of(
        new AnimalPlacement(new Position(110, 260), Animal.TypeAnimal.LOUP),
        new AnimalPlacement(new Position(120, 480), Animal.TypeAnimal.OURS),
        new AnimalPlacement(new Position(260, 120), Animal.TypeAnimal.CROCODILE),
        new AnimalPlacement(new Position(320, 260), Animal.TypeAnimal.LOUP),
        new AnimalPlacement(new Position(260, 480), Animal.TypeAnimal.OURS),
        new AnimalPlacement(new Position(440, 120), Animal.TypeAnimal.CROCODILE),
        new AnimalPlacement(new Position(460, 300), Animal.TypeAnimal.LOUP),
        new AnimalPlacement(new Position(520, 500), Animal.TypeAnimal.OURS)
    );

    private static final List<ObstaclePlacement> OBSTACLES_BASE = List.of(
        new ObstaclePlacement(new Position(120, 120), Obstacle.TypeObstacle.ROCHER),
        new ObstaclePlacement(new Position(60, 340), Obstacle.TypeObstacle.ARBRE),
        new ObstaclePlacement(new Position(120, 520), Obstacle.TypeObstacle.MUR),
        new ObstaclePlacement(new Position(300, 180), Obstacle.TypeObstacle.RIVIERE),
        new ObstaclePlacement(new Position(300, 340), Obstacle.TypeObstacle.ROCHER),
        new ObstaclePlacement(new Position(300, 520), Obstacle.TypeObstacle.ARBRE),
        new ObstaclePlacement(new Position(520, 240), Obstacle.TypeObstacle.ROCHER),
        new ObstaclePlacement(new Position(520, 520), Obstacle.TypeObstacle.MUR)
    );

    private static final List<Position> FUSILS_BASE = List.of(
        new Position(360, 300) // Centre pour un accès partagé
    );

    // Compteurs pour IDs
    private int prochainIdTresor = 0;
    private int prochainIdAnimal = 0;
    private int prochainIdObstacle = 0;
    private int prochainIdFusil = 0;
    
    // Nombre de fusils par défaut
    public static final int NB_FUSILS_DEFAUT = 1;

    public Carte() {
        this(LARGEUR_DEFAUT, HAUTEUR_DEFAUT, ZONES_X_DEFAUT, ZONES_Y_DEFAUT,
             NB_TRESORS_DEFAUT, NB_ANIMAUX_DEFAUT, NB_OBSTACLES_DEFAUT);
    }

    public Carte(int largeur, int hauteur, int nbZonesX, int nbZonesY,
                 int nombreTresors, int nombreAnimaux, int nombreObstacles) {
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.nbZonesX = nbZonesX;
        this.nbZonesY = nbZonesY;
        this.tailleZoneX = largeur / nbZonesX;
        this.tailleZoneY = hauteur / nbZonesY;
        
        this.grille = new Zone[nbZonesX][nbZonesY];
        this.zones = new ArrayList<>();
        this.objetsParId = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        
        initialiserZones();
        placerConfigurationFixe(nombreTresors, nombreAnimaux, nombreObstacles);
    }

    private void initialiserZones() {
        int idZone = 0;
        // QG en haut à gauche (0, 0)
        int qgX = 0;
        int qgY = 0;
        
        for (int i = 0; i < nbZonesX; i++) {
            for (int j = 0; j < nbZonesY; j++) {
                Position debut = new Position(i * tailleZoneX, j * tailleZoneY);
                Position fin = new Position((i + 1) * tailleZoneX - 1, (j + 1) * tailleZoneY - 1);
                
                boolean estQG = (i == qgX && j == qgY);
                Zone zone = new Zone(idZone++, i, j, debut, fin, estQG);
                grille[i][j] = zone;
                zones.add(zone);
                
                if (estQG) {
                    this.qg = zone;
                }
            }
        }
        System.out.println("🗺️ Carte créée: " + nbZonesX + "x" + nbZonesY + " zones (" + 
                          CASES_PAR_ZONE + "x" + CASES_PAR_ZONE + " cases/zone), QG en [0,0]");
    }

    private void placerConfigurationFixe(int nombreTresors, int nombreAnimaux, int nombreObstacles) {
        placerTresorsFixes(nombreTresors);
        placerAnimauxFixes(nombreAnimaux);
        placerObstaclesFixes(nombreObstacles);
        placerFusilsFixes(NB_FUSILS_DEFAUT);
    }

    private void placerTresorsFixes(int nombre) {
        int limit = Math.min(nombre, TRESORS_BASE.size());
        for (int i = 0; i < limit; i++) {
            Position pos = copier(TRESORS_BASE.get(i));
            Tresor tresor = new Tresor(prochainIdTresor++, pos);
            Zone zone = getZoneAt(pos);
            if (zone != null) {
                zone.ajouterObjet(tresor);
                objetsParId.put(tresor.getId(), tresor);
            }
        }
    }

    private void placerAnimauxFixes(int nombre) {
        int limit = Math.min(nombre, ANIMAUX_BASE.size());
        for (int i = 0; i < limit; i++) {
            AnimalPlacement placement = ANIMAUX_BASE.get(i);
            Position pos = copier(placement.position());
            Zone zone = getZoneAt(pos);
            if (zone != null && !zone.estQG()) {
                Animal animal = new Animal(prochainIdAnimal++, pos, placement.type());
                zone.ajouterObjet(animal);
                objetsParId.put(animal.getId() + 1000, animal);
            }
        }
    }

    private void placerObstaclesFixes(int nombre) {
        int limit = Math.min(nombre, OBSTACLES_BASE.size());
        for (int i = 0; i < limit; i++) {
            ObstaclePlacement placement = OBSTACLES_BASE.get(i);
            Position pos = copier(placement.position());
            Zone zone = getZoneAt(pos);
            if (zone != null) {
                Obstacle obstacle = new Obstacle(prochainIdObstacle++, pos, placement.type());
                zone.ajouterObjet(obstacle);
                objetsParId.put(obstacle.getId() + 2000, obstacle);
            }
        }
    }
    
    private void placerFusilsFixes(int nombre) {
        int limit = Math.min(nombre, FUSILS_BASE.size());
        for (int i = 0; i < limit; i++) {
            Position pos = copier(FUSILS_BASE.get(i));
            Zone zone = getZoneAt(pos);
            if (zone != null) {
                Fusil fusil = new Fusil(prochainIdFusil++, pos);
                zone.ajouterObjet(fusil);
                objetsParId.put(fusil.getId() + 3000, fusil);
            }
        }
        System.out.println("🔫 " + limit + " fusil(s) placés sur la carte");
    }

    private Position copier(Position p) {
        return new Position(p.getX(), p.getY());
    }

    public Zone getZoneAt(Position pos) {
        lock.readLock().lock();
        try {
            int zoneX = Math.min(pos.getX() / tailleZoneX, nbZonesX - 1);
            int zoneY = Math.min(pos.getY() / tailleZoneY, nbZonesY - 1);
            zoneX = Math.max(0, zoneX);
            zoneY = Math.max(0, zoneY);
            return grille[zoneX][zoneY];
        } finally {
            lock.readLock().unlock();
        }
    }

    public Zone getZoneParIndices(int i, int j) {
        if (i >= 0 && i < nbZonesX && j >= 0 && j < nbZonesY) {
            return grille[i][j];
        }
        return null;
    }

    public List<Zone> getZonesAdjacentes(Zone zone) {
        List<Zone> adjacentes = new ArrayList<>();
        int i = zone.getLigneGrille();
        int j = zone.getColonneGrille();

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int ni = i + dir[0];
            int nj = j + dir[1];
            if (ni >= 0 && ni < nbZonesX && nj >= 0 && nj < nbZonesY) {
                adjacentes.add(grille[ni][nj]);
            }
        }
        return adjacentes;
    }

    public boolean positionValide(Position pos) {
        return pos.getX() >= 0 && pos.getX() < largeur && pos.getY() >= 0 && pos.getY() < hauteur;
    }

    public boolean positionAccessible(Position pos) {
        if (!positionValide(pos)) return false;
        
        Zone zone = getZoneAt(pos);
        if (zone == null) return false;
        
        for (Obstacle obs : zone.getObstacles()) {
            if (obs.getPosition().distanceTo(pos) < 10 && !obs.isFranchissable()) {
                return false;
            }
        }
        return true;
    }

    public List<Tresor> getTousTresorsNonCollectes() {
        List<Tresor> tresors = new ArrayList<>();
        for (Zone zone : zones) {
            tresors.addAll(zone.getTresorsNonCollectes());
        }
        return tresors;
    }

    public int getNombreZones() {
        return zones.size();
    }

    // Getters
    public int getLargeur() { return largeur; }
    public int getHauteur() { return hauteur; }
    public int getNbZonesX() { return nbZonesX; }
    public int getNbZonesY() { return nbZonesY; }
    public int getTailleZoneX() { return tailleZoneX; }
    public int getTailleZoneY() { return tailleZoneY; }
    public List<Zone> getZones() { return new ArrayList<>(zones); }
    public Zone getQG() { return qg; }
    public Zone[][] getGrille() { return grille; }
}
