package pt.dei.googol.Projeto_SD.Servers.WebServer.Components;


import pt.dei.googol.Projeto_SD.Common.DataStructures.*;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Services.Googol.SystemStatsService;

import org.springframework.stereotype.Component;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

@Component
public class WebServer extends UnicastRemoteObject{

    private final SystemStatsService systemStatsService;

    public WebServer(SystemStatsService systemStatsService) throws RemoteException {
        super();
        this.systemStatsService = systemStatsService;
    }

    public void shutdown() {
        systemStatsService.stopSystemStatsThread();
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException ignored) {}
    }
}