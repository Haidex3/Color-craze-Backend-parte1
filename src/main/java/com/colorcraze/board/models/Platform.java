package com.colorcraze.board.models;

import com.colorcraze.utils.enums.ColorStatus;

/**
 * Represents a platform on the game board.
 * Inherits from {@link Box} and holds the platform's color status.
 */
public class Platform extends Box{

    /**
     * Constructs a new Platform with the specified color.
     *
     * @param color the color of the platform
     */
    public Platform(ColorStatus color) {
        super(color);
    }
    
    public Platform() {
        super(ColorStatus.WHITE);
    }
}