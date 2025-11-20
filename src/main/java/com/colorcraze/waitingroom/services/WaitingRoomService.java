package com.colorcraze.waitingroom.services;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.colorcraze.board.models.Board;
import com.colorcraze.board.services.BoardService;
import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.models.WaitingRoom;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for managing waiting rooms.
 * It handles creation, joining, leaving, color selection, countdown,
 * and starting the game when countdown ends.
 */
@Slf4j
@Service
public class WaitingRoomService {

    private final Map<String, WaitingRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> roomSchedulers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final BoardService boardService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ROOM_ID_LENGTH = 4;
    private static final int DEFAULT_WAIT_SECONDS = 10;
    private final SecureRandom random = new SecureRandom();

    /**
     * Constructor for WaitingRoomService.
     *
     * @param messagingTemplate the messaging template for sending WebSocket messages
     * @param boardService      the board service for creating game boards
     */
    public WaitingRoomService(SimpMessagingTemplate messagingTemplate, BoardService boardService) {
        this.messagingTemplate = messagingTemplate;
        this.boardService = boardService;
    }

    /**
     * Generates a random room ID of fixed length.
     *
     * @return a new unique room ID
     */
    private String generateRoomId() {
        StringBuilder sb = new StringBuilder(ROOM_ID_LENGTH);
        for (int i = 0; i < ROOM_ID_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * Creates a new waiting room and starts its countdown.
     *
     * @return the newly created waiting room
     */
    public WaitingRoom createRoom() {
        String roomId;
        do {
            roomId = generateRoomId();
        } while (rooms.containsKey(roomId));

        WaitingRoom room = new WaitingRoom(roomId, DEFAULT_WAIT_SECONDS);
        rooms.put(roomId, room);
        startCountdown(room);
        return room;
    }

    /**
     * Starts the countdown for a waiting room.
     * Sends periodic updates to subscribed clients via WebSocket.
     *
     * @param room the waiting room to start the countdown for
     */
    private void startCountdown(WaitingRoom room) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        roomSchedulers.put(room.getRoomId(), scheduler);

        Runnable task = createCountdownTask(room, scheduler);
        scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Creates a countdown task for the waiting room.
     *
     * @param room      the waiting room
     * @param scheduler the scheduler managing this task
     * @return a runnable countdown task
     */
    private Runnable createCountdownTask(WaitingRoom room, ScheduledExecutorService scheduler) {
        return () -> {
            synchronized (room.getLock()) {
                try {
                    int seconds = room.getSeconds();
                    if (seconds > 0) {
                        room.setSeconds(seconds - 1);
                        sendRoomState(room);
                    } else {
                        startGameIfPlayersExist(room);
                        cleanupRoom(room, scheduler);
                    }
                } catch (Exception e) {
                    log.error("Error while executing countdown for room {}", room.getRoomId(), e);
                    cleanupRoom(room, scheduler);
                }
            }
        };
    }

    /**
     * Sends the current state of the waiting room to subscribed clients.
     *
     * @param room the waiting room
     */
    private void sendRoomState(WaitingRoom room) {
        WaitingRoomState state = new WaitingRoomState(
                room.getRoomId(),
                room.getPlayers(),
                room.getPlayerColors(),
                room.isFull(),
                room.getSeconds()
        );
        messagingTemplate.convertAndSend("/topic/waiting-room/" + room.getRoomId(), state);
    }

    /**
     * Starts the game if the waiting room has players.
     *
     * @param room the waiting room
     */
    private void startGameIfPlayersExist(WaitingRoom room) {
        Map<String, ColorStatus> playerColors = room.getPlayerColors();
        if (!playerColors.isEmpty()) {
            Board board = boardService.createBoardWithPlayers(room.getRoomId(), playerColors);
            messagingTemplate.convertAndSend("/topic/waiting-room/" + room.getRoomId() + "/start", board);
        }
    }

    /**
     * Cleans up the waiting room after countdown ends or on error.
     *
     * @param room      the waiting room
     * @param scheduler the scheduler to shut down
     */
    private void cleanupRoom(WaitingRoom room, ScheduledExecutorService scheduler) {
        rooms.remove(room.getRoomId());
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        roomSchedulers.remove(room.getRoomId());
    }

    /**
     * Retrieves a waiting room by its ID.
     *
     * @param roomId the room ID
     * @return an Optional containing the waiting room if it exists
     */
    public Optional<WaitingRoom> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    /**
     * Adds a player to a waiting room.
     *
     * @param roomId   the room ID
     * @param playerId the player ID
     * @return the updated waiting room state, or null if the room does not exist
     */
    public WaitingRoomState joinRoom(String roomId, String playerId) {
        WaitingRoom room = rooms.get(roomId);
        if (room == null) return null;
        return room.addPlayer(playerId);
    }

    /**
     * Removes a player from a waiting room.
     *
     * @param roomId   the room ID
     * @param playerId the player ID
     * @return true if the player was removed, false otherwise
     */
    public boolean leaveRoom(String roomId, String playerId) {
        WaitingRoom room = rooms.get(roomId);
        if (room == null) return false;
        boolean removed = room.removePlayer(playerId);
        if (room.getPlayers().isEmpty()) {
            rooms.remove(roomId);
            ScheduledExecutorService scheduler = roomSchedulers.remove(roomId);
            if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdown();
        }
        return removed;
    }

    /**
     * Allows a player to select a color in the waiting room.
     *
     * @param roomId   the room ID
     * @param playerId the player ID
     * @param color    the color to select
     * @return true if selection was successful, false otherwise
     */
    public boolean selectColor(String roomId, String playerId, ColorStatus color) {
        WaitingRoom room = rooms.get(roomId);
        if (room == null) return false;
        return room.selectColor(playerId, color);
    }

    /**
     * Retrieves the current state of a waiting room.
     *
     * @param roomId the room ID
     * @return the waiting room state, or null if the room does not exist
     */
    public WaitingRoomState getRoomState(String roomId) {
        WaitingRoom room = rooms.get(roomId);
        if (room == null) return null;
        return new WaitingRoomState(
                room.getRoomId(),
                room.getPlayers(),
                room.getPlayerColors(),
                room.isFull(),
                room.getSeconds()
        );
    }

    /**
     * Removes a waiting room immediately.
     *
     * @param roomId the room ID
     */
    public void removeRoom(String roomId) {
        rooms.remove(roomId);
        ScheduledExecutorService scheduler = roomSchedulers.remove(roomId);
        if (scheduler != null && !scheduler.isShutdown()) scheduler.shutdown();
    }

}
