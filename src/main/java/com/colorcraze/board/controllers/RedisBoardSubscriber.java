package com.colorcraze.board.controllers;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RedisBoardSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String serverId;
    private static final String BOARD_TOPIC_PREFIX = "/topic/board.";

    public RedisBoardSubscriber(SimpMessagingTemplate messagingTemplate, String serverId) {
        this.messagingTemplate = messagingTemplate;
        this.serverId = serverId;
    }

    public void handleMessage(byte[] messageBody) {
        try {
            String message = new String(messageBody, StandardCharsets.UTF_8);
            handleMessageString(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMessage(String message) {
        handleMessageString(message);
    }

        private void handleMessageString(String message) {
            try {
                Map<String, Object> payload = objectMapper.readValue(
                    message, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );

                String origin = (String) payload.get("origin");
                if (origin != null && origin.equals(serverId)) {
                    return;
                }

                String gameId = (String) payload.get("gameId");
                Object body = payload.get("payload");

                messagingTemplate.convertAndSend(BOARD_TOPIC_PREFIX + gameId, body);

            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}
