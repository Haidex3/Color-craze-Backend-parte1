package com.Color_craze.board.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import com.Color_craze.board.models.Player;
import com.Color_craze.board.services.BoardService;
import com.Color_craze.utils.enums.ColorStatus;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final BoardService boardService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createGame() {
        String gameId = boardService.createNewBoard();
        return ResponseEntity.ok(Map.of("gameId", gameId));
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<Player> joinGame(@PathVariable String gameId, ColorStatus color) {
        Player player = boardService.addPlayerToBoard(gameId, color);
        return ResponseEntity.ok(player);
    }
}
