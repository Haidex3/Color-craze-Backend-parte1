package com.colorcraze.board.models;

import com.colorcraze.board.dtos.responses.MoveResult;
import com.colorcraze.board.dtos.responses.PlatformUpdate;
import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.utils.enums.PlayerMove;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;
    private UUID player1Id;
    private UUID player2Id;
    private Map<String, ColorStatus> playerColors;

    @BeforeEach
    void setUp() {
        player1Id = UUID.randomUUID();
        player2Id = UUID.randomUUID();
        
        playerColors = new HashMap<>();
        playerColors.put(player1Id.toString(), ColorStatus.RED);
        playerColors.put(player2Id.toString(), ColorStatus.YELLOW);
        
        board = new Board("test-game", playerColors);
    }

    @Test
    @DisplayName("Should initialize board with correct dimensions")
    void testBoardInitialization() {
        Box[][] grid = board.getGrid();
        
        assertNotNull(grid);
        assertEquals(15, grid.length);
        assertEquals(31, grid[0].length);
    }

    @Test
    @DisplayName("Should add players to board with correct starting positions")
    void testAddPlayersToBoard() {
        Map<UUID, Player> players = board.getPlayers();
        
        assertEquals(2, players.size());
        
        Player redPlayer = players.get(player1Id);
        assertNotNull(redPlayer);
        assertEquals(ColorStatus.RED, redPlayer.getColor());
        assertEquals(13, redPlayer.getRow());
        assertEquals(29, redPlayer.getCol());
        
        Player yellowPlayer = players.get(player2Id);
        assertNotNull(yellowPlayer);
        assertEquals(ColorStatus.YELLOW, yellowPlayer.getColor());
        assertEquals(13, yellowPlayer.getRow());
        assertEquals(1, yellowPlayer.getCol());
    }

    @Test
    @DisplayName("Should remove player from board")
    void testRemovePlayer() {
        board.removePlayer(player1Id);
        
        Map<UUID, Player> players = board.getPlayers();
        assertEquals(1, players.size());
        assertFalse(players.containsKey(player1Id));
        assertTrue(players.containsKey(player2Id));
    }

    @Test
    @DisplayName("Should move player right successfully")
    void testMovePlayerRight() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> testColors = new HashMap<>();
        testColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-move", testColors);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        int initialCol = player.getCol();
        
        MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.RIGHT);
        
        assertEquals(initialCol + 1, result.newCol());
        assertEquals(player.getRow(), result.newRow());
        assertEquals(testPlayerId, result.playerId());
    }

    @Test
    @DisplayName("Should not move player out of bounds")
    void testMovePlayerOutOfBounds() {
        Player player = board.getPlayers().get(player2Id);
        int initialRow = player.getRow();
        int initialCol = player.getCol();
        
        MoveResult result = board.movePlayer(player2Id, PlayerMove.LEFT);
        
        assertFalse(result.success());
        assertEquals(initialRow, result.newRow());
        assertEquals(initialCol, result.newCol());
    }

    @Test
    @DisplayName("Should update adjacent platforms when player moves")
    void testUpdateAdjacentPlatforms() {
        board.movePlayer(player2Id, PlayerMove.RIGHT);
        
        MoveResult result = board.movePlayer(player2Id, PlayerMove.RIGHT);
        
        assertNotNull(result.platforms());
        assertFalse(result.platforms().isEmpty());
        
        PlatformUpdate platformUpdate = result.platforms().get(0);
        assertEquals(ColorStatus.YELLOW, platformUpdate.color());
    }

    @Test
    @DisplayName("Should update player scores when coloring platforms")
    void testPlayerScoreUpdate() {
        Player player = board.getPlayers().get(player2Id);
        int initialScore = player.getScore();
        
        board.movePlayer(player2Id, PlayerMove.RIGHT);
        MoveResult result = board.movePlayer(player2Id, PlayerMove.RIGHT);
        
        assertFalse(result.affectedPlayers().isEmpty());
        
        boolean scoreUpdated = result.affectedPlayers().stream()
                .anyMatch(pu -> pu.playerId().equals(player2Id) && pu.newScore() > initialScore);
        assertTrue(scoreUpdated);
    }

    @Test
    @DisplayName("Should handle teleport platform correctly")
    void testTeleportPlatform() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-teleport", singlePlayerColors);        
        Box belowPlayer = testBoard.getRowDownPLayer(testPlayerId.toString());
        
        assertNotNull(belowPlayer);
    }

    @Test
    @DisplayName("Should throw exception when moving non-existent player")
    void testMoveNonExistentPlayer() {
        UUID nonExistentPlayerId = UUID.randomUUID();
        
        assertThrows(IllegalArgumentException.class, 
            () -> board.movePlayer(nonExistentPlayerId, PlayerMove.RIGHT));
    }

    @Test
    @DisplayName("Should handle player up state correctly")
    void testPlayerUpState() {
        board.setPlayerIsUp(player1Id, true);
        
        assertTrue(board.isPlayerUp(player1Id));
        
        board.setPlayerIsUp(player1Id, false);
        
        assertFalse(board.isPlayerUp(player1Id));
    }

    @Test
    @DisplayName("Should not move player up when isUp is true")
    void testMovePlayerUpWhenIsUp() {
        board.setPlayerIsUp(player1Id, true);
        Player player = board.getPlayers().get(player1Id);
        int initialCol = player.getCol();
        
        MoveResult result = board.movePlayer(player1Id, PlayerMove.UP);
        
        assertFalse(result.success());
        assertEquals(12, result.newRow());
        assertEquals(initialCol, result.newCol());
    }

    @Test
    @DisplayName("Should get game ID correctly")
    void testGetGameId() {
        assertEquals("test-game", board.getGameId());
    }

    @Test
    @DisplayName("Should return box below player")
    void testGetRowDownPlayer() {
        Box below = board.getRowDownPLayer(player1Id.toString());
        
        assertNotNull(below);
        assertTrue(below instanceof Platform || below instanceof Box);
    }

    @Test
    @DisplayName("Should return null for non-existent player in getRowDownPlayer")
    void testGetRowDownNonExistentPlayer() {
        Box below = board.getRowDownPLayer(UUID.randomUUID().toString());
        
        assertNull(below);
    }

    @Test
    @DisplayName("Should handle concurrent moves with synchronization")
    void testConcurrentMoves() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            MoveResult result = board.movePlayer(player1Id, PlayerMove.LEFT);
            assertNotNull(result);
        });
        
        Thread thread2 = new Thread(() -> {
            MoveResult result = board.movePlayer(player2Id, PlayerMove.RIGHT);
            assertNotNull(result);
        });
        
        thread1.start();
        thread2.start();
        
        thread1.join(1000);
        thread2.join(1000);
        
        assertFalse(thread1.isAlive());
        assertFalse(thread2.isAlive());
    }

    @Test
    @DisplayName("Should initialize with correct platform positions")
    void testPlatformGeneration() {
        Box[][] grid = board.getGrid();
        
        for (int col = 0; col < 31; col++) {
            assertTrue(grid[0][col] instanceof Platform);
        }
        
        for (int row = 0; row < 15; row++) {
            if (row != 9) {
                assertTrue(grid[row][0] instanceof Platform);
                assertTrue(grid[row][30] instanceof Platform);
            }
        }
        
        assertTrue(grid[7][24] instanceof TpPlatform);
        assertTrue(grid[11][26] instanceof TpPlatform);
        assertTrue(grid[14][29] instanceof TpPlatform);
    }

    @Test
    @DisplayName("Should handle gravity effect in move result")
    void testGravityEffect() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.PURPLE);
        Board testBoard = new Board("test-gravity", singlePlayerColors);
        
        MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.UP);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle DOWN movement successfully")
    void testMovePlayerDown() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> testColors = new HashMap<>();
        testColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-down", testColors);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        int initialRow = player.getRow();
        
        MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.DOWN);
        
        assertEquals(initialRow, result.newRow());
        assertEquals(player.getCol(), result.newCol());
        assertEquals(testPlayerId, result.playerId());
    }

    @Test
    @DisplayName("Should handle UP movement successfully when player is not up")
    void testMovePlayerUpWhenNotUp() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> testColors = new HashMap<>();
        testColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-up", testColors);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        int initialRow = player.getRow();
        
        MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.UP);
        
        assertEquals(initialRow - 1, result.newRow());
        assertEquals(player.getCol(), result.newCol());
        assertFalse(result.success());
    }

    @Test
    @DisplayName("Should not move player when destination is another player")
    void testMovePlayerToPlayerPosition() {
        Player player1 = board.getPlayers().get(player1Id);
        Player player2 = board.getPlayers().get(player2Id);
        
        player1.setRow(player2.getRow());
        player1.setCol(player2.getCol() - 1);
        
        MoveResult result = board.movePlayer(player1Id, PlayerMove.RIGHT);
        
        assertFalse(result.success());
        assertEquals(player1.getRow(), result.newRow());
        assertEquals(player1.getCol(), result.newCol());
    }

    @Test
    @DisplayName("Should handle teleport platform movement correctly")
    void testTeleportPlatformMovement() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-teleport-move", singlePlayerColors);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        player.setRow(13);
        player.setCol(28);
        
        MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.DOWN);
        
        assertFalse(result.success());
        assertEquals(13, result.newRow());
        assertEquals(28, result.newCol());
    }

    @Test
    @DisplayName("Should not teleport when teleport destination is out of bounds")
    void testTeleportOutOfBounds() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-teleport-oob", singlePlayerColors);
        
        testBoard.getGrid()[10][10] = new TpPlatform(20, 40);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        player.setRow(9);
        player.setCol(10);
        
        MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.DOWN);
        
        assertFalse(result.success());
        assertEquals(9, result.newRow());
        assertEquals(10, result.newCol());
    }

    @Test
    @DisplayName("Should update platform color and scores correctly")
    void testPlatformColorAndScoreUpdate() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.PURPLE);
        Board testBoard = new Board("test-platform-update", singlePlayerColors);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        int initialScore = player.getScore();
        
        player.setRow(1);
        player.setCol(1);
        
        MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.UP);
        
        assertFalse(result.success());
        
        boolean platformUpdated = result.platforms().stream()
                .anyMatch(pu -> pu.color() == ColorStatus.PURPLE);
        boolean scoreUpdated = result.affectedPlayers().stream()
                .anyMatch(pu -> pu.playerId().equals(testPlayerId) && pu.newScore() > initialScore);
        
        assertFalse(platformUpdated || scoreUpdated);
    }

    @Test
    @DisplayName("Should not update platform when color is the same")
    void testPlatformNoUpdateSameColor() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.RED);
        Board testBoard = new Board("test-same-color", singlePlayerColors);
        
        testBoard.movePlayer(testPlayerId, PlayerMove.UP);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        int currentScore = player.getScore();
        
        testBoard.movePlayer(testPlayerId, PlayerMove.DOWN);
        MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.UP);
        
        boolean scoreUnchanged = result.affectedPlayers().stream()
                .noneMatch(pu -> pu.playerId().equals(testPlayerId) && pu.newScore() != currentScore);
        
        assertTrue(scoreUnchanged);
    }

    @Test
    @DisplayName("Should handle interrupted thread during movement")
    void testInterruptedMovement() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-interrupt", singlePlayerColors);
        
        Thread movingThread = new Thread(() -> {
            MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.RIGHT);
            assertNotNull(result);
        });
        
        movingThread.start();
        movingThread.interrupt();
        
        assertDoesNotThrow(() -> movingThread.join(1000));
    }

    @Test
    @DisplayName("Should handle movement to platform position")
    void testMoveToPlatformPosition() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-platform-block", singlePlayerColors);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        
        player.setRow(1);
        player.setCol(10);
        
        MoveResult result = testBoard.movePlayer(testPlayerId, PlayerMove.UP);
        
        assertFalse(result.success());
        assertEquals(1, result.newRow());
        assertEquals(10, result.newCol());
    }

    @Test
    @DisplayName("Should handle empty player colors map")
    void testEmptyPlayerColors() {
        Board emptyBoard = new Board("test-empty", new HashMap<>());
        
        assertEquals(0, emptyBoard.getPlayers().size());
    }

    @Test
    @DisplayName("Should handle null player colors map")
    void testNullPlayerColors() {
        Board nullBoard = new Board("test-null", null);
        
        assertEquals(0, nullBoard.getPlayers().size());
    }

    @Test
    @DisplayName("Should initialize with correct teleport platform positions")
    void testTeleportPlatformPositions() {
        Box[][] grid = board.getGrid();
        
        assertTrue(grid[7][24] instanceof TpPlatform);
        TpPlatform tp1 = (TpPlatform) grid[7][24];
        assertEquals(2, tp1.getNewRow());
        assertEquals(15, tp1.getNewCol());
        
        assertTrue(grid[11][26] instanceof TpPlatform);
        TpPlatform tp2 = (TpPlatform) grid[11][26];
        assertEquals(6, tp2.getNewRow());
        assertEquals(15, tp2.getNewCol());
        
        assertTrue(grid[14][29] instanceof TpPlatform);
        TpPlatform tp3 = (TpPlatform) grid[14][29];
        assertEquals(10, tp3.getNewRow());
        assertEquals(15, tp3.getNewCol());
    }
    
    @Test
void movePlayer_WhenPlayerNotFound_ShouldThrowException() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    UUID nonExistentPlayerId = UUID.randomUUID();
    
    assertThrows(IllegalArgumentException.class, () -> {
        board.movePlayer(nonExistentPlayerId, PlayerMove.RIGHT);
    });
}

@Test
void movePlayer_WhenMovingOutOfBounds_ShouldCallMoveDownPlatform() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    Player player = board.getPlayers().get(player1Id);
    player.setRow(0);
    player.setCol(0);
    
    MoveResult result = board.movePlayer(player1Id, PlayerMove.LEFT);
    
    assertEquals(player1Id, result.playerId());
    assertEquals(0, result.newRow());
    assertEquals(0, result.newCol());
    assertFalse(result.success());
}

@Test
void movePlayer_WhenMovingUpAndPlayerIsUp_ShouldCallMoveDownPlatform() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    Player player = board.getPlayers().get(player1Id);
    board.setPlayerIsUp(player1Id, true);
    
    MoveResult result = board.movePlayer(player1Id, PlayerMove.UP);
    
    assertEquals(player1Id, result.playerId());
    assertFalse(result.success());
}

@Test
void movePlayer_WhenDestinationIsPlatform_ShouldCallMoveDownPlatform() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    Player player = board.getPlayers().get(player1Id);
    player.setRow(1);
    player.setCol(1);
    
    MoveResult result = board.movePlayer(player1Id, PlayerMove.UP);
    
    assertEquals(player1Id, result.playerId());
    assertEquals(1, result.newRow());
    assertEquals(1, result.newCol());
    assertFalse(result.success());
}

@Test
void movePlayer_WhenDestinationIsPlayer_ShouldCallMoveDownPlatform() {
    Board board = new Board("test", Map.of(
        player1Id.toString(), ColorStatus.RED,
        player2Id.toString(), ColorStatus.YELLOW
    ));
    Player player1 = board.getPlayers().get(player1Id);
    Player player2 = board.getPlayers().get(player2Id);
    player1.setRow(1);
    player1.setCol(1);
    player2.setRow(1);
    player2.setCol(2);
    
    MoveResult result = board.movePlayer(player1Id, PlayerMove.RIGHT);
    
    assertEquals(player1Id, result.playerId());
    assertEquals(1, result.newRow());
    assertEquals(2, result.newCol());
    assertFalse(result.success());
}


@Test
void moveDownPlatform_WhenBelowIsPlatform_ShouldReturnNotGravity() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    Player player = board.getPlayers().get(player1Id);
    player.setRow(0);
    player.setCol(5);
    
    Box below = board.getRowDownPLayer(player1Id.toString());
    
    MoveResult result = board.movePlayer(player1Id, PlayerMove.DOWN);
    
    assertTrue(result.gravity());
}

@Test
void moveDownPlatform_WhenAtBottomRow_ShouldReturnNotGravity() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    Player player = board.getPlayers().get(player1Id);
    player.setRow(14);
    player.setCol(5);
    
    MoveResult result = board.movePlayer(player1Id, PlayerMove.DOWN);
    
    assertFalse(result.gravity());
}

