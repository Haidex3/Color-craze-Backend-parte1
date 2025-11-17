package com.colorcraze.board.dtos.requests;

import com.colorcraze.utils.enums.PlayerMove;

import lombok.Data;

@Data
public class PlayerMoveMessage {
    private String playerId;
    private PlayerMove direction;
    private String room;
}
