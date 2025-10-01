package com.Color_craze.board.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.Color_craze.board.dtos.Responses.MoveResult;
import com.Color_craze.board.dtos.Responses.PlatformUpdate;
import com.Color_craze.utils.enums.ColorStatus;
import com.Color_craze.utils.enums.PlayerMove;

public class Board {

    private static final int ROWS = 15;
    private static final int COLS = 31;

    private final Box[][] grid;
    private final Map<UUID, Player> players;

    public Board() {
        this.grid = new Box[ROWS][COLS];
        this.players = new HashMap<>();
        initBoard();
    }

    private void initBoard() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = new Box(ColorStatus.WHITE);
            }
        }
    }

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
        grid[player.getRow()][player.getCol()] = player;
    }

    public void removePlayer(UUID playerId) {
        Player player = players.remove(playerId);
        if (player != null) {
            grid[player.getRow()][player.getCol()] = new Box(ColorStatus.WHITE);
        }
    }

    public MoveResult movePlayer(UUID playerId, PlayerMove playerMove) {
        Player player = players.get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }

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
            return new MoveResult(currentRow, currentCol, currentRow, currentCol, List.of());
        }

        Box destination = grid[newRow][newCol];
        if (destination instanceof Platform) {
            return new MoveResult(currentRow, currentCol, currentRow, currentCol, List.of());
        }

        grid[currentRow][currentCol] = new Box(ColorStatus.WHITE);
        player.setRow(newRow);
        player.setCol(newCol);
        grid[newRow][newCol] = player;

        List<PlatformUpdate> updatedPlatforms = updateAdjacentPlatforms(newRow, newCol, player.getColor());

        return new MoveResult(currentRow, currentCol, newRow, newCol, updatedPlatforms);
    }

    private List<PlatformUpdate> updateAdjacentPlatforms(int row, int col, ColorStatus playerColor) {
        int[][] directions = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
        };

        List<PlatformUpdate> updates = new ArrayList<>();

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];

            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                Box box = grid[r][c];
                if (box instanceof Platform platform) {
                    platform.setColor(playerColor);
                    updates.add(new PlatformUpdate(r, c, playerColor));
                }
            }
        }

        return updates;
    }

    public Box[][] getGrid() {
        return grid;
    }

    public Map<UUID, Player> getPlayers() {
        return players;
    }
}