package Servers.CrawlerServer.Interfaces;

import Servers.BarrelServer.Interfaces.IBarrelCrawler;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICrawlerBarrel extends Remote {
    boolean ping() throws RemoteException;
    boolean registerBarrel(String name, IBarrelCrawler stub) throws RemoteException;
    void unregisterBarrel(String barrelName) throws RemoteException;
}