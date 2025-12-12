package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers.Thymeleaf.Googol;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchControllerT {
    @GetMapping("/search")
    public String search() {
        return "Googol/search";
    }
}