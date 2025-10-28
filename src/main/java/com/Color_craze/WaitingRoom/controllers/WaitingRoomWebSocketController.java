package com.Color_craze.WaitingRoom.controllers;

import com.Color_craze.WaitingRoom.dtos.Requests.SelectColorMessage;
import com.Color_craze.WaitingRoom.dtos.Responses.PlayerColorState;
import com.Color_craze.WaitingRoom.dtos.Responses.WaitingRoomState;
import com.Color_craze.WaitingRoom.services.WaitingRoomService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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