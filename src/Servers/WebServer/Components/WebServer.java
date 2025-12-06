package Servers.WebServer.Components;


import Common.Functions.RMIConnectionManager;
import Common.DataStructures.*;
import Servers.WebServer.Interfaces.IWebGateway;
import Servers.GatewayServer.Interfaces.IGatewayWeb;

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

    public static List<String> cleanSearchWords(String input) {
        List<String> tokens = new ArrayList<>();
        if (input != null && !input.isBlank()) {
            for (String token : input.split("\\s+")) {
                token = token.replaceAll("^\\p{Punct}+|\\p{Punct}+$", "").toLowerCase();
                if (!token.isBlank()) tokens.add(token);
            }
        }
        return tokens;
    }

    public static void indexURL(String url) {
        if (url.isBlank()) {
            System.out.println("[Client] URL is empty.");
            return;
        }

        IGatewayWeb gatewayStub = gatewayConnectionManager.connect(IGatewayWeb.class);
        if (gatewayStub == null) {
            System.err.println("[Client] Error: Gateway Service unavailable. Please try again later.");
        } else {
            try {
                int code = gatewayStub.submitURLClientGateway(url);
                switch (code) {
                    case -1 ->
                            System.err.println("[Client] Error: Failed to index URL. Service may be unavailable. Please try again later.");
                    case 0 -> System.out.println("[Client] Failed to submit URL: URL '" + url + "' already submitted.");
                    case 1 -> System.out.println("[Client] URL '" + url + "' successfully submitted.");
                }

            }
            catch(RemoteException e) {
                System.err.println("[Client] Error: Failed to connect to Gateway Server");
            }
        }
    }

    public static void browse(List<String> searchTokens) {
        if (searchTokens.isEmpty()) {
            System.out.println("[Client] No valid search terms entered.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        int pageNumber = 0;

        while (true) {
            IGatewayWeb gatewayStub = gatewayConnectionManager.connect(IGatewayWeb.class);
            if (gatewayStub == null) {
                System.err.println("[Client] Error: Gateway Service unavailable. Please try again later.");
                break;
            }

            SearchResult searchResult = search(gatewayStub, searchTokens, pageNumber);

            int code = searchResult.code();
            List<URLHeader> results = searchResult.results();

            switch (code) {
                //Server Error
                case -1 -> {
                    System.err.println("[Client] Error: Failed to load page. Service may be unavailable. Please try again later.");
                    return;
                }

                //Results Empty
                case 0 -> {
                    //No more Search Results
                    if (pageNumber > 0) {
                        System.out.println("No more search results found.");
                        System.out.print("\n[p] Previous page | [q] Quit: ");

                        String command = scanner.nextLine().trim().toLowerCase();

                        switch (command) {
                            case "p" -> pageNumber--;
                            case "q" -> {
                                System.out.println("[Client] Exiting search.");
                                return;
                            }
                            default -> System.out.println("[Client] Invalid command. Use 'p' or 'q'.");
                        }
                    } else if (pageNumber == 0) {
                        System.out.println("[Client] No search results found. Please try again later.");
                        System.out.println("[Client] Exiting search.");
                        return;
                    }

                }
                case 1 -> {
                    if (results.isEmpty()) {
                        System.err.println("[Client] Error: Page is empty.");
                        System.out.println("[Client] Exiting search.");
                        break;
                    }

                    printSearchResults(results, pageNumber);

                    if (pageNumber == 0) {
                        System.out.print("\n[n] Next page | [q] Quit: ");
                        String command = scanner.nextLine().trim().toLowerCase();

                        switch (command) {
                            case "n" -> pageNumber++;
                            case "q" -> {
                                System.out.println("[Client] Exiting search.");
                                return;
                            }
                            default -> System.out.println("[Client] Invalid command. Use 'n' or 'q'.");
                        }
                    }
                    else {
                        System.out.print("\n[n] Next page | [p] Previous page | [q] Quit: ");
                        String command = scanner.nextLine().trim().toLowerCase();

                        switch (command) {
                            case "n" -> pageNumber++;
                            case "p" -> pageNumber--;
                            case "q" -> {
                                System.out.println("[Client] Exiting search.");
                                return;
                            }
                            default -> System.out.println("[Client] Invalid command. Use 'n', 'p' or 'q'.");
                        }
                    }
                }
            }
        }
    }

    public static SearchResult search(IGatewayWeb gatewayStub, List<String> searchTokens, int pageNumber) {
        try {
            return gatewayStub.searchClientGateway(searchTokens, pageNumber, URLsPerPage);
        } catch (RemoteException e) {
            System.err.println("[Client] Error: Failed to connect to Gateway Server");
        }
        return new SearchResult(-1, Collections.emptyList());
    }

    public static void printSearchResults(List<URLHeader> results, int pageNumber) {
        System.out.println("\n[Client]");
        System.out.println("========== Page " + (pageNumber + 1) + " ==========");

        //Print Page Content
        for (URLHeader result : results) {
            System.out.println("URL    : " + result.url());
            System.out.println("Title  : " + result.title());
            System.out.println("Snippet: " + result.snippet());
            System.out.println("------------------------------------");
        }
    }

    public static void getLinksToURL(String url) {
        if (url.isBlank()) {
            System.out.println("[Client] Target URL is empty.");
            return;
        }

        IGatewayWeb gatewayStub = gatewayConnectionManager.connect(IGatewayWeb.class);
        if (gatewayStub == null) {
            System.err.println("[Client] Error: Gateway Service is unavailable. Please try again later.");
        } else {
            try {
                LinkingURLsResult linksToURLResult = gatewayStub.getLinkingURLsClientGateway(url);
                int code = linksToURLResult.code();
                switch (code) {
                    case -1 -> System.err.println("[Client] Error: Failed to submit target link. Service may be unavailable. Please try again later.");
                    case 0 -> System.out.println("[Client] Target URL '" + url + "' has no other URLs linking to it.");
                    case 1 -> {
                        System.out.println("[Client] URL '" + url + "' successfully submitted.");
                        Set<String> links = linksToURLResult.links();
                        printLinks(url, links);
                    }
                }
            }
            catch (RemoteException e) {
                System.err.println("[Client] Error: Failed to connect to Gateway Server");
            }
        }
    }

    public static void printLinks(String url, Set<String> links) {
        if (links == null || links.isEmpty()) {
            System.err.println("[Client] Error: Links is empty");
            return;
        }
        System.out.println("\n[Client]");
        System.out.println("🎯 Target link: '" + url + "'");
        System.out.println("🔗 Links encontrados (" + links.size() + "):");
        System.out.println("------------------------------------");

        for (String link : links) {
            System.out.println(link);
        }

        System.out.println("------------------------------------");
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

    public static boolean startClient() {
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
            boolean run = startClient();
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