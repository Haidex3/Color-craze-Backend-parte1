package com.colorcraze.board.dtos.responses;

import com.colorcraze.utils.enums.ColorStatus;

/**
 * Response DTO representing an update to a platform on the game board.
 * Contains the platform's position and its current color status.
 */
public record PlatformUpdate(
    int row, 
    int col, 
    ColorStatus color) 
    {}