package com.colorcraze.board.controllers;

import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RedisBoardSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisBoardSubscriber(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void handleMessage(byte[] messageBody) {
        try {
            String message = new String(messageBody);
            Map<String, Object> payload = objectMapper.readValue(
                message, new com.fasterxml.jackson.core.type.TypeReference<>() {}
            );

            String gameId = (String) payload.get("gameId");
            messagingTemplate.convertAndSend("/topic/board." + gameId, payload.get("results"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
