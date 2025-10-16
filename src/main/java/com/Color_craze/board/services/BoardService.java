package com.Color_craze.board.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.Color_craze.board.models.Box;
import com.Color_craze.board.models.Player;
import com.Color_craze.utils.enums.ColorStatus;
import com.Color_craze.utils.enums.PlayerMove;
import com.Color_craze.board.dtos.Responses.MoveResult;
import com.Color_craze.board.models.Board;

@Service
public class BoardService {

    private final Map<String, Board> boards = new ConcurrentHashMap<>();

    private Board getOrCreateBoard(String gameId) {
        return boards.computeIfAbsent(gameId, id -> {
            Board newBoard = new Board();

            Player p1 = new Player(UUID.randomUUID(), ColorStatus.PINK);
            Player p2 = new Player(UUID.randomUUID(), ColorStatus.YELLOW);

            newBoard.addPlayer(p1);
            newBoard.addPlayer(p2);

            return newBoard;
        });
    }

    public Board getBoard(String gameId) {
        return getOrCreateBoard(gameId);
    }

    public Box getBlock(String gameId, int row, int col) {
        return getOrCreateBoard(gameId).getGrid()[row][col];
    }

    public void setBlock(String gameId, int row, int col, Box block) {
        getOrCreateBoard(gameId).getGrid()[row][col] = block;
    }

    public List<MoveResult> movePlayer(String gameId, String playerId, PlayerMove playerMove) {
        Board board = getOrCreateBoard(gameId);
        UUID uuid = UUID.fromString(playerId);

        List<MoveResult> results = new ArrayList<>();

        if (playerMove == PlayerMove.UP) {
            if (board.isPlayerUp(uuid)){
                return null;
            }
            board.setPlayerIsUp(uuid, true);

            for (int i = 0; i < 4; i++) {
                MoveResult stepResult = board.movePlayer(uuid, playerMove);
                results.add(stepResult);
            }

            board.setPlayerIsUp(uuid, false);
        } else {
            results.add(board.movePlayer(uuid, playerMove));
        }

        return results;
    }

    public String createNewBoard() {
        String gameId = UUID.randomUUID().toString();
        boards.put(gameId, new Board());
        return gameId;
    }

    public Player addPlayerToBoard(String gameId, ColorStatus color) {
        Board board = getOrCreateBoard(gameId);
        Player newPlayer = new Player(UUID.randomUUID(), color);
        board.addPlayer(newPlayer);
        return newPlayer;
    }
}
