package Servers.GatewayServer.DataStructures;

import Servers.BarrelServer.Interfaces.IBarrelBarrel;
import Servers.BarrelServer.Interfaces.IBarrelGateway;

public record BarrelInfo(String name, IBarrelGateway stub, IBarrelBarrel backupStub) {
}
