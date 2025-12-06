package pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces;

import pt.dei.googol.Projeto_SD.Common.DataStructures.LinkingURLsResult;
import pt.dei.googol.Projeto_SD.Common.DataStructures.SearchResult;
import pt.dei.googol.Projeto_SD.Common.DataStructures.SystemStats;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Interfaces.IWebGateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IGatewayWeb extends Remote {
    boolean ping() throws RemoteException;
    int submitURLClientGateway(String url) throws RemoteException;
    SearchResult searchClientGateway(List<String> searchWords, int pageNumber, int URLsPerPage) throws RemoteException;
    LinkingURLsResult getLinkingURLsClientGateway(String url) throws RemoteException;
    SystemStats getSystemStats() throws RemoteException;
    void registerWebServer(IWebGateway webStub) throws RemoteException;
    void unregisterWebServer(IWebGateway webStub) throws RemoteException;
}