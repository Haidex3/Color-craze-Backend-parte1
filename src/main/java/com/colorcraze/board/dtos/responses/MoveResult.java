package com.colorcraze.board.dtos.responses;

import java.util.List;
import java.util.UUID;

public record MoveResult(
    UUID playerId,
    int newRow,
    int newCol,
    List<PlatformUpdate> platforms,
    List<PlayerUpdate> affectedPlayers,
    boolean success,
    boolean gravity
) {}
