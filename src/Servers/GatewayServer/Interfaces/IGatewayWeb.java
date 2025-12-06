package Servers.GatewayServer.Interfaces;

import Common.DataStructures.LinkingURLsResult;
import Common.DataStructures.SearchResult;
import Common.DataStructures.SystemStats;
import Servers.WebServer.Interfaces.IWebGateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IGatewayWeb extends Remote {
    boolean ping() throws RemoteException;
    int submitURLClientGateway(String url) throws RemoteException;
    SearchResult searchClientGateway(List<String> searchWords, int pageNumber, int URLsPerPage) throws RemoteException;
    LinkingURLsResult getLinkingURLsClientGateway(String url) throws RemoteException;
    SystemStats getSystemStats() throws RemoteException;
    void registerWebServer(IWebGateway clientStub) throws RemoteException;
    void unregisterWebServer(IWebGateway clientStub) throws RemoteException;
}