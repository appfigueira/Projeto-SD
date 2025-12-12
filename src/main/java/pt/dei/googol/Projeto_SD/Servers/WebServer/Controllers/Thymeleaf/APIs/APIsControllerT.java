package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers.Thymeleaf.APIs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class APIsControllerT {
    @GetMapping("/apis")
    public String search() {
        return "APIs/apis";
    }
}
