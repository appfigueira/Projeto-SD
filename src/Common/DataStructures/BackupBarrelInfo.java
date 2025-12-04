package Common.DataStructures;

import Servers.BarrelServer.Interfaces.IBarrelBarrel;

import java.io.Serializable;

public record BackupBarrelInfo(String name, IBarrelBarrel stub) implements Serializable {}