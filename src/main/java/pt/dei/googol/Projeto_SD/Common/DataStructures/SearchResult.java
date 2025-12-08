package pt.dei.googol.Projeto_SD.Common.DataStructures;

import java.io.Serializable;
import java.util.List;

public record SearchResult(int status, List<URLHeader> results) implements Serializable {}