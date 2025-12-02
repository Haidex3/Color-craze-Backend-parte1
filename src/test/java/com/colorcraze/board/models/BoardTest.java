package com.colorcraze.board.models;

import com.colorcraze.board.dtos.responses.MoveResult;
import com.colorcraze.board.dtos.responses.PlatformUpdate;
import com.colorcraze.board.dtos.responses.PlayerUpdate;
import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.utils.enums.PlayerMove;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    @DisplayName("Should throw exception when moving non-existent player")
    void testMoveNonExistentPlayer() {
        UUID nonExistentPlayerId = UUID.randomUUID();
        
        assertThrows(IllegalArgumentException.class, 
            () -> board.movePlayer(nonExistentPlayerId, PlayerMove.RIGHT));
    }

    @Test
    void testEmptyConstructor() {
        Board board2 = new Board();

        assertNotNull(board2.getGrid(), "El grid no debe ser null");
        assertEquals(15, board2.getGrid().length, "El número de filas debe ser 15");
        assertEquals(31, board2.getGrid()[0].length, "El número de columnas debe ser 31");

        assertNotNull(board2.getPlayers(), "El mapa de jugadores no debe ser null");
        assertTrue(board2.getPlayers().isEmpty(), "El mapa de jugadores debe iniciar vacío");

        assertNull(board2.getGameId(), "El gameId debe iniciar en null");
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
    void updateAdjacentPlatforms_WhenPlatformAlreadyPlayerColor_ShouldNotChangeScore() {
        Player player = board.getPlayers().get(player1Id);
        player.setRow(1);
        player.setCol(1);
        
        Box adjacentPlatform = board.getGrid()[0][1];
        if (adjacentPlatform instanceof Platform platform) {
            platform.setColor(ColorStatus.RED);
        }
    
        board.movePlayer(player1Id, PlayerMove.RIGHT);
        
        assertEquals(1, player.getScore());
    }

    @Test
    void updateAdjacentPlatforms_WhenPlatformIsWhite_ShouldIncreasePaintingPlayerScore() {
        Player player = board.getPlayers().get(player1Id);
        player.setRow(1);
        player.setCol(1);
        
        Box adjacentPlatform = board.getGrid()[0][1];
        if (adjacentPlatform instanceof Platform platform) {
            platform.setColor(ColorStatus.WHITE);
        }
        
        int initialScore = player.getScore();
        board.movePlayer(player1Id, PlayerMove.RIGHT);
        
        assertEquals(initialScore + 1, player.getScore());
    }

    @Test
    void getRowDownPlayer_WhenRowBelowIsOutOfBounds_ShouldReturnNull() {
        Player player = board.getPlayers().get(player1Id);
        player.setRow(14);
        
        Box result = board.getRowDownPLayer(player1Id.toString());
        
        assertNull(result);
    }

    @Test
    @DisplayName("Should update platform from white to player color and increase painting player score")
    void testUpdatePlatformFromWhiteToPlayerColor() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.YELLOW);
        Board testBoard = new Board("test-white-to-color", singlePlayerColors);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        int initialScore = player.getScore();
        
        Platform whitePlatform = new Platform(ColorStatus.WHITE);
        
        List<PlayerUpdate> affectedPlayers = new ArrayList<>();
        
        try {
            Method updatePlatformMethod = Board.class.getDeclaredMethod("updatePlatformAndScores", 
                Platform.class, ColorStatus.class, List.class);
            updatePlatformMethod.setAccessible(true);
            updatePlatformMethod.invoke(testBoard, whitePlatform, ColorStatus.YELLOW, affectedPlayers);
            
            assertEquals(ColorStatus.YELLOW, whitePlatform.getColor());
            assertEquals(1, affectedPlayers.size());
            assertEquals(testPlayerId, affectedPlayers.get(0).playerId());
            assertEquals(initialScore + 1, affectedPlayers.get(0).newScore());
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should update platform from one color to another and update both players scores")
    void testUpdatePlatformFromOneColorToAnother() {
        UUID redPlayerId = UUID.randomUUID();
        UUID yellowPlayerId = UUID.randomUUID();
        
        playerColors = new HashMap<>();
        playerColors.put(redPlayerId.toString(), ColorStatus.RED);
        playerColors.put(yellowPlayerId.toString(), ColorStatus.YELLOW);
        
        Board testBoard = new Board("test-color-change", playerColors);
        
        Player redPlayer = testBoard.getPlayers().get(redPlayerId);
        Player yellowPlayer = testBoard.getPlayers().get(yellowPlayerId);
        
        redPlayer.setScore(5);
        yellowPlayer.setScore(3);
        
        Platform platform = new Platform(ColorStatus.RED);
        
        List<PlayerUpdate> affectedPlayers = new ArrayList<>();
        
        try {
            Method updatePlatformMethod = Board.class.getDeclaredMethod("updatePlatformAndScores", 
                Platform.class, ColorStatus.class, List.class);
            updatePlatformMethod.setAccessible(true);
            updatePlatformMethod.invoke(testBoard, platform, ColorStatus.YELLOW, affectedPlayers);
            
            assertEquals(ColorStatus.YELLOW, platform.getColor());
            assertEquals(2, affectedPlayers.size());
            
            PlayerUpdate yellowUpdate = affectedPlayers.stream()
                .filter(pu -> pu.playerId().equals(yellowPlayerId))
                .findFirst()
                .orElse(null);
            assertNotNull(yellowUpdate);
            assertEquals(4, yellowUpdate.newScore());
            
            PlayerUpdate redUpdate = affectedPlayers.stream()
                .filter(pu -> pu.playerId().equals(redPlayerId))
                .findFirst()
                .orElse(null);
            assertNotNull(redUpdate);
            assertEquals(4, redUpdate.newScore());
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should not update platform when new color is same as current color")
    void testUpdatePlatformSameColor() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-same-color", singlePlayerColors);
        
        Player player = testBoard.getPlayers().get(testPlayerId);
        int initialScore = player.getScore();
        
        Platform platform = new Platform(ColorStatus.GREEN);
        
        List<PlayerUpdate> affectedPlayers = new ArrayList<>();
        
        try {
            Method updatePlatformMethod = Board.class.getDeclaredMethod("updatePlatformAndScores", 
                Platform.class, ColorStatus.class, List.class);
            updatePlatformMethod.setAccessible(true);
            updatePlatformMethod.invoke(testBoard, platform, ColorStatus.GREEN, affectedPlayers);
            
            assertEquals(ColorStatus.GREEN, platform.getColor());
            assertTrue(affectedPlayers.isEmpty());
            assertEquals(initialScore, player.getScore());
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle platform update when previous color player has zero score")
    void testUpdatePlatformPreviousPlayerZeroScore() {
        UUID redPlayerId = UUID.randomUUID();
        UUID yellowPlayerId = UUID.randomUUID();
        
        playerColors = new HashMap<>();
        playerColors.put(redPlayerId.toString(), ColorStatus.RED);
        playerColors.put(yellowPlayerId.toString(), ColorStatus.YELLOW);
        
        Board testBoard = new Board("test-zero-score", playerColors);
        
        Player redPlayer = testBoard.getPlayers().get(redPlayerId);
        Player yellowPlayer = testBoard.getPlayers().get(yellowPlayerId);
        
        redPlayer.setScore(0);
        yellowPlayer.setScore(2);
        
        Platform platform = new Platform(ColorStatus.RED);
        
        List<PlayerUpdate> affectedPlayers = new ArrayList<>();
        
        try {
            Method updatePlatformMethod = Board.class.getDeclaredMethod("updatePlatformAndScores", 
                Platform.class, ColorStatus.class, List.class);
            updatePlatformMethod.setAccessible(true);
            updatePlatformMethod.invoke(testBoard, platform, ColorStatus.YELLOW, affectedPlayers);
            
            assertEquals(ColorStatus.YELLOW, platform.getColor());
            assertEquals(1, affectedPlayers.size());
            assertEquals(yellowPlayerId, affectedPlayers.get(0).playerId());
            assertEquals(3, affectedPlayers.get(0).newScore());
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should return true when player is first in queue")
    void testIsFirstInQueueWhenFirst() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        try {
            Method isFirstInQueueMethod = Board.class.getDeclaredMethod("isFirstInQueue", UUID.class);
            isFirstInQueueMethod.setAccessible(true);
            
            Field lockQueueField = Board.class.getDeclaredField("lockQueue");
            lockQueueField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<UUID> queue = (List<UUID>) lockQueueField.get(board);
            queue.clear();
            queue.add(player1);
            queue.add(player2);
            
            boolean result = (boolean) isFirstInQueueMethod.invoke(board, player1);
            assertTrue(result);
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should return false when player is not first in queue")
    void testIsFirstInQueueWhenNotFirst() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        try {
            Method isFirstInQueueMethod = Board.class.getDeclaredMethod("isFirstInQueue", UUID.class);
            isFirstInQueueMethod.setAccessible(true);
            
            Field lockQueueField = Board.class.getDeclaredField("lockQueue");
            lockQueueField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<UUID> queue = (List<UUID>) lockQueueField.get(board);
            queue.clear();
            queue.add(player1);
            queue.add(player2);
            
            boolean result = (boolean) isFirstInQueueMethod.invoke(board, player2);
            assertFalse(result);
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should update all adjacent platforms in four directions")
    void testUpdateAdjacentPlatformsAllDirections() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.PURPLE);
        Board testBoard = new Board("test-all-directions", singlePlayerColors);
        
        int testRow = 5;
        int testCol = 5;
        
        testBoard.getGrid()[4][5] = new Platform(ColorStatus.WHITE);
        testBoard.getGrid()[6][5] = new Platform(ColorStatus.WHITE);
        testBoard.getGrid()[5][4] = new Platform(ColorStatus.WHITE);
        testBoard.getGrid()[5][6] = new Platform(ColorStatus.WHITE);
        
        List<PlayerUpdate> affectedPlayers = new ArrayList<>();
        
        try {
            Method updateAdjacentMethod = Board.class.getDeclaredMethod("updateAdjacentPlatforms", 
                int.class, int.class, ColorStatus.class, List.class);
            updateAdjacentMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<PlatformUpdate> result = (List<PlatformUpdate>) updateAdjacentMethod.invoke(
                testBoard, testRow, testCol, ColorStatus.PURPLE, affectedPlayers);
            
            assertEquals(4, result.size());
            
            assertEquals(ColorStatus.PURPLE, ((Platform) testBoard.getGrid()[4][5]).getColor());
            assertEquals(ColorStatus.PURPLE, ((Platform) testBoard.getGrid()[6][5]).getColor());
            assertEquals(ColorStatus.PURPLE, ((Platform) testBoard.getGrid()[5][4]).getColor());
            assertEquals(ColorStatus.PURPLE, ((Platform) testBoard.getGrid()[5][6]).getColor());
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should ignore non-platform adjacent boxes")
    void testUpdateAdjacentPlatformsIgnoresNonPlatforms() {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.PURPLE);
        Board testBoard = new Board("test-non-platforms", singlePlayerColors);
        
        int testRow = 5;
        int testCol = 5;
        
        testBoard.getGrid()[4][5] = new Box(ColorStatus.GREEN);
        testBoard.getGrid()[6][5] = new Box(ColorStatus.GREEN);
        testBoard.getGrid()[5][4] = new Box(ColorStatus.GREEN);
        testBoard.getGrid()[5][6] = new Box(ColorStatus.GREEN);
        
        List<PlayerUpdate> affectedPlayers = new ArrayList<>();
        
        try {
            Method updateAdjacentMethod = Board.class.getDeclaredMethod("updateAdjacentPlatforms", 
                int.class, int.class, ColorStatus.class, List.class);
            updateAdjacentMethod.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<PlatformUpdate> result = (List<PlatformUpdate>) updateAdjacentMethod.invoke(
                testBoard, testRow, testCol, ColorStatus.PURPLE, affectedPlayers);
            
            assertEquals(0, result.size());
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Should handle InterruptedException when waiting in queue")
    void testMovePlayerInterruptedExceptionInQueue() throws Exception {
        UUID testPlayerId = UUID.randomUUID();
        Map<String, ColorStatus> singlePlayerColors = new HashMap<>();
        singlePlayerColors.put(testPlayerId.toString(), ColorStatus.GREEN);
        Board testBoard = new Board("test-interrupt", singlePlayerColors);

        Field lockQueueField = Board.class.getDeclaredField("lockQueue");
        lockQueueField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<UUID> queue = (List<UUID>) lockQueueField.get(testBoard);

        UUID otherPlayerId = UUID.randomUUID();
        queue.add(otherPlayerId);
        queue.add(testPlayerId);

        CountDownLatch latch = new CountDownLatch(1);

        Thread testThread = new Thread(() -> {
            latch.countDown();
            testBoard.movePlayer(testPlayerId, PlayerMove.RIGHT);
        });

        testThread.start();
        latch.await(1, TimeUnit.SECONDS);
        testThread.interrupt();
        testThread.join(1000);
        assertFalse(testThread.isAlive());

        assertFalse(queue.contains(testPlayerId));
    }

}