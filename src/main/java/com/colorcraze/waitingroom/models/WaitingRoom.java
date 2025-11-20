package com.colorcraze.waitingroom.models;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;

import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;

/**
 * Represents a waiting room where players join before the game starts.
 * 
 * Manages players, their selected colors, maximum capacity, and countdown timer.
 * Thread-safe operations are ensured for adding/removing players and color selection.
 */
@Getter
public class WaitingRoom {

    private final String roomId;
    private final Map<String, ColorStatus> playerColors;
    private final Set<String> players;
    private final int maxPlayers;
    private final Object lock = new Object(); 
    
    private int seconds;
    private ScheduledExecutorService scheduler;

    /**
     * Constructs a new waiting room with a given room ID and initial countdown seconds.
     * 
     * @param roomId  Unique identifier of the waiting room.
     * @param seconds Initial countdown time in seconds.
     */
    public WaitingRoom(String roomId, int seconds) {
        this.roomId = roomId;
        this.maxPlayers = 4;
        this.players = new CopyOnWriteArraySet<>();
        this.playerColors = new ConcurrentHashMap<>();
        this.seconds = seconds;
    }

    /**
     * Returns a list of available colors that are not WHITE and not already selected.
     * 
     * @return List of available ColorStatus for new players.
     */
    private List<ColorStatus> getAvailableColors() {
        List<ColorStatus> available = new ArrayList<>();
        for (ColorStatus color : ColorStatus.values()) {
            if (color != ColorStatus.WHITE && !playerColors.containsValue(color)) {
                available.add(color);
            }
        }
        return available;
    }

    /**
     * Adds a player to the waiting room and assigns an available color.
     * 
     * @param playerId ID of the player to add.
     * @return WaitingRoomState reflecting the updated room state, or null if full.
     */
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

    /**
     * Removes a player from the waiting room.
     * 
     * @param playerId ID of the player to remove.
     * @return true if the player was successfully removed, false otherwise.
     */
    public synchronized boolean removePlayer(String playerId) {
        playerColors.remove(playerId);
        return players.remove(playerId);
    }

    /**
     * Attempts to set a player's selected color.
     * 
     * @param playerId ID of the player selecting a color.
     * @param color    ColorStatus the player wants to select.
     * @return true if selection was successful, false if invalid or already taken.
     */
    public synchronized boolean selectColor(String playerId, ColorStatus color) {
        if (!players.contains(playerId)) return false;
        if (color == ColorStatus.WHITE) return false;
        if (playerColors.containsValue(color)) return false;
        playerColors.put(playerId, color);
        return true;
    }

    /**
     * Checks if the waiting room has reached maximum capacity.
     * 
     * @return true if full, false otherwise.
     */
    public synchronized boolean isFull() {
        return players.size() >= maxPlayers;
    }

    /**
     * Starts a countdown timer for the waiting room.
     * Decrements seconds every second until reaching zero.
     */
    public void startCountdown() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (this) {
                if (seconds > 0) {
                    seconds--;
                } else {
                    stopCountdown();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Stops the countdown timer if running.
     */
    public void stopCountdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * Returns the remaining seconds in the countdown.
     * 
     * @return Remaining countdown seconds.
     */
    public int getSeconds() {
        return seconds;
    }

    /**
     * Sets the remaining seconds in the countdown.
     * 
     * @param seconds New countdown value in seconds.
     */
    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }
}
