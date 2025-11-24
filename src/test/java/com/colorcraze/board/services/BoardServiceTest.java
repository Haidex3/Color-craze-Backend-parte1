package com.colorcraze.board.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.colorcraze.board.dtos.responses.MoveResult;
import com.colorcraze.board.dtos.responses.PlatformUpdate;
import com.colorcraze.board.dtos.responses.PlayerUpdate;
import com.colorcraze.board.models.Board;
import com.colorcraze.board.models.Box;
import com.colorcraze.board.models.Platform;
import com.colorcraze.board.models.Player;
import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.utils.enums.PlayerMove;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private BoardService boardService = new BoardService(messagingTemplate);

    @Test
    void createBoardWithPlayers_Success() {
        String gameId = "test-game";
        UUID playerId1 = UUID.randomUUID();
        UUID playerId2 = UUID.randomUUID();
        Map<String, ColorStatus> playerColors = Map.of(
            playerId1.toString(), ColorStatus.RED,
            playerId2.toString(), ColorStatus.GREEN
        );

        Board board = boardService.createBoardWithPlayers(gameId, playerColors);

        assertNotNull(board);
        assertEquals(gameId, board.getGameId());
        assertTrue(boardService.getAllBoardIds().contains(gameId));
    }

    @Test
    void createBoardWithPlayers_AlreadyExists() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        Map<String, ColorStatus> playerColors = Map.of(playerId.toString(), ColorStatus.RED);

        boardService.createBoardWithPlayers(gameId, playerColors);

        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> boardService.createBoardWithPlayers(gameId, playerColors));

        assertEquals("El tablero con id test-game ya existe", exception.getMessage());
    }

    @Test
    void createBoardWithPlayers_NullPlayerColors() {
        String gameId = "test-game-null";

        Board board = boardService.createBoardWithPlayers(gameId, null);

        assertNotNull(board);
        assertEquals(gameId, board.getGameId());
    }

    @Test
    void getBoard_Success() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        Map<String, ColorStatus> playerColors = Map.of(playerId.toString(), ColorStatus.RED);
        boardService.createBoardWithPlayers(gameId, playerColors);

        Board board = boardService.getBoard(gameId);

        assertNotNull(board);
        assertEquals(gameId, board.getGameId());
    }

    @Test
    void getBoard_NotFound() {
        String gameId = "non-existent-game";

        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> boardService.getBoard(gameId));

        assertEquals("No existe un tablero con id non-existent-game", exception.getMessage());
    }

    @Test
    void getBlock_Success() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        Map<String, ColorStatus> playerColors = Map.of(playerId.toString(), ColorStatus.RED);
        boardService.createBoardWithPlayers(gameId, playerColors);

        Box block = boardService.getBlock(gameId, 0, 0);

        assertNotNull(block);
    }

    @Test
    void setBlock_Success() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        Map<String, ColorStatus> playerColors = Map.of(playerId.toString(), ColorStatus.RED);
        boardService.createBoardWithPlayers(gameId, playerColors);
        
        Box newBlock = new Platform(ColorStatus.GREEN);
        boardService.setBlock(gameId, 0, 0, newBlock);

        Box retrievedBlock = boardService.getBlock(gameId, 0, 0);
        assertEquals(newBlock, retrievedBlock);
    }

    @Test
    void movePlayer_UpMove_Success() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        
        Board mockBoard = mock(Board.class);
        
        when(mockBoard.isPlayerUp(playerId)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId.toString())).thenReturn(new Platform(ColorStatus.RED));
        
        MoveResult mockMoveResult = new MoveResult(
            playerId, 1, 1, 
            List.of(new PlatformUpdate(1, 1, ColorStatus.RED)),
            List.of(new PlayerUpdate(UUID.randomUUID(), ColorStatus.GREEN, 2)),
            true, false
        );
        
        when(mockBoard.movePlayer(playerId, PlayerMove.UP))
            .thenReturn(mockMoveResult);

        Map<String, Board> boards = getBoardsField();
        boards.put(gameId, mockBoard);

        List<MoveResult> results = boardService.movePlayer(gameId, playerId.toString(), PlayerMove.UP);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        verify(mockBoard).setPlayerIsUp(playerId, true);
        verify(mockBoard).setPlayerIsUp(playerId, false);
    }

    @Test
    void movePlayer_UpMove_PlayerAlreadyUp() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        
        Board mockBoard = mock(Board.class);
        when(mockBoard.isPlayerUp(playerId)).thenReturn(true);
        
        Map<String, Board> boards = getBoardsField();
        boards.put(gameId, mockBoard);

        List<MoveResult> results = boardService.movePlayer(gameId, playerId.toString(), PlayerMove.UP);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockBoard, never()).setPlayerIsUp(any(), anyBoolean());
    }

    @Test
    void movePlayer_UpMove_NoPlatformBelow() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        
        Board mockBoard = mock(Board.class);
        when(mockBoard.isPlayerUp(playerId)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId.toString())).thenReturn(new Box(ColorStatus.WHITE));
        
        Map<String, Board> boards = getBoardsField();
        boards.put(gameId, mockBoard);

        List<MoveResult> results = boardService.movePlayer(gameId, playerId.toString(), PlayerMove.UP);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockBoard, never()).setPlayerIsUp(any(), anyBoolean());
    }

    @Test
    void movePlayer_UpMove_NullStepResults() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        
        Board mockBoard = mock(Board.class);
        when(mockBoard.isPlayerUp(playerId)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId.toString())).thenReturn(new Platform(ColorStatus.RED));
        when(mockBoard.movePlayer(playerId, PlayerMove.UP)).thenReturn(null);
        
        Map<String, Board> boards = getBoardsField();
        boards.put(gameId, mockBoard);

        List<MoveResult> results = boardService.movePlayer(gameId, playerId.toString(), PlayerMove.UP);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockBoard).setPlayerIsUp(playerId, true);
        verify(mockBoard).setPlayerIsUp(playerId, false);
    }

    @Test
    void movePlayer_UpMove_PartialSteps() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        
        Board mockBoard = mock(Board.class);
        when(mockBoard.isPlayerUp(playerId)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId.toString())).thenReturn(new Platform(ColorStatus.RED));
        
        MoveResult firstStep = new MoveResult(
            playerId, 1, 1, 
            List.of(new PlatformUpdate(1, 1, ColorStatus.RED)),
            List.of(new PlayerUpdate(UUID.randomUUID(), ColorStatus.GREEN, 2)),
            true, false
        );
        
        MoveResult secondStep = new MoveResult(
            playerId, 2, 1, 
            List.of(new PlatformUpdate(2, 1, ColorStatus.RED)),
            List.of(new PlayerUpdate(UUID.randomUUID(), ColorStatus.PURPLE, 2)),
            true, false
        );
        
        when(mockBoard.movePlayer(playerId, PlayerMove.UP))
            .thenReturn(firstStep)
            .thenReturn(secondStep)
            .thenReturn(null);
        
        Map<String, Board> boards = getBoardsField();
        boards.put(gameId, mockBoard);

        List<MoveResult> results = boardService.movePlayer(gameId, playerId.toString(), PlayerMove.UP);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(secondStep.newRow(), results.get(0).newRow());
        assertEquals(secondStep.newCol(), results.get(0).newCol());
        verify(mockBoard).setPlayerIsUp(playerId, true);
        verify(mockBoard).setPlayerIsUp(playerId, false);
    }

    @Test
    void movePlayer_OtherMove_Success() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        
        Board mockBoard = mock(Board.class);
        
        MoveResult mockMoveResult = new MoveResult(
            playerId, 1, 2, 
            List.of(), List.of(), true, false
        );
        
        when(mockBoard.movePlayer(playerId, PlayerMove.RIGHT))
            .thenReturn(mockMoveResult);
        
        Map<String, Board> boards = getBoardsField();
        boards.put(gameId, mockBoard);

        List<MoveResult> results = boardService.movePlayer(gameId, playerId.toString(), PlayerMove.RIGHT);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(mockMoveResult, results.get(0));
    }

    @Test
    void movePlayer_OtherMove_NullResult() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        
        Board mockBoard = mock(Board.class);
        
        when(mockBoard.movePlayer(playerId, PlayerMove.LEFT))
            .thenReturn(null);
        
        Map<String, Board> boards = getBoardsField();
        boards.put(gameId, mockBoard);

        List<MoveResult> results = boardService.movePlayer(gameId, playerId.toString(), PlayerMove.LEFT);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertNull(results.get(0));
    }

    @Test
    void movePlayer_DownMove_Success() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        
        Board mockBoard = mock(Board.class);
        
        MoveResult mockMoveResult = new MoveResult(
            playerId, 2, 1, 
            List.of(), List.of(), true, true
        );
        
        when(mockBoard.movePlayer(playerId, PlayerMove.DOWN))
            .thenReturn(mockMoveResult);
        
        Map<String, Board> boards = getBoardsField();
        boards.put(gameId, mockBoard);

        List<MoveResult> results = boardService.movePlayer(gameId, playerId.toString(), PlayerMove.DOWN);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(mockMoveResult, results.get(0));
    }

    @Test
    void movePlayer_UpMove_WithGravityInFinalResult() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        
        Board mockBoard = mock(Board.class);
        when(mockBoard.isPlayerUp(playerId)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId.toString())).thenReturn(new Platform(ColorStatus.RED));
        
        MoveResult stepWithGravity = new MoveResult(
            playerId, 3, 1, 
            List.of(new PlatformUpdate(3, 1, ColorStatus.RED)),
            List.of(new PlayerUpdate(UUID.randomUUID(), ColorStatus.PURPLE, 2)),
            true, true
        );
        
        when(mockBoard.movePlayer(playerId, PlayerMove.UP))
            .thenReturn(stepWithGravity)
            .thenReturn(null);
        
        Map<String, Board> boards = getBoardsField();
        boards.put(gameId, mockBoard);

        List<MoveResult> results = boardService.movePlayer(gameId, playerId.toString(), PlayerMove.UP);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).gravity());
        verify(mockBoard).setPlayerIsUp(playerId, true);
        verify(mockBoard).setPlayerIsUp(playerId, false);
    }

    @Test
    void movePlayer_BoardNotFound() {
        String gameId = "non-existent-game";
        UUID playerId = UUID.randomUUID();
        String playerIdStr = playerId.toString();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, 
            () -> boardService.movePlayer(gameId, playerIdStr, PlayerMove.RIGHT)
        );

        assertEquals("No existe un tablero con id non-existent-game", exception.getMessage());
    }

    @Test
    void addPlayerToBoard_Success() {
        String gameId = "test-game";
        UUID playerId = UUID.randomUUID();
        Map<String, ColorStatus> playerColors = Map.of(playerId.toString(), ColorStatus.RED);
        boardService.createBoardWithPlayers(gameId, playerColors);

        Player newPlayer = boardService.addPlayerToBoard(gameId, ColorStatus.GREEN);

        assertNotNull(newPlayer);
        assertEquals(ColorStatus.GREEN, newPlayer.getColor());
        assertNotNull(newPlayer.getId());
    }

    @Test
    void getAllBoardIds_Success() {
        String gameId1 = "test-game-1";
        String gameId2 = "test-game-2";
        UUID playerId1 = UUID.randomUUID();
        UUID playerId2 = UUID.randomUUID();
        
        boardService.createBoardWithPlayers(gameId1, Map.of(playerId1.toString(), ColorStatus.RED));
        boardService.createBoardWithPlayers(gameId2, Map.of(playerId2.toString(), ColorStatus.GREEN));

        Set<String> boardIds = boardService.getAllBoardIds();

        assertNotNull(boardIds);
        assertTrue(boardIds.contains(gameId1));
        assertTrue(boardIds.contains(gameId2));
        assertEquals(2, boardIds.size());
    }

    @Test
    void getAllBoardIds_Empty() {
        Map<String, Board> boards = getBoardsField();
        boards.clear();

        Set<String> boardIds = boardService.getAllBoardIds();

        assertNotNull(boardIds);
        assertTrue(boardIds.isEmpty());
    }

    @Test
    void startGameTimer_Success() {
        boardService = new BoardService(messagingTemplate);
        
        String gameId = "test-game";
        Map<String, ColorStatus> playerColors = Map.of(
            UUID.randomUUID().toString(), ColorStatus.RED
        );
        
        boardService.createBoardWithPlayers(gameId, playerColors);
        
        boardService.startGameTimer(gameId, 60);
        
        await().atMost(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> 
                verify(messagingTemplate, atLeastOnce())
                    .convertAndSend(eq("/topic/board." + gameId), any(Map.class))
            );
    }

    @Test
    void startGameTimer_AlreadyExists_DoesNotStartNewTimer() {
        boardService = spy(new BoardService(messagingTemplate));
        
        String gameId = "test-game";
        Map<String, ColorStatus> playerColors = Map.of(
            UUID.randomUUID().toString(), ColorStatus.RED
        );
        
        boardService.createBoardWithPlayers(gameId, playerColors);
        
        boardService.startGameTimer(gameId, 60);
        boardService.startGameTimer(gameId, 60);
        
        await().atMost(200, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> 
                verify(messagingTemplate, atLeastOnce())
                    .convertAndSend(eq("/topic/board." + gameId), any(Map.class))
            );
    }

    @Test
    void startGameTimer_CountdownDecrementsCorrectly() {
        boardService = new BoardService(messagingTemplate);

        String gameId = "test-game";
        Map<String, ColorStatus> playerColors = Map.of(
            UUID.randomUUID().toString(), ColorStatus.RED
        );

        boardService.createBoardWithPlayers(gameId, playerColors);
        boardService.startGameTimer(gameId, 3);

        await().atMost(4, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                ArgumentCaptor<Map<?, ?>> messageCaptor = ArgumentCaptor.forClass(Map.class);

                verify(messagingTemplate, atLeast(3))
                    .convertAndSend(eq("/topic/board." + gameId), messageCaptor.capture());

                var allValues = messageCaptor.getAllValues();
                boolean foundDecreasingValues = false;

                for (int i = 0; i < allValues.size() - 1; i++) {
                    Integer time1 = (Integer) allValues.get(i).get("timeLeft");
                    Integer time2 = (Integer) allValues.get(i + 1).get("timeLeft");
                    if (time1 != null && time2 != null && time1 > time2) {
                        foundDecreasingValues = true;
                        break;
                    }
                }

                assertTrue(foundDecreasingValues, "El tiempo debería decrementar en las actualizaciones");
            });
    }


    @Test
    void startGameTimer_ReachesZero_EndsGame() {
        boardService = new BoardService(messagingTemplate);

        String gameId = "test-game";
        Map<String, ColorStatus> playerColors = Map.of(
            UUID.randomUUID().toString(), ColorStatus.RED
        );

        boardService.createBoardWithPlayers(gameId, playerColors);
        boardService.startGameTimer(gameId, 1);

        await().atMost(3, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                ArgumentCaptor<Map<?, ?>> messageCaptor = ArgumentCaptor.forClass(Map.class);

                verify(messagingTemplate, atLeastOnce())
                    .convertAndSend(eq("/topic/board." + gameId), messageCaptor.capture());

                boolean gameEnded = messageCaptor.getAllValues().stream()
                    .anyMatch(msg -> {
                        Object val = msg.get("gameOver");
                        return Boolean.TRUE.equals(val);
                    });

                assertTrue(gameEnded, "Debería haberse enviado el mensaje de fin del juego");
            });
    }


    @Test
    void startGameTimer_RemovesBoardAfterGameEnd() {
        boardService = new BoardService(messagingTemplate);
        
        String gameId = "test-game";
        Map<String, ColorStatus> playerColors = Map.of(
            UUID.randomUUID().toString(), ColorStatus.RED
        );
        
        boardService.createBoardWithPlayers(gameId, playerColors);
        
        assertTrue(boardService.getAllBoardIds().contains(gameId));
        
        boardService.startGameTimer(gameId, 1);
        
        await().atMost(3, TimeUnit.SECONDS)
            .untilAsserted(() -> 
                assertFalse(boardService.getAllBoardIds().contains(gameId), 
                    "El board debería ser removido cuando el juego termina")
            );
    }

    @Test
    void endGame_Success() {
        boardService = new BoardService(messagingTemplate);

        String gameId = "test-game";
        Map<String, ColorStatus> playerColors = Map.of(
            UUID.randomUUID().toString(), ColorStatus.RED,
            UUID.randomUUID().toString(), ColorStatus.GREEN
        );

        boardService.createBoardWithPlayers(gameId, playerColors);

        assertTrue(boardService.getAllBoardIds().contains(gameId));

        boardService.endGame(gameId);

        ArgumentCaptor<Map<?, ?>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/board." + gameId), messageCaptor.capture());

        Map<?, ?> sentMessage = messageCaptor.getValue();

        assertNotNull(sentMessage.get("players"), "Debería incluir la información de jugadores");
        assertFalse(boardService.getAllBoardIds().contains(gameId),
            "El board debería ser removido después de endGame");
    }


    @Test
    void startGameTimer_MultipleGames_IndependentTimers() {
        boardService = new BoardService(messagingTemplate);
        
        String gameId1 = "game-1";
        String gameId2 = "game-2";
        
        Map<String, ColorStatus> playerColors = Map.of(
            UUID.randomUUID().toString(), ColorStatus.RED
        );
        
        boardService.createBoardWithPlayers(gameId1, playerColors);
        boardService.createBoardWithPlayers(gameId2, playerColors);
        
        boardService.startGameTimer(gameId1, 2);
        boardService.startGameTimer(gameId2, 3);
        
        await().atMost(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertTrue(boardService.getAllBoardIds().contains(gameId1));
                assertTrue(boardService.getAllBoardIds().contains(gameId2));
            });
        
        await().atMost(4, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(messagingTemplate, atLeastOnce())
                    .convertAndSend(eq("/topic/board." + gameId1), any(Map.class));
                verify(messagingTemplate, atLeastOnce())
                    .convertAndSend(eq("/topic/board." + gameId2), any(Map.class));
            });
    }

    @Test
    void startGameTimer_CancelsPreviousTimer_WhenNewTimerStarted() {
        boardService = new BoardService(messagingTemplate);
        
        String gameId = "test-game";
        Map<String, ColorStatus> playerColors = Map.of(
            UUID.randomUUID().toString(), ColorStatus.RED
        );
        
        boardService.createBoardWithPlayers(gameId, playerColors);
        
        boardService.startGameTimer(gameId, 10);
        
        await().atMost(200, TimeUnit.MILLISECONDS);
        
        boardService.startGameTimer(gameId, 5);
        
        await().atMost(7, TimeUnit.SECONDS)
            .untilAsserted(() -> 
                verify(messagingTemplate, atLeast(6))
                    .convertAndSend(eq("/topic/board." + gameId), any(Map.class))
            );
    }
    @SuppressWarnings("unchecked")
    private Map<String, Board> getBoardsField() {
        try {
            var field = boardService.getClass().getDeclaredField("boards");
            field.setAccessible(true);
            return (Map<String, Board>) field.get(boardService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access field: boards", e);
        }
    }
}