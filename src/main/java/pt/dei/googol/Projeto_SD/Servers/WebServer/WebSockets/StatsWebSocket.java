package pt.dei.googol.Projeto_SD.Servers.WebServer.WebSockets;


import pt.dei.googol.Projeto_SD.Common.DataStructures.SystemStats;
import pt.dei.googol.Projeto_SD.Servers.WebServer.Services.Googol.SystemStatsService;

import com.google.gson.Gson;
import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StatsWebSocket extends TextWebSocketHandler {
    private final SystemStatsService systemStatsService;
    private boolean registered = false;

    public StatsWebSocket(@Lazy SystemStatsService systemStatsService) {
        this.systemStatsService = systemStatsService;
    }

    //Client Sessions List
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<WebSocketSession, Object> sessionLocks = new ConcurrentHashMap<>();


    @Override
    public void afterConnectionEstablished(@Nonnull WebSocketSession session) {
        sessions.add(session);
        sessionLocks.put(session, new Object());
        System.out.println("[Stats WS] Client connected: " + session.getId());
        if (!registered) {
            systemStatsService.getSystemStats();
            registered = true;
            System.out.println("[Stats WS] Web Server registered to Gateway Server for system stats.");
        }
    }

    @Override
    public void afterConnectionClosed(@Nonnull WebSocketSession session, @Nonnull CloseStatus status) {
        sessions.remove(session);
        sessionLocks.remove(session);
        System.out.println("[Stats WS] Client disconnected: " + session.getId());
    }

    public void broadcastStats(SystemStats systemStats) {
        String json = new Gson().toJson(systemStats);

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) continue;

            Object lock = sessionLocks.get(session);

            synchronized (lock) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    System.err.println("[Stats WS] Failed to send stats: " + e.getMessage());
                }
            }
        }
    }
}
