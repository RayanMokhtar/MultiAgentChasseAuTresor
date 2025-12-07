package sma.gui;

public class SimuPara {
    
    // Fenêtre
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 1200;
    
    // Carte
    public static final int CASE_SIZE = 16;
    public static final int ZONE_MARGIN = 0;
    
    // Simulation
    public static long DELAY_MS = 100;
    public static final int SIMULATION_DURATION = 60000;
    
    //Agents
    public static int NB_AGENTS_REACTIFS = 5;
    public static int NB_AGENTS_COGNITIFS = 3; // réduit par défaut
    public static int NB_AGENTS_COMMUNICANTS = 3;  
    
    //Objets
    public static int NB_TRESORS_PAR_ZONE = 1;
    public static int NB_ANIMAUX_PAR_ZONE = 2;
    public static int NB_OBSTACLES_PAR_ZONE = 5;

    public static final int MAX_DEGATS_ANIMAUX = 20; //plus de fair play :) // mais seront quand mm aléatoire j'ai mis la logique dans la partie simualtion 

}