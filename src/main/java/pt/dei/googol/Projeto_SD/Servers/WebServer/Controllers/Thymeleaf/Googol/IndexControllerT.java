package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers.Thymeleaf.Googol;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexControllerT {
    @GetMapping("/index")
    public String index() {
        return "Googol/index";
    }
}
