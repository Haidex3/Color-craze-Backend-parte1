package com.Color_craze.board.models;

import java.util.HashSet;
import java.util.Set;

import com.Color_craze.utils.enums.ColorStatus;
import com.Color_craze.utils.enums.PlayerMove;

public class Board {

    private Box[][] grid;
    private Player[] players;

    public static final int ROWS = 15;
    public static final int COLS = 31;

    public Board(Player[] players) {
        grid = new Box[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                grid[i][j] = new Box(ColorStatus.WHITE);
            }
        }

        Set<Position> platforms = generatePlatforms(ROWS, COLS);
        for (Position pos : platforms) {
            grid[pos.row()][pos.col()] = new Platform(ColorStatus.WHITE);
        }

        this.players = players;

        for (Player player : players) {
            int row = player.getRow();
            int col = player.getCol();

            if (row < 0 || row >= ROWS || col < 0 || col >= COLS) {
                throw new IllegalArgumentException(
                    "Jugador fuera de los límites del tablero: (" + row + "," + col + ")"
                );
            }

            grid[row][col] = player;
        }
    }


    public Box getBlock(int row, int col) {
        return grid[row][col];
    }

    public void setBlock(int row, int col, Box block) {
        grid[row][col] = block;
    }

    private Set<Position> generatePlatforms(int rows, int cols) {
        Set<Position> platforms = new HashSet<>();

        platforms.add(new Position(9, 1));
        platforms.add(new Position(9, 29));
        platforms.add(new Position(10, 15));
        platforms.add(new Position(11, 16));
        platforms.add(new Position(11, 14));
        platforms.add(new Position(12, 13));
        platforms.add(new Position(12, 17));

        for (int i = 0; i < cols; i++) {
            platforms.add(new Position(0, i));
            platforms.add(new Position(rows - 1, i));

            if (i >= 8 && i <= 22) platforms.add(new Position(3, i));
            if ((i >= 3 && i < 6) || (i > 24 && i <= 27)) platforms.add(new Position(6, i));
            if ((i >= 6 && i <= 9) || (i >= 21 && i <= 24)) platforms.add(new Position(7, i));
            if ((i >= 4 && i <= 8) || (i >= 22 && i <= 26)) platforms.add(new Position(11, i));
        }

        for (int i = 0; i < rows; i++) {
            if (i != 9) {
                platforms.add(new Position(i, 0));
                platforms.add(new Position(i, cols - 1));
            }
        }

        return platforms;
    }

    public Board movePlayer(Player player, PlayerMove playerMove) {
        int currentRow = player.getRow();
        int currentCol = player.getCol();
        int newRow = currentRow;
        int newCol = currentCol;
        switch (playerMove) {
            case RIGHT -> newCol++;
            case LEFT  -> newCol--;
            case UP    -> newRow--;
            case DOWN  -> newRow++;
        }
        if (newRow < 0 || newRow >= ROWS || newCol < 0 || newCol >= COLS) {
            return this;
        }
        Box destination = grid[newRow][newCol];
        if (destination instanceof Platform) {
            return this; 
        }
        grid[currentRow][currentCol] = new Box(ColorStatus.WHITE);
        player.setRow(newRow);
        player.setCol(newCol);
        grid[newRow][newCol] = player;
        updateAdjacentPlatforms(newRow, newCol, player.getColor());

        return this;
    }

    private void updateAdjacentPlatforms(int row, int col, ColorStatus playerColor) {
        int[][] directions = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
        };

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];

            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                Box box = grid[r][c];
                if (box instanceof Platform platform) {
                    platform.setColor(playerColor);
                }
            }
        }
    }



    private record Position(int row, int col) {}

}