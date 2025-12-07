package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/index")
public class IndexController {

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> indexUrl(@RequestBody JsonNode request) {
        String url = request.get("url").asText();

        System.out.println("URL recebido: " + url);

        // indexService.index(url);

        return ResponseEntity.ok(Map.of("message", "URL recebido com sucesso"));
    }
}