package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers.Thymeleaf.APIs.HackerNews;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HackerNewsT {
    @GetMapping("/apis/hackernews")
    public String search() {
        return "APIs/HackerNews/hackernews";
    }
}
