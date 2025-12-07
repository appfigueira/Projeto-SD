package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers;

import pt.dei.googol.Projeto_SD.Servers.WebServer.Components.WebServer;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @return status:
 * - 200: Success
 * - 400: URL already indexed
 * - 401: Invalid URL
 * - 500: Service Unavailable
 */
@RestController
@RequestMapping("/index")
public class IndexController {

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> indexUrl(@RequestBody JsonNode request) {
        String url = request.get("url").asText();

        if (url.isBlank()) {
            System.err.println("[Controller] Error: URL is empty.");
            return ResponseEntity.
                    status(400).
                    body(Map.of("msg", "URL is empty."));
        }
        url = url.trim();
        System.out.println("[Controller]: " + url);

        int status = WebServer.indexURL(url);

        switch (status) {
            case -1 -> {
                return ResponseEntity.
                        status(500).
                        body(Map.of("msg", "Service unavailable."));

            }
            case 0 -> {
                return ResponseEntity.
                        status(200).
                        body(Map.of("msg", "URL indexed"));
            }
            case 1 -> {
                return ResponseEntity.
                        status(400).
                        body(Map.of("msg", "URL already indexed"));
            }
            case 2 -> {
                return ResponseEntity.
                        status(401).
                        body(Map.of("msg", "Invalid URL"));
            }
        }

        return ResponseEntity.ok(Map.of("message", "URL recebido com sucesso"));
    }
}