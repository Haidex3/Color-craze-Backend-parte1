package com.colorcraze.board.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.colorcraze.board.dtos.responses.MoveResult;
import com.colorcraze.board.dtos.responses.PlatformUpdate;
import com.colorcraze.board.dtos.responses.PlayerUpdate;
import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.utils.enums.PlayerMove;


/**
 * Represents the game board for Color Craze.
 * 
 * Handles player positions, platform updates, movements, scoring, and teleport mechanics.
 * The board has a fixed number of rows and columns and supports multiple players moving simultaneously.
 */
public class Board {

    private static final int ROWS = 15;
    private static final int COLS = 31;

    private final Box[][] grid;
    private final Map<UUID, Player> players;

    private final Object gridLock = new Object();
    private final List<UUID> lockQueue = new ArrayList<>();

    private final String gameId;
    

    public Board() {
        this.grid = new Box[ROWS][COLS];
        this.players = new HashMap<>();
        this.gameId = null;
    }


    /**
     * Creates a new Board instance with the specified game ID and initial player colors.
     * 
     * @param gameId      Unique identifier for the game.
     * @param playerColors Map of player UUIDs as strings to their ColorStatus.
     */
    public Board(String gameId, Map<String, ColorStatus> playerColors) {
        this.gameId = gameId;
        this.grid = new Box[ROWS][COLS];
        this.players = new HashMap<>();
        initBoard();
        addPlayersToBoard(playerColors);
    }

    /**
     * Adds players to the board from a map of UUIDs and colors.
     * 
     * @param playerColors Map of player UUIDs as strings to their ColorStatus.
     */
    private void addPlayersToBoard(Map<String, ColorStatus> playerColors) {
        if (playerColors == null) return;

        for (Map.Entry<String, ColorStatus> entry : playerColors.entrySet()) {
            UUID playerId = UUID.fromString(entry.getKey());
            ColorStatus color = entry.getValue();
            Player player = new Player(playerId, color);
            addPlayer(player);
        }
    }

