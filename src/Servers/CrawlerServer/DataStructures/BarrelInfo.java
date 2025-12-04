package Servers.CrawlerServer.DataStructures;

import Servers.BarrelServer.Interfaces.IBarrelCrawler;

public class BarrelInfo {
    private String barrelName;
    private IBarrelCrawler stub;
    boolean status;

    public BarrelInfo() {
        this.barrelName = null;
        this.stub = null;
        this.status = false;
    }

    public synchronized void setBarrelName(String name) {
        this.barrelName = name;
    }

    public synchronized void setStub(IBarrelCrawler stub) {
        this.stub = stub;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public synchronized String getBarrelName() {
        return barrelName;
    }

    public synchronized IBarrelCrawler getStub() {
        return stub;
    }

    public boolean getStatus() {
        return status;
    }
}