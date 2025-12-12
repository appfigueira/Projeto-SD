package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers.REST.HackerNews;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import pt.dei.googol.Projeto_SD.Common.DataStructures.NewsHeader;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Services.HackerNews.HackerNewsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RequestMapping(value = "/api/hackernews")
@RestController
public class HackerNewsControllerR {
    private final HackerNewsService hackerNewsService;

    public HackerNewsControllerR(HackerNewsService hackerNewsService) {
        this.hackerNewsService = hackerNewsService;
    }

    /**
     * link example: "<a href="https://www.googol.dei.pt/hackernews/search?q=search_words_example&p=0">...</a>"
     * @param q: search words
     * @param p: page number (start at 0, first page -> p=0)
     * @return HTTP status:
     * - 200: Success
     * - 201: No Search Results
     * - 400: Query Empty
     * - 401: Invalid Search
     * - 500: Service Unavailable
     */
    @GetMapping(value = "/search", produces = "application/json")
    public ResponseEntity<?> searchNews(@RequestParam String q, @RequestParam(defaultValue = "0") int p) {
        if (q.isBlank()){
            return ResponseEntity.
                    status(400).
                    body(Map.of("msg", "Query is empty"));
        }

        List<NewsHeader> news = hackerNewsService.searchNews(q, p);

        if (news == null) {
            return ResponseEntity.
                    status(500).
                    body(Map.of("msg", "Service unavailable"));
        }

        else if (news.isEmpty()){
            return ResponseEntity.
                    status(201).
                    body(Map.of("msg", "No search result"));
        }

        return ResponseEntity.
                status(200).
                body(news);
    }
}