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

    public String generateText(String prompt) throws Exception {
        try {
            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-2.5-flash",
                            prompt,
                            null
                    );
            return response.text();
        } catch (Exception e) {
            throw new Exception("Error in Gemini Service: " + e.getMessage(), e);
        }
    }
}
