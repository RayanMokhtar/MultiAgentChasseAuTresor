package sma.environnement;

import sma.objets.Tresor;

public class Carte {

    public static final int NB_ZONES_COTE = 3;
    private final Zone[][] zones = new Zone[NB_ZONES_COTE][NB_ZONES_COTE];

    public Carte() {
        initialiserZones();
    }

    private void initialiserZones() {
        int compteur = 0;
        for (int x = 0; x < NB_ZONES_COTE; x++) {
            for (int y = 0; y < NB_ZONES_COTE; y++) {
                zones[x][y] = new Zone(compteur, x, y);
                compteur++;
            }
        }
    }

    public Zone getZone(int zoneX, int zoneY) {
        if (estZoneDansLimites(zoneX, zoneY)) {
            return zones[zoneX][zoneY];
        }
        return null;
    }

    public Zone getZoneById(int id) {
        int x = id / NB_ZONES_COTE;
        int y = id % NB_ZONES_COTE;
        return getZone(x, y);
    }

    public Case getCaseQG() {
        return zones[0][0].getCase(0, 0);
    }

    public boolean estZoneDansLimites(int zoneX, int zoneY) {
        return zoneX >= 0 && zoneX < NB_ZONES_COTE && zoneY >= 0 && zoneY < NB_ZONES_COTE;
    }

    /**
     * Vérifie si tous les trésors de la carte sont collectés.
     */
    public boolean tousTresorsCollectes() {
        for (int zx = 0; zx < NB_ZONES_COTE; zx++) {
            for (int zy = 0; zy < NB_ZONES_COTE; zy++) {
                Zone zone = zones[zx][zy];
                for (int x = 0; x < Zone.TAILLE; x++) {
                    for (int y = 0; y < Zone.TAILLE; y++) {
                        Case c = zone.getCase(x, y);
                        if (c != null && c.getObjet() instanceof Tresor) {
                            Tresor t = (Tresor) c.getObjet();
                            if (!t.isCollecte()) {
                                return false; // Au moins un trésor non collecté
                            }
                        }
                    }
                }
            }
        }
        return true; // Tous collectés
    }

    /**
     * Compte les trésors restants (non collectés).
     */
    public int compterTresorsRestants() {
        int count = 0;
        for (int zx = 0; zx < NB_ZONES_COTE; zx++) {
            for (int zy = 0; zy < NB_ZONES_COTE; zy++) {
                Zone zone = zones[zx][zy];
                for (int x = 0; x < Zone.TAILLE; x++) {
                    for (int y = 0; y < Zone.TAILLE; y++) {
                        Case c = zone.getCase(x, y);
                        if (c != null && c.getObjet() instanceof Tresor) {
                            Tresor t = (Tresor) c.getObjet();
                            if (!t.isCollecte()) {
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return String.format("Carte[9 zones, 900 cases]");
    }
}
