package pt.dei.googol.Projeto_SD;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Components.WebServer;

@SpringBootApplication
public class ProjetoSdApplication {

    @PostConstruct
    public void startWebServer() {
        WebServer.startWebServer();
    }


    public static void main(String[] args) {
		SpringApplication.run(ProjetoSdApplication.class, args);
	}

}
