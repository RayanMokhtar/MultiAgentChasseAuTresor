package sma.environnement;

import sma.objets.*;
import sma.agents.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Représente la carte de l'environnement d'exploration
 * Thread-safe avec verrous lecture/écriture
 */
public class Carte {
    
    // Paramètres par défaut (modifiables)
    public static final int LARGEUR_DEFAUT = 600;
    public static final int HAUTEUR_DEFAUT = 600;
    public static final int ZONES_X_DEFAUT = 5;
    public static final int ZONES_Y_DEFAUT = 5;
    public static final int NB_TRESORS_DEFAUT = 12;
    public static final int NB_ANIMAUX_DEFAUT = 8;
    public static final int NB_OBSTACLES_DEFAUT = 15;
    
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
    private final Random random;
    private final ReentrantReadWriteLock lock;

    // Compteurs pour IDs
    private int prochainIdTresor = 0;
    private int prochainIdAnimal = 0;
    private int prochainIdObstacle = 0;

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
        this.random = new Random();
        this.lock = new ReentrantReadWriteLock();
        
        initialiserZones();
        placerTresors(nombreTresors);
        placerAnimaux(nombreAnimaux);
        placerObstacles(nombreObstacles);
    }

    private void initialiserZones() {
        int idZone = 0;
        int qgX = nbZonesX / 2;
        int qgY = nbZonesY / 2;
        
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
    }

    private void placerTresors(int nombre) {
        for (int i = 0; i < nombre; i++) {
            Zone zone = getZoneAleatoireNonQG();
            Position pos = getPositionAleatoireDansZone(zone);
            int valeur = 50 + random.nextInt(150); // Valeur entre 50 et 200
            Tresor tresor = new Tresor(prochainIdTresor++, pos, valeur);
            zone.ajouterObjet(tresor);
            objetsParId.put(tresor.getId(), tresor);
        }
    }

    private void placerAnimaux(int nombre) {
        Animal.TypeAnimal[] types = Animal.TypeAnimal.values();
        for (int i = 0; i < nombre; i++) {
            Zone zone = getZoneAleatoireNonQG();
            Position pos = getPositionAleatoireDansZone(zone);
            Animal.TypeAnimal type = types[random.nextInt(types.length)];
            Animal animal = new Animal(prochainIdAnimal++, pos, type);
            zone.ajouterObjet(animal);
            objetsParId.put(animal.getId() + 1000, animal); // Offset pour éviter conflits ID
        }
    }

    private void placerObstacles(int nombre) {
        Obstacle.TypeObstacle[] types = Obstacle.TypeObstacle.values();
        for (int i = 0; i < nombre; i++) {
            Zone zone = getZoneAleatoireNonQG();
            Position pos = getPositionAleatoireDansZone(zone);
            Obstacle.TypeObstacle type = types[random.nextInt(types.length)];
            Obstacle obstacle = new Obstacle(prochainIdObstacle++, pos, type);
            zone.ajouterObjet(obstacle);
            objetsParId.put(obstacle.getId() + 2000, obstacle);
        }
    }

    private Zone getZoneAleatoireNonQG() {
        Zone zone;
        do {
            zone = zones.get(random.nextInt(zones.size()));
        } while (zone.estQG());
        return zone;
    }

    private Position getPositionAleatoireDansZone(Zone zone) {
        int marge = 5; // Marge pour ne pas placer sur les bords
        int x = zone.getPositionDebut().getX() + marge + random.nextInt(Math.max(1, tailleZoneX - 2 * marge));
        int y = zone.getPositionDebut().getY() + marge + random.nextInt(Math.max(1, tailleZoneY - 2 * marge));
        return new Position(x, y);
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
