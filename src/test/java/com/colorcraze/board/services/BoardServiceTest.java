package com.colorcraze.board.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.colorcraze.board.models.Board;
import com.colorcraze.utils.enums.ColorStatus;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ChannelTopic boardTopic;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    private BoardService boardService;
    
    private final String serverId = "test-server-1";
    private final String gameId = "test-game-123";
    private final String boardKey = "board:test-game-123";
    private final String playerId1 = "550e8400-e29b-41d4-a716-446655440000";
    private final String playerId2 = "550e8400-e29b-41d4-a716-446655440001";
    private final UUID uuid1 = UUID.fromString(playerId1);
    private final UUID uuid2 = UUID.fromString(playerId2);
    private final ColorStatus color1 = ColorStatus.YELLOW;
    private final ColorStatus color2 = ColorStatus.RED;
    
    private Map<String, ColorStatus> playerColors;

    @BeforeEach
    void setUp() {
        playerColors = new HashMap<>();
        playerColors.put(playerId1, color1);
        playerColors.put(playerId2, color2);
        
        boardService = new BoardService(redisTemplate, messagingTemplate, boardTopic, serverId);
        
        try {
            var schedulerField = BoardService.class.getDeclaredField("scheduler");
            schedulerField.setAccessible(true);
            schedulerField.set(boardService, taskScheduler);
            
            var timersField = BoardService.class.getDeclaredField("gameTimers");
            timersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ScheduledFuture<?>> timers = (Map<String, ScheduledFuture<?>>) timersField.get(boardService);
            timers.clear();
        } catch (Exception e) {
            throw new RuntimeException("Error setting up test", e);
        }
    }

    @Test
    void createBoardWithPlayers_Success() {
        when(redisTemplate.hasKey(boardKey)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board result = boardService.createBoardWithPlayers(gameId, playerColors);
        
        assertNotNull(result);
        assertEquals(gameId, result.getGameId());
        assertNotNull(result.getPlayers());
        assertEquals(2, result.getPlayers().size());
        assertTrue(result.getPlayers().containsKey(uuid1));
        assertTrue(result.getPlayers().containsKey(uuid2));
        
        verify(redisTemplate).hasKey(boardKey);
        verify(valueOperations).set(boardKey, result);
    }

    @Test
    void createBoardWithPlayers_AlreadyExists() {
        when(redisTemplate.hasKey(boardKey)).thenReturn(true);
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> boardService.createBoardWithPlayers(gameId, playerColors)
        );
        
        assertEquals("Board with id " + gameId + " already exists", exception.getMessage());
        
        verify(redisTemplate).hasKey(boardKey);
        verify(redisTemplate, never()).opsForValue();
        verify(valueOperations, never()).set(anyString(), any());
    }

    @Test
    void createBoardWithPlayers_NullPlayerColors() {
        when(redisTemplate.hasKey(boardKey)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board result = boardService.createBoardWithPlayers(gameId, null);
        
        assertNotNull(result);
        assertEquals(gameId, result.getGameId());
        assertNotNull(result.getPlayers());
        assertTrue(result.getPlayers().isEmpty());
        
        verify(redisTemplate).hasKey(boardKey);
        verify(valueOperations).set(boardKey, result);
    }

    @Test
    void getBoard_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Board mockBoard = new Board(gameId, playerColors);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        Board result = boardService.getBoard(gameId);
        
        assertNotNull(result);
        assertEquals(gameId, result.getGameId());
        assertEquals(mockBoard, result);
        
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(boardKey);
    }

    @Test
    void getBoard_NotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(boardKey)).thenReturn(null);
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> boardService.getBoard(gameId)
        );
        
        assertEquals("No board exists with id " + gameId, exception.getMessage());
        
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(boardKey);
    }

    @Test
    void getBlock_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Board mockBoard = new Board(gameId, playerColors);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        var block = boardService.getBlock(gameId, 0, 0);
        
        assertNotNull(block);
        
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(boardKey);
    }

    @Test
    void setBlock_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Board mockBoard = new Board(gameId, playerColors);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        var newBlock = new com.colorcraze.board.models.Box(ColorStatus.RED);
        
        boardService.setBlock(gameId, 0, 0, newBlock);
        
        assertEquals(newBlock, mockBoard.getGrid()[0][0]);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
    }

    @Test
    void movePlayer_UpMove_Success() {
        when(boardTopic.getTopic()).thenReturn("/topic/board");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board mockBoard = mock(Board.class);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        when(mockBoard.isPlayerUp(uuid1)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId1)).thenReturn(mock(com.colorcraze.board.models.Platform.class));
        when(mockBoard.movePlayer(uuid1, com.colorcraze.utils.enums.PlayerMove.UP))
            .thenReturn(new com.colorcraze.board.dtos.responses.MoveResult(
                uuid1, 1, 1, List.of(), List.of(), true, false
            ));
        
        var results = boardService.movePlayer(gameId, playerId1, com.colorcraze.utils.enums.PlayerMove.UP);
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        verify(mockBoard).setPlayerIsUp(uuid1, true);
        verify(mockBoard).setPlayerIsUp(uuid1, false);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
        verify(redisTemplate).convertAndSend(eq("/topic/board"), any(Map.class));
    }

    @Test
    void movePlayer_UpMove_PlayerAlreadyUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Board mockBoard = mock(Board.class);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        when(mockBoard.isPlayerUp(uuid1)).thenReturn(true);
        
        var results = boardService.movePlayer(gameId, playerId1, com.colorcraze.utils.enums.PlayerMove.UP);
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        verify(mockBoard, never()).setPlayerIsUp(any(), anyBoolean());
        verify(mockBoard, never()).movePlayer(any(), any());
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
    }

    @Test
    void movePlayer_UpMove_NoPlatformBelow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Board mockBoard = mock(Board.class);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        when(mockBoard.isPlayerUp(uuid1)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId1)).thenReturn(null);
        
        var results = boardService.movePlayer(gameId, playerId1, com.colorcraze.utils.enums.PlayerMove.UP);
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        verify(mockBoard, never()).setPlayerIsUp(any(), anyBoolean());
        verify(mockBoard, never()).movePlayer(any(), any());
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
    }

    @Test
    void movePlayer_UpMove_NullStepResults() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Board mockBoard = mock(Board.class);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        when(mockBoard.isPlayerUp(uuid1)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId1)).thenReturn(mock(com.colorcraze.board.models.Platform.class));
        
        when(mockBoard.movePlayer(uuid1, com.colorcraze.utils.enums.PlayerMove.UP))
            .thenReturn(null);

        var results = boardService.movePlayer(gameId, playerId1, com.colorcraze.utils.enums.PlayerMove.UP);
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        verify(mockBoard).setPlayerIsUp(uuid1, true);
        verify(mockBoard).setPlayerIsUp(uuid1, false);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
    }

    @Test
    void movePlayer_UpMove_PartialSteps() {
        when(boardTopic.getTopic()).thenReturn("/topic/board");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board mockBoard = mock(Board.class);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        when(mockBoard.isPlayerUp(uuid1)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId1)).thenReturn(mock(com.colorcraze.board.models.Platform.class));
        
        com.colorcraze.board.dtos.responses.MoveResult step1 = new com.colorcraze.board.dtos.responses.MoveResult(
            uuid1, 1, 1, List.of(), List.of(), true, false
        );
        com.colorcraze.board.dtos.responses.MoveResult step2 = new com.colorcraze.board.dtos.responses.MoveResult(
            uuid1, 2, 1, List.of(), List.of(), true, false
        );
        
        when(mockBoard.movePlayer(uuid1, com.colorcraze.utils.enums.PlayerMove.UP))
            .thenReturn(step1)
            .thenReturn(step2)
            .thenReturn(null);

        var results = boardService.movePlayer(gameId, playerId1, com.colorcraze.utils.enums.PlayerMove.UP);
        
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).newRow());
        
        verify(mockBoard, times(3)).movePlayer(
            uuid1,
            com.colorcraze.utils.enums.PlayerMove.UP
        );

        verify(mockBoard).setPlayerIsUp(uuid1, true);
        verify(mockBoard).setPlayerIsUp(uuid1, false);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
        verify(redisTemplate).convertAndSend(eq("/topic/board"), any(Map.class));
    }

    @Test
    void movePlayer_OtherMove_Success() {
        when(boardTopic.getTopic()).thenReturn("/topic/board");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board mockBoard = mock(Board.class);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        com.colorcraze.board.dtos.responses.MoveResult moveResult = new com.colorcraze.board.dtos.responses.MoveResult(
            uuid1, 1, 2, List.of(), List.of(), true, false
        );
        
        when(mockBoard.movePlayer(uuid1, com.colorcraze.utils.enums.PlayerMove.RIGHT))
            .thenReturn(moveResult);    

        
        var results = boardService.movePlayer(gameId, playerId1, com.colorcraze.utils.enums.PlayerMove.RIGHT);
        
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(moveResult, results.get(0));
        
        verify(mockBoard, never()).isPlayerUp(any());
        verify(mockBoard, never()).getRowDownPLayer(any());
        verify(mockBoard, never()).setPlayerIsUp(any(), anyBoolean());
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
        verify(redisTemplate).convertAndSend(eq("/topic/board"), any(Map.class));
    }

    @Test
    void movePlayer_DownMove_Success() {
        when(boardTopic.getTopic()).thenReturn("/topic/board");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board mockBoard = mock(Board.class);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        com.colorcraze.board.dtos.responses.MoveResult moveResult = new com.colorcraze.board.dtos.responses.MoveResult(
            uuid1, 2, 1, List.of(), List.of(), true, false
        );
        
        when(mockBoard.movePlayer(uuid1, com.colorcraze.utils.enums.PlayerMove.DOWN))
            .thenReturn(moveResult);
        
        var results = boardService.movePlayer(gameId, playerId1, com.colorcraze.utils.enums.PlayerMove.DOWN);
        
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(moveResult, results.get(0));
        
        verify(mockBoard, never()).isPlayerUp(any());
        verify(mockBoard, never()).getRowDownPLayer(any());
        verify(mockBoard, never()).setPlayerIsUp(any(), anyBoolean());
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
        verify(redisTemplate).convertAndSend(eq("/topic/board"), any(Map.class));
    }

    @Test
    void movePlayer_UpMove_WithGravityInFinalResult() {
        when(boardTopic.getTopic()).thenReturn("/topic/board");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board mockBoard = mock(Board.class);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        when(mockBoard.isPlayerUp(uuid1)).thenReturn(false);
        when(mockBoard.getRowDownPLayer(playerId1)).thenReturn(mock(com.colorcraze.board.models.Platform.class));
        
        com.colorcraze.board.dtos.responses.MoveResult stepResult = new com.colorcraze.board.dtos.responses.MoveResult(
            uuid1, 3, 1, 
            List.of(new com.colorcraze.board.dtos.responses.PlatformUpdate(2, 1, color1)),
            List.of(new com.colorcraze.board.dtos.responses.PlayerUpdate(uuid1, color1, 5)),
            true, true
        );
        
        when(mockBoard.movePlayer(uuid1, com.colorcraze.utils.enums.PlayerMove.UP))
        .thenReturn(stepResult)
        .thenReturn(null);

        
        var results = boardService.movePlayer(gameId, playerId1, com.colorcraze.utils.enums.PlayerMove.UP);
        
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).gravity());
        assertEquals(1, results.get(0).platforms().size());
        assertEquals(1, results.get(0).affectedPlayers().size());
        
        verify(mockBoard, times(2)).movePlayer(
                uuid1,
                com.colorcraze.utils.enums.PlayerMove.UP
            );
        verify(mockBoard).setPlayerIsUp(uuid1, true);
        verify(mockBoard).setPlayerIsUp(uuid1, false);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
        verify(redisTemplate).convertAndSend(eq("/topic/board"), any(Map.class));
    }

    @Test
    void movePlayer_BoardNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(boardKey)).thenReturn(null);
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> boardService.movePlayer(gameId, playerId1, com.colorcraze.utils.enums.PlayerMove.UP)
        );
        
        assertEquals("No board exists with id " + gameId, exception.getMessage());
        
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations, never()).set(anyString(), any());
    }

    @Test
    void addPlayerToBoard_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Board mockBoard = mock(Board.class);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        var result = boardService.addPlayerToBoard(gameId, color1);
        
        assertNotNull(result);
        assertEquals(color1, result.getColor());
        
        verify(mockBoard).addPlayer(any(com.colorcraze.board.models.Player.class));
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(boardKey);
        verify(valueOperations).set(boardKey, mockBoard);
    }

    @Test
    void getAllBoardIds_Success() {
        Set<String> keys = new HashSet<>();
        keys.add("board:game1");
        keys.add("board:game2");
        keys.add("other:key");
        
        when(redisTemplate.keys("board:*")).thenReturn(keys);
        
        Set<String> result = boardService.getAllBoardIds();
        
        assertNotNull(result);
        assertTrue(1< result.size());
        assertTrue(result.contains("board:game1"));
        assertTrue(result.contains("board:game2"));
        assertTrue(result.contains("other:key"));
        
        verify(redisTemplate).keys("board:*");
    }

    @Test
    void getAllBoardIds_Empty() {
        when(redisTemplate.keys("board:*")).thenReturn(new HashSet<>());
        
        Set<String> result = boardService.getAllBoardIds();
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(redisTemplate).keys("board:*");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void startGameTimer_Success() {

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(taskScheduler.scheduleAtFixedRate(runnableCaptor.capture(), any(Duration.class)))
            .thenReturn((ScheduledFuture) scheduledFuture);

        boardService.startGameTimer(gameId, 1);

        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        verify(messagingTemplate, atLeastOnce())
            .convertAndSend(eq("/topic/board." + gameId), any(Map.class));
    }

    @Test
    void startGameTimer_AlreadyExists_DoesNotStartNewTimer() {
        try {
            var timersField = BoardService.class.getDeclaredField("gameTimers");
            timersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ScheduledFuture<?>> timers = (Map<String, ScheduledFuture<?>>) timersField.get(boardService);
            timers.put(gameId, scheduledFuture);
        } catch (Exception e) {
            fail("Error accessing gameTimers field: " + e.getMessage());
        }
        
        boardService.startGameTimer(gameId, 60);
        
        verify(taskScheduler, never()).scheduleAtFixedRate(any(Runnable.class), any(Duration.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void startGameTimer_CountdownDecrementsCorrectly() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(taskScheduler.scheduleAtFixedRate(runnableCaptor.capture(), any(Duration.class)))
            .thenReturn((ScheduledFuture) scheduledFuture);
        
        boardService.startGameTimer(gameId, 3);
        
        Runnable scheduledTask = runnableCaptor.getValue();
        
        Map<String, Object> expectedMessage = new HashMap<>();
        expectedMessage.put("timeLeft", 2);
        
        scheduledTask.run();
        verify(messagingTemplate).convertAndSend(
            "/topic/board." + gameId,
            expectedMessage
        );

        expectedMessage.put("timeLeft", 1);
        scheduledTask.run();
        verify(messagingTemplate).convertAndSend(
                "/topic/board." + gameId,
                expectedMessage
            );
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void startGameTimer_ReachesZero_EndsGame() {
        when(boardTopic.getTopic()).thenReturn("/topic/board");
        
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(taskScheduler.scheduleAtFixedRate(runnableCaptor.capture(), any(Duration.class)))
            .thenReturn((ScheduledFuture) scheduledFuture);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board mockBoard = new Board(gameId, playerColors);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        boardService.startGameTimer(gameId, 0);
        
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();
        
        verify(messagingTemplate).convertAndSend(eq("/topic/board." + gameId), any(Map.class));
        verify(scheduledFuture).cancel(true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void startGameTimer_RemovesBoardAfterGameEnd() {
        when(boardTopic.getTopic()).thenReturn("/topic/board");
        
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(taskScheduler.scheduleAtFixedRate(runnableCaptor.capture(), any(Duration.class)))
            .thenReturn((ScheduledFuture) scheduledFuture);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board mockBoard = new Board(gameId, playerColors);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        boardService.startGameTimer(gameId, 0);
        
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();
        
        verify(redisTemplate).delete(boardKey);
    }
    
    @Test
    void endGame_Success() {
        when(boardTopic.getTopic()).thenReturn("/topic/board");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        Board mockBoard = new Board(gameId, playerColors);
        when(valueOperations.get(boardKey)).thenReturn(mockBoard);
        
        boardService.endGame(gameId);
        
        verify(messagingTemplate).convertAndSend(eq("/topic/board." + gameId), any(Map.class));
        verify(redisTemplate).convertAndSend(eq("/topic/board"), any(Map.class));
        verify(redisTemplate).delete(boardKey);
    }
}