package Servers.BarrelServer.Interfaces;

import Servers.BarrelServer.DataStructures.PageHeader;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;

public interface IBarrelBarrel extends Remote {
    Map<String, PageHeader> getPageHeaderIndex() throws RemoteException;
    Map<String, Set<String>> getTokenIndex() throws RemoteException;
    Map<String, Set<String>> getLinkIndex() throws RemoteException;
}
