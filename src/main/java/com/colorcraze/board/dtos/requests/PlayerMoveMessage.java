package com.colorcraze.board.dtos.requests;

import com.colorcraze.utils.enums.PlayerMove;

import lombok.Data;

/**
 * DTO representing a player's move message sent via WebSocket.
 * Contains the player ID, move direction, and the associated room.
 */
@Data
public class PlayerMoveMessage {
    private String playerId;
    private PlayerMove direction;
    private String room;
}
