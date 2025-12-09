package pt.dei.googol.Projeto_SD.Servers.WebServer.Components.WebSockets;


import pt.dei.googol.Projeto_SD.Common.DataStructures.SystemStats;

import com.google.gson.Gson;
import jakarta.annotation.Nonnull;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StatsWebSocket extends TextWebSocketHandler {

    //Client Sessions List
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(@Nonnull WebSocketSession session) {
        sessions.add(session);
        System.out.println("[Stats WS] Client connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(@Nonnull WebSocketSession session, @Nonnull CloseStatus status) {
        sessions.remove(session);
        System.out.println("[Stats WS] Client disconnected: " + session.getId());
    }

    public void broadcastStats(SystemStats systemStats) {
        String jsonSystemStats = new Gson().toJson(systemStats);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonSystemStats));
                }
            } catch (IOException e) {
                System.err.println("[Stats WS] Error: Failed to broadcast system stats: " + e.getMessage());
            }
        }
    }
}
