package sma.agents;

import java.util.List;
import java.util.Random;

import sma.environnement.Carte;
import sma.environnement.Case;

public class AgentReactif extends Agent {
    
    private final Random random = new Random();

    public AgentReactif(Case positionInitiale, Carte carte) {
        super(TypologieAgent.REACTIF, positionInitiale, carte);
    }

    @Override
    public void step() {
        List<Case> casesAccessibles = getCasesAdjacentes();
        
        if (casesAccessibles.isEmpty()) {
            return;
        }
        
        Case destination = casesAccessibles.get(random.nextInt(casesAccessibles.size()));
        deplacerVers(destination);
    }
}