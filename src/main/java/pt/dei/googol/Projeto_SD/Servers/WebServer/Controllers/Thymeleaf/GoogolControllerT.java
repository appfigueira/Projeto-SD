package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers.Thymeleaf;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GoogolControllerT {

    @GetMapping("/")
    public String googol() {
        return "googol";
    }
}