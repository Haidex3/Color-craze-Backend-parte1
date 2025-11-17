package com.colorcraze.waitingroom.models;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;

import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;

@Getter
public class WaitingRoom {

    private final String roomId;
    private final Map<String, ColorStatus> playerColors;
    private final Set<String> players;
    private final int maxPlayers;
    
    private int seconds;
    private ScheduledExecutorService scheduler;

    public WaitingRoom(String roomId, int seconds) {
        this.roomId = roomId;
        this.maxPlayers = 4;
        this.players = new CopyOnWriteArraySet<>();
        this.playerColors = new ConcurrentHashMap<>();
        this.seconds = seconds;
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
                isFull(),
                seconds
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

    public void startCountdown() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (this) {
                if (seconds > 0) {
                    seconds--;
                    System.out.println("Sala " + roomId + " tiempo restante: " + seconds + "s");
                } else {
                    System.out.println("Sala " + roomId + " terminó el tiempo de espera");
                    stopCountdown();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopCountdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }
}
