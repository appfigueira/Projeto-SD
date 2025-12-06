package pt.dei.googol.Projeto_SD.Common.DataStructures;

import java.io.Serializable;
import java.util.Set;

public record LinkingURLsResult(int code, Set<String> links) implements Serializable {}