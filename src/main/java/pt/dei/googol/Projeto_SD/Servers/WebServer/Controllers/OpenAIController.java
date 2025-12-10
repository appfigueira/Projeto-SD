package pt.dei.googol.Projeto_SD.Servers.WebServer.Components.Controllers;


import org.apache.coyote.Response;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Components.WebServer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/openai")
public class OpenAIController {
    /**
     * javadoc
     */
    @PostMapping(value = "/generate", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?>generate(@RequestBody Map<String, String> body) {
        String query = body.get("query");
        if (query.isBlank()) {
            return ResponseEntity.
                    status(400).
                    body(Map.of("msg", "Prompt is empty"));
        }

        WebServer.generate();
    }
}
