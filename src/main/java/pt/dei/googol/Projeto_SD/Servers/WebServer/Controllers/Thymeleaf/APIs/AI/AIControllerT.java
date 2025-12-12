package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers.Thymeleaf.APIs.AI;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AIControllerT {
    @GetMapping("/apis/ai")
    public String search() {
        return "APIs/AI/ai";
    }
}

