package Servers.CrawlerServer.Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICrawlerGateway extends Remote {
    boolean ping() throws RemoteException;
    void systemShutdown() throws RemoteException, InterruptedException;
    int submitURLGatewayCrawler(String url) throws RemoteException;
}