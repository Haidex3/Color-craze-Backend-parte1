package com.colorcraze.board.models;

/**
 * Represents a teleport platform on the game board.
 * Inherits from {@link Box} and contains target coordinates for teleportation.
 */
public class TpPlatform extends Box{

    private int newCol;
    private int newRow;

    /**
     * Constructs a new TpPlatform with the specified target position.
     *
     * @param newRow the target row for teleportation
     * @param newCol the target column for teleportation
     */
    public TpPlatform(int newRow, int newCol) {
        super(null);
        this.newCol = newCol;
        this.newRow = newRow; 
    }

    public int getNewCol() {
        return newCol;
    }

    public int getNewRow() {
        return newRow;
    }
}
