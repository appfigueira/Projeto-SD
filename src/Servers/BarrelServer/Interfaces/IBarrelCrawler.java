package Servers.BarrelServer.Interfaces;

import Common.DataStructures.PageData;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IBarrelCrawler extends Remote {
    boolean ping() throws RemoteException;
    boolean submitPageData(PageData pageData) throws RemoteException;
}