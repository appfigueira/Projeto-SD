package pt.dei.googol.Projeto_SD.Servers.WebServer.Interfaces;

import pt.dei.googol.Projeto_SD.Common.DataStructures.SystemStats;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWebGateway extends Remote {
    void updateSystemStats(SystemStats systemStats) throws RemoteException;
}
