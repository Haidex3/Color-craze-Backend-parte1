package com.colorcraze.board.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.colorcraze.board.models.Board;
import com.colorcraze.board.services.BoardService;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.services.WaitingRoomService;

import java.util.Map;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final BoardService boardService;
    private final WaitingRoomService waitingRoomService;

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

    @GetMapping("/{gameId}")
    public ResponseEntity<Object> getBoardState(@PathVariable String gameId) {
        Board board = boardService.getBoard(gameId);

        if (board == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Juego no encontrado"));
        }

        return ResponseEntity.ok(board);
    }

}
