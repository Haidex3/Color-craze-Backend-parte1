package com.Color_craze.board.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.Color_craze.board.dtos.Responses.MoveResult;
import com.Color_craze.board.dtos.Responses.PlatformUpdate;
import com.Color_craze.board.dtos.Responses.PlayerUpdate;
import com.Color_craze.users.controllers.prueba;
import com.Color_craze.utils.enums.ColorStatus;
import com.Color_craze.utils.enums.PlayerMove;

public class Board {

    private static final int ROWS = 15;
    private static final int COLS = 31;

    private final Box[][] grid;
    private final Map<UUID, Player> players;

    private final Object gridLock = new Object();
    private final List<UUID> lockQueue = new ArrayList<>();

    private final String gameId;

    public Board(String gameId, Map<String, ColorStatus> playerColors) {
        this.gameId = gameId;
        this.grid = new Box[ROWS][COLS];
        this.players = new HashMap<>();
        initBoard();
        addPlayersToBoard(playerColors);
    }

    private void addPlayersToBoard(Map<String, ColorStatus> playerColors) {
        if (playerColors == null) return;

        for (Map.Entry<String, ColorStatus> entry : playerColors.entrySet()) {
            UUID playerId = UUID.fromString(entry.getKey()); 
            ColorStatus color = entry.getValue();
            Player player = new Player(playerId, color);
            addPlayer(player);
        }
    }

    private void initBoard() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = new Box(ColorStatus.GREEN);
            }
        }

        List<Position> platforms = generatePlatforms(ROWS, COLS);
        for (Position pos : platforms) {
            grid[pos.getRow()][pos.getCol()] = new Platform(ColorStatus.WHITE);
        }
    }

    private List<Position> generatePlatforms(int rows, int cols) {
        List<Position> platforms = new ArrayList<>();

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

    private static class Position {
        private final int row;
        private final int col;

        public Position(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public int getRow() { return row; }
        public int getCol() { return col; }
    }

    public String getGameId() {
        return gameId;
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
            if(grid[newRow+1][newCol] instanceof Platform) {
                System.out.println("No gravity applied for player " + playerId+ newRow + "," + newCol);
                return new MoveResult(playerId, currentRow, currentCol, List.of(), List.of(), false, false);
            }
            return new MoveResult(playerId, currentRow, currentCol, List.of(), List.of(), false, true);
        }

        Box destination = grid[newRow][newCol];
        if (destination instanceof Platform || destination instanceof Player) {
            if(grid[newRow+1][newCol] instanceof Platform) {
                System.out.println("No gravity applied for player " + playerId+ newRow + "," + newCol);
                return new MoveResult(playerId, currentRow, currentCol, List.of(), List.of(), false, false);
            }
            return new MoveResult(playerId, currentRow, currentCol, List.of(), List.of(), false, true);
        }

        synchronized (getGridLock(playerId)) {
            while (!canAcquireLock(playerId)) {
                try {
                    getGridLock(playerId).wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if(grid[newRow+1][newCol] instanceof Platform) {
                        System.out.println("No gravity applied for player " + playerId+ newRow + "," + newCol);
                        return new MoveResult(playerId, currentRow, currentCol, List.of(), List.of(), false, false);
                    }
                    return new MoveResult(playerId, currentRow, currentCol, List.of(), List.of(), false, true);
                }
            }

            try {
                grid[currentRow][currentCol] = new Box(ColorStatus.WHITE);
                player.setRow(newRow);
                player.setCol(newCol);
                grid[newRow][newCol] = player;

                List<PlayerUpdate> affectedPlayers = new ArrayList<>();
                List<PlatformUpdate> updatedPlatforms = updateAdjacentPlatforms(newRow, newCol, player.getColor(), affectedPlayers);
                if(grid[newRow+1][newCol] instanceof Platform) {
                    System.out.println("No gravity applied for player " + playerId+ newRow + "," + newCol);
                    return new MoveResult(playerId, newRow, newCol, updatedPlatforms, affectedPlayers, true, false);
                }
                return new MoveResult(playerId, newRow, newCol, updatedPlatforms, affectedPlayers, true, true);
            } finally {
                releaseLock(playerId);
                getGridLock(playerId).notifyAll();
            }
        }
    }


    private List<PlatformUpdate> updateAdjacentPlatforms(int row, int col, ColorStatus playerColor, List<PlayerUpdate> affectedPlayers) {
        int[][] directions = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
        };

        List<PlatformUpdate> updates = new ArrayList<>();

        for (int[] dir : directions) {
            int r = row + dir[0];
            int c = col + dir[1];

            if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
                Box box = grid[r][c];
                if (box instanceof Platform platform) {
                    updatePlatformAndScores(platform, playerColor, affectedPlayers);
                    updates.add(new PlatformUpdate(r, c, playerColor));
                }
            }
        }

        return updates;
    }


    private void updatePlatformAndScores(Platform platform, ColorStatus newColor, List<PlayerUpdate> affectedPlayers) {
        ColorStatus previousColor = platform.getColor();

        if (previousColor == newColor) {
            return;
        }

        Player paintingPlayer = findPlayerByColor(newColor);
        if (paintingPlayer != null) {
            paintingPlayer.setScore(paintingPlayer.getScore() + 1);
            affectedPlayers.add(new PlayerUpdate(paintingPlayer.getId(), paintingPlayer.getColor(), paintingPlayer.getScore()));
        }

        if (previousColor != ColorStatus.WHITE && previousColor != newColor) {
            Player previousPlayer = findPlayerByColor(previousColor);
            if (previousPlayer != null && previousPlayer.getScore() > 0) {
                previousPlayer.setScore(previousPlayer.getScore() - 1);
                affectedPlayers.add(new PlayerUpdate(previousPlayer.getId(), previousPlayer.getColor(), previousPlayer.getScore()));
            }
        }

        platform.setColor(newColor);
    }


    private Player findPlayerByColor(ColorStatus color) {
        return players.values().stream()
                .filter(p -> p.getColor() == color)
                .findFirst()
                .orElse(null);
    }


    public Box[][] getGrid() {
        return grid;
    }

    public Map<UUID, Player> getPlayers() {
        return players;
    }

    public void setPlayerIsUp(UUID uuid, boolean b) {
        players.get(uuid).setUp(b);
    }

    public boolean isPlayerUp(UUID uuid) {
        return players.get(uuid).isUp();
    }
    
    //Herramientas para el bloqueo 

    private Object getGridLock(UUID playerId) {
        synchronized (gridLock) {
            if (!lockQueue.contains(playerId)) {
                lockQueue.add(playerId);
                lockQueue.sort(UUID::compareTo);
            }
            return gridLock;
        }
    }

    private boolean canAcquireLock(UUID playerId) {
        synchronized (gridLock) {
            return !lockQueue.isEmpty() && lockQueue.get(0).equals(playerId);
        }
    }

    private void releaseLock(UUID playerId) {
        synchronized (gridLock) {
            lockQueue.remove(playerId);
        }
    }


    public Box getRowDownPLayer(String playerId){
        UUID uuid = UUID.fromString(playerId);
        Player player = players.get(uuid);
        int rowBelow = player.getRow() + 1;
        if (rowBelow >= this.grid.length) return null;
        return this.grid[rowBelow][player.getCol()];
    }
}