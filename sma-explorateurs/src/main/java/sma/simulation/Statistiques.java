package sma.simulation;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Classe pour gérer les statistiques de la simulation
 * Thread-safe avec variables atomiques
 */
public class Statistiques {
    
    private final AtomicInteger totalIterations = new AtomicInteger(0);
    private final AtomicInteger totalCombats = new AtomicInteger(0);
    private final AtomicInteger totalMorts = new AtomicInteger(0);
    private final AtomicInteger totalTresorsCollectes = new AtomicInteger(0);
    private final AtomicInteger scoreTotal = new AtomicInteger(0);
    private final AtomicInteger zonesExplorees = new AtomicInteger(0);
    private final AtomicLong tempsDebut = new AtomicLong(0);
    private final AtomicLong tempsFin = new AtomicLong(0);
    
    private volatile boolean simulationTerminee = false;

    public void demarrer() {
        tempsDebut.set(System.currentTimeMillis());
        simulationTerminee = false;
    }

    public void terminer() {
        tempsFin.set(System.currentTimeMillis());
        simulationTerminee = true;
    }

    public void incrementerIterations() {
        totalIterations.incrementAndGet();
    }

    public void enregistrerCombat() {
        totalCombats.incrementAndGet();
    }

    public void enregistrerMort() {
        totalMorts.incrementAndGet();
    }

    public void enregistrerTresorCollecte(int valeur) {
        totalTresorsCollectes.incrementAndGet();
        scoreTotal.addAndGet(valeur);
    }

    public void setZonesExplorees(int nombre) {
        zonesExplorees.set(nombre);
    }

    // Getters
    public int getTotalIterations() { return totalIterations.get(); }
    public int getTotalCombats() { return totalCombats.get(); }
    public int getTotalMorts() { return totalMorts.get(); }
    public int getTotalTresorsCollectes() { return totalTresorsCollectes.get(); }
    public int getScoreTotal() { return scoreTotal.get(); }
    public int getZonesExplorees() { return zonesExplorees.get(); }
    
    public long getDureeSimulation() {
        if (tempsDebut.get() == 0) return 0;
        long fin = simulationTerminee ? tempsFin.get() : System.currentTimeMillis();
        return fin - tempsDebut.get();
    }

    public String getDureeFormatee() {
        long duree = getDureeSimulation();
        long secondes = duree / 1000;
        long minutes = secondes / 60;
        secondes = secondes % 60;
        return String.format("%02d:%02d", minutes, secondes);
    }

    public void reset() {
        totalIterations.set(0);
        totalCombats.set(0);
        totalMorts.set(0);
        totalTresorsCollectes.set(0);
        scoreTotal.set(0);
        zonesExplorees.set(0);
        tempsDebut.set(0);
        tempsFin.set(0);
        simulationTerminee = false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════╗\n");
        sb.append("║       STATISTIQUES SIMULATION        ║\n");
        sb.append("╠══════════════════════════════════════╣\n");
        sb.append(String.format("║ ⏱️  Durée: %-25s ║\n", getDureeFormatee()));
        sb.append(String.format("║ 🔄 Itérations: %-21d ║\n", totalIterations.get()));
        sb.append(String.format("║ ⚔️  Combats: %-23d ║\n", totalCombats.get()));
        sb.append(String.format("║ 💀 Morts: %-26d ║\n", totalMorts.get()));
        sb.append(String.format("║ 💰 Trésors: %-24d ║\n", totalTresorsCollectes.get()));
        sb.append(String.format("║ 🏆 Score: %-26d ║\n", scoreTotal.get()));
        sb.append(String.format("║ 🗺️  Zones explorées: %-15d ║\n", zonesExplorees.get()));
        sb.append("╚══════════════════════════════════════╝");
        return sb.toString();
    }
}
