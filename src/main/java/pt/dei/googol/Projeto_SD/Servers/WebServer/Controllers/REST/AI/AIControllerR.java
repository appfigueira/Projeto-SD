package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers.REST.AI;


import pt.dei.googol.Projeto_SD.Servers.WebServer.Services.AI.AIService;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/apis/ai")
public class AIControllerR {

    private final AIService aiService;

    public AIControllerR(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping(value = "/generate", produces = "application/json")
    public Map<String, String> generateText(@RequestBody Map<String, String> body) {
        Map<String, String> result = new HashMap<>();
        try {
            String prompt = body.get("prompt");
            String text = aiService.generateText(prompt);
            result.put("text", text); // chave "text"
        } catch (Exception e) {
            result.put("text", "Erro: " + e.getMessage());
        }
        return result;
    }
}
