package sma.environnement;

/**
 * Une Zone = une région de la carte, composée de 100 cases (10x10).
 * La carte contient 9 zones (3x3).
 */
public class Zone {
    
    public static final int TAILLE = 10;
    
    private final int id;
    private final int zoneX; 
    private final int zoneY;
    private final Case[][] cases;

    public Zone(int id, int zoneX, int zoneY) {
        this.id = id;
        this.zoneX = zoneX;
        this.zoneY = zoneY;
        this.cases = new Case[TAILLE][TAILLE];
        
        for (int x = 0; x < TAILLE; x++) {
            for (int y = 0; y < TAILLE; y++) {
                cases[x][y] = new Case(x, y);
                cases[x][y].setZone(this); //lier chaque case à la zone parente ( 0,0) mais zone 0,0
            }
        }
    }

    // ========== GETTERS ==========
    
    public int getId() { return id; }
    public int getZoneX() { return zoneX; }
    public int getZoneY() { return zoneY; }

    public Case getCase(int x, int y) {
        if (estDansLimites(x, y)) {
            return cases[x][y];
        }
        return null;
    }

    public boolean estDansLimites(int x, int y) {
        return x >= 0 && x < TAILLE && y >= 0 && y < TAILLE;
    }

    @Override
    public String toString() {
        return String.format("Zone[id=%d, pos=(%d,%d)]", id, zoneX, zoneY);
    }

}