package com.colorcraze.board.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RedisBoardSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String serverId; // injected

    public RedisBoardSubscriber(SimpMessagingTemplate messagingTemplate, String serverId) {
        this.messagingTemplate = messagingTemplate;
        this.serverId = serverId;
    }

    // MessageListenerAdapter will probably deliver a byte[]; handle both byte[] and String safely.
    public void handleMessage(byte[] messageBody) {
        try {
            String message = new String(messageBody, StandardCharsets.UTF_8);
            handleMessageString(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // fallback if adapter passes a String
    public void handleMessage(String message) {
        handleMessageString(message);
    }

    private void handleMessageString(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                message, new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {}
            );

            String origin = (String) payload.get("origin");
            if (origin != null && origin.equals(serverId)) {
                // skip messages originated in this instance to avoid duplicate broadcasts
                return;
            }

            String gameId = (String) payload.get("gameId");
            String type = (String) payload.get("type");
            Object body = payload.get("payload");

            if ("move".equals(type)) {
                // payload is a single MoveResult (or may be map representing it)
                messagingTemplate.convertAndSend("/topic/board." + gameId, body);
            } else if ("timer".equals(type) || "end".equals(type) || "other".equals(type)) {
                messagingTemplate.convertAndSend("/topic/board." + gameId, body);
            } else {
                // default: forward entire payload
                messagingTemplate.convertAndSend("/topic/board." + gameId, body);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
