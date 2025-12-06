package pt.dei.googol.Projeto_SD.Common.DataStructures;

import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelBarrel;

import java.io.Serializable;

public record BackupBarrelInfo(String name, IBarrelBarrel stub) implements Serializable {}