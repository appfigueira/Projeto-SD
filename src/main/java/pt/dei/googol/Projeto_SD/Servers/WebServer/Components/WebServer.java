package pt.dei.googol.Projeto_SD.Servers.WebServer.Components;


import pt.dei.googol.Projeto_SD.Common.Functions.RMIConnectionManager;
import pt.dei.googol.Projeto_SD.Common.DataStructures.*;
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
    private final StatsWebSocket statsWebSocket;
    static String host = "localhost"; // Gateway IP
    static int port; // RMI registry port
    private static final int URLsPerPage = 10;
    private static volatile boolean gatewayThreadRunning = false;
    private static Thread gatewayConnectionThread;
    private static RMIConnectionManager<IGatewayWeb> gatewayConnectionManager;


    public WebServer(StatsWebSocket statsWebSocket) throws RemoteException {
        super();
        this.statsWebSocket = statsWebSocket;
        instance = this;
    }


    public static int indexURL(String url) {
        IGatewayWeb gatewayStub = gatewayConnectionManager.connect(IGatewayWeb.class);
        if (gatewayStub == null) {
            return -1;
        } else {
            try {
                return gatewayStub.indexURLClientGateway(url); //status
            }
            catch(RemoteException e) {
                return -1;
            }
        }
    }


    public static SearchResult search(List<String> searchTokens, int pageNumber) {
        IGatewayWeb gatewayStub = gatewayConnectionManager.connect(IGatewayWeb.class);
        if (gatewayStub == null) {
            System.err.println("[Client] Error: Gateway Service unavailable. Please try again later.");
            return new SearchResult(-1, Collections.emptyList());
        }
        try {
            return gatewayStub.searchClientGateway(searchTokens, pageNumber, URLsPerPage);
        } catch (RemoteException e) {
            System.err.println("[Client] Error: Failed to connect to Gateway Server");
            return new SearchResult(-1, Collections.emptyList());
        }
    }

    public static LinkingURLsResult getLinksToURL(String url) {
        IGatewayWeb gatewayStub = gatewayConnectionManager.connect(IGatewayWeb.class);
        if (gatewayStub == null) {
            System.err.println("[Client] Error: Gateway Service is unavailable. Please try again later.");
            return new LinkingURLsResult(-1, null);
        } else {
            try {
                LinkingURLsResult linksToURL = gatewayStub.getLinkingURLsClientGateway(url);
                int status = linksToURL.status();
                return switch (status) {
                    case -1 -> new LinkingURLsResult(-1, null);
                    case 0, 1 -> linksToURL;
                    default -> new LinkingURLsResult(-1, null);
                };
            }
            catch (RemoteException e) {
                System.err.println("[Client] Error: Failed to connect to Gateway Server");
                return new LinkingURLsResult(-1, null);
            }
        }
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