package pt.dei.googol.Projeto_SD;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Components.WebServer;

@SpringBootApplication
public class ProjetoSdApplication {
    private final WebServer webServer;

    public ProjetoSdApplication(WebServer webServer) {
        this.webServer = webServer;
    }

    @PreDestroy
    public void stopWebServer() {
        webServer.shutdown();
    }


    public static void main(String[] args) {
		SpringApplication.run(ProjetoSdApplication.class, args);
	}

}
