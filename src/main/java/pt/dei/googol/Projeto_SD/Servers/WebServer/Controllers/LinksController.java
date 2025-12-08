package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers;

import pt.dei.googol.Projeto_SD.Common.DataStructures.LinkingURLsResult;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Components.WebServer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/search")
public class LinksController {

    /**
     * link example: "<a href="https://www.googol.dei.pt/search?url=https://www.example.com">...</a>"
     * @param url: target url
     * @return HTTP status:
     * - 200: Success
     * - 201: No Links
     * - 400: URL Empty
     * - 500: Service Unavailable
     */
    @GetMapping(produces = "application/json")
    public ResponseEntity<?> links(@RequestParam String url) {
        if (url.isBlank()){
            return ResponseEntity.
                    status(400).
                    body(Map.of("msg", "URL is empty"));
        }

        LinkingURLsResult linksToURL = WebServer.getLinksToURL(url);

        int status = linksToURL.status();

        switch (status) {
            case -1 -> {
                return ResponseEntity.
                        status(500).
                        body(Map.of("msg", "Service unavailable."));

            }
            case 0 -> {
                return ResponseEntity.
                        status(200).
                        body(linksToURL.links());
            }
            case 1 -> {
                return ResponseEntity.
                        status(201).
                        body(Map.of("msg", "No links to target URL"));
            }
        }
        throw new IllegalStateException("Unexpected status: " + status);
    }
}
