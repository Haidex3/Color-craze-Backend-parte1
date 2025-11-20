package com.colorcraze.waitingroom.dtos.requests;

import com.colorcraze.utils.enums.ColorStatus;

import lombok.Data;


/**
 * Data Transfer Object representing a player's color selection in the waiting room.
 * 
 * This object is sent via WebSocket when a player selects a color.
 */
@Data
public class SelectColorMessage {
    private String playerId;
    private ColorStatus color;
}
