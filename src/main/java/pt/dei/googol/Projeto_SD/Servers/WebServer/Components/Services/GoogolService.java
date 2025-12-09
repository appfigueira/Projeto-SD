package pt.dei.googol.Projeto_SD.Servers.WebServer.Components.Services;

import pt.dei.googol.Projeto_SD.Common.Functions.RMIConnectionManager;
import pt.dei.googol.Projeto_SD.Common.DataStructures.*;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces.IGatewayWeb;

import org.springframework.stereotype.Service;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.List;

@Service
public class GoogolService {

    private final RMIConnectionManager<IGatewayWeb> gatewayConnectionManager;
    private static final int URLsPerPage = 10;

    public GoogolService(RMIConnectionManager<IGatewayWeb> gatewayConnectionManager) {
        this.gatewayConnectionManager = gatewayConnectionManager;
    }

    public int index(String url) {
        IGatewayWeb gatewayStub = gatewayConnectionManager.connect(IGatewayWeb.class);
        if (gatewayStub == null) return -1;

        try {
            return gatewayStub.indexURLClientGateway(url);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public SearchResult search(List<String> searchTokens, int pageNumber) {
        IGatewayWeb gatewayStub = gatewayConnectionManager.connect(IGatewayWeb.class);
        if (gatewayStub == null) {
            System.err.println("[Client] Gateway Service unavailable");
            return new SearchResult(-1, Collections.emptyList());
        }

        try {
            return gatewayStub.searchClientGateway(searchTokens, pageNumber, URLsPerPage);
        } catch (RemoteException e) {
            System.err.println("[Client] Failed to connect to Gateway Server");
            return new SearchResult(-1, Collections.emptyList());
        }
    }

    public LinkingURLsResult links(String url) {
        IGatewayWeb gatewayStub = gatewayConnectionManager.connect(IGatewayWeb.class);
        if (gatewayStub == null) {
            System.err.println("[Client] Gateway Service unavailable");
            return new LinkingURLsResult(-1, null);
        }

        try {
            LinkingURLsResult linksToURL = gatewayStub.getLinkingURLsClientGateway(url);
            int status = linksToURL.status();
            return switch (status) {
                case -1 -> new LinkingURLsResult(-1, null);
                case 0, 1 -> linksToURL;
                default -> new LinkingURLsResult(-1, null);
            };
        } catch (RemoteException e) {
            System.err.println("[Client] Failed to connect to Gateway Server");
            return new LinkingURLsResult(-1, null);
        }
    }
}
