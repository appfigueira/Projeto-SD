package pt.dei.googol.Projeto_SD.Servers.WebServer.Components;


import pt.dei.googol.Projeto_SD.Common.Functions.RMIConnectionManager;
import pt.dei.googol.Projeto_SD.Common.DataStructures.*;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Components.Services.GoogolService;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Components.WebSockets.StatsWebSocket;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Interfaces.IWebGateway;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces.IGatewayWeb;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

@Component
public class WebServer extends UnicastRemoteObject implements IWebGateway {

    private static WebServer instance;
    private static GoogolService googolService;
    private final StatsWebSocket statsWebSocket;
    static String host = "localhost"; // Gateway IP
    static int port; // RMI registry port
    private static volatile boolean gatewayThreadRunning = false;
    private static Thread gatewayConnectionThread;
    private static RMIConnectionManager<IGatewayWeb> gatewayConnectionManager;


        public WebServer(StatsWebSocket statsWebSocket, GoogolService googolService) throws RemoteException {
            this.statsWebSocket = statsWebSocket;
            WebServer.googolService = googolService;
            instance = this;
        }

        // Exemplo de uso no WebServer
        public static int index(String url) {
            return googolService.index(url);
        }

        public static SearchResult search(List<String> tokens, int pageNumber) {
            return googolService.search(tokens, pageNumber);
        }

        public static LinkingURLsResult links(String url) {
            return googolService.links(url);
        }

    //Request System Stats and Ping Gateway
    public void getSystemStats() throws RemoteException {
        gatewayThreadRunning = true;
        gatewayConnectionThread = new Thread(() -> {
            IWebGateway webStub;
            try {
                webStub = (IWebGateway) UnicastRemoteObject.exportObject(this, 0);
            } catch (RemoteException e) {
                System.err.println("[Client] Error: Failed to export interface stub.");
                return;
            }

            try {
                while (gatewayThreadRunning) {
                    SystemStats systemStats;
                    try {
                        IGatewayWeb stub = gatewayConnectionManager.connect(IGatewayWeb.class);
                        if (stub == null) {
                            System.err.println("[Client] Error: Gateway Service unavailable. Please try again later.");
                            systemStats = new SystemStats();
                        } else {
                            try {
                                stub.registerWebServer(webStub);
                                try {
                                    systemStats = stub.getSystemStats();
                                } catch (RemoteException e) {
                                    System.err.println("[Client] Error: Failed to connect to Gateway Server");
                                    systemStats = new SystemStats();
                                }
                            } catch (RemoteException e) {
                                System.err.println("[Client] Error: Failed to connect to Gateway Server");
                                systemStats = new SystemStats();
                            }
                        }

                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    try {
                        updateSystemStats(systemStats);
                    } catch (RemoteException ignored) {
                    }
                }

            } finally {
                try {
                    IGatewayWeb stub = gatewayConnectionManager.connect(IGatewayWeb.class);
                    if (stub != null) {
                        stub.unregisterWebServer(webStub);
                    }
                } catch (RemoteException e) {
                    System.err.println("[Client] Error: Failed to connect to Gateway Server");
                }
            }
        });
        gatewayConnectionThread.start();
    }

    //Remote Callback from Gateway
    @Override
    public void updateSystemStats(SystemStats systemStats) throws RemoteException {
        statsWebSocket.broadcastStats(systemStats);
    }

    public static void startWebServer() {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream("files/SystemConfiguration")) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("[Barrel Server] Failed to load SystemConfiguration file.");
            e.printStackTrace();
            return;
        }
        host = config.getProperty("gateway.host", "localhost");
        port = Integer.parseInt(config.getProperty("gateway.portRMI", "1099"));
        String gatewayServiceName = config.getProperty("gateway.serviceName", "Gateway");

        gatewayConnectionManager = new RMIConnectionManager<>(host, port, gatewayServiceName);
    }

    public static void shutdownWebServer() {
        gatewayThreadRunning = false;
        if (gatewayConnectionThread != null) {
            try {
                gatewayConnectionThread.join();
            } catch (InterruptedException ignore) {}
        }

        if (instance != null) {
            instance.unexportRMI();
        }
    }

    public void unexportRMI() {
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException ignore) {}

    }
}