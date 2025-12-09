package pt.dei.googol.Projeto_SD.Servers.WebServer.Components.Controllers;

import pt.dei.googol.Projeto_SD.Servers.WebServer.Components.WebServer;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/index")
public class IndexController {

    /**
     * link example: "<a href="https://www.googol.dei.pt/index">...</a>"
     * @param request:
     * - body: {"url"}
     * @return HTTP status:
     * - 200: Success
     * - 400: URL Empty
     * - 401: URL already indexed
     * - 402: Invalid URL
     * - 500: Service Unavailable
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> index(@RequestBody JsonNode request) {
        String url = request.get("url").asText();

        if (url.isBlank()) {
            return ResponseEntity.
                    status(400).
                    body(Map.of("msg", "URL is empty"));
        }
        url = url.trim();

        int status = WebServer.indexURL(url);

        return switch (status) {
            case -1 -> ResponseEntity.
                        status(500).
                        body(Map.of("msg", "Service unavailable."));

            case 0 ->ResponseEntity.
                        status(200).
                        body(Map.of("msg", "URL indexed"));
            case 1 -> ResponseEntity.
                        status(401).
                        body(Map.of("msg", "URL already indexed"));
            case 2 -> ResponseEntity.
                        status(402).
                        body(Map.of("msg", "Invalid URL"));
            default -> throw new IllegalStateException("Unexpected value: " + status);
        };
    }
}