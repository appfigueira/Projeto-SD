package pt.dei.googol.Projeto_SD.Common.Functions;

import java.net.URI;
import java.net.URISyntaxException;

public record URLCleaner() {
    public static String cleanURL(String url) {
        if (url == null || url.isBlank()) return null;

        //Force URL to start with "https://"
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        //Remove " " (spaces)
        url = url.trim().replaceAll("\\s+", "");

        try {
            URI uri = new URI(url.trim());

            String host = uri.getHost();
            if (host == null) return null;

            //Remove "www."
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            String path = uri.getPath() != null ? uri.getPath() : "";

            //Remove "/"
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // Reconstruir URL com https
            return "https://" + host + path;

        } catch (URISyntaxException e) {
            return null;
        }
    }
}
