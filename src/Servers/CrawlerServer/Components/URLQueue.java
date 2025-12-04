package Servers.CrawlerServer.Components;

import Common.Functions.URLCleaner;
import Servers.CrawlerServer.Interfaces.ICrawlerGateway;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class URLQueue extends UnicastRemoteObject implements ICrawlerGateway {

    private final BlockingQueue<String> URLs = new LinkedBlockingQueue<>();
    private final Set<String> visitedURLs = ConcurrentHashMap.newKeySet();

    public URLQueue() throws RemoteException {
        super();
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    @Override
    public void systemShutdown() throws RemoteException, InterruptedException {
        CrawlerServer.shutdown();
    }

    //Add URL RMI Method
    @Override
    public int submitURLGatewayCrawler(String url) throws RemoteException {
        url = URLCleaner.cleanURL(url);
        if (url == null) return 0;
        if (visitedURLs.add(url)) {
            URLs.add(url);
            addToVisited(url);
        }
        return 1;
    }

    //Add URL if New Local Method
    public void addURL(String url) {
        url = URLCleaner.cleanURL(url);
        if (url == null) return;
        if (visitedURLs.add(url)) {
            URLs.add(url);
            addToVisited(url);
        }
    }

    public void addToVisited(String url) {
        visitedURLs.add(url);
    }

    //Get URL Local Method
    public String getURL() throws InterruptedException {
        return URLs.take();
    }

    public void loadURLsFromFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String url;
            while ((url = br.readLine()) != null) {
                addURL(url.trim());
            }
            System.out.println("[Crawler Server] Finished loading URLs from file: '" + filename + "'.");
        } catch (IOException e) {
            System.out.println("[Crawler Server] Error: Failed to read file '" + filename + "'.");
            e.printStackTrace();
        }
    }
}