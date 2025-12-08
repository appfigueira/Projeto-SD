package pt.dei.googol.Projeto_SD.Servers.WebServer.Components;


import pt.dei.googol.Projeto_SD.Common.Functions.RMIConnectionManager;
import pt.dei.googol.Projeto_SD.Common.DataStructures.*;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Interfaces.IWebGateway;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces.IGatewayWeb;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class WebServer extends UnicastRemoteObject implements IWebGateway {

    static String host = "localhost"; // Gateway IP
    static int port; // RMI registry port
    private static final int URLsPerPage = 10;
    private static volatile boolean gatewayThreadRunning = false;
    private static Thread gatewayConnectionThread;
    private static RMIConnectionManager<IGatewayWeb> gatewayConnectionManager;


    public WebServer() throws RemoteException {
        super();
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
    public static void getSystemStats() throws RemoteException {
        gatewayThreadRunning = true;
        gatewayConnectionThread = new Thread(() -> {
            try {
                IWebGateway webStub = new WebServer();
                while (gatewayThreadRunning) {
                    try {
                        IGatewayWeb stub = gatewayConnectionManager.connect(IGatewayWeb.class);
                        if (stub == null) {
                            //Set Status Offline -> systemStats
                            System.err.println("[Client] Error: Gateway Service unavailable. Please try again later.");
                        } else {
                            try {
                                stub.registerWebServer(webStub);
                                try {
                                    SystemStats systemStats = stub.getSystemStats();
                                    //Status ONLINE -> systemStats
                                } catch (RemoteException e) {
                                    System.err.println("[Client] Error: Failed to connect to Gateway Server");
                                }
                            } catch (RemoteException e) {
                                System.err.println("[Client] Error: Failed to connect to Gateway Server");
                            }
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    //SEND SYSTEM STATS TO CLIENTS
                }

                try {
                    IGatewayWeb stub = gatewayConnectionManager.connect(IGatewayWeb.class);
                    if (stub != null) {
                        stub.unregisterWebServer(webStub);
                    }
                } catch (RemoteException e) {
                    System.err.println("[Client] Error: Failed to connect to Gateway Server");
                }
            } catch (RemoteException e) {
                System.err.println("[Client] Error: Failed to export interface stub.");
            }

        });
        gatewayConnectionThread.start();
    }

    //Remote Callback from Gateway
    @Override
    public void updateSystemStats(SystemStats systemStats) throws RemoteException {
        //Send systemStats to Clients
    }

    public static boolean startWebServer() {
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream("files/SystemConfiguration")) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("[Barrel Server] Failed to load SystemConfiguration file.");
            e.printStackTrace();
            return false;
        }
        host = config.getProperty("gateway.host", "localhost");
        port = Integer.parseInt(config.getProperty("gateway.portRMI", "1099"));
        String gatewayServiceName = config.getProperty("gateway.serviceName", "Gateway");

        gatewayConnectionManager = new RMIConnectionManager<>(host, port, gatewayServiceName);
        return true;
    }

    public void shutdown() {
        gatewayThreadRunning = false;
        if (gatewayConnectionThread != null) {
            try {
                gatewayConnectionThread.join();
            } catch (InterruptedException ignore) {}
        }

        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException ignore) {}

        System.exit(0);
    }

    void main() {
        try {
            boolean run = startWebServer();
            while (run) {
                try {
                    System.out.print("""
                            \n[Client]
                            ------------------------------------
                            📚 Googol Search Engine Client Menu
                            ------------------------------------
                            Please choose an option:
                            1 - Index a new URL
                            2 - Search pages
                            3 - Get URLs linking to a page
                            4 - View system statistics
                            0 - Exit
                            ------------------------------------\s
                            """);

                    Scanner scanner = new Scanner(System.in);
                    String option = scanner.nextLine().trim();
                    System.out.println("------------------------------------");

                    switch (option) {
                        case "1" -> {
                            System.out.print("[Client] Enter URL: ");
                            String url = scanner.nextLine().trim();
                            indexURL(url);
                        }

                        case "2" -> {
                            System.out.print("[Client] Enter search terms: ");
                            String input = scanner.nextLine().trim();
                            List<String> searchTokens = cleanSearchWords(input);
                            browse(searchTokens);
                        }

                        case "3" -> {
                            System.out.print("Enter target URL: ");
                            String url = scanner.nextLine().trim();
                            getLinksToURL(url);
                        }

                        case "4" -> {
                            getSystemStats();
                            gatewayThreadRunning = false;
                        }

                        case "0" -> {
                            System.out.println("[Client] Exiting...");
                            run = false;
                        }
                        default -> System.out.println("Invalid option. Please try again...");
                    }
                }
                catch (Exception e) {
                    System.err.println("[Client] Error:");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("[Client] Error:");
            e.printStackTrace();
        }
        shutdown();
    }
}