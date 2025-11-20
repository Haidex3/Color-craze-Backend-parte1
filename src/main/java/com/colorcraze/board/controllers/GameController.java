package com.colorcraze.board.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.colorcraze.board.models.Board;
import com.colorcraze.board.services.BoardService;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.services.WaitingRoomService;

import java.util.Map;

/**
 * REST controller responsible for handling game-related operations.
 * Provides endpoints to create a game from a waiting room and retrieve board state.
 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final BoardService boardService;
    private final WaitingRoomService waitingRoomService;

    /**
     * Creates a new game using the players in a specified waiting room.
     * Removes the waiting room after creating the game.
     *
     * @param roomId the ID of the waiting room
     * @return a response containing the new game ID and the list of players,
     * or a bad request response if the room does not exist or is empty
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

    /**
     * Retrieves the current state of a game board by its ID.
     *
     * @param gameId the ID of the game
     * @return a response containing the board state, or a 404 error if the game is not found
     */
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
