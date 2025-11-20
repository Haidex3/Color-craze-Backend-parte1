package com.colorcraze.waitingroom.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Set;

import com.colorcraze.utils.enums.ColorStatus;

/**
 * Data Transfer Object representing the current state of a waiting room.
 * 
 * Sent to clients to inform about players in the room, their selected colors,
 * whether the room is full, and (optionally) the remaining time.
 */
@Data
@AllArgsConstructor
public class WaitingRoomState {
    private String roomId;
    private Set<String> players;
    private Map<String, ColorStatus> playerColors;
    private boolean isFull;
    private int seconds;
}
