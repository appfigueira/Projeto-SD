package pt.dei.googol.Projeto_SD.Servers.BarrelServer.DataStructures;

import java.io.Serializable;

public record PageHeader(String title, String snippet) implements Serializable {
}