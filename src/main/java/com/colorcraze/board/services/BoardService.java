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

/**
 * Service responsible for managing game boards and handling player moves.
 * Provides methods to create boards, retrieve boards, manipulate blocks, 
 * move players, and manage board state.
 */
@Service
public class BoardService {

    private final Map<String, Board> boards = new ConcurrentHashMap<>();

    /**
     * Creates a new board with players assigned specified colors.
     *
     * @param gameId the unique ID of the game
     * @param playerColors a map of player IDs to their assigned colors
     * @return the created {@link Board} instance
     * @throws IllegalStateException if a board with the same ID already exists
     */
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

    /**
     * Retrieves the board corresponding to the given game ID.
     *
     * @param gameId the ID of the game
     * @return the {@link Board} instance
     * @throws IllegalStateException if no board exists for the given ID
     */
    public Board getBoard(String gameId) {
        Board board = boards.get(gameId);
        if (board == null) {
            throw new IllegalStateException("No existe un tablero con id " + gameId);
        }
        return board;
    }

    /**
     * Retrieves a block (Box) from the board at the specified coordinates.
     *
     * @param gameId the ID of the game
     * @param row the row index
     * @param col the column index
     * @return the {@link Box} at the given position
     */
    public Box getBlock(String gameId, int row, int col) {
        return getBoard(gameId).getGrid()[row][col];
    }

    /**
     * Sets a block (Box) on the board at the specified coordinates.
     *
     * @param gameId the ID of the game
     * @param row the row index
     * @param col the column index
     * @param block the {@link Box} to place
     */
    public void setBlock(String gameId, int row, int col, Box block) {
        getBoard(gameId).getGrid()[row][col] = block;
    }

    /**
     * Moves a player on the board in the specified direction.
     * Handles special logic for upward moves and gravity effects.
     *
     * @param gameId the ID of the game
     * @param playerId the ID of the player
     * @param playerMove the direction of the move
     * @return a list of {@link MoveResult} representing the move and its effects
     */
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

    /**
     * Adds a new player with the specified color to an existing board.
     *
     * @param gameId the ID of the game
     * @param color the color assigned to the new player
     * @return the newly created {@link Player}
     */
    public Player addPlayerToBoard(String gameId, ColorStatus color) {
        Board board = getBoard(gameId);
        Player newPlayer = new Player(UUID.randomUUID(), color);
        board.addPlayer(newPlayer);
        return newPlayer;
    }

    /**
     * Retrieves the set of all currently active board IDs.
     *
     * @return a {@link Set} of board IDs
     */
    public Set<String> getAllBoardIds() {
        return boards.keySet();
    }
}
