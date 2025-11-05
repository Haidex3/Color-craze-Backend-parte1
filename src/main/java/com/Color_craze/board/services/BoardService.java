package com.Color_craze.board.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.Color_craze.board.models.Box;
import com.Color_craze.board.models.Platform;
import com.Color_craze.board.models.Player;
import com.Color_craze.board.models.Board;
import com.Color_craze.board.dtos.Responses.MoveResult;
import com.Color_craze.utils.enums.ColorStatus;
import com.Color_craze.utils.enums.PlayerMove;

@Service
public class BoardService {

    private final Map<String, Board> boards = new ConcurrentHashMap<>();

    /**
     * Crea un nuevo tablero con los jugadores dados.
     * Si el gameId ya existe, lanza excepción.
     */
    public Board createBoardWithPlayers(String gameId, Map<String, ColorStatus> playerColors) {
        System.out.println("Intentando crear tablero con id: " + gameId + "colores de jugadores: " + playerColors);
        if (boards.containsKey(gameId)) {
            throw new IllegalStateException("El tablero con id " + gameId + " ya existe");
        }

        System.out.println("Creando nuevo tablero con jugadores para gameId: " + gameId);

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

    /**
     * Obtiene un tablero existente.
     * Si no existe, lanza excepción.
     */
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
            if (board.isPlayerUp(uuid) || !(board.getRowDownPLayer(playerId) instanceof Platform) ) {
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

    public List<MoveResult> applyGravity(String gameId) {
        Board board = getBoard(gameId);
        List<MoveResult> results = new ArrayList<>();

        for (Player player : board.getPlayers().values()) {
            if (!player.isUp() && !isStandingOnPlatform(board, player)) {
                results.add(board.movePlayer(player.getId(), PlayerMove.DOWN));
            }
        }

        return results;
    }

    private boolean isStandingOnPlatform(Board board, Player player) {
        int rowBelow = player.getRow() + 1;
        if (rowBelow >= board.getGrid().length) return true;
        Box below = board.getGrid()[rowBelow][player.getCol()];
        return below instanceof Platform || below instanceof Player;
    }


    /**
     * Agrega un nuevo jugador a un tablero existente.
     */
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
