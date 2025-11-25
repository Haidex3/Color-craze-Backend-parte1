package com.colorcraze.waitingroom.controllers;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.colorcraze.configs.ratelimit.RateLimit;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.models.WaitingRoom;
import com.colorcraze.waitingroom.services.WaitingRoomService;

/**
 * REST controller for managing waiting rooms in the game.
 *
 * Provides endpoints for creating a new waiting room and joining an existing one.
 * Uses WebSocket messaging to broadcast updates to clients subscribed to a room.
 */
@RestController
@RequestMapping("/api/waiting-room")
@AllArgsConstructor
public class WaitingRoomRestController {

    private static final Logger logger = LoggerFactory.getLogger(WaitingRoomRestController.class);

    private final WaitingRoomService waitingRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Creates a new waiting room and adds the requesting player to it.
     *
     * @param playerId ID of the player creating the room.
     * @return ResponseEntity containing the current state of the waiting room.
     */
    @PostMapping("/create/{playerId}")
    public ResponseEntity<WaitingRoomState> createRoom(@PathVariable String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            WaitingRoom room = waitingRoomService.createRoom();
            if (room == null) {
                return ResponseEntity.internalServerError().build();
            }

            WaitingRoomState state = waitingRoomService.joinRoom(room.getRoomId(), playerId);
            if (state == null) {
                return ResponseEntity.internalServerError().build();
            }

            return ResponseEntity.ok(state);
        } catch (Exception e) {
            logger.error("Unexpected error while creating room for player {}: {}", playerId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Adds a player to an existing waiting room and broadcasts the updated state.
     *
     * @param roomId   ID of the waiting room to join.
     * @param playerId ID of the player joining the room.
     * @return ResponseEntity containing the updated waiting room state, or a bad request response if the room is not found or join fails.
     */
    @PostMapping("/join/{roomId}/{playerId}")
    @RateLimit(limit = 3)
    public ResponseEntity<WaitingRoomState> joinRoom(
            @PathVariable String roomId,
            @PathVariable String playerId
    ) {
        if (roomId == null || roomId.trim().isEmpty() ||
            playerId == null || playerId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            WaitingRoomState state = waitingRoomService.joinRoom(roomId, playerId);
            if (state == null) {
                return ResponseEntity.badRequest().build();
            }

            sendRoomStateUpdate(roomId, state);

            return ResponseEntity.ok(state);
        } catch (Exception e) {
            logger.error("Unexpected error while joining room {} with player {}: {}", roomId, playerId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Sends an update about the current state of a waiting room to all subscribed WebSocket clients.
     * Extracted method to comply with SonarQube clean code rule for avoiding nested try/catch blocks.
     *
     * @param roomId the identifier of the waiting room whose state should be broadcasted
     * @param state  the current state of the room that will be sent to WebSocket subscribers
     */
    private void sendRoomStateUpdate(String roomId, WaitingRoomState state) {
        try {
            messagingTemplate.convertAndSend("/topic/waiting-room/" + roomId, state);
        } catch (Exception e) {
            logger.error("Failed to send WebSocket update for room {}: {}", roomId, e.getMessage(), e);
        }
    }
}
