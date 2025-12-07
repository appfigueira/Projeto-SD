package pt.dei.googol.Projeto_SD.Servers.BarrelServer.Components;

import pt.dei.googol.Projeto_SD.Common.Functions.RMIConnectionManager;
import pt.dei.googol.Projeto_SD.Common.DataStructures.BackupBarrelInfo;
import pt.dei.googol.Projeto_SD.Common.DataStructures.BarrelStats;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.DataStructures.PageHeader;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelBarrel;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelCrawler;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelGateway;
import pt.dei.googol.Projeto_SD.Servers.CrawlerServer.Interfaces.ICrawlerBarrel;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces.IGatewayBarrel;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

public class BarrelServer {
    static String gatewayHost; // Gateway IP
    static int gatewayPort; // Gateway Registry Port
    private static volatile Barrel barrel;
    private static volatile boolean gatewayThreadRunning = true;
    private static volatile boolean crawlerThreadRunning = true;
    private static Thread gatewayConnectionThread = null;
    private static Thread crawlerConnectionThread = null;
    private static RMIConnectionManager<IGatewayBarrel> gatewayConnectionManager = null;
    private static RMIConnectionManager<ICrawlerBarrel> crawlerConnectionManager = null;

    private static void startBarrelServer() throws RemoteException, InterruptedException {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream("files/SystemConfiguration")) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("[Barrel Server] Failed to load SystemConfiguration file.");
            e.printStackTrace();
            shutdown();
        }

        //Gateway Server Connection Manager Setup
        gatewayHost = config.getProperty("gateway.host", "localhost");
        gatewayPort = Integer.parseInt(config.getProperty("gateway.portRMI", "1099"));
        String gatewayServiceName = config.getProperty("gateway.serviceName", "Gateway");
        gatewayConnectionManager = new RMIConnectionManager<>(gatewayHost, gatewayPort, gatewayServiceName);

        //Crawler Server Connection Manager Setup
        String crawlerHost = config.getProperty("crawler.host", "localhost");
        int crawlerPort = Integer.parseInt(config.getProperty("crawler.portRMI", "1100"));
        String crawlerServiceName = config.getProperty("crawler.serviceName", "CrawlerServer");
        crawlerConnectionManager = new RMIConnectionManager<>(crawlerHost, crawlerPort, crawlerServiceName);

        //Get barrel name from Gateway
        String barrelName = getBarrelName();
        System.out.println("[Barrel Server] Barrel name: '" + barrelName + "'.");

        //Start barrel empty BEFORE register
        barrel = new Barrel(barrelName);

        //Register barrel in Gateway and Crawler Servers
        registerBarrelToGateway(barrelName, barrel, barrel);
        registerBarrelToCrawler(barrelName, barrel);

        //Get backup barrel from Gateway Server
        BackupBarrelInfo backupBarrelInfo = getBackupBarrelInfo(barrelName);

        if (backupBarrelInfo != null) {
            String backupBarrelName = backupBarrelInfo.name();
            IBarrelBarrel backupBarrelStub = backupBarrelInfo.stub();

            if (backupBarrelStub == null) {
                System.out.println("[Barrel Server] Backup barrel '" + backupBarrelName + "' not available.");
                barrel.setReady(true);
            } else { //Barrel backup
                System.out.println("[Barrel Server] Retrieving backup data from '" + backupBarrelName + "'...");

                try {
                    Map<String, PageHeader> pageHeaderIndexCopy = backupBarrelStub.getPageHeaderIndex();
                    Map<String, Set<String>> tokenIndexCopy = backupBarrelStub.getTokenIndex();
                    Map<String, Set<String>> linkIndexCopy = backupBarrelStub.getLinkIndex();

                    barrel.restoreBackup(pageHeaderIndexCopy, tokenIndexCopy, linkIndexCopy);

                    System.out.println("[Barrel Server] Backup restore complete.");
                } catch (Exception e) {
                    System.err.println("[Barrel Server] Error retrieving backup: " + e.getMessage());
                }
            }
        } else {
            barrel.setReady(true);
        }
        startGatewayConnectionThread(barrelName, barrel, barrel);
        startCrawlerConnectionThread(barrelName, barrel);
    }

    public static void startGatewayConnectionThread(String barrelName, IBarrelGateway barrelStub, IBarrelBarrel backupStub) {
        gatewayConnectionThread = new Thread(() -> {
            while (gatewayThreadRunning) {
                try {
                    IGatewayBarrel gatewayStub = gatewayConnectionManager.connect(IGatewayBarrel.class);
                    if (gatewayStub != null) {
                        if (!gatewayStub.registerBarrel(barrelName, barrelStub, backupStub)) {
                            System.err.println("[Barrel Server] Gateway registration failed. Retrying in 5s...");
                        }
                    } else {
                        System.err.println("[Barrel Server] Gateway Server unavailable. Retrying in 5s...");
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    System.err.println("[Barrel Server] Gateway connection thread interrupted.");
                    Thread.currentThread().interrupt();
                    break;
                } catch (RemoteException e) {
                    System.err.println("[Barrel Server] Error: Failed to connect to Gateway");
                }
            }
            System.out.println("[Barrel Server] Gateway reconnection thread stopped.");
        });
        gatewayConnectionThread.start();
    }

    public static void startCrawlerConnectionThread(String barrelName, IBarrelCrawler barrelStub) {
        crawlerConnectionThread = new Thread(() -> {
            while (crawlerThreadRunning) {
                try {
                    ICrawlerBarrel crawlerStub = crawlerConnectionManager.connect(ICrawlerBarrel.class);
                    if (crawlerStub != null) {
                        if (!crawlerStub.registerBarrel(barrelName, barrelStub)) {
                            System.err.println("[Barrel Server] Crawler registration failed. Retrying in 5s...");
                        }
                    } else {
                        System.err.println("[Barrel Server] Crawler Server unavailable. Retrying in 5s...");
                    }
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    System.err.println("[Barrel Server] Crawler connection thread interrupted.");
                    Thread.currentThread().interrupt();
                    break;
                } catch (RemoteException e) {
                    System.err.println("[Barrel Server] Error: Failed to connect to Crawler");
                }
            }
            System.out.println("[Barrel Server] Crawler reconnection thread stopped.");
        });
        crawlerConnectionThread.start();
    }

    private static String getBarrelName() throws RemoteException, InterruptedException {
        String barrelName = null;
        while (barrelName == null) {
            IGatewayBarrel gatewayStub = gatewayConnectionManager.connect(IGatewayBarrel.class);
            if (gatewayStub == null) {
                System.err.println("[Barrel Server] Gateway Server unavailable. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            //Get available barrel name from Gateway Server
            barrelName = gatewayStub.getAvailableBarrelName();
            if (barrelName == null) {
                System.err.println("[Barrel Server] No barrel server names available. Retrying in 5s...");
                Thread.sleep(5000);
            }
        }
        return barrelName;
    }

    private static BackupBarrelInfo getBackupBarrelInfo(String barrelName) throws RemoteException, InterruptedException {
        BackupBarrelInfo backupBarrelInfo;
        while (true) {
            IGatewayBarrel gatewayStub = gatewayConnectionManager.connect(IGatewayBarrel.class);
            if (gatewayStub == null) {
                System.err.println("[Barrel Server] Gateway Server unavailable. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }
            //Get backup barrel name from Gateway
            backupBarrelInfo = gatewayStub.getBackupBarrel(barrelName);
            break;
        }

        if (backupBarrelInfo != null) {
            System.out.println("[Barrel Server] Backup barrel name: '" + backupBarrelInfo.name() + "'.");
            return backupBarrelInfo;
        }
        else {
            System.out.println("[Barrel Server] No backup barrel available.");
            return null;
        }
    }

    private static void registerBarrelToGateway(String barrelName, IBarrelGateway barrelStub, IBarrelBarrel backupStub) throws RemoteException, InterruptedException {
        while (true) {
            IGatewayBarrel gatewayStub = gatewayConnectionManager.connect(IGatewayBarrel.class);
            if (gatewayStub == null) {
                System.err.println("[Barrel Server] Gateway Server unavailable. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            if (gatewayStub.registerBarrel(barrelName, barrelStub, backupStub)){
                System.out.println("[Barrel Server] Barrel '" + barrelName + "' registered to Gateway Server.");
                break;
            }
            else {
                System.err.println("[Barrel Server] Error: Failed to register Barrel '" + barrel.getBarrelName() + "' to Gateway Server. Retrying in 5s");
                Thread.sleep(5000);
            }
        }
    }

    private static void registerBarrelToCrawler(String barrelName, IBarrelCrawler barrelStub) throws RemoteException, InterruptedException {
        while (true) {
            ICrawlerBarrel crawlerStub = crawlerConnectionManager.connect(ICrawlerBarrel.class);
            if (crawlerStub == null) {
                System.err.println("[Barrel Server] Crawler Server unavailable. Retrying in 5s...");
                Thread.sleep(5000);
                continue;
            }

            if (crawlerStub.registerBarrel(barrelName, barrelStub)){
                System.out.println("[Barrel Server] Barrel '" + barrelName + "' registered to Crawler Server.");
                break;
            }
            else {
                System.err.println("[Barrel Server] Error: Failed to register Barrel '" + barrel.getBarrelName() + "' to Crawler Server. Retrying in 5s...");
                Thread.sleep(5000);
            }
        }
    }

    public static void exportBarrelStats(BarrelStats barrelStats) throws RemoteException {
        IGatewayBarrel gatewayStub = gatewayConnectionManager.connect(IGatewayBarrel.class);
        if (gatewayStub == null) {
            System.err.println("[Barrel Server] Failed to export Barrel Stats. Gateway unavailable.");
            return;
        }
        //Send Barrel stats to Gateway Server everytime stats change
        gatewayStub.updateSystemStats(barrelStats);
    }

    public static void shutdown() throws RemoteException {
        System.out.println("[Barrel Server] Barrel Server shutting down...");

        gatewayThreadRunning = false;
        crawlerThreadRunning = false;

        try {
            if (gatewayConnectionThread != null) {gatewayConnectionThread.join();}
            if (crawlerConnectionThread != null) {crawlerConnectionThread.join();}
        }
        catch (InterruptedException ignore) {}



        //Unregister from Gateway Server
        IGatewayBarrel gatewayStub = gatewayConnectionManager.connect(IGatewayBarrel.class);
        if (gatewayStub == null) {
            System.err.println("[Barrel Server] Failed to notify Gateway Server.");
        }
        else{
            gatewayStub.unregisterBarrel(barrel.name);
        }

        //Unregister from Crawler Server
        ICrawlerBarrel crawlerStub = crawlerConnectionManager.connect(ICrawlerBarrel.class);
        if (crawlerStub == null) {
            System.err.println("[Barrel Server] Failed to unregister from Crawler Server.");
        }
        else{
            crawlerStub.unregisterBarrel(barrel.name);
        }

        System.out.println("[Barrel Server] Barrel Server shutdown complete.");
        System.exit(0);
    }

    static void main() throws RemoteException, InterruptedException {
        Thread startThread = new Thread(() -> {
            try {
                startBarrelServer();
            } catch (Exception e) {
                System.err.println("[Barrel Server] Error:");
                e.printStackTrace();
            }
            System.out.println("[Barrel Server] Press Enter to shutdown Barrel Server.");
        });
        startThread.start();

        boolean run = true;
        while (run) {
            System.out.println("[Barrel Server] Press Enter to shutdown Barrel Server.");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                run = false;
            }
            else {
                System.out.println("[Barrel Server] Invalid input. Please try again...");
            }
        }
        startThread.interrupt();
        startThread.join();
        shutdown();
    }
}