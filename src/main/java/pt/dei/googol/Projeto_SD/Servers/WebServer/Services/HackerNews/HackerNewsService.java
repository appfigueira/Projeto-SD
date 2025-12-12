package pt.dei.googol.Projeto_SD.Servers.WebServer.Services.HackerNews;

import pt.dei.googol.Projeto_SD.Common.DataStructures.NewsHeader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Service
public class HackerNewsService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HackerNewsService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public ArrayList<NewsHeader> searchNews(String query, int page) {
        int itemsPerPage = 10;
        ArrayList<NewsHeader> results = new ArrayList<>();

        try {
            String url = String.format(
                    "https://hn.algolia.com/api/v1/search?query=%s&tags=story&page=%d&hitsPerPage=%d",
                    query, page, itemsPerPage);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isEmpty()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode hits = root.path("hits");

            if (hits.isMissingNode() || !hits.isArray()) {
                return results;
            }

            for (JsonNode hit : hits) {
                String title = hit.path("title").asText();
                String link = hit.path("url").asText();

                // If no link -> Create link through ID
                if (link == null || link.isEmpty()) {
                    link = "https://news.ycombinator.com/item?id=" + hit.path("objectID").asText();
                }

                String snippet = "";
                if (hit.has("story_text") && !hit.path("story_text").asText().isEmpty()) {
                    snippet = hit.path("story_text").asText();
                    if (snippet.length() > 250) {
                        snippet = snippet.substring(0, 250) + "...";
                    }
                }

                results.add(new NewsHeader(title, link, snippet));
            }

            return results;

        } catch (Exception e) {
            System.err.println("[Hacker News Service] Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}