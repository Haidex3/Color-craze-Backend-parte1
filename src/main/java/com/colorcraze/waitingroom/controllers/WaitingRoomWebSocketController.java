package com.colorcraze.waitingroom.controllers;

import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.colorcraze.waitingroom.dtos.requests.SelectColorMessage;
import com.colorcraze.waitingroom.dtos.responses.PlayerColorState;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.services.WaitingRoomService;

/**
 * WebSocket controller for handling real-time updates in waiting rooms.
 * 
 * Manages player actions such as selecting a color and broadcasts updates to
 * all clients subscribed to the room or sends private error messages if selection fails.
 */
@Controller
@AllArgsConstructor
public class WaitingRoomWebSocketController {

    private final WaitingRoomService waitingRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles player color selection in a waiting room.
     * 
     * If the color is successfully updated, the new state of the waiting room
     * is broadcast to all subscribers of the room. If the selection fails,
     * a private message is sent to the player with an error state.
     * 
     * @param roomId  The ID of the waiting room.
     * @param message Payload containing the player's ID and chosen color.
     */
    @MessageMapping("/waiting-room/{roomId}/select-color")
    public void selectColor(
            @DestinationVariable String roomId,
            @Payload SelectColorMessage message
    ) {
        boolean updated = waitingRoomService.selectColor(roomId, message.getPlayerId(), message.getColor());

        if (updated) {
            WaitingRoomState state = waitingRoomService.getRoomState(roomId);
            messagingTemplate.convertAndSend("/topic/waiting-room/" + roomId, state);
        } else {
            PlayerColorState playerState = new PlayerColorState(
                    message.getPlayerId(),
                    message.getColor(),
                    false
            );
            messagingTemplate.convertAndSendToUser(
                    message.getPlayerId(), 
                    "/queue/waiting-room/color-error", 
                    playerState
            );
        }
    }

}