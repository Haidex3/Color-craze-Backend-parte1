package com.colorcraze.waitingroom.services;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.colorcraze.board.models.Board;
import com.colorcraze.board.services.BoardService;
import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.models.WaitingRoom;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class WaitingRoomService {

    private final Map<String, WaitingRoom> rooms = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final BoardService boardService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ROOM_ID_LENGTH = 4;
    private static final int DEFAULT_WAIT_SECONDS = 10;
    private final SecureRandom random = new SecureRandom();

    public WaitingRoomService(SimpMessagingTemplate messagingTemplate, BoardService boardService) {
        this.messagingTemplate = messagingTemplate;
        this.boardService = boardService;
    }

    private String generateRoomId() {
        StringBuilder sb = new StringBuilder(ROOM_ID_LENGTH);
        for (int i = 0; i < ROOM_ID_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

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

    private void startCountdown(WaitingRoom room) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (room) {
                if (room.getSeconds() > 0) {
                    room.setSeconds(room.getSeconds() - 1);

                    WaitingRoomState state = new WaitingRoomState(
                            room.getRoomId(),
                            room.getPlayers(),
                            room.getPlayerColors(),
                            room.isFull(),
                            room.getSeconds()
                    );
                    messagingTemplate.convertAndSend("/topic/waiting-room/" + room.getRoomId(), state);

                } else {
                    scheduler.shutdown();
                    try {
                        Map<String, ColorStatus> playerColors = room.getPlayerColors();
                        if (!playerColors.isEmpty()) {
                            Board board = boardService.createBoardWithPlayers(room.getRoomId(), playerColors);

                            messagingTemplate.convertAndSend("/topic/waiting-room/" + room.getRoomId() + "/start", board);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    rooms.remove(room.getRoomId());
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public Optional<WaitingRoom> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public WaitingRoomState joinRoom(String roomId, String playerId) {
        WaitingRoom room = rooms.get(roomId);
        if (room == null) return null;
        return room.addPlayer(playerId);
    }

    public boolean leaveRoom(String roomId, String playerId) {
        WaitingRoom room = rooms.get(roomId);
        if (room == null) return false;
        boolean removed = room.removePlayer(playerId);
        if (room.getPlayers().isEmpty()) {
            rooms.remove(roomId);
        }
        return removed;
    }

    public boolean selectColor(String roomId, String playerId, ColorStatus color) {
        WaitingRoom room = rooms.get(roomId);
        if (room == null) return false;
        return room.selectColor(playerId, color);
    }

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

    public void removeRoom(String roomId) {
        rooms.remove(roomId);
    }

}
