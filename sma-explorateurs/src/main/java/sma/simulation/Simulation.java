package sma.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sma.agents.Agent;
import sma.agents.AgentCognitif;
import sma.agents.AgentCommunicant;
import sma.agents.AgentReactif;
import sma.concurrent.AgentManager;
import sma.environnement.Carte;
import sma.environnement.Case;
import sma.environnement.Zone;
import sma.gui.SimuPara;
import sma.objets.Animal;
import sma.objets.Obstacle;
import sma.objets.Tresor;

public class Simulation {

    private final Carte carte;
    private final List<Agent> agents;
    private final List<AgentManager> agentManagers;
    private final Random random;
    private boolean running;
    private long tempsDebut;
    private long tempsFin;

    public Simulation() {
        this.carte = new Carte();
        this.agents = new ArrayList<>();
        this.agentManagers = new ArrayList<>();
        this.random = new Random(); //aléatoire 
        this.running = false;
        //initialiser deux méthodes des objets 
        initialiserObjets();
        initialiserAgents();
    }

    private void initialiserObjets() {
        for (int zx = 0; zx < Carte.NB_ZONES_COTE; zx++) {
            for (int zy = 0; zy < Carte.NB_ZONES_COTE; zy++) {

                //Partie zoneqg, dans les règles du rapport on a dit c'est sage
                Zone zone = carte.getZone(zx, zy);
                if (zx == 0 && zy == 0) {
                    System.out.println("Zone qg skip prochaine itération ");
                    continue;
                }
                //placer tresors valeurs aléatoires car pas très important ici 
                placerTresors(zone, SimuPara.NB_TRESORS_PAR_ZONE);

                // Placer les animaux
                placerAnimaux(zone, SimuPara.NB_ANIMAUX_PAR_ZONE);

                // Placer les obstacles
                placerObstacles(zone, SimuPara.NB_OBSTACLES_PAR_ZONE);
            }
        }
    }

    private void placerTresors(Zone zone, int nombre) {
        int cpt = 0;
        int tentatives = 0;

        while (cpt < nombre && tentatives < 100) {
            int x = random.nextInt(Zone.TAILLE);
            int y = random.nextInt(Zone.TAILLE); //renvoie zone 1,2
            Case caseAleatoire = zone.getCase(x, y); // case renvoie une case aléatoire .. 

            //case non valide => null 
            if (caseAleatoire != null && !caseAleatoire.hasObjet()) {
                int valeurTresor = random.nextInt(100);
                caseAleatoire.setObjet(new Tresor(valeurTresor));
                cpt++;
            }
            tentatives++;
        }
    }

    private void placerAnimaux(Zone zone, int nombre) {
        int cpt = 0;
        int tentatives = 0;

        while (cpt < nombre && tentatives < 100) {
            int x = random.nextInt(Zone.TAILLE);
            int y = random.nextInt(Zone.TAILLE);
            Case caseAleatoire = zone.getCase(x, y);
            int valeurMaxDegats = SimuPara.MAX_DEGATS_ANIMAUX;
            if (caseAleatoire != null && !caseAleatoire.hasObjet()) {
                int valeursDegats = random.nextInt(valeurMaxDegats);
                caseAleatoire.setObjet(new Animal("hérisson des ténébres", valeursDegats)); //faire une ressemblance avec une ortie (bouge pas masi pique)
                cpt++;
            }
            tentatives++;
        }
    }

    private void placerObstacles(Zone zone, int nombre) {
        int cpt = 0;
        int tentatives = 0;

        while (cpt < nombre && tentatives < 100) {
            int x = random.nextInt(Zone.TAILLE);
            int y = random.nextInt(Zone.TAILLE);
            Case caseAleatoire = zone.getCase(x, y);

            if (caseAleatoire != null && !caseAleatoire.hasObjet()) {
                caseAleatoire.setObjet(new Obstacle("Grand Rocher de Cergy"));
                cpt++;
            }
            tentatives++;
        }
    }

    private void initialiserAgents() {
        Case qg = carte.getCaseQG();

        //cognitifs explorent et si sont dans zone communicants => recoivent messages
        for (int i = 0; i < SimuPara.NB_AGENTS_COGNITIFS; i++) {
            ajouterAgent(new AgentCognitif(qg, carte));
        }

        //exploration simple réactifs pour 'linstant '
        for (int i = 0; i < SimuPara.NB_AGENTS_REACTIFS; i++) {
            ajouterAgent(new AgentReactif(qg, carte));
        }

        // M<N , si tous trésors collectés alors dans ce cas ... on se teleporte 
        int zoneIndex = 1;
        for (int i = 0; i < SimuPara.NB_AGENTS_COMMUNICANTS && zoneIndex < 9; i++) {
            Zone zone = carte.getZoneById(zoneIndex);
            if (zone != null) {
                Case spawnCase = trouverCaseAccessibleDansZone(zone); //simplifier cette partie sinon agent cognitif mourra // ou on enleve logique take damage mais obfusquera animaux dans la map
                if (spawnCase != null) {
                    ajouterAgent(new AgentCommunicant(spawnCase, carte));
                    System.out.println("Agent Communicant spawné dans Zone " + zoneIndex);
                }
            }
            zoneIndex++;
        }
    }

    private Case trouverCaseAccessibleDansZone(Zone zone) {
        for (int x = 0; x < Zone.TAILLE; x++) {
            for (int y = 0; y < Zone.TAILLE; y++) {
                Case c = zone.getCase(x, y);
                if (c != null && c.isAccessible() && !c.hasObjet()) {
                    return c;
                }
            }
        }
        // Si pas de case vide, prendre une accessible
        for (int x = 0; x < Zone.TAILLE; x++) {
            for (int y = 0; y < Zone.TAILLE; y++) {
                Case c = zone.getCase(x, y);
                if (c != null && c.isAccessible()) {
                    return c;
                }
            }
        }
        return null;
    }

    private void ajouterAgent(Agent agent) {
        agents.add(agent);
        AgentManager manager = new AgentManager(agent, this , SimuPara.DELAY_MS);
        agentManagers.add(manager);
    }

    public void demarrer() {
        if (running) {
            return;
        }
        running = true;
        tempsDebut = System.currentTimeMillis(); //pour compter => mais mm probleme que python retourne datetime actuel  en milisecondes 

        for (AgentManager manager : agentManagers) {
            manager.start();
        }
    }

    public void arreter() {
        running = false;
        tempsFin = System.currentTimeMillis();
        for (AgentManager manager : agentManagers) {
            manager.stopAgent();
        }
        afficherResultatsSimulationActuelle();
    }


    public void afficherResultatsSimulationActuelle(){
        long dureeSimulation = tempsFin - tempsDebut;
        long dureeSecondes = dureeSimulation / 1000;
        long minutes = dureeSecondes / 60;
        long secondesRestantes = dureeSecondes % 60; //sinon minutes avec virgules
        System.out.println("La simulation est terminée , la durée totale était de : "+minutes+" minutes et de "+secondesRestantes+ "secondes! bien joué");
    }
    
    public boolean isRunning() {
        return running;
    }

    public Carte getCarte() {
        return carte;
    }

    public List<Agent> getAgents() {
        return agents;
    }
}
