package sma.agents;

public class AgentStats {
    
    private int tresorsCollectes;
    private int valeurTotale;
    private int degatsSubis;
    private int casesVisitees;
    private int iterations;
    private int nbrMorts;
    private int nbrRespawn;
    private int nbrSecours; 

    public AgentStats() {
        this.tresorsCollectes = 0;
        this.valeurTotale = 0;
        this.degatsSubis = 0;
        this.casesVisitees = 0;
        this.iterations = 0;
        this.nbrMorts = 0;
        this.nbrRespawn = 0;
        this.nbrSecours = 0; 
    }
    
    public void ajouterTresor(int valeur) {
        this.tresorsCollectes++;
        this.valeurTotale += valeur;
    }

    public void ajouterDegats(int degats) {
        this.degatsSubis += degats;
    }

    public void incrementerCasesVisitees() {
        this.casesVisitees++;
    }

    public void incrementerIterations() {
        this.iterations++;
    }

    public void incrementerMorts() {
        this.nbrMorts++;
    }

    public void incrementerRespawn() {
        this.nbrRespawn++;
    }


    public void incrementerSecours() {
        this.nbrSecours++;
    }


    
    public int getTresorsCollectes() {
        return tresorsCollectes;
    }

    public int getValeurTotale() {
        return valeurTotale;
    }

    public int getDegatsSubis() {
        return degatsSubis;
    }

    public int getCasesVisitees() {
        return casesVisitees;
    }

    public int getIterations() {
        return iterations;
    }

    public int getNbrMorts() {
        return nbrMorts;
    }

    public int getNbrRespawn() {
        return nbrRespawn;
    }
    public int getNbrSecours() {
        return nbrSecours;
    }

    @Override
    public String toString() {
        return String.format("Stats[trésors=%d, valeur=%d, dégâts=%d, cases=%d, morts=%d, respawns=%d, secours=%d]",
            tresorsCollectes, valeurTotale, degatsSubis, casesVisitees, nbrMorts, nbrRespawn, nbrSecours);
    }
}