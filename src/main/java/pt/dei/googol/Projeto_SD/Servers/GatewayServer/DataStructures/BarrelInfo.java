package pt.dei.googol.Projeto_SD.Servers.GatewayServer.DataStructures;

import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelBarrel;
import pt.dei.googol.Projeto_SD.Servers.BarrelServer.Interfaces.IBarrelGateway;

public record BarrelInfo(String name, IBarrelGateway stub, IBarrelBarrel backupStub) {
}
