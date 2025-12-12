package pt.dei.googol.Projeto_SD.Servers.WebServer.WebSockets;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {
    private final StatsWebSocket statsWebSocket;

    public WebSocketConfiguration(StatsWebSocket statsWebSocket) {
        this.statsWebSocket = statsWebSocket;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(statsWebSocket, "/ws/stats");
    }
}
