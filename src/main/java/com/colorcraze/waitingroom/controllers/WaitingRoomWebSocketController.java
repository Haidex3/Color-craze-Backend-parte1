package com.colorcraze.waitingroom.controllers;

import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.colorcraze.waitingroom.dtos.requests.SelectColorMessage;
import com.colorcraze.waitingroom.dtos.responses.PlayerColorState;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.services.WaitingRoomService;

@Controller
@AllArgsConstructor
public class WaitingRoomWebSocketController {

    private final WaitingRoomService waitingRoomService;
    private final SimpMessagingTemplate messagingTemplate;

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