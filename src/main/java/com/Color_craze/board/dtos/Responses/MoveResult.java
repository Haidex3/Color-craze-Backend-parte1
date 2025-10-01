package com.Color_craze.board.dtos.Responses;

import java.util.List;

public record MoveResult(
    int oldRow,
    int oldCol,
    int newRow,
    int newCol,
    List<PlatformUpdate> platforms
) {}
