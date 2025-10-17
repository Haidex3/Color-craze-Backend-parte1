package com.Color_craze.WaitingRoom.services;

import com.Color_craze.WaitingRoom.dtos.Responses.WaitingRoomState;
import com.Color_craze.WaitingRoom.models.WaitingRoom;
import com.Color_craze.utils.enums.ColorStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WaitingRoomService {

    private final Map<String, WaitingRoom> rooms = new ConcurrentHashMap<>();

    public WaitingRoom createRoom() {
        String roomId = UUID.randomUUID().toString();
        WaitingRoom room = new WaitingRoom(roomId);
        rooms.put(roomId, room);
        return room;
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
                room.isFull()
        );
    }

    public void removeRoom(String roomId) {
        rooms.remove(roomId);
    }

}
