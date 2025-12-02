package com.colorcraze.board.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisBoardSubscriberTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private RedisBoardSubscriber redisBoardSubscriber;
    private final String serverId = "test-server-1";
    private final String otherServerId = "other-server-2";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        redisBoardSubscriber = new RedisBoardSubscriber(messagingTemplate, serverId);
    }

    @Test
    void handleMessage_WithByteArray_ValidMessage_SendsToCorrectTopic() throws Exception {
        String gameId = "test-game-123";
        Map<String, Object> payload = Map.of(
            "move", "test-move",
            "player", "player-1"
        );
        
        Map<String, Object> message = Map.of(
            "origin", otherServerId,
            "gameId", gameId,
            "payload", payload
        );
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        byte[] messageBytes = jsonMessage.getBytes();
        
        redisBoardSubscriber.handleMessage(messageBytes);
        
        verify(messagingTemplate).convertAndSend(
                "/topic/board." + gameId,
                payload
            );
    }

    @Test
    void handleMessage_WithString_ValidMessage_SendsToCorrectTopic() throws Exception {
        String gameId = "test-game-123";
        Map<String, Object> payload = Map.of(
            "timer", Map.of("timeLeft", 30),
            "type", "countdown"
        );
        
        Map<String, Object> message = Map.of(
            "origin", otherServerId,
            "gameId", gameId,
            "payload", payload
        );
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        
        verify(messagingTemplate).convertAndSend(
                "/topic/board." + gameId,
                payload
            );
    }

    @Test
    void handleMessage_FromSameServer_DoesNotSendMessage() throws Exception {
        String gameId = "test-game-123";
        Map<String, Object> payload = Map.of("move", "test");
        
        Map<String, Object> message = Map.of(
            "origin", serverId,
            "gameId", gameId,
            "payload", payload
        );
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void handleMessage_InvalidJson_DoesNotThrowException() {
        String invalidJson = "{invalid json";
        
        assertDoesNotThrow(() -> redisBoardSubscriber.handleMessage(invalidJson));
        assertDoesNotThrow(() -> redisBoardSubscriber.handleMessage(invalidJson.getBytes()));
        
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void handleMessage_MissingOriginField_SendsMessage() throws Exception {
        String gameId = "test-game-123";
        Map<String, Object> payload = Map.of("update", "test");
        
        Map<String, Object> message = Map.of(
            "gameId", gameId,
            "payload", payload
        );
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        
        verify(messagingTemplate).convertAndSend(
                "/topic/board." + gameId,
                payload
            );
    }

    @Test
    void handleMessage_MissingGameId_SendsToNullDestination() throws Exception {
        Map<String, Object> payload = Map.of("update", "test");
        
        Map<String, Object> message = Map.of(
            "origin", otherServerId,
            "payload", payload
        );
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        
        verify(messagingTemplate).convertAndSend(
                "/topic/board.null",
                payload
            );
    }

    @Test
    void handleMessage_GameIdIsNullInJson_SendsToNullDestination() throws Exception {
        Map<String, Object> payload = Map.of("update", "test");
        
        String jsonMessage = String.format(
            "{\"origin\":\"%s\",\"gameId\":null,\"payload\":%s}",
            otherServerId,
            objectMapper.writeValueAsString(payload)
        );
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        
        verify(messagingTemplate).convertAndSend(
                "/topic/board.null",
                payload
            );
    }

    @Test
    void handleMessage_MissingPayload_DoesNotSendMessage() throws Exception {
        String gameId = "test-game-123";
        
        Map<String, Object> message = Map.of(
            "origin", otherServerId,
            "gameId", gameId
        );
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void handleMessage_NullOrigin_SendsMessage() throws Exception {
        String gameId = "test-game-123";
        Map<String, Object> payload = Map.of("update", "test");
        
        String jsonMessage = String.format(
            "{\"origin\":null,\"gameId\":\"%s\",\"payload\":%s}",
            gameId,
            objectMapper.writeValueAsString(payload)
        );
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        
        verify(messagingTemplate).convertAndSend(
                "/topic/board." + gameId,
                payload
            );
    }

    @Test
    void handleMessage_EmptyPayload_SendsEmptyPayload() throws Exception {
        String gameId = "test-game-123";
        Map<String, Object> payload = Map.of();
        
        Map<String, Object> message = Map.of(
            "origin", otherServerId,
            "gameId", gameId,
            "payload", payload
        );
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        
        verify(messagingTemplate).convertAndSend(
                "/topic/board." + gameId,
                payload
            );
    }

    @Test
    void handleMessage_ComplexPayloadStructure_SendsCorrectly() throws Exception {
        String gameId = "test-game-123";
        
        Map<String, Object> player1 = Map.of("score", 100, "color", "RED");
        Map<String, Object> player2 = Map.of("score", 85, "color", "BLUE");
        Map<String, Object> players = Map.of("player1", player1, "player2", player2);
        
        Map<String, Object> nestedPayload = Map.of(
            "players", players,
            "boardState", "active",
            "timestamp", 1234567890
        );
        
        Map<String, Object> message = Map.of(
            "origin", otherServerId,
            "gameId", gameId,
            "payload", nestedPayload
        );
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        
        Map<String, Object> expectedPayload = objectMapper.readValue(
            objectMapper.writeValueAsString(nestedPayload), 
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
        );
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        
        verify(messagingTemplate).convertAndSend(
                "/topic/board." + gameId,
                expectedPayload
            );
    }

    @Test
    void handleMessage_ByteArrayAndStringMethods_EquivalentBehavior() throws Exception {
        String gameId = "test-game-456";
        Map<String, Object> payload = Map.of("action", "move");
        
        Map<String, Object> message = Map.of(
            "origin", otherServerId,
            "gameId", gameId,
            "payload", payload
        );
        
        String jsonMessage = objectMapper.writeValueAsString(message);
        byte[] messageBytes = jsonMessage.getBytes();
        
        redisBoardSubscriber.handleMessage(messageBytes);
        verify(messagingTemplate).convertAndSend(
            "/topic/board." + gameId,
            payload
        );
        
        reset(messagingTemplate);
        
        redisBoardSubscriber.handleMessage(jsonMessage);
        verify(messagingTemplate).convertAndSend(
            "/topic/board." + gameId,
            payload
        );

    }

    @Test
    void handleMessage_MalformedByteArray_DoesNotThrow() {
        byte[] malformedBytes = new byte[] {0, 1, 2, 3};
        
        assertDoesNotThrow(() -> redisBoardSubscriber.handleMessage(malformedBytes));
        
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void handleMessage_NullInput_DoesNotThrow() {
        assertDoesNotThrow(() -> redisBoardSubscriber.handleMessage((String) null));
        assertDoesNotThrow(() -> redisBoardSubscriber.handleMessage((byte[]) null));
        
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}