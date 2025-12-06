package Servers.WebServer.Interfaces;

import Common.DataStructures.SystemStats;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWebGateway extends Remote {
    void updateSystemStats(SystemStats systemStats) throws RemoteException;
}
