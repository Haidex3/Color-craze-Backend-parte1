package com.Color_craze.WaitingRoom.models;

import com.Color_craze.WaitingRoom.dtos.Responses.WaitingRoomState;
import com.Color_craze.utils.enums.ColorStatus;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
public class WaitingRoom {

    private final String roomId;
    private final Map<String, ColorStatus> playerColors;
    private final Set<String> players;
    private final int maxPlayers;

    public WaitingRoom(String roomId) {
        this.roomId = roomId;
        this.maxPlayers = 4;
        this.players = new CopyOnWriteArraySet<>();
        this.playerColors = new ConcurrentHashMap<>();
    }

    private List<ColorStatus> getAvailableColors() {
        List<ColorStatus> available = new ArrayList<>();
        for (ColorStatus color : ColorStatus.values()) {
            if (color != ColorStatus.WHITE && !playerColors.containsValue(color)) {
                available.add(color);
            }
        }
        return available;
    }

    public synchronized WaitingRoomState addPlayer(String playerId) {
        if (players.size() >= maxPlayers) return null; 

        if (players.add(playerId)) {
            List<ColorStatus> availableColors = getAvailableColors();
            if (!availableColors.isEmpty()) {
                playerColors.put(playerId, availableColors.get(0));
            } else {
                playerColors.put(playerId, ColorStatus.WHITE);
            }
            return new WaitingRoomState(
                roomId,
                Collections.unmodifiableSet(players),
                Collections.unmodifiableMap(playerColors),
                isFull()
            );
        }
        return null;
    }


    public synchronized boolean removePlayer(String playerId) {
        playerColors.remove(playerId);
        return players.remove(playerId);
    }

    public synchronized boolean selectColor(String playerId, ColorStatus color) {
        if (!players.contains(playerId)) return false;
        if (color == ColorStatus.WHITE) return false;
        if (playerColors.containsValue(color)) return false;
        playerColors.put(playerId, color);
        return true;
    }

    public synchronized boolean isFull() {
        return players.size() >= maxPlayers;
    }
}
