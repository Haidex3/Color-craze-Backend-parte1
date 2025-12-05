package com.colorcraze.board.services;

import java.time.Duration;
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

import java.util.regex.Pattern;

/**
 * Service responsible for managing game boards and handling player moves.
 * Provides methods to create boards, retrieve boards, manipulate blocks, 
 * move players, and manage board state using Redis as a backing store.
 */
@Service
public class BoardService {

    private static final String BOARD_KEY_PREFIX = "board:";
    private static final String GAME_ID_FIELD = "gameId";
    private static final String ORIGIN_FIELD = "origin";
    private static final String TYPE_FIELD = "type";
    private static final String PAYLOAD_FIELD = "payload";
    private static final Pattern SAFE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9-]+$");

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

    public Board createBoardWithPlayers(String gameId, Map<String, ColorStatus> playerColors) {
        String key = BOARD_KEY_PREFIX + gameId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            throw new IllegalStateException("Board with id " + gameId + " already exists");
        }

        Board newBoard = new Board(gameId, playerColors);
        redisTemplate.opsForValue().set(key, newBoard);
        return newBoard;
    }

    public Board getBoard(String gameId) {
        try {
            String key = BOARD_KEY_PREFIX + gameId;
            Board board = (Board) redisTemplate.opsForValue().get(key);
            if (board == null) {
                throw new IllegalStateException("No board exists with id " + gameId);
            }
            return board;
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public Box getBlock(String gameId, int row, int col) {
        return getBoard(gameId).getGrid()[row][col];
    }

    public void setBlock(String gameId, int row, int col, Box block) {
        Board board = getBoard(gameId);
        board.getGrid()[row][col] = block;
        redisTemplate.opsForValue().set(BOARD_KEY_PREFIX + gameId, board);
    }

    public Player addPlayerToBoard(String gameId, ColorStatus color) {
        Board board = getBoard(gameId);
        Player newPlayer = new Player(UUID.randomUUID(), color);
        board.addPlayer(newPlayer);
        redisTemplate.opsForValue().set(BOARD_KEY_PREFIX + gameId, board);
        return newPlayer;
    }

    public Set<String> getAllBoardIds() {
        return redisTemplate.keys(BOARD_KEY_PREFIX + "*");
    }

    private void validateParameters(String gameId, String playerId, PlayerMove playerMove) {
        if (!SAFE_ID_PATTERN.matcher(gameId).matches()) {
            throw new IllegalArgumentException("Invalid gameId");
        }
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Invalid playerId");
        }
        if (playerMove == null) {
            throw new IllegalArgumentException("Player move cannot be null");
        }
    }

    public List<MoveResult> movePlayer(String gameId, String playerId, PlayerMove playerMove) {
        validateParameters(gameId, playerId, playerMove);
        Board board = getBoard(gameId);
        List<MoveResult> results = boardServiceLocalMove(board, playerId, playerMove);

        redisTemplate.opsForValue().set(BOARD_KEY_PREFIX + gameId, board);

        for (MoveResult r : results) {

            Map<String, Object> msg = Map.of(
                    TYPE_FIELD, "move",
                    GAME_ID_FIELD, gameId,
                    ORIGIN_FIELD, serverId,
                    PAYLOAD_FIELD, r
            );
            redisTemplate.convertAndSend(boardTopic.getTopic(), msg);
        }
        return List.copyOf(results);
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

    public void startGameTimer(String gameId, int durationSeconds) {
        if (gameTimers.containsKey(gameId)) return;

        final int[] secondsLeft = {durationSeconds};

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (secondsLeft[0] > 0) {
                secondsLeft[0]--;
                messagingTemplate.convertAndSend("/topic/board." + gameId, Map.of("timeLeft", secondsLeft[0]));

                Map<String, Object> msg = Map.of(
                    TYPE_FIELD, "timer",
                    GAME_ID_FIELD, gameId,
                    ORIGIN_FIELD, serverId,
                    PAYLOAD_FIELD, Map.of("timeLeft", secondsLeft[0])
                );
                redisTemplate.convertAndSend(boardTopic.getTopic(), msg);

            } else {
                endGame(gameId);
                ScheduledFuture<?> f = gameTimers.remove(gameId);
                f.cancel(true);
            }
        }, Duration.ofSeconds(1));

        gameTimers.put(gameId, future);
    }

    public void endGame(String gameId) {
        Board board = getBoard(gameId);
        List<Player> players = new ArrayList<>(board.getPlayers().values());
        messagingTemplate.convertAndSend("/topic/board." + gameId, Map.of(
                "gameOver", true,
                "players", players
        ));

        Map<String, Object> msg = Map.of(
            TYPE_FIELD, "end",
            GAME_ID_FIELD, gameId,
            ORIGIN_FIELD, serverId,
            PAYLOAD_FIELD, Map.of(
                        "gameOver", true,
                        "players", players
                )
        );
        redisTemplate.convertAndSend(boardTopic.getTopic(), msg);

        redisTemplate.delete(BOARD_KEY_PREFIX + gameId);
    }
}
