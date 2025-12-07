package sma.gui;

public class SimuPara {

    // Fenêtre
    public static int WINDOW_WIDTH = 1200;
    public static int WINDOW_HEIGHT = 900;

    // Carte
    public static int CASE_SIZE = 16;
    public static int ZONE_MARGIN = 0;

    // Simulation
    public static long DELAY_MS = 100;
    public static int SIMULATION_DURATION = 60000;

    // Agents (maintenant modifiables)
    public static int NB_AGENTS_REACTIFS = 5;
    public static int NB_AGENTS_COGNITIFS = 5;
    public static int NB_AGENTS_COMMUNICANTS = 3;

    // Objets (maintenant modifiables)
    public static int NB_TRESORS_PAR_ZONE = 1;
    public static int NB_ANIMAUX_PAR_ZONE = 2;
    public static int NB_OBSTACLES_PAR_ZONE = 5;

    public static int MAX_DEGATS_ANIMAUX = 20;
}