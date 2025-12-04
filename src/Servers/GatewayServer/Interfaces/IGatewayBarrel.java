package Servers.GatewayServer.Interfaces;

import Common.DataStructures.BackupBarrelInfo;
import Common.DataStructures.BarrelStats;
import Servers.BarrelServer.Interfaces.IBarrelBarrel;
import Servers.BarrelServer.Interfaces.IBarrelGateway;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IGatewayBarrel extends Remote {
    boolean ping() throws RemoteException;
    BackupBarrelInfo getBackupBarrel(String barrelName) throws RemoteException;
    String getAvailableBarrelName() throws RemoteException;
    void updateSystemStats(BarrelStats barrelStats) throws RemoteException;
    boolean registerBarrel(String barrelName, IBarrelGateway barrelStub, IBarrelBarrel backupStub) throws RemoteException;
    void unregisterBarrel(String barrelName) throws RemoteException;
}
