package com.Color_craze.board.models;

public class TpPlatform extends Box{

    private int newCol, newRow;

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
