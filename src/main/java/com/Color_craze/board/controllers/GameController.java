package com.Color_craze.board.controllers;

import com.Color_craze.WaitingRoom.dtos.Responses.WaitingRoomState;
import com.Color_craze.WaitingRoom.services.WaitingRoomService;
import com.Color_craze.board.models.Board;
import com.Color_craze.board.services.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final BoardService boardService;
    private final WaitingRoomService waitingRoomService;

    /**
     * Crear partida a partir de una sala de espera
     */
    @PostMapping("/create-from-room/{roomId}")
    public ResponseEntity<Map<String, Object>> createGameFromRoom(@PathVariable String roomId) {
        WaitingRoomState roomState = waitingRoomService.getRoomState(roomId);

        if (roomState == null || roomState.getPlayers().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sala no existe o está vacía"));
        }
        Board board = boardService.createBoardWithPlayers(roomState.getRoomId(), roomState.getPlayerColors());
        waitingRoomService.removeRoom(roomId);
        return ResponseEntity.ok(Map.of(
                "gameId", board.getGameId(),
                "players", board.getPlayers()
        ));
    }
}
