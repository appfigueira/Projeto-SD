package pt.dei.googol.Projeto_SD.Servers.WebServer.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import pt.dei.googol.Projeto_SD.Common.Functions.RMIConnectionManager;
import pt.dei.googol.Projeto_SD.Servers.GatewayServer.Interfaces.IGatewayWeb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class RMIConfiguration {

    @Bean
    public RMIConnectionManager<IGatewayWeb> gatewayConnectionManager() {
        Properties config = new Properties();
        try (InputStream is = new ClassPathResource("files/SystemConfiguration").getInputStream()) {
            config.load(is);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load config");
        }

        String host = config.getProperty("gateway.host", "localhost");
        int port = Integer.parseInt(config.getProperty("gateway.portRMI", "1099"));
        String serviceName = config.getProperty("gateway.serviceName", "Gateway");

        return new RMIConnectionManager<>(host, port, serviceName);
    }
}