package pt.dei.googol.Projeto_SD.Servers.WebServer.Services.AI;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.stereotype.Service;

@Service
public class AIService {

    private final Client client;

    public AIService() {
        this.client = new Client();
    }

    public String generateText(String prompt) {
        try {
            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-2.5-flash",
                            prompt,
                            null
                    );
            return response.text();
        } catch (Exception e) {
            System.err.println("[AI Service] Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
