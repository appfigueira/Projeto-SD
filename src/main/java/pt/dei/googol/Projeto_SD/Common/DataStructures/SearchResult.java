package pt.dei.googol.Projeto_SD.Common.DataStructures;

import java.io.Serializable;
import java.util.List;

public record SearchResult(int code, List<URLHeader> results) implements Serializable {}