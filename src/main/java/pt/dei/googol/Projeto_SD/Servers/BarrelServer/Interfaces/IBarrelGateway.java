package pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces;

import pt.dei.googol.Projeto_SD.Common.DataStructures.BarrelStats;
import pt.dei.googol.Projeto_SD.Common.DataStructures.LinkingURLsResult;
import pt.dei.googol.Projeto_SD.Common.DataStructures.SearchResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IBarrelGateway extends Remote {
    boolean ping() throws RemoteException;
    void systemShutdown() throws RemoteException, InterruptedException;
    boolean checkURL(String url) throws RemoteException;
    SearchResult searchGatewayBarrel(List<String> searchWords, int pageNumber, int URLsPerPage) throws RemoteException;
    LinkingURLsResult getLinkingURLsGatewayBarrel(String url) throws RemoteException;
    BarrelStats getBarrelStats() throws RemoteException;
    void stopExportBarrelStats() throws RemoteException;
}