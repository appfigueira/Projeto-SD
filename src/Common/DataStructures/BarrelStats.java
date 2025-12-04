package Common.DataStructures;

import java.io.Serializable;

public class BarrelStats implements Serializable {
    private boolean status;
    private final String name;
    private final int nPagesReceived;
    private final int nPages;
    private final int nTokens;
    private final int nTokenURLs;
    private final int nURLs;
    private final int nLinkingURLs;
    private float avgResponseTime;
    private int nRequests;

    //Barrel Online
    public BarrelStats(String name, int nPagesReceived, int nPages, int nTokens, int nTokenURLs, int nURLs, int nLinkingURL) {
        this.name = name;
        this.nPagesReceived = nPagesReceived;
        this.nPages = nPages;
        this.nTokens = nTokens;
        this.nTokenURLs = nTokenURLs;
        this.nURLs = nURLs;
        this.nLinkingURLs = nLinkingURL;
    }

    //Barrel Offline
    public BarrelStats(String name) {
        this.name = name;
        this.nPagesReceived = 0;
        this.nPages = 0;
        this.nTokens = 0;
        this.nTokenURLs = 0;
        this.nURLs = 0;
        this.nLinkingURLs = 0;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setAvgResponseTime(float avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public void setNRequests(int nRequests) {
        this.nRequests = nRequests;
    }

    public boolean getStatus() { return status; }

    public String getName() { return name; }

    public int getPagesReceived() { return nPagesReceived;}

    public int getPages() { return nPages; }

    public int getTokens() { return nTokens; }

    public int getTokenURLs() { return nTokenURLs; }

    public int getURLs() { return nURLs; }

    public int getLinkingURLs() { return nLinkingURLs; }

    public float getAvgResponseTime() { return avgResponseTime; }

    public int getNRequests() {return nRequests; }
}