package pt.dei.googol.Projeto_SD.Common.DataStructures;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SystemStats implements Serializable {
    private final int status;
    private final List<String> top10Searches;
    private List<BarrelStats> barrelsStats = new ArrayList<>();

    public SystemStats() { //
        this.status = -1;
        this.top10Searches = null;
        this.barrelsStats = null;
    }

    public SystemStats(List<String> top10Searches) {
        this.status = 0;
        this.top10Searches = top10Searches;
    }

    public void addBarrelStats(BarrelStats barrelStats) {
        if (barrelStats == null) return;
        this.barrelsStats.add(barrelStats);
    }

    public int getStatus() {return status;}

    public List<String> getTop10Searches() {
        return top10Searches;
    }

    public List<BarrelStats> getBarrelsStats() {
        return barrelsStats;
    }
}