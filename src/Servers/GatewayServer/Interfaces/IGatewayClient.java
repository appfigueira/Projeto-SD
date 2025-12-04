package Servers.GatewayServer.Interfaces;

import Common.DataStructures.LinkingURLsResult;
import Common.DataStructures.SearchResult;
import Common.DataStructures.SystemStats;
import Servers.Client.Interfaces.IClientGateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IGatewayClient extends Remote {
    boolean ping() throws RemoteException;
    int submitURLClientGateway(String url) throws RemoteException;
    SearchResult searchClientGateway(List<String> searchWords, int pageNumber, int URLsPerPage) throws RemoteException;
    LinkingURLsResult getLinkingURLsClientGateway(String url) throws RemoteException;
    SystemStats getSystemStats() throws RemoteException;
    void registerClient(IClientGateway clientStub) throws RemoteException;
    void unregisterClient(IClientGateway clientStub) throws RemoteException;
}