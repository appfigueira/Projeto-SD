package pt.dei.googol.Projeto_SD.Servers.GatewayServer.Components;


import pt.dei.googol.Projeto_SD.Common.Functions.RMIConnectionManager;
import pt.dei.googol.Projeto_SD.Common.DataStructures.*;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelBarrel;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelGateway;
import pt.dei.googol.Projeto_SD.Servers.CrawlerServer.Interfaces.ICrawlerGateway;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.DataStructures.BarrelInfo;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces.IGatewayBarrel;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces.IGatewayWeb;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces.IGatewayCrawler;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Interfaces.IWebGateway;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GatewayServer extends UnicastRemoteObject implements IGatewayWeb, IGatewayCrawler, IGatewayBarrel {
    private static String gatewayServiceName = null;
    private GatewayServer gateway = null;
    private static int gatewayPort; // RMI registry port
    private static int numberOfCrawlers;
    private static int numberOfBarrels;
    private static volatile boolean pingRunning = true;
    private Thread pingThread = null;
    private static final ConcurrentHashMap<String, BarrelInfo> barrelsMap = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<BarrelInfo> barrelsList = new CopyOnWriteArrayList<>();
    private static final List<String> barrelNames = new ArrayList<>();
    private static final ConcurrentHashMap<String, BarrelStats> barrelsStats = new ConcurrentHashMap<>();
    private static final Map<String, Integer> searchCounts = new HashMap<>();
    private static final Map<String, Long> barrelsLastResponseTime = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> barrelsAvgResponseTime = new ConcurrentHashMap<>(); //<barrel, time(ms)>
    private static final ConcurrentHashMap<String, Integer> barrelsNRequests = new ConcurrentHashMap<>(); //<barrel, nRequests>
    private static final List<IWebGateway> webServers = new CopyOnWriteArrayList<>();
    private static RMIConnectionManager<ICrawlerGateway> crawlerConnectionManager = null;

    protected GatewayServer() throws RemoteException {
        super();
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    //status values:
    //-1: Error
    //0: Success
    //1: Already Index (comes from Barrel)
    //2: Invalid URL (comes from Crawler)
    @Override
    public int indexURLClientGateway(String url) throws RemoteException {
        System.out.println("[Gateway Server] Received URL from Client: " + url);

        //Barrel Server connection
        IBarrelGateway barrelStub = null;
        String barrelName = null;
        for (BarrelInfo barrelInfo : barrelsList) {
            barrelStub = barrelInfo.stub();
            try {
                if (barrelStub.ping()) {
                    barrelName = barrelInfo.name();
                    System.out.println("[Gateway Server] Connected to Barrel '" + barrelInfo.name() + "'.");
                    break;
                } else {
                    System.err.println("[Gateway Server] Error: Failed to connect to Barrel '" + barrelInfo.name() + "'.");
                }
            } catch (RemoteException e) {
                System.err.println("[Gateway Server] Error: Failed to connect to Barrel '" + barrelInfo.name() + "'.");
            }
        }

        boolean indexedURL = false;

        //Barrel connected
        if (barrelStub != null) {
            //Check if URL is already indexed
            try {
                long startTime = System.currentTimeMillis();

                indexedURL = barrelStub.checkURL(url);

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                updateBarrelStats(barrelName, duration);
            }
            catch (RemoteException e) {
                System.err.println("[Gateway Server] Error: Failed to check URL on Barrel:");
                e.printStackTrace();
            }
        }
        else {System.err.println("[Gateway Server] No barrels available to verify if URL is already submitted.");}

        if (indexedURL) {
            return 1;
        }

        //Crawler Server connection
        ICrawlerGateway crawlerStub = crawlerConnectionManager.connect(ICrawlerGateway.class);
        if (crawlerStub == null) {
            System.err.println("[Gateway Server] Error: Cannot connect to Crawler Server. URL '" + url + "' not submitted.");
            return -1;
        }
        try {
            int code = crawlerStub.indexURLGatewayCrawler(url);
            System.out.println("[Gateway Server] URL sent to Crawler Queue: " + url);
            return code;
        }
        catch (RemoteException e) {
            System.err.println("[Gateway Server] Error: Failed to send URL to Crawler:");
            e.printStackTrace();
        }
        return -1;
    }

    //status values:
    //-1: Error
    //0: Results
    //1: Empty Results (comes from Barrel)
    @Override
    public SearchResult searchClientGateway(List<String> searchWords, int pageNumber, int URLsPerPage) throws RemoteException {
        System.out.println("[Gateway Server] Received search from Client: " + String.join(" ", searchWords));

        //Save search to searchCounts index
        searchCounts.merge(String.join(" ", searchWords), 1, Integer::sum);
        updateSystemStats(null);
        String barrelName = null;

        //Barrel Server connection
        IBarrelGateway barrelStub = null;
        for (BarrelInfo barrelInfo : barrelsList) {
            barrelStub = barrelInfo.stub();
            try {
                if (barrelStub.ping()) {
                    System.out.println("[Gateway Server] Connected to Barrel '" + barrelInfo.name() + "'.");
                    barrelName = barrelInfo.name();
                    break;
                } else {
                    System.err.println("[Gateway Server] Error: Failed to connect to Barrel '" + barrelInfo.name() + "'.");
                }
            } catch (RemoteException e) {
                System.err.println("[Gateway Server] Error: Failed to connect to Barrel '" + barrelInfo.name() + "'.");
            }
        }

        //No Barrel Connected
        if (barrelStub == null) {
            System.err.println("[Gateway Server] No barrels available.");
            return new SearchResult(-1, null);
        }

        //Search
        try {
            long startTime = System.currentTimeMillis();

            SearchResult searchResult = barrelStub.searchGatewayBarrel(searchWords, pageNumber, URLsPerPage);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            updateBarrelStats(barrelName, duration);

            return searchResult;
        }
        catch (RemoteException e) {
            System.err.println("[Gateway Server] Error: Failed to call search on Barrel:");
            e.printStackTrace();
            return new SearchResult(-1, null);
        }
    }

    //status values:
    //-1: Error
    //0: Links
    //1: Empty Links (comes from Barrel)
    @Override
    public LinkingURLsResult getLinkingURLsClientGateway(String url) {
        System.out.println("[Gateway Server] Received target URL from Client: " + url);
        String barrelName = null;

        //Barrel Server connection
        IBarrelGateway barrelStub = null;
        for (BarrelInfo barrelInfo : barrelsList) {
            barrelStub = barrelInfo.stub();
            try {
                if (barrelStub.ping()) {
                    System.out.println("[Gateway Server] Connected to Barrel '" + barrelInfo.name() + "'.");
                    barrelName = barrelInfo.name();
                    break;
                } else {
                    System.err.println("[Gateway Server] Error: Failed to connect to Barrel '" + barrelInfo.name() + "'.");
                }
            } catch (RemoteException e) {
                System.err.println("[Gateway Server] Error: Failed to connect to Barrel '" + barrelInfo.name() + "'.");
            }
        }

        //No barrel connected
        if (barrelStub == null) {
            System.err.println("[Gateway Server] No barrels available.");
            return new LinkingURLsResult(-1, null);
        }

        //Get Linking URLs
        try {
            long startTime = System.currentTimeMillis();

            LinkingURLsResult linkingURLsResult = barrelStub.getLinkingURLsGatewayBarrel(url);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            updateBarrelStats(barrelName, duration);

            return linkingURLsResult;
        }
        catch (RemoteException e) {
            System.err.println("[Gateway Server] Error: Failed to call search on Barrel:");
            e.printStackTrace();
            return new LinkingURLsResult(-1, null);
        }
    }

    //Asks each barrel for barrel stats, turns on barrel stats export
    @Override
    public SystemStats getSystemStats() throws RemoteException {
        System.out.println("[Gateway Server] Received System Stats request from Client.");
        List<String> top10Searches = getTop10Searches();
        SystemStats systemStats = new SystemStats(top10Searches);
        BarrelStats barrelStats;

        //Barrel Server connection
        for (String barrelName : barrelNames) {
            BarrelInfo barrelInfo = null;
            IBarrelGateway barrelStub = null;
            for (BarrelInfo info : barrelsList) {
                if (info.name().equals(barrelName)) {
                    barrelInfo = info;
                    barrelStub = info.stub();
                    break;
                }
            }

            try {
                if (barrelStub != null && barrelStub.ping()) {
                    System.out.println("[Gateway Server] Connected to Barrel '" + barrelInfo.name() + "'.");

                    barrelStats = barrelStub.getBarrelStats();

                    //Add status to barrelStats
                    barrelStats.setStatus(true);

                    //Add Response Times and Number of Requests made by Gateway to Barrel to barrelStats
                    addGatewayToBarrelStats(barrelStats);

                    //Add barrelStats to systemStats
                    systemStats.addBarrelStats(barrelStats);
                } else {
                        System.err.println("[Gateway Server] Error: Failed to connect to Barrel '" + barrelName + "'.");
                        barrelStats = new BarrelStats(barrelName);

                        //Add status to barrelStats
                        barrelStats.setStatus(false);

                        //Add barrelStats to systemStats
                        systemStats.addBarrelStats(barrelStats);
                }
            } catch (RemoteException e) {
                System.err.println("[Gateway Server] Error: Failed to connect to Barrel '" + barrelInfo.name() + "'.");
                barrelStats = new BarrelStats(barrelInfo.name());

                //Add status to barrelStats
                barrelStats.setStatus(false);

                //Add barrelStats to systemStats
                systemStats.addBarrelStats(barrelStats);
            }
            saveBarrelStats(barrelStats);
        }
        return systemStats;
    }

    //Barrel sends barrel stats everytime stats change, then sent to client
    @Override
    public void updateSystemStats(BarrelStats newBarrelStats) throws RemoteException {
        //Stop sending barrel stats if no clients are "listening"
        if (webServers.isEmpty()){
            for (BarrelInfo barrelInfo : barrelsList) {
                IBarrelGateway barrelStub = barrelInfo.stub();
                try {
                    if (barrelStub.ping()) {
                        barrelStub.stopExportBarrelStats();
                    }
                } catch (RemoteException e) {
                    System.err.println("[Gateway Server] Error: Failed to connect to Barrel '" + barrelInfo.name() + "'.");
                }
            }
        }

        if (newBarrelStats != null){
            //Add status to barrelStats
            newBarrelStats.setStatus(true);

            //Add Response Times and Number of Requests made by Gateway to Barrel to barrelStats
            addGatewayToBarrelStats(newBarrelStats);

            saveBarrelStats(newBarrelStats);
        }

        List<String> top10Searches = getTop10Searches();
        SystemStats systemStats = new SystemStats(top10Searches);

        synchronized (barrelsStats) {
            for (String barrelName : barrelNames) {
                BarrelStats barrelStats = barrelsStats.get(barrelName);
                if (barrelStats != null) {
                    systemStats.addBarrelStats(barrelStats);
                }
            }
        }

        for (IWebGateway webServer : webServers) {
            try {
                webServer.updateSystemStats(systemStats);
            } catch (RemoteException e) {
                System.err.println("[Gateway Server] Failed to update a Client. Removing Client from list.");
                webServers.remove(webServer);
            }
        }
    }

    private void saveBarrelStats(BarrelStats barrelStats) {
        if (barrelStats != null) {barrelsStats.put(barrelStats.getName(), barrelStats);}
    }

    public void updateBarrelStats(String barrelName, long duration){
        barrelsNRequests.merge(barrelName, 1, Integer::sum);
        int barrelNRequests = barrelsNRequests.get(barrelName);

        long avgResponseTime = barrelsAvgResponseTime.getOrDefault(barrelName, 0L);
        avgResponseTime = avgResponseTime + (duration - avgResponseTime) / barrelNRequests; //Increments Avg Response Time by recalculating avgResponseTime
        barrelsAvgResponseTime.put(barrelName, avgResponseTime);

        barrelsLastResponseTime.put(barrelName, duration);

        reorderBarrels();
    }

    //Reorder barrels for fastest barrel connection
    public static void reorderBarrels() {
        List<BarrelInfo> copy = new ArrayList<>(barrelsList);
        copy.sort(Comparator.comparingLong(bi ->
                barrelsLastResponseTime.getOrDefault(bi.name(), Long.MAX_VALUE)
        ));
        barrelsList.clear();
        barrelsList.addAll(copy);
    }

    public List<String> getTop10Searches() {
        return searchCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) //Compare count value, put in reverse order
                .limit(10).map(Map.Entry::getKey).toList();
    }

    private void addGatewayToBarrelStats(BarrelStats barrelStats) {
        String barrelName = barrelStats.getName();

        //Get time in ds
        float avgResponseTime = (float) barrelsAvgResponseTime.getOrDefault(barrelName, 0L) / 100;

        //Get number of requests
        int nRequests = barrelsNRequests.getOrDefault(barrelName, 0);

        barrelStats.setAvgResponseTime(avgResponseTime);
        barrelStats.setNRequests(nRequests);
    }

    //Save web server stub for callback
    @Override
    public void registerWebServer(IWebGateway webServer) {
        if (!webServers.contains(webServer)) {
            webServers.add(webServer);
        }
    }

    //Remove web server stub for callback
    @Override
    public void unregisterWebServer(IWebGateway webServer) {
        webServers.remove(webServer);
        System.out.println("[Gateway] Web Server unregistered.");
    }

    @Override
    public int getNumberOfCrawlers() throws RemoteException {
        return numberOfCrawlers;
    }

    @Override
    public int getNumberOfBarrels() throws RemoteException {
        return numberOfBarrels;
    }

    //Return barrel name if:
    @Override
    public String getAvailableBarrelName() throws RemoteException {
        for (String candidate : barrelNames) {
            BarrelInfo barrelInfo = barrelsMap.get(candidate);
            //Barrel not registered
            if (barrelInfo == null) {
                return candidate;
            }
            //Barrel "dead"
            try {
                if (!barrelInfo.stub().ping()) {
                    return candidate;
                }
            }
            //Barrel failed to respond
            catch (RemoteException e) {
                return candidate;
            }
        }
        //No barrel names available
        return null;
    }

    //Assign backup barrel to barrel (barrelName)
    @Override
    public BackupBarrelInfo getBackupBarrel(String barrelName) {
        System.out.println("[Gateway Server] Barrel '" + barrelName + "' requested backup barrel.");

        synchronized (barrelsList) {
            for (BarrelInfo barrelInfo : barrelsList) {
                String backupName = barrelInfo.name();
                IBarrelBarrel backupStub = barrelInfo.backupStub();

                if (backupName != null && !backupName.equals(barrelName) && backupStub != null) {
                    System.out.println("[Gateway Server] Barrel '" + backupName + "' assigned as backup barrel to '" + barrelName + "'.");
                    return new BackupBarrelInfo(backupName, backupStub);
                }
            }
        }

        System.out.println("[Gateway Server] No backup barrels available for '" + barrelName + "'.");
        return null;
    }

    //Save barrel stub for callback
    @Override
    public synchronized boolean registerBarrel(String barrelName, IBarrelGateway barrelStub, IBarrelBarrel backupStub) throws RemoteException {
        if (barrelsMap.containsKey(barrelName)) {
            BarrelInfo barrel = new BarrelInfo(barrelName, barrelStub, backupStub);
            barrelsMap.put(barrelName, barrel);
            for (int i = 0; i < barrelsList.size(); i++) {
                if (barrelsList.get(i).name().equals(barrelName)) {
                    barrelsList.set(i, barrel);
                    break;
                }
            }
            //System.out.println("[Gateway Server] BarrelInfo '" + barrelName + "' updated to Gateway Server.");
            return true;
        }
        if (barrelsMap.size() >= numberOfBarrels) {
            System.err.println("[Gateway Server] Error: Failed to register Barrel '" + barrelName + "'. Barrel limit reached.");
            return false;
        }
        BarrelInfo barrel = new BarrelInfo(barrelName, barrelStub, backupStub);
        barrelsMap.put(barrelName, barrel);
        barrelsList.add(barrel);
        System.out.println("[Gateway Server] Barrel '" + barrelName + "' registered to Gateway Server.");
        return true;
    }

    //Remove barrel stub for callback
    @Override
    public synchronized void unregisterBarrel(String barrelName) throws RemoteException {
        BarrelStats stats = barrelsStats.get(barrelName);
        if (stats != null) {
            stats.setStatus(false);
        } else {
            System.err.println("[Gateway Server] Error: Failed to set Barrel '" + barrelName + "' status to 'false'. Barrel not found.");
        }

        barrelsMap.remove(barrelName);
        boolean removedFromList = barrelsList.removeIf(bi -> bi.name().equals(barrelName));
        if (!removedFromList) {
            System.err.println("[Gateway Server] Error: Failed to remove Barrel '" + barrelName + "' stub from list. Barrel not found.");
        }
    }

    //Ping thread periodically pings barrels to get their response times to reorder barrel list and update barrel status
    private void startPingThread() {
        pingThread = new Thread(() -> {
            while (pingRunning) {
                for (BarrelInfo barrelInfo : barrelsList) {
                    IBarrelGateway barrelStub = barrelInfo.stub();
                    String barrelName = barrelInfo.name();
                    BarrelStats barrelStats = barrelsStats.get(barrelName);
                    try {
                        long startTime = System.currentTimeMillis();
                        boolean alive = barrelStub != null && barrelStub.ping();
                        long duration = System.currentTimeMillis() - startTime;


                        if (alive) {
                            if (barrelStats != null){barrelStats.setStatus(true);}
                            barrelsLastResponseTime.put(barrelName, duration);
                        } else {
                            if (barrelStats != null){barrelStats.setStatus(false);}
                            barrelsLastResponseTime.put(barrelName, Long.MAX_VALUE);
                        }
                    } catch (RemoteException e) {
                        if (barrelStats != null){barrelStats.setStatus(false);}
                        barrelsLastResponseTime.put(barrelName, Long.MAX_VALUE);
                    }
                }

                reorderBarrels();

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        });
        pingThread.setDaemon(true);
        pingThread.start();
    }

    private void setupSystem() throws RemoteException, MalformedURLException {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream("files/SystemConfiguration")) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("[Gateway Server] Error: Failed to load SystemConfiguration file.");
            e.printStackTrace();
        }

        //Gateway Server
        String gatewayHost = config.getProperty("gateway.host", "localhost");
        gatewayPort = Integer.parseInt(config.getProperty("gateway.portRMI", "1099"));
        gatewayServiceName = config.getProperty("gateway.serviceName", "Gateway");

        try {
            gateway = new GatewayServer();
            // Create Registry RMI
            System.setProperty("java.rmi.server.hostname", gatewayHost);
            Registry registry = LocateRegistry.createRegistry(gatewayPort);
            Naming.rebind("rmi://" + gatewayHost + ":1099/Gateway", gateway);
            System.out.println("[Gateway Server] Gateway RMI Registry started on port: " + gatewayPort);

            registry.rebind(gatewayServiceName, gateway);

            System.out.println("[Gateway Server] Gateway bound to Registry.");

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //Crawler Server
        String crawlerHost = config.getProperty("crawler.host", "localhost");
        int crawlerPort = Integer.parseInt(config.getProperty("crawler.portRMI", "1100"));
        String queueServiceName = config.getProperty("crawler.queue", "CrawlerServer");

        try {
            numberOfCrawlers = Integer.parseInt(config.getProperty("crawler.numberOfCrawlers", "0"));
        }
        catch (NumberFormatException e) {
            System.err.println("[Gateway Server] Error: Invalid number of Downloaders/Crawlers. Number of Downloaders/Crawlers must be an Integer. Please modify SystemConfiguration File.");
            shutdown();
        }
        if (numberOfCrawlers > 0) {
            System.out.println("[Gateway Server] Number of Downloaders/Crawlers set to: " + numberOfCrawlers);
        }
        else {
            System.err.println("[Gateway Server] Error: Invalid number of Downloaders/Crawlers. Number of Downloaders/Crawlers must be greater than 0. Please modify SystemConfiguration File.");
            shutdown();
        }

        crawlerConnectionManager = new RMIConnectionManager<>(crawlerHost, crawlerPort, queueServiceName);
        System.out.println("[Gateway Server] Crawler Server set up successfully.");

        // Barrel Servers
        try {
            numberOfBarrels = Integer.parseInt(config.getProperty("barrels.numberOfBarrels", "0"));
        }
        catch (NumberFormatException e) {
            System.err.println("[Gateway Server] Error: Invalid number of Barrel Servers. Number of Barrel Servers must be an Integer. Please modify SystemConfiguration File.");
            shutdown();
        }
        if (numberOfBarrels > 0) {
            System.out.println("[Gateway Server] Number of Barrel Servers set to: " + numberOfBarrels);
        }
        else {
            System.err.println("[Gateway Server] Error: Invalid number of Barrel Servers. Number of Barrel Servers must be greater than 0. Please modify SystemConfiguration File.");
            shutdown();
        }

        String[] barrelNamesConfig = config.getProperty("barrels.serviceNames", "").split(",");
        int count = 0;
        for (String barrelServiceName : barrelNamesConfig) {
            barrelServiceName = barrelServiceName.trim();
            if (barrelServiceName.isBlank()) {
                System.err.println("[Gateway Server] Error: Invalid Barrel Server name. Please modify SystemConfiguration File.");
                shutdown();
            }

            if (count >= numberOfBarrels) {break;}

            barrelNames.add(barrelServiceName);
            count++;
        }
        System.out.println("[Gateway Server] Barrel Server names:\n" + String.join("\n", barrelNames));

        startPingThread();
        System.out.println("[Gateway Server] Server Ping thread started.");

        System.out.println("[Gateway Server] System set up successfully.");
    }

    void shutdown() throws RemoteException {
        System.out.println("[Gateway Server] Shutting down System...");
        System.out.println("[Gateway Server] Shutting down Crawler Server...");

        //Crawler Server connection
        if (crawlerConnectionManager != null) {
            ICrawlerGateway crawlerStub = crawlerConnectionManager.connect(ICrawlerGateway.class);
            if (crawlerStub == null) {
                System.err.println("[Gateway Server] Error: Failed to connect to Crawler Server. " +
                        "Crawler Server may already be offline. Please check Crawler Server manually.");
            }
            else {
                try {
                    crawlerStub.systemShutdown();

                    //Confirm Crawler Server is offline
                    for (int i = 0; i < 10; i++) { //10s
                        Thread.sleep(1000);
                        crawlerStub = crawlerConnectionManager.connect(ICrawlerGateway.class);
                        if (crawlerStub == null) break;
                        System.out.println(".");
                    }
                    if (crawlerStub == null) {
                        System.out.println("[Gateway Server] Crawler Server shutdown.");
                    } else {
                        System.err.println("[Gateway Server] Error: Failed to shutdown to Crawler Server. " +
                                "Crawler Server may still be running. Please check Crawler Server manually.");
                    }

                } catch (RemoteException e) {
                    System.out.println("[Gateway Server] Crawler Server shutdown.");
                } catch (InterruptedException ignored) {}
            }
        }

        //Barrel Servers connection
        System.out.println("[Gateway Server] Shutting down Barrel Servers...");

        pingRunning = false;
        try {
            if (pingThread != null) {
                pingThread.join();
            }
        } catch (InterruptedException ignored) {}

        for (BarrelInfo barrelInfo : barrelsList) {
            String barrelName = barrelInfo.name();
            System.out.print("[Gateway Server] Shutting down Barrel Server '" + barrelName + "'...");

            //Barrel Server connection
            IBarrelGateway barrelStub = barrelInfo.stub();
            boolean stopped = false;
            try {
                barrelStub.systemShutdown();

                //Confirm Crawler Server is offline
                for (int i = 0; i < 10; i++) { //10s
                    if (barrelStub.ping()) {
                        barrelStub.systemShutdown();
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException ignored) {
                        }
                        System.out.print(".");
                    }
                    else {
                        stopped = true;
                        break;
                    }
                }
            } catch (RemoteException e) {
                stopped = true;
            }
            catch (InterruptedException ignored) {}

            if (stopped) {
                System.out.println("[Gateway Server] Barrel Server '" + barrelName + "' shutdown.");
            }
            else {
                System.err.println("[Gateway Server] Error: Failed to shutdown to Barrel Server '" + barrelName + "'. " +
                        "Barrel Server '" + barrelName + "' may still be running. Please check Barrel Server '" + barrelName + "' manually.");
            }
        }

        //Unbind gateway object from Registry
        try {
            Registry registry = LocateRegistry.getRegistry(gatewayPort);
            registry.unbind(gatewayServiceName);
            UnicastRemoteObject.unexportObject(gateway, true);
            System.out.println("[Gateway Server] Gateway unbound from registry.");
        } catch (NotBoundException e) {
            System.err.println("[Gateway Server] Error: Gateway was not bound to registry.");
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        System.out.println("[Gateway Server] System shutdown complete.");
        System.exit(0);
    }

    void main() throws RemoteException {
        Thread startThread = new Thread(() -> {
            try {
                setupSystem();
            } catch (RemoteException | MalformedURLException e) {
                System.err.println("[Crawler Server] Error:");
                e.printStackTrace();
            }
            System.out.println("[Gateway Server] Press Enter to shutdown System.");
        });
        startThread.start();

        boolean run = true;
        while (run) {
            System.out.println("[Gateway Server] Press Enter to shutdown System.");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                run = false;
            }
            else {
                System.out.println("[Gateway Server] Invalid input. Please try again...");
            }
        }
        shutdown();
    }
}