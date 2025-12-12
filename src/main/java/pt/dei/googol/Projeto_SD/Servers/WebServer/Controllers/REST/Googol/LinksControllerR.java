package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers.REST.Googol;


import pt.dei.googol.Projeto_SD.Common.DataStructures.LinkingURLsResult;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Services.Googol.GoogolService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/links")
public class LinksControllerR {
    private final GoogolService googolService;

    public LinksControllerR(GoogolService googolService) {
        this.googolService = googolService;
    }

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
    public ResponseEntity<?> getLinks(@RequestParam String url) {
        if (url.isBlank()){
            return ResponseEntity.
                    status(400).
                    body(Map.of("msg", "URL is empty"));
        }

        LinkingURLsResult linksResult = googolService.getLinks(url);
        System.out.println(linksResult);

        int status = linksResult.status();

        switch (status) {
            case -1 -> {
                return ResponseEntity.
                        status(500).
                        body(Map.of("msg", "Service unavailable."));

            }
            case 0 -> {
                return ResponseEntity.
                        status(200).
                        body(linksResult.links());
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
