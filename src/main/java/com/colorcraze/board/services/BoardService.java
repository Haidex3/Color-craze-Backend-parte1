package com.colorcraze.board.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.colorcraze.board.dtos.responses.MoveResult;
import com.colorcraze.board.dtos.responses.PlatformUpdate;
import com.colorcraze.board.dtos.responses.PlayerUpdate;
import com.colorcraze.board.models.Board;
import com.colorcraze.board.models.Box;
import com.colorcraze.board.models.Platform;
import com.colorcraze.board.models.Player;
import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.utils.enums.PlayerMove;

@Service
public class BoardService {

    private final Map<String, Board> boards = new ConcurrentHashMap<>();

    public Board createBoardWithPlayers(String gameId, Map<String, ColorStatus> playerColors) {
        if (boards.containsKey(gameId)) {
            throw new IllegalStateException("El tablero con id " + gameId + " ya existe");
        }

        Board newBoard = new Board(gameId, playerColors);
        if (playerColors != null) {
            playerColors.forEach((playerId, color) -> {
                Player player = new Player(UUID.fromString(playerId), color);
                newBoard.addPlayer(player);
            });
        }

        boards.put(gameId, newBoard);
        return newBoard;
    }

    public Board getBoard(String gameId) {
        Board board = boards.get(gameId);
        if (board == null) {
            throw new IllegalStateException("No existe un tablero con id " + gameId);
        }
        return board;
    }

    public Box getBlock(String gameId, int row, int col) {
        return getBoard(gameId).getGrid()[row][col];
    }

    public void setBlock(String gameId, int row, int col, Box block) {
        getBoard(gameId).getGrid()[row][col] = block;
    }

    public List<MoveResult> movePlayer(String gameId, String playerId, PlayerMove playerMove) {
        Board board = getBoard(gameId);
        UUID uuid = UUID.fromString(playerId);

        List<MoveResult> results = new ArrayList<>();

        if (playerMove == PlayerMove.UP) {
            if (board.isPlayerUp(uuid) || !(board.getRowDownPLayer(playerId) instanceof Platform)) {
                return List.of();
            }

            board.setPlayerIsUp(uuid, true);

            List<PlatformUpdate> totalPlatformUpdates = new ArrayList<>();
            List<PlayerUpdate> totalPlayerUpdates = new ArrayList<>();
            MoveResult lastStep = null;

            for (int i = 0; i < 4; i++) {
                MoveResult stepResult = board.movePlayer(uuid, PlayerMove.UP);
                if (stepResult == null) break;

                totalPlatformUpdates.addAll(stepResult.platforms());
                totalPlayerUpdates.addAll(stepResult.affectedPlayers());
                lastStep = stepResult;
            }

            board.setPlayerIsUp(uuid, false);

            if (lastStep != null) {
                MoveResult finalResult = new MoveResult(
                    uuid,
                    lastStep.newRow(),
                    lastStep.newCol(),
                    totalPlatformUpdates,
                    totalPlayerUpdates,
                    lastStep.success(),
                    lastStep.gravity()
                );
                results.add(finalResult);
            }

            return results;
        }

        else {
            results.add(board.movePlayer(uuid, playerMove));
        }
        return results;
    }

    public Player addPlayerToBoard(String gameId, ColorStatus color) {
        Board board = getBoard(gameId);
        Player newPlayer = new Player(UUID.randomUUID(), color);
        board.addPlayer(newPlayer);
        return newPlayer;
    }

    public Set<String> getAllBoardIds() {
        return boards.keySet();
    }
}
