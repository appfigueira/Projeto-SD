package pt.dei.googol.Projeto_SD.Servers.WebServer.Components.Controllers;

import pt.dei.googol.Projeto_SD.Common.DataStructures.SearchResult;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Components.WebServer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
public class SearchController {

    /**
     * link example: "<a href="https://www.googol.dei.pt/search?q=search_words_example&p=0">...</a>"
     * @param q: search words
     * @param p: page number (start at 0, first page -> p=0)
     * @return HTTP status:
     * - 200: Success
     * - 201: No Search Results
     * - 400: Query Empty
     * - 401: Invalid Search
     * - 500: Service Unavailable
     */
    @GetMapping(produces = "application/json")
    public ResponseEntity<?> search(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int p) {
        if (q.isBlank()){
            return ResponseEntity.
                    status(400).
                    body(Map.of("msg", "Query is empty"));
        }

        List<String> searchTokens = cleanSearchWords(q);

        if (searchTokens.isEmpty()){
            return ResponseEntity.
                    status(401).
                    body(Map.of("msg", "Invalid search"));
        }

        SearchResult searchResult = WebServer.search(searchTokens, p);
        int status = searchResult.status();

        return switch (status) {
            case -1 -> ResponseEntity.
                        status(500).
                        body(Map.of("msg", "Service unavailable"));
            case 0 -> ResponseEntity.
                        status(200).
                        body(searchResult.results());
            case 1 -> ResponseEntity.
                        status(201).
                        body(Map.of("msg", "No search results"));
            default -> throw new IllegalStateException("Unexpected value: " + status);
        };
    }

    public static List<String> cleanSearchWords(String input) {
        List<String> tokens = new ArrayList<>();
        if (input != null && !input.isBlank()) {
            for (String token : input.split("\\s+")) {
                token = token.replaceAll("^\\p{Punct}+|\\p{Punct}+$", "").toLowerCase();
                if (!token.isBlank()) tokens.add(token);
            }
        }
        return tokens;
    }
}