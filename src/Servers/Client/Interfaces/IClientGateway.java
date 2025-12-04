package Servers.Client.Interfaces;

import Common.DataStructures.SystemStats;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientGateway extends Remote {
    void updateSystemStats(SystemStats systemStats) throws RemoteException;
}
