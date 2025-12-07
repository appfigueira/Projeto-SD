package pt.dei.googol.Projeto_SD.Common.DataStructures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SystemStats implements Serializable {
    private final List<String> top10Searches;
    private final List<BarrelStats> barrelsStats = new ArrayList<>();

    public SystemStats(List<String> top10Searches) {
        this.top10Searches = top10Searches;
    }

    public void addBarrelStats(BarrelStats barrelStats) {
        if (barrelStats == null) return;
        this.barrelsStats.add(barrelStats);
    }


    public List<String> getTop10Searches() {
        return top10Searches;
    }

    public List<BarrelStats> getBarrelsStats() {
        return barrelsStats;
    }
}