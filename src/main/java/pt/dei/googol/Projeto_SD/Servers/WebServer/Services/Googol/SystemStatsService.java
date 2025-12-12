package pt.dei.googol.Projeto_SD.Servers.WebServer.Services.Googol;

import pt.dei.googol.Projeto_SD.Common.DataStructures.SystemStats;
import pt.dei.googol.Projeto_SD.Servers.WebServer.WebSockets.StatsWebSocket;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Interfaces.IWebGateway;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces.IGatewayWeb;
import pt.dei.googol.Projeto_SD.Common.Functions.RMIConnectionManager;

import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

@Service
public class SystemStatsService extends UnicastRemoteObject implements IWebGateway {

    private final StatsWebSocket statsWebSocket;
    private final RMIConnectionManager<IGatewayWeb> gatewayConnectionManager;

    private volatile boolean gatewayThreadRunning = false;
    private static Thread gatewayConnectionThread;

    public SystemStatsService(StatsWebSocket statsWebSocket, RMIConnectionManager<IGatewayWeb> gatewayConnectionManager) throws RemoteException {
        super();
        this.statsWebSocket = statsWebSocket;
        this.gatewayConnectionManager = gatewayConnectionManager;
    }

    //Request system stats and ping Gateway
    public synchronized void getSystemStats() {
        gatewayThreadRunning = true;
        gatewayConnectionThread = new Thread(() -> {
            IWebGateway webStub = this;
            try {
                while (gatewayThreadRunning) {
                    SystemStats systemStats;
                    try {
                        IGatewayWeb stub = gatewayConnectionManager.connect(IGatewayWeb.class);
                        if (stub == null) {
                            System.err.println("[Stats WS] Error: Gateway unavailable.");
                            systemStats = new SystemStats();
                        } else {
                            try {
                                stub.registerWebServer(webStub);
                                try {
                                    systemStats = stub.getSystemStats();
                                } catch (RemoteException e) {
                                    System.err.println("[Stats WS] Error: Failed to connect to Gateway Server");
                                    systemStats = new SystemStats();
                                }
                            } catch (RemoteException e) {
                                System.err.println("[Stats WS] Error: Gateway unavailable.");
                                systemStats = new SystemStats();
                            }
                        }

                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    updateSystemStats(systemStats);
                }

            } finally {
                try {
                    IGatewayWeb stub = gatewayConnectionManager.connect(IGatewayWeb.class);
                    if (stub != null) {
                        stub.unregisterWebServer(webStub);
                    }
                } catch (RemoteException e) {
                    System.err.println("[Stats WS] Error: Gateway unavailable.");
                }
            }
        });
        gatewayConnectionThread.start();
    }

    //Remote callback from Gateway
    @Override
    public void updateSystemStats(SystemStats systemStats) {
        statsWebSocket.broadcastStats(systemStats);
    }

    public void stopSystemStatsThread() {
        gatewayThreadRunning = false;
        if (gatewayConnectionThread != null) {
            try {
                gatewayConnectionThread.join();
            } catch (InterruptedException ignored) {}
        }
    }
}
