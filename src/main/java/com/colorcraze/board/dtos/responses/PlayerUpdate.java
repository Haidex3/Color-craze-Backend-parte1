package com.colorcraze.board.dtos.responses;

import java.util.UUID;

import com.colorcraze.utils.enums.ColorStatus;


/**
 * Response DTO representing an update to a player on the game board.
 * Contains the player's ID, current color status, and updated score.
 */
public record PlayerUpdate(
    UUID playerId,
    ColorStatus color,
    int newScore
) {}
