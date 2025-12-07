package pt.dei.googol.Projeto_SD.Servers.WebServer.Controllers;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/index")
public class IndexController {

    //private final GoogolClient googolClient;

    /*public IndexController(GoogolClient googolClient) {
        this.googolClient = googolClient;
    }
     */

    @GetMapping
    public String showIndexForm(Model model) {
        model.addAttribute("urlToIndex", "");
        return "index_form";
    }

    @PostMapping
    public String submitUrlForIndexing(
            @RequestParam("url") String url,
            RedirectAttributes redirectAttributes
    ) {
        try {
            //googolClient.indexUrl(url);  // Chamada RPC/RMI
            redirectAttributes.addFlashAttribute("success", "URL enviado para indexação: " + url);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao indexar: " + e.getMessage());
        }

        return "redirect:/index";
    }
}