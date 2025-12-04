package Servers.GatewayServer.Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IGatewayCrawler extends Remote {
    boolean ping() throws RemoteException;
    int getNumberOfCrawlers() throws RemoteException;
    int getNumberOfBarrels() throws RemoteException;
}