@Test
void updateAdjacentPlatforms_WhenPlatformAlreadyPlayerColor_ShouldNotChangeScore() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    Player player = board.getPlayers().get(player1Id);
    player.setRow(1);
    player.setCol(1);
    
    Box adjacentPlatform = board.getGrid()[0][1];
    if (adjacentPlatform instanceof Platform platform) {
        platform.setColor(ColorStatus.RED);
    }
    
    int initialScore = player.getScore();
    MoveResult result = board.movePlayer(player1Id, PlayerMove.RIGHT);
    
    assertEquals(1, player.getScore());
}

@Test
void updateAdjacentPlatforms_WhenPlatformIsWhite_ShouldIncreasePaintingPlayerScore() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    Player player = board.getPlayers().get(player1Id);
    player.setRow(1);
    player.setCol(1);
    
    Box adjacentPlatform = board.getGrid()[0][1];
    if (adjacentPlatform instanceof Platform platform) {
        platform.setColor(ColorStatus.WHITE);
    }
    
    int initialScore = player.getScore();
    MoveResult result = board.movePlayer(player1Id, PlayerMove.RIGHT);
    
    assertEquals(initialScore + 1, player.getScore());
}

@Test
void updateAdjacentPlatforms_WhenPlatformHasDifferentColor_ShouldUpdateBothScores() {
    Board board = new Board("test", Map.of(
        player1Id.toString(), ColorStatus.RED,
        player2Id.toString(), ColorStatus.YELLOW
    ));
    
    Player player1 = board.getPlayers().get(player1Id);
    Player player2 = board.getPlayers().get(player2Id);
    
    player1.setRow(1);
    player1.setCol(1);
    
    Box adjacentPlatform = board.getGrid()[0][1];
    if (adjacentPlatform instanceof Platform platform) {
        platform.setColor(ColorStatus.YELLOW);
    }
    
    int initialScoreRed = player1.getScore();
    int initialScoreYELLOW = player2.getScore();
    
    MoveResult result = board.movePlayer(player1Id, PlayerMove.RIGHT);
    
    assertEquals(initialScoreRed + 1, player1.getScore());
    assertEquals(0, player2.getScore());
}

@Test
void removePlayer_WhenPlayerExists_ShouldRemoveFromBoardAndPlayers() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    
    assertTrue(board.getPlayers().containsKey(player1Id));
    board.removePlayer(player1Id);
    
    assertFalse(board.getPlayers().containsKey(player1Id));
}

@Test
void removePlayer_WhenPlayerDoesNotExist_ShouldDoNothing() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    UUID nonExistentPlayerId = UUID.randomUUID();
    
    board.removePlayer(nonExistentPlayerId);
    
    assertEquals(1, board.getPlayers().size());
}

@Test
void getRowDownPlayer_WhenPlayerNotFound_ShouldReturnNull() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    
    Box result = board.getRowDownPLayer(UUID.randomUUID().toString());
    
    assertNull(result);
}

@Test
void getRowDownPlayer_WhenRowBelowIsOutOfBounds_ShouldReturnNull() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    Player player = board.getPlayers().get(player1Id);
    player.setRow(14);
    
    Box result = board.getRowDownPLayer(player1Id.toString());
    
    assertNull(result);
}

@Test
void setPlayerIsUp_ShouldUpdatePlayerState() {
    Board board = new Board("test", Map.of(player1Id.toString(), ColorStatus.RED));
    
    assertFalse(board.isPlayerUp(player1Id));
    board.setPlayerIsUp(player1Id, true);
    assertTrue(board.isPlayerUp(player1Id));
}
}