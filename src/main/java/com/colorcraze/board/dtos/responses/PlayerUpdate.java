package com.colorcraze.board.dtos.responses;

import java.util.UUID;

import com.colorcraze.utils.enums.ColorStatus;

public record PlayerUpdate(
    UUID playerId,
    ColorStatus color,
    int newScore
) {}
