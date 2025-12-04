package Common.DataStructures;

import java.io.Serializable;

public record URLHeader(String url, String title, String snippet) implements Serializable {}