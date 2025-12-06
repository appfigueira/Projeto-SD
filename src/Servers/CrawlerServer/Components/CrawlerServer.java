package Servers.CrawlerServer.Components;


import Common.Functions.RMIConnectionManager;
import Servers.BarrelServer.Interfaces.IBarrelCrawler;
import Servers.CrawlerServer.DataStructures.BarrelInfo;
import Servers.CrawlerServer.Interfaces.ICrawlerBarrel;
import Servers.GatewayServer.Interfaces.IGatewayCrawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

//NOTE: Commented code is my preferred solution, way better performance for very small data consistency loss

public class CrawlerServer extends UnicastRemoteObject implements ICrawlerBarrel {

    private static URLQueue queue;
    /*
    private static final List<BarrelSender> barrelSenders = new ArrayList<>();
    private static final List<Thread> barrelSenderThreads = new ArrayList<>();
     */
    private static final List<Crawler> crawlers = new ArrayList<>();
    private static final List<Thread> crawlerThreads = new ArrayList<>();
    private static List<BarrelInfo> barrelsList = Collections.synchronizedList(new ArrayList<>());
    private static RMIConnectionManager<IGatewayCrawler> gatewayConnectionManager = null;

    public CrawlerServer(List<BarrelInfo> barrelsList) throws RemoteException {
        super();
        CrawlerServer.barrelsList = barrelsList;
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    //Save barrel stub for callback
    @Override
    public boolean registerBarrel(String barrelName, IBarrelCrawler stub) throws RemoteException {
        for (BarrelInfo barrelInfo : barrelsList) {
            if (barrelName.equals(barrelInfo.getBarrelName())) {
                barrelInfo.setStub(stub);
                barrelInfo.setStatus(true);
                //System.out.println("[Crawler Server] Barrel Info updated for Barrel '" + barrelName + "'.");
                return true;
            }
        }

        for (BarrelInfo barrelInfo : barrelsList) {
            if (barrelInfo.getStub() == null) {
                barrelInfo.setBarrelName(barrelName);
                barrelInfo.setStub(stub);
                barrelInfo.setStatus(true);
                System.out.println("[Crawler Server] Barrel '" + barrelName + "' registered to Crawler Server.");
                return true;
            }
        }

        System.err.println("[Crawler Server] Error: Failed to register Barrel '" + barrelName + "'. Barrel limit reached.");
        return false;
    }

    //Remove barrel stub for callback
    @Override
    public void unregisterBarrel(String barrelName) throws RemoteException {
        for (BarrelInfo barrelInfo : barrelsList) {
            if (barrelName.equals(barrelInfo.getBarrelName())) {
                barrelInfo.setStatus(false);
                barrelInfo.setStub(null);
                barrelInfo.setBarrelName(null);
                System.out.println("[Crawler Server] Barrel '" + barrelName + "' unregistered from Crawler Server.");
                return;
            }
        }
        System.err.println("[Crawler Server] Error: Failed to unregister Barrel '" + barrelName + "'. Barrel not found.");
    }

    /*
    private static List<BarrelSender> startBarrelSenders() throws RemoteException, InterruptedException {
        while (true) {
            IGatewayCrawler gatewayStub = gatewayConnectionManager.connect(IGatewayCrawler.class);
            if (gatewayStub == null) {
                System.err.println("[Crawler Server] Gateway Service unavailable. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            int numberOfBarrels = gatewayStub.getNumberOfBarrels();
            if (numberOfBarrels == 0) {
                System.err.println("[Client] Error: Number of barrels not submitted to Gateway. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            for (int i = 0; i < numberOfBarrels; i++) {
                barrelsList.add(new BarrelInfo());
            }

            List<String> barrelSenderNames = gatewayStub.getBarrelNames();
            if (barrelSenderNames == null) {
                System.err.println("[Client] Error: Barrel names not yet submitted to Gateway. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            for (int id = 0; id < numberOfBarrels; id++) {
                BarrelSender barrelSender = new BarrelSender(id, barrelsList);
                barrelSenders.add(barrelSender);
                Thread barrelSenderThread = new Thread(barrelSender);
                barrelSenderThreads.add(barrelSenderThread);
                barrelSenderThread.start();
            }

            System.out.println("[Crawler Server] " + numberOfBarrels + " Barrel Sender threads started.");
            return barrelSenders;
        }
    }

    private static void startCrawlersWithBarrelSenders(URLQueue queue, List<BarrelSender> barrelSenders) throws RemoteException, InterruptedException {
        while (true) {
            IGatewayCrawler gatewayStub = gatewayConnectionManager.connect(IGatewayCrawler.class);
            if (gatewayStub == null) {
                System.err.println("[Crawler Server] Gateway Service unavailable. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            int numberOfCrawlers = gatewayStub.getNumberOfCrawlers();
            if (numberOfCrawlers == 0) {
                System.err.println("[Client] Error: Number of crawlers not yet submitted to Gateway. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            System.out.println("[Crawler Server] Starting " + numberOfCrawlers + " crawler threads...");
            for (int id = 0; id < numberOfCrawlers; id++) {
                Crawler crawler = new Crawler(id, queue, barrelSenders);
                crawlers.add(crawler);
                Thread crawlerThread = new Thread(crawler);
                crawlerThreads.add(crawlerThread);
                crawlerThread.start();
            }
            System.out.println("[Crawler Server] " + numberOfCrawlers + " Crawler threads started.");
            break;
        }
    }
     */

    private static void startCrawlers() throws RemoteException, InterruptedException {
        while (true) {
            IGatewayCrawler gatewayStub = gatewayConnectionManager.connect(IGatewayCrawler.class);
            if (gatewayStub == null) {
                System.err.println("[Crawler Server] Gateway Service unavailable. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            int numberOfBarrels = gatewayStub.getNumberOfBarrels();
            if (numberOfBarrels == 0) {
                System.err.println("[Client] Error: Number of barrels not submitted to Gateway. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            for (int i = 0; i < numberOfBarrels; i++) {
                barrelsList.add(new BarrelInfo());
            }

            int numberOfCrawlers = gatewayStub.getNumberOfCrawlers();
            if (numberOfCrawlers == 0) {
                System.err.println("[Client] Error: Number of crawlers not yet submitted to Gateway. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            System.out.println("[Crawler Server] Starting " + numberOfCrawlers + " crawler threads...");
            for (int id = 0; id < numberOfCrawlers; id++) {
                Crawler crawler = new Crawler(id, queue, barrelsList);
                crawlers.add(crawler);
                Thread crawlerThread = new Thread(crawler);
                crawlerThreads.add(crawlerThread);
                crawlerThread.start();
            }
            System.out.println("[Crawler Server] " + numberOfCrawlers + " Crawler threads started.");
            break;
        }
    }

    private static void startCrawlerServer() throws InterruptedException, RemoteException, MalformedURLException {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream("files/SystemConfiguration")) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("[Barrel Server] Failed to load SystemConfiguration file.");
            e.printStackTrace();
            shutdown();
        }

        //Crawler Server
        String crawlerHost = config.getProperty("crawler.host", "crawlerHost");
        int crawlerPort = Integer.parseInt(config.getProperty("crawler.port", "1100"));
        String crawlerServiceName = config.getProperty("crawler.serviceName", "CrawlerServer");
        String queueServiceName = config.getProperty("crawler.queue", "URLQueue");
        boolean readURLFile = Boolean.parseBoolean(config.getProperty("crawler.readFile", "false"));

        //URL Queue
        queue = new URLQueue();
        CrawlerServer crawlerServer = new CrawlerServer(barrelsList);
        System.out.println("[Crawler Server] URLQueue created.");
        if (readURLFile) {
            queue.loadURLsFromFile("files/URLs");
            System.out.println("[Crawler Server] URLQueue loaded with starting URLs.");
        }
        else {System.out.println("[Crawler Server] URLQueue is empty.");}

        //Gateway Server
        String gatewayHost = config.getProperty("gateway.host", "crawlerHost");
        int gatewayPort = Integer.parseInt(config.getProperty("gateway.portRMI", "1099"));
        String gatewayServiceName = config.getProperty("gateway.serviceName", "Gateway");

        gatewayConnectionManager = new RMIConnectionManager<>(gatewayHost, gatewayPort, gatewayServiceName);

        /*
        barrelSenders = startBarrelSenders();
        startCrawlersWithBarrelSenders(queue, barrelSenders);
         */

        startCrawlers();

        //Crawler Registry
        System.setProperty("java.rmi.server.hostname", crawlerHost);
        Registry crawlerRegistry = LocateRegistry.createRegistry(crawlerPort);
        Naming.rebind("rmi://" + crawlerHost + ":1100/URLQueue", queue);
        Naming.rebind("rmi://" + crawlerHost + ":1100/CrawlerServer", crawlerServer);
        System.out.println("[Crawler Server] Crawler RMI Registry started on port: " + crawlerPort);

        //Export URLQueue
        crawlerRegistry.rebind(queueServiceName, queue);
        System.out.println("[Crawler Server] URLQueue bound to Registry.");

        //Export CrawlerServer
        crawlerRegistry.rebind(crawlerServiceName, crawlerServer);
        System.out.println("[Crawler Server] CrawlerServer bound to Registry.");

        System.out.println("[Crawler Server] Crawler server set up successfully.");
    }

    public static void shutdown() throws InterruptedException {
        System.out.println("[Crawler Server] Crawler Server shutting down...");

        //Stop Crawlers
        for (Crawler crawler : crawlers) {
            crawler.shutdown();
        }


        //Wait Crawlers
        for (Thread t : crawlerThreads) {
            t.join();
        }

        /*
        //Stop Barrel Senders
        for (BarrelSender barrelSender : barrelSenders) {
            barrelSender.shutdown();
        }

        //Wait Barrel Senders
        for (Thread t : barrelSenderThreads) {
            t.join();
        }
         */

        if (queue != null) {
            try {
                java.rmi.server.UnicastRemoteObject.unexportObject(queue, true);
            } catch (Exception e) {
                System.err.println("[Barrel Server] Error during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[Barrel Server] Barrel Server shutdown complete.");
        System.exit(0);
    }

    static void main() throws InterruptedException {
        Thread startThread = new Thread(() -> {
            try {
                startCrawlerServer();
            } catch (InterruptedException | RemoteException | MalformedURLException e) {
                e.printStackTrace();
            }
            System.out.println("[Gateway Server] Press Enter to shutdown Crawler Server.");
        });
        startThread.start();

        boolean run = true;
        while (run) {
            System.out.println("[Barrel Server] Press Enter to shutdown Crawler Server.");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                run = false;
            }
            else {
                System.out.println("[Crawler Server] Invalid input. Please try again...");
            }
        }
        startThread.interrupt();
        startThread.join();
        shutdown();
    }
}