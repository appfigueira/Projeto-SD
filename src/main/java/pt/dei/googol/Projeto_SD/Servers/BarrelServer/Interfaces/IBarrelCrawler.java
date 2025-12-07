package pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces;

import pt.dei.googol.Projeto_SD.Common.DataStructures.PageData;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IBarrelCrawler extends Remote {
    boolean ping() throws RemoteException;
    boolean submitPageData(PageData pageData) throws RemoteException;
}