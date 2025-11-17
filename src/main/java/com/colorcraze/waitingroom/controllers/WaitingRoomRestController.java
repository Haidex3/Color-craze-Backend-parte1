package com.colorcraze.waitingroom.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.models.WaitingRoom;
import com.colorcraze.waitingroom.services.WaitingRoomService;

@RestController
@RequestMapping("/api/waiting-room")
@AllArgsConstructor
public class WaitingRoomRestController {

    private final WaitingRoomService waitingRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/create/{playerId}")
    public ResponseEntity<WaitingRoomState> createRoom(@PathVariable String playerId) {
        System.out.println("Creating room for player: " + playerId);
        WaitingRoom room = waitingRoomService.createRoom();
        WaitingRoomState state = new WaitingRoomState(
                room.getRoomId(),
                room.getPlayers(),
                room.getPlayerColors(),
                room.isFull(),
                room.getSeconds()   
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
