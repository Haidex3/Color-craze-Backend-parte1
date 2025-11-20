package com.colorcraze.waitingroom.dtos.responses;

import com.colorcraze.utils.enums.ColorStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object representing the result of a player's color selection.
 * 
 * Sent as a response to a player after attempting to select a color in the waiting room.
 */
@Data
@AllArgsConstructor
public class PlayerColorState {
    private String playerId;
    private ColorStatus color;
    private boolean success;
}
