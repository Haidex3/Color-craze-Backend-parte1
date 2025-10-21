package com.Color_craze.WaitingRoom.controllers;

import com.Color_craze.WaitingRoom.dtos.Responses.WaitingRoomState;
import com.Color_craze.WaitingRoom.models.WaitingRoom;
import com.Color_craze.WaitingRoom.services.WaitingRoomService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/waiting-room")
@AllArgsConstructor
public class WaitingRoomRestController {

    private final WaitingRoomService waitingRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/create")
    public ResponseEntity<WaitingRoomState> createRoom(String playerId) {
        WaitingRoom room = waitingRoomService.createRoom();
        WaitingRoomState state = new WaitingRoomState(
                room.getRoomId(),
                room.getPlayers(),
                room.getPlayerColors(),
                room.isFull()
        );
        state = waitingRoomService.joinRoom(room.getRoomId(), playerId);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/join/{roomId}/{playerId}")
        public ResponseEntity<WaitingRoomState> joinRoom(
                @PathVariable String roomId,
                @PathVariable String playerId
        ) {
            WaitingRoomState state = waitingRoomService.joinRoom(roomId, playerId);
            if (state == null) return ResponseEntity.badRequest().build();
            messagingTemplate.convertAndSend("/topic/waiting-room/" + roomId, state);

            return ResponseEntity.ok(state);
        }

}
