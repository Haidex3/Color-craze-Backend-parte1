package com.colorcraze.board.dtos.responses;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing the result of a player's move on the board.
 * Contains the player's new position, affected platforms and players, 
 * and flags indicating move success and gravity effects.
 */
public record MoveResult(
    UUID playerId,
    int newRow,
    int newCol,
    List<PlatformUpdate> platforms,
    List<PlayerUpdate> affectedPlayers,
    boolean success,
    boolean gravity
) {}
