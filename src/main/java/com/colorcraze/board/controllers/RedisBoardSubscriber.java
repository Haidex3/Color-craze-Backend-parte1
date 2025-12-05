package com.colorcraze.board.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.colorcraze.board.dtos.responses.MoveResult;
import com.colorcraze.board.dtos.responses.PlatformUpdate;
import com.colorcraze.board.dtos.responses.PlayerUpdate;
import com.colorcraze.board.dtos.responses.TimerResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class RedisBoardSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String serverId;

    private static final String BOARD_TOPIC_PREFIX = "/topic/board.";

    private static final Logger logger = LoggerFactory.getLogger(RedisBoardSubscriber.class);

    public RedisBoardSubscriber(SimpMessagingTemplate messagingTemplate, String serverId) {
        this.messagingTemplate = messagingTemplate;
        this.serverId = serverId;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void handleMessage(byte[] messageBody) {
        try {
            String message = new String(messageBody, StandardCharsets.UTF_8);
            handleMessageString(message);
        } catch (Exception e) {
            logger.error("Error handling raw message", e);
        }
    }

    public void handleMessage(String message) {
        handleMessageString(message);
    }

    private void handleMessageString(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            if (root.has("origin") && serverId.equals(root.get("origin").asText())) {
                return;
            }

            String type = root.path("type").asText();
            String gameId = root.path("gameId").asText();
            JsonNode payload = root.path("payload");

            switch (type) {
                case "move" -> handleMoveMessage(gameId, payload);
                case "timer" -> handleTimerMessage(gameId, payload);
                case "end" -> handleEndGameMessage(gameId, payload);
                default -> logger.warn("Tipo desconocido recibido desde Redis: {}", type);
            }

        } catch (Exception e) {
            logger.error("Error handling message: {}", message, e);
        }
    }

    private void handleMoveMessage(String gameId, JsonNode payload) {

        JsonNode platformsNode = payload.path("platforms");
        JsonNode playersNode = payload.path("affectedPlayers");

        List<PlatformUpdate> platforms = parseListField(platformsNode, new TypeReference<List<PlatformUpdate>>() {});
        List<PlayerUpdate> affectedPlayers = parseListField(playersNode, new TypeReference<List<PlayerUpdate>>() {});

        MoveResult result = new MoveResult(
                payload.path("playerId").isTextual()
                        ? java.util.UUID.fromString(payload.get("playerId").asText())
                        : null,
                payload.path("newRow").asInt(),
                payload.path("newCol").asInt(),
                platforms,
                affectedPlayers,
                payload.path("success").asBoolean(),
                payload.path("gravity").asBoolean()
        );

        messagingTemplate.convertAndSend(BOARD_TOPIC_PREFIX + gameId, result);
    }

    private void handleTimerMessage(String gameId, JsonNode payload) {
        TimerResponse timer = objectMapper.convertValue(payload, TimerResponse.class);
        messagingTemplate.convertAndSend(BOARD_TOPIC_PREFIX + gameId, timer);
    }
    private <T> List<T> parseListField(JsonNode node, TypeReference<List<T>> ref) {

        try {
            if (node.isArray()
                    && node.size() == 2
                    && node.get(0).isTextual()
                    && node.get(1).isArray()) {

                return objectMapper.convertValue(node.get(1), ref);
            }
            if (node.isArray()) {
                return objectMapper.convertValue(node, ref);
            }

        } catch (Exception e) {
            logger.error("Error parsing list field: {}", node, e);
        }

        return List.of();
    }

    private void handleEndGameMessage(String gameId, JsonNode payload) {
            try {
                JsonNode clean = cleanEndPayload(payload);
                messagingTemplate.convertAndSend(BOARD_TOPIC_PREFIX + gameId, clean);
            } catch (Exception e) {
                logger.error("Error handling end game message: {}", payload, e);
            }
        }

        private JsonNode cleanEndPayload(JsonNode payload) {
        ObjectMapper mapper = this.objectMapper;
        ObjectNode clean = mapper.createObjectNode();

        clean.put("gameOver", payload.path("gameOver").asBoolean());

        JsonNode playersNode = payload.path("players");

        if (playersNode.isArray()
                && playersNode.size() == 2
                && playersNode.get(0).isTextual()
                && playersNode.get(1).isArray()) {

            clean.set("players", playersNode.get(1));
            return clean;
        }
        if (playersNode.isArray()) {
            clean.set("players", playersNode);
            return clean;
        }

        clean.set("players", mapper.createArrayNode());
        return clean;
    }


}
