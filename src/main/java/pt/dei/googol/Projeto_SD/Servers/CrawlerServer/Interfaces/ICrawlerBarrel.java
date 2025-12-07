package pt.dei.googol.Projeto_SD.Servers.CrawlerServer.Interfaces;

import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelCrawler;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICrawlerBarrel extends Remote {
    boolean ping() throws RemoteException;
    boolean registerBarrel(String name, IBarrelCrawler stub) throws RemoteException;
    void unregisterBarrel(String barrelName) throws RemoteException;
}