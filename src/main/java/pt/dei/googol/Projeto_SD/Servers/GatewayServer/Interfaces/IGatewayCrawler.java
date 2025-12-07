package pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IGatewayCrawler extends Remote {
    boolean ping() throws RemoteException;
    int getNumberOfCrawlers() throws RemoteException;
    int getNumberOfBarrels() throws RemoteException;
}
