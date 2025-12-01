package com.colorcraze.board.services;

import java.util.*;
import java.util.concurrent.*;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
import java.nio.charset.StandardCharsets;

/**
 * Service responsible for managing game boards and handling player moves.
 * Provides methods to create boards, retrieve boards, manipulate blocks, 
 * move players, and manage board state using Redis as a backing store.
 */
@Service
public class BoardService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChannelTopic boardTopic;
    private final Map<String, ScheduledFuture<?>> gameTimers = new ConcurrentHashMap<>();
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private final String serverId;

    public BoardService(RedisTemplate<String, Object> redisTemplate,
                        SimpMessagingTemplate messagingTemplate,
                        ChannelTopic boardTopic,
                        String serverId) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.boardTopic = boardTopic;
        this.serverId = serverId;
        scheduler.initialize();
    }

    /**
     * Creates a new board with players assigned specified colors.
     *
     * @param gameId the unique ID of the game
     * @param playerColors a map of player IDs to their assigned colors
     * @return the created {@link Board} instance
     * @throws IllegalStateException if a board with the same ID already exists
     */
    public Board createBoardWithPlayers(String gameId, Map<String, ColorStatus> playerColors) {
        String key = "board:" + gameId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            throw new IllegalStateException("Board with id " + gameId + " already exists");
        }

        Board newBoard = new Board(gameId, playerColors);
        redisTemplate.opsForValue().set(key, newBoard);
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
        String key = "board:" + gameId;
        Board board = (Board) redisTemplate.opsForValue().get(key);
        if (board == null) throw new IllegalStateException("No board exists with id " + gameId);
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
        Board board = getBoard(gameId);
        board.getGrid()[row][col] = block;
        redisTemplate.opsForValue().set("board:" + gameId, board);
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
        redisTemplate.opsForValue().set("board:" + gameId, board);
        return newPlayer;
    }

    /**
     * Retrieves the set of all currently active board IDs.
     *
     * @return a {@link Set} of board IDs
     */
    public Set<String> getAllBoardIds() {
        // Redis does not provide a direct way to list keys in production, adjust if needed
        return redisTemplate.keys("board:*");
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
        List<MoveResult> results = boardServiceLocalMove(board, playerId, playerMove);

        redisTemplate.opsForValue().set("board:" + gameId, board);

        if (results != null) {
            for (MoveResult r : results) {
                Map<String, Object> msg = Map.of(
                        "type", "move",
                        "gameId", gameId,
                        "origin", serverId,
                        "payload", r
                );
                redisTemplate.convertAndSend(boardTopic.getTopic(), msg);
            }
        }

        return results;
    }


    private List<MoveResult> boardServiceLocalMove(Board board, String playerId, PlayerMove playerMove) {
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
                results.add(new MoveResult(
                        uuid,
                        lastStep.newRow(),
                        lastStep.newCol(),
                        totalPlatformUpdates,
                        totalPlayerUpdates,
                        lastStep.success(),
                        lastStep.gravity()
                ));
            }
        } else {
            results.add(board.movePlayer(uuid, playerMove));
        }

        return results;
    }

    /**
     * Starts a countdown timer for a game, sending periodic updates via WebSocket.
     * 
     * @param gameId the board's unique identifier
     * @param durationSeconds the duration of the timer in seconds
     */
    public void startGameTimer(String gameId, int durationSeconds) {
        if (gameTimers.containsKey(gameId)) return;

        final int[] secondsLeft = {durationSeconds};

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (secondsLeft[0] > 0) {
                secondsLeft[0]--;
                // local websocket (sigue existiendo)
                messagingTemplate.convertAndSend("/topic/board." + gameId, Map.of("timeLeft", secondsLeft[0]));

                // publish via redis so other nodes receive it
                Map<String, Object> msg = Map.of(
                    "type", "timer",
                    "gameId", gameId,
                    "origin", serverId,
                    "payload", Map.of("timeLeft", secondsLeft[0])
                );
                redisTemplate.convertAndSend(boardTopic.getTopic(), msg);

            } else {
                endGame(gameId);
                ScheduledFuture<?> f = gameTimers.remove(gameId);
                if (f != null) f.cancel(true);
            }
        }, 1000);

        gameTimers.put(gameId, future);
    }


    /**
     * Ends the game and broadcasts the final results to all subscribers via WebSocket.
     * 
     * @param gameId the board's unique identifier
     */
    public void endGame(String gameId) {
        Board board = getBoard(gameId);

        // Local send
        messagingTemplate.convertAndSend("/topic/board." + gameId, Map.of(
                "gameOver", true,
                "players", board.getPlayers().values()
        ));

        // Publish endGame to redis so other nodes re-broadcast
        Map<String, Object> msg = Map.of(
            "type", "end",
            "gameId", gameId,
            "origin", serverId,
            "payload", Map.of("gameOver", true, "players", board.getPlayers().values())
        );
        redisTemplate.convertAndSend(boardTopic.getTopic(), msg);

        redisTemplate.delete("board:" + gameId);
    }
}