    /**
     * Initializes the board with default boxes and platforms.
     * Teleport platforms are placed at specific coordinates.
     */
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
        grid[7][24] = new TpPlatform(2, 15);
        grid[11][26] = new TpPlatform(6, 15);
        grid[14][29] = new TpPlatform(10, 15);

    }

    /**
     * Generates the positions of all platforms on the board.
     * 
     * @param rows Number of rows in the board.
     * @param cols Number of columns in the board.
     * @return List of positions where platforms are located.
     */
    private List<Position> generatePlatforms(int rows, int cols) {
        List<Position> platforms = new ArrayList<>();

        for (int i = 0; i < cols; i++) {
            platforms.add(new Position(0, i));
            platforms.add(new Position(rows - 1, i));

            if (i >= 8 && i <= 22) platforms.add(new Position(3, i));
            if (i >= 6 && i < 24) platforms.add(new Position(7, i));
            if (i >= 4 && i <= 25) platforms.add(new Position(11, i));
        }

        for (int i = 0; i < rows; i++) {
            if (i != 9) {
                platforms.add(new Position(i, 0));
                platforms.add(new Position(i, cols - 1));
            }
        }

        return platforms;
    }

    /**
     * Helper class representing a row and column position.
     */
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

    /**
     * Returns the unique identifier of the game.
     *
     * @return String representing the game ID.
     */
    public String getGameId() {
        return gameId;
    }

    /**
     * Adds a player to the board at their starting position.
     * 
     * @param player The player to add.
     */
    public void addPlayer(Player player) {
        players.put(player.getId(), player);
        grid[player.getRow()][player.getCol()] = player;
    }

    /**
     * Removes a player from the board and replaces their position with a white box.
     * 
     * @param playerId UUID of the player to remove.
     */
    public void removePlayer(UUID playerId) {
        Player player = players.remove(playerId);
        if (player != null) {
            grid[player.getRow()][player.getCol()] = new Box(ColorStatus.WHITE);
        }
    }

    /**
     * Moves a player in the specified direction.
     * Handles collisions, platforms, teleporters, and updates affected players and platforms.
     * Synchronizes moves to prevent concurrent modification issues.
     * 
     * @param playerId   UUID of the player to move.
     * @param playerMove Direction to move the player.
     * @return MoveResult containing updated positions, platform changes, and player statuses.
     */
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

        Box destination = grid[newRow][newCol];
        if (destination instanceof Platform || destination instanceof Player || destination instanceof TpPlatform || (playerMove!=PlayerMove.UP && player.isUp())) {
            return moveDownPlatform(playerId, currentRow, currentCol, List.of(), List.of());
        }

        synchronized (gridLock) {
            ensureInQueue(playerId);

            while (!isFirstInQueue(playerId)) {
                try {
                    gridLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    removeFromQueue(playerId);
                    gridLock.notifyAll();
                    return moveDownPlatform(playerId, currentRow, currentCol, List.of(), List.of());
                }
            }

            try {
                grid[currentRow][currentCol] = new Box(ColorStatus.WHITE);
                player.setRow(newRow);
                player.setCol(newCol);
                grid[newRow][newCol] = player;

                List<PlayerUpdate> affectedPlayers = new ArrayList<>();
                List<PlatformUpdate> updatedPlatforms = updateAdjacentPlatforms(newRow, newCol, player.getColor(), affectedPlayers);
                return moveDownPlatform(playerId, newRow, newCol, updatedPlatforms, affectedPlayers);
            } finally {
                removeFromQueue(playerId);
                gridLock.notifyAll();
            }
        }
    }

    /**
     * Handles downward movement for platforms and teleporters.
     * Called internally by movePlayer.
     * 
     * @param playerId         UUID of the player.
     * @param currentRow       Current row of the player.
     * @param currentCol       Current column of the player.
     * @param affectedPlatforms List of platform updates to include.
     * @param affectedPlayers   List of player updates to include.
     * @return MoveResult representing the new state of the player and affected elements.
     */
    private MoveResult moveDownPlatform(UUID playerId, int currentRow, int currentCol, List<PlatformUpdate> affectedPlatforms, List<PlayerUpdate> affectedPlayers){
        Player p = players.get(playerId);
        Box below = grid[currentRow + 1][currentCol];
        if (below instanceof TpPlatform tp) {
            Player player = players.get(playerId);

            int newRow = tp.getNewRow();
            int newCol = tp.getNewCol();

            grid[currentRow][currentCol] = new Box(ColorStatus.WHITE);
            player.setRow(newRow);
            player.setCol(newCol);
            grid[newRow][newCol] = player;

            List<PlatformUpdate> updatedPlatforms = updateAdjacentPlatforms(newRow, newCol, player.getColor(), affectedPlayers);
            return new MoveResult(playerId, p.getRow(), p.getCol(), updatedPlatforms, affectedPlayers, true, false);
        }

        if (below instanceof Platform || below instanceof Player) {
            return new MoveResult(playerId, p.getRow(), p.getCol(), affectedPlatforms, affectedPlayers, false, false);
        }
        return new MoveResult(playerId, p.getRow(), p.getCol(), affectedPlatforms, affectedPlayers, false, true);
    }


    /**
     * Updates adjacent platforms' colors and adjusts player scores accordingly.
     * 
     * @param row             Row of the player.
     * @param col             Column of the player.
     * @param playerColor     Color of the moving player.
     * @param affectedPlayers List of players whose scores have changed.
     * @return List of PlatformUpdate objects for the affected platforms.
     */
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

    /**
     * Updates the color of a platform and adjusts the scores of affected players.
     * 
     * @param platform        Platform to update.
     * @param newColor        New color applied by the player.
     * @param affectedPlayers List of players whose scores have changed.
     */
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

        if (previousColor != ColorStatus.WHITE) {
            Player previousPlayer = findPlayerByColor(previousColor);
            if (previousPlayer != null && previousPlayer.getScore() > 0) {
                previousPlayer.setScore(previousPlayer.getScore() - 1);
                affectedPlayers.add(new PlayerUpdate(previousPlayer.getId(), previousPlayer.getColor(), previousPlayer.getScore()));
            }
        }

        platform.setColor(newColor);
    }

    /**
     * Finds a player on the board by their color.
     * 
     * @param color ColorStatus to search for.
     * @return Player instance if found, otherwise null.
     */
    private Player findPlayerByColor(ColorStatus color) {
        return players.values().stream()
                .filter(p -> p.getColor() == color)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the entire board grid.
     * 
     * @return 2D array of Box objects representing the grid.
     */
    public Box[][] getGrid() {
        return grid;
    }

    /**
     * Returns all players currently on the board.
     * 
     * @return Map of UUID to Player.
     */
    public Map<UUID, Player> getPlayers() {
        return players;
    }

    /**
     * Sets the "up" state of a player.
     * 
     * @param uuid UUID of the player.
     * @param up   True to set player as up, false otherwise.
     */
    public void setPlayerIsUp(UUID uuid, boolean b) {
        players.get(uuid).setUp(b);
    }

    /**
     * Checks if a player is in "up" state.
     * 
     * @param uuid UUID of the player.
     * @return True if the player is up, false otherwise.
     */
    public boolean isPlayerUp(UUID uuid) {
        return players.get(uuid).isUp();
    }

    /**
     * Ensures a player is in the lock queue for synchronized movement.
     * 
     * @param playerId UUID of the player.
     */
    private void ensureInQueue(UUID playerId) {
        if (!lockQueue.contains(playerId)) {
            lockQueue.add(playerId);
            lockQueue.sort(UUID::compareTo);
        }
    }

    /**
     * Checks if a player is the first in the movement queue.
     * 
     * @param playerId UUID of the player.
     * @return True if first in queue, false otherwise.
     */
    private boolean isFirstInQueue(UUID playerId) {
        return !lockQueue.isEmpty() && lockQueue.get(0).equals(playerId);
    }

    /**
     * Removes a player from the movement queue.
     * 
     * @param playerId UUID of the player.
     */
    private void removeFromQueue(UUID playerId) {
        lockQueue.remove(playerId);
    }

    /**
     * Returns the Box immediately below a player.
     * 
     * @param playerId UUID of the player as string.
     * @return Box below the player, or null if out of bounds.
     */
    public Box getRowDownPLayer(String playerId){
        UUID uuid = UUID.fromString(playerId);
        Player player = players.get(uuid);
        if (player == null) return null;
        int rowBelow = player.getRow() + 1;
        if (rowBelow >= this.grid.length) return null;
        return this.grid[rowBelow][player.getCol()];
    }

}
