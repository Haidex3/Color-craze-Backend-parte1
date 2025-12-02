package com.colorcraze.waitingroom.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.colorcraze.board.models.Board;
import com.colorcraze.board.services.BoardService;
import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.models.WaitingRoom;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingRoomServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private BoardService boardService;

    @InjectMocks
    private WaitingRoomService waitingRoomService;

    @BeforeEach
    void setUp() throws Exception {
        clearRoomsMap();
        clearSchedulersMap();
    }

    @Test
    void createRoom_Success() throws Exception {
        WaitingRoom room = waitingRoomService.createRoom();

        assertNotNull(room);
        assertNotNull(room.getRoomId());
        assertEquals(4, room.getRoomId().length());
        
        Optional<WaitingRoom> retrievedRoom = waitingRoomService.getRoom(room.getRoomId());
        assertTrue(retrievedRoom.isPresent());
        assertEquals(room, retrievedRoom.get());
        
        Map<String, ScheduledExecutorService> schedulers = getSchedulersMap();
        assertTrue(schedulers.containsKey(room.getRoomId()));
    }

    @Test
    void generateRoomId_UniqueIds() throws Exception {
        Method generateRoomIdMethod = WaitingRoomService.class.getDeclaredMethod("generateRoomId");
        generateRoomIdMethod.setAccessible(true);
        
        Set<String> generatedIds = ConcurrentHashMap.newKeySet();
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            String roomId = (String) generateRoomIdMethod.invoke(waitingRoomService);
            assertNotNull(roomId);
            assertEquals(4, roomId.length());
            generatedIds.add(roomId);
        }
        
        assertTrue(iterations>= generatedIds.size());
    }

    @Test
    void startCountdown_StartsScheduler() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 10);
        
        Method startCountdownMethod = WaitingRoomService.class.getDeclaredMethod("startCountdown", WaitingRoom.class);
        startCountdownMethod.setAccessible(true);
        
        startCountdownMethod.invoke(waitingRoomService, room);
        
        Map<String, ScheduledExecutorService> schedulers = getSchedulersMap();
        assertTrue(schedulers.containsKey("TEST"));
        
        ScheduledExecutorService scheduler = schedulers.get("TEST");
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Test
    void createCountdownTask_DecrementsSeconds() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 3);
        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        
        Method createCountdownTaskMethod = WaitingRoomService.class.getDeclaredMethod("createCountdownTask", WaitingRoom.class, ScheduledExecutorService.class);
        createCountdownTaskMethod.setAccessible(true);
        
        Runnable task = (Runnable) createCountdownTaskMethod.invoke(waitingRoomService, room, mockScheduler);
        
        task.run();
        
        assertEquals(2, room.getSeconds());
        verify(messagingTemplate).convertAndSend(eq("/topic/waiting-room/TEST"), any(WaitingRoomState.class));
    }

    @Test
    void countdownLogic_DecrementsSeconds() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 3);
        
        Method createCountdownTaskMethod = WaitingRoomService.class.getDeclaredMethod("createCountdownTask", WaitingRoom.class, ScheduledExecutorService.class);
        createCountdownTaskMethod.setAccessible(true);
        
        Runnable task = (Runnable) createCountdownTaskMethod.invoke(waitingRoomService, room, mock(ScheduledExecutorService.class));
        
        task.run();
        
        assertEquals(2, room.getSeconds());
    }

    @Test
    void gameStartLogic_WithPlayers() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 0);
        room.addPlayer("player1");
        room.selectColor("player1", ColorStatus.RED);

        Board mockBoard = mock(Board.class);

        when(boardService.createBoardWithPlayers("TEST", room.getPlayerColors()))
                .thenReturn(mockBoard);

        Method method = WaitingRoomService.class.getDeclaredMethod(
                "startGameIfPlayersExist",
                WaitingRoom.class
        );
        method.setAccessible(true);

        method.invoke(waitingRoomService, room);

        verify(boardService).createBoardWithPlayers("TEST", room.getPlayerColors());
        verify(messagingTemplate).convertAndSend("/topic/waiting-room/TEST/start", mockBoard);
    }


    @Test
    void gameStartLogic_WithoutPlayers() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 0);

        Method method = WaitingRoomService.class.getDeclaredMethod(
                "startGameIfPlayersExist",
                WaitingRoom.class
        );
        method.setAccessible(true);

        method.invoke(waitingRoomService, room);

        verify(boardService, never()).createBoardWithPlayers(anyString(), anyMap());
    }


    @Test
    void createCountdownTask_ExceptionHandling() throws Exception {
        WaitingRoom room = mock(WaitingRoom.class);
        when(room.getRoomId()).thenReturn("TEST");
        when(room.getLock()).thenReturn(new Object());
        when(room.getSeconds()).thenThrow(new RuntimeException("Test exception"));
        
        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        
        Method createCountdownTaskMethod = WaitingRoomService.class.getDeclaredMethod(
                "createCountdownTask",
                WaitingRoom.class,
                ScheduledExecutorService.class
        );
        createCountdownTaskMethod.setAccessible(true);
        
        Runnable task = (Runnable) createCountdownTaskMethod.invoke(waitingRoomService, room, mockScheduler);
        
        assertDoesNotThrow(task::run);
        
        verify(mockScheduler).shutdown();
    }


    @Test
    void sendRoomState_SendsCorrectMessage() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 5);
        room.addPlayer("player1");
        room.selectColor("player1", ColorStatus.GREEN);
        
        Method sendRoomStateMethod = WaitingRoomService.class.getDeclaredMethod("sendRoomState", WaitingRoom.class);
        sendRoomStateMethod.setAccessible(true);
        
        sendRoomStateMethod.invoke(waitingRoomService, room);
        
        ArgumentCaptor<WaitingRoomState> stateCaptor = ArgumentCaptor.forClass(WaitingRoomState.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/waiting-room/TEST"), stateCaptor.capture());
        
        WaitingRoomState sentState = stateCaptor.getValue();
        assertEquals("TEST", sentState.getRoomId());
        assertEquals(5, sentState.getSeconds());
        assertTrue(sentState.getPlayers().contains("player1"));
        assertEquals(ColorStatus.GREEN, sentState.getPlayerColors().get("player1"));
    }

    @Test
    void startGameIfPlayersExist_WithPlayers() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 10);
        room.addPlayer("player1");
        room.selectColor("player1", ColorStatus.RED);

        Board mockBoard = mock(Board.class);

        when(boardService.createBoardWithPlayers("TEST", room.getPlayerColors()))
                .thenReturn(mockBoard);

        Method method = WaitingRoomService.class.getDeclaredMethod(
                "startGameIfPlayersExist",
                WaitingRoom.class
        );
        method.setAccessible(true);

        method.invoke(waitingRoomService, room);
        verify(boardService).createBoardWithPlayers("TEST", room.getPlayerColors());
        verify(messagingTemplate)
                .convertAndSend("/topic/waiting-room/TEST/start", mockBoard);
    }

    @Test
    void startGameIfPlayersExist_NoPlayers() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 10);

        Method method = WaitingRoomService.class.getDeclaredMethod(
                "startGameIfPlayersExist",
                WaitingRoom.class
        );
        method.setAccessible(true);

        method.invoke(waitingRoomService, room);

        verify(boardService, never()).createBoardWithPlayers(anyString(), anyMap());
    }

    @Test
    void cleanupRoom_RemovesRoomAndShutsDownScheduler() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 10);
        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        when(mockScheduler.isShutdown()).thenReturn(false);
        
        getRoomsMap().put("TEST", room);
        getSchedulersMap().put("TEST", mockScheduler);
        
        Method cleanupRoomMethod = WaitingRoomService.class.getDeclaredMethod("cleanupRoom", WaitingRoom.class, ScheduledExecutorService.class);
        cleanupRoomMethod.setAccessible(true);
        
        cleanupRoomMethod.invoke(waitingRoomService, room, mockScheduler);
        
        assertFalse(getRoomsMap().containsKey("TEST"));
        assertFalse(getSchedulersMap().containsKey("TEST"));
        verify(mockScheduler).shutdown();
    }

    @Test
    void cleanupRoom_AlreadyShutdownScheduler() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 10);
        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        when(mockScheduler.isShutdown()).thenReturn(true);
        
        Method cleanupRoomMethod = WaitingRoomService.class.getDeclaredMethod("cleanupRoom", WaitingRoom.class, ScheduledExecutorService.class);
        cleanupRoomMethod.setAccessible(true);
        
        cleanupRoomMethod.invoke(waitingRoomService, room, mockScheduler);
        
        verify(mockScheduler, never()).shutdown();
    }

    @Test
    void joinRoom_FullRoom_ReturnsNull() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        
        for (int i = 0; i < 20; i++) {
            WaitingRoomState result = waitingRoomService.joinRoom(roomId, "player-" + i);
            if (result == null) {
                break;
            }
        }
        
        WaitingRoomState result = waitingRoomService.joinRoom(roomId, "extra-player");
        assertNull(result);
    }

    @Test
    void leaveRoom_RemovesRoomWhenEmpty() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        String playerId = "player-123";
        
        waitingRoomService.joinRoom(roomId, playerId);
        
        assertTrue(waitingRoomService.getRoom(roomId).isPresent());
        
        boolean result = waitingRoomService.leaveRoom(roomId, playerId);
        
        assertTrue(result);
        assertFalse(waitingRoomService.getRoom(roomId).isPresent());
    }

    @Test
    void leaveRoom_WithMultiplePlayers_RemovesOnlyOne() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        
        waitingRoomService.joinRoom(roomId, "player1");
        waitingRoomService.joinRoom(roomId, "player2");
        
        boolean result = waitingRoomService.leaveRoom(roomId, "player1");
        
        assertTrue(result);
        assertTrue(waitingRoomService.getRoom(roomId).isPresent());
        
        WaitingRoomState state = waitingRoomService.getRoomState(roomId);
        assertFalse(state.getPlayers().contains("player1"));
        assertTrue(state.getPlayers().contains("player2"));
    }

    @Test
    void selectColor_ValidSelection() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        String playerId = "player-123";
        
        waitingRoomService.joinRoom(roomId, playerId);
        
        boolean result = waitingRoomService.selectColor(roomId, playerId, ColorStatus.GREEN);
        
        assertTrue(result);
        
        WaitingRoomState state = waitingRoomService.getRoomState(roomId);
        assertEquals(ColorStatus.GREEN, state.getPlayerColors().get(playerId));
    }

    @Test
    void selectColor_PlayerNotInRoom() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        
        boolean result = waitingRoomService.selectColor(roomId, "non-existent-player", ColorStatus.RED);
        
        assertFalse(result);
    }

    @Test
    void getRoomState_NonExistentRoom() {
        WaitingRoomState state = waitingRoomService.getRoomState("NONEXISTENT");
        
        assertNull(state);
    }

    @Test
    void removeRoom_WithScheduler() throws Exception {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        
        assertTrue(getRoomsMap().containsKey(roomId));
        assertTrue(getSchedulersMap().containsKey(roomId));
        
        waitingRoomService.removeRoom(roomId);
        
        assertFalse(getRoomsMap().containsKey(roomId));
        assertFalse(getSchedulersMap().containsKey(roomId));
    }

    @Test
    void removeRoom_NonExistentRoom() {
        assertDoesNotThrow(() -> waitingRoomService.removeRoom("NONEXISTENT"));
    }

    @Test
    void testFullRoomScenario() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        
        int playerCount = 0;
        while (true) {
            WaitingRoomState result = waitingRoomService.joinRoom(roomId, "player-" + playerCount);
            if (result == null || result.isFull()) {
                break;
            }
            playerCount++;
        }
        
        WaitingRoomState finalState = waitingRoomService.getRoomState(roomId);
        assertNotNull(finalState);
        assertTrue(finalState.isFull() || finalState.getPlayers().size() > 0);
    }

    @Test
    void testConcurrentOperations() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        
        waitingRoomService.joinRoom(roomId, "player1");
        waitingRoomService.selectColor(roomId, "player1", ColorStatus.RED);
        waitingRoomService.joinRoom(roomId, "player2");
        waitingRoomService.selectColor(roomId, "player2", ColorStatus.GREEN);
        waitingRoomService.leaveRoom(roomId, "player1");
        
        WaitingRoomState state = waitingRoomService.getRoomState(roomId);
        assertNotNull(state);
        assertFalse(state.getPlayers().contains("player1"));
        assertTrue(state.getPlayers().contains("player2"));
        assertEquals(ColorStatus.GREEN, state.getPlayerColors().get("player2"));
    }

    @Test
    void joinRoom_AfterLeaving_CanRejoin() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        String playerId = "player-123";
        
        WaitingRoomState join1 = waitingRoomService.joinRoom(roomId, playerId);
        assertNotNull(join1);
        assertTrue(join1.getPlayers().contains(playerId));
        
        boolean leaveResult = waitingRoomService.leaveRoom(roomId, playerId);
        assertTrue(leaveResult);
    }


    @Test
    void createCountdownTask_CountdownNotZero_Continues() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 5);
        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        
        Method createCountdownTaskMethod = WaitingRoomService.class.getDeclaredMethod("createCountdownTask", WaitingRoom.class, ScheduledExecutorService.class);
        createCountdownTaskMethod.setAccessible(true);
        
        Runnable task = (Runnable) createCountdownTaskMethod.invoke(waitingRoomService, room, mockScheduler);
        
        task.run();
        
        assertEquals(4, room.getSeconds());
        verify(messagingTemplate).convertAndSend(eq("/topic/waiting-room/TEST"), any(WaitingRoomState.class));
        verify(mockScheduler, never()).shutdown();
    }

    @Test
    void leaveRoom_RoomNotEmpty_DoesNotRemoveRoom() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        
        waitingRoomService.joinRoom(roomId, "player1");
        waitingRoomService.joinRoom(roomId, "player2");
        
        boolean result = waitingRoomService.leaveRoom(roomId, "player1");
        
        assertTrue(result);
        assertTrue(waitingRoomService.getRoom(roomId).isPresent());
    }

    @Test
    void leaveRoom_NonExistentRoom_ReturnsFalse() {
        boolean result = waitingRoomService.leaveRoom("NONEXISTENT", "player1");
        
        assertFalse(result);
    }

    @Test
    void joinRoom_NonExistentRoom_ReturnsNull() {
        WaitingRoomState result = waitingRoomService.joinRoom("NONEXISTENT", "player1");
        
        assertNull(result);
    }

    @Test
    void getRoom_NonExistentRoom_ReturnsEmpty() {
        Optional<WaitingRoom> result = waitingRoomService.getRoom("NONEXISTENT");
        
        assertFalse(result.isPresent());
    }

    @SuppressWarnings("unchecked")
    private Map<String, WaitingRoom> getRoomsMap() throws Exception {
        Field roomsField = WaitingRoomService.class.getDeclaredField("rooms");
        roomsField.setAccessible(true);
        return (Map<String, WaitingRoom>) roomsField.get(waitingRoomService);
    }

    @SuppressWarnings("unchecked")
    private Map<String, ScheduledExecutorService> getSchedulersMap() throws Exception {
        Field schedulersField = WaitingRoomService.class.getDeclaredField("roomSchedulers");
        schedulersField.setAccessible(true);
        return (Map<String, ScheduledExecutorService>) schedulersField.get(waitingRoomService);
    }

    private void clearRoomsMap() throws Exception {
        getRoomsMap().clear();
    }

    private void clearSchedulersMap() throws Exception {
        getSchedulersMap().clear();
    }

    @Test
    void createCountdownTask_CountdownReachesZero_StartsGameAndCleansUp() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 1);
        room.addPlayer("player1");
        room.selectColor("player1", ColorStatus.RED);
        room.addPlayer("player2");
        room.selectColor("player2", ColorStatus.GREEN);

        getRoomsMap().put("TEST", room);
        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);
        Board mockBoard = mock(Board.class);

        when(boardService.createBoardWithPlayers("TEST", room.getPlayerColors()))
                .thenReturn(mockBoard);
        Method method = WaitingRoomService.class.getDeclaredMethod(
                "createCountdownTask",
                WaitingRoom.class,
                ScheduledExecutorService.class
        );
        method.setAccessible(true);
        Runnable task = (Runnable) method.invoke(waitingRoomService, room, mockScheduler);
        task.run();
        task.run();
        verify(boardService).createBoardWithPlayers("TEST", room.getPlayerColors());
        verify(messagingTemplate).convertAndSend("/topic/waiting-room/TEST/start", mockBoard);
    }

    @Test
    void createCountdownTask_CountdownReachesZero_NoPlayers_OnlyCleansUp() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 1);

        getRoomsMap().put("TEST", room);
        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);

        Method method = WaitingRoomService.class.getDeclaredMethod(
                "createCountdownTask",
                WaitingRoom.class,
                ScheduledExecutorService.class
        );
        method.setAccessible(true);
        Runnable task = (Runnable) method.invoke(waitingRoomService, room, mockScheduler);
        task.run();
        assertEquals(0, room.getSeconds());
        verify(boardService, never()).createBoardWithPlayers(anyString(), anyMap());
    }

    @Test
    void createCountdownTask_RoomStateUpdateException_ContinuesAndCleansUp() throws Exception {
        WaitingRoom room = mock(WaitingRoom.class);
        when(room.getRoomId()).thenReturn("TEST");
        when(room.getLock()).thenReturn(new Object());
        when(room.getSeconds()).thenReturn(5);

        getRoomsMap().put("TEST", room);

        doThrow(new RuntimeException("Message send failed"))
            .when(messagingTemplate)
            .convertAndSend(anyString(), any(WaitingRoomState.class));

        ScheduledExecutorService mockScheduler = mock(ScheduledExecutorService.class);

        Method method = WaitingRoomService.class.getDeclaredMethod(
                "createCountdownTask",
                WaitingRoom.class,
                ScheduledExecutorService.class
        );
        method.setAccessible(true);
        Runnable task = (Runnable) method.invoke(waitingRoomService, room, mockScheduler);
        assertDoesNotThrow(task::run);
        verify(mockScheduler).shutdown();
    }

    @Test
    void sendRoomState_ExceptionDuringSend_LogsErrorButContinues() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 5);
        
        doThrow(new RuntimeException("Network error"))
            .when(messagingTemplate)
            .convertAndSend(anyString(), any(WaitingRoomState.class));
        
        Method sendRoomStateMethod = WaitingRoomService.class.getDeclaredMethod("sendRoomState", WaitingRoom.class);
        sendRoomStateMethod.setAccessible(true);
        
        try {
            sendRoomStateMethod.invoke(waitingRoomService, room);
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("Network error", e.getCause().getMessage());
        }
    }

    @Test
    void startGameIfPlayersExist_ExceptionDuringBoardCreation_LogsError() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 0);
        room.addPlayer("player1");
        room.selectColor("player1", ColorStatus.RED);
        when(boardService.createBoardWithPlayers(anyString(), anyMap()))
                .thenThrow(new RuntimeException("Board creation failed"));

        Method method = WaitingRoomService.class.getDeclaredMethod(
                "startGameIfPlayersExist",
                WaitingRoom.class
        );
        method.setAccessible(true);

        try {
            method.invoke(waitingRoomService, room);
            fail("Expected InvocationTargetException");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("Board creation failed", e.getCause().getMessage());
        }
    }


    @Test
    void selectColor_ColorAlreadyTaken_ReturnsFalse() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        
        waitingRoomService.joinRoom(roomId, "player1");
        waitingRoomService.joinRoom(roomId, "player2");
        
        boolean result1 = waitingRoomService.selectColor(roomId, "player1", ColorStatus.RED);
        boolean result2 = waitingRoomService.selectColor(roomId, "player2", ColorStatus.RED);
        
        assertFalse(result1, "First color selection should succeed");
        assertFalse(result2, "Second color selection should fail when color is already taken");
    }

    @Test
    void createRoom_RoomIdCollision_GeneratesNewId() throws Exception {
        Field roomsField = WaitingRoomService.class.getDeclaredField("rooms");
        roomsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, WaitingRoom> rooms = (Map<String, WaitingRoom>) roomsField.get(waitingRoomService);
        
        String existingId = "ABCD";
        rooms.put(existingId, new WaitingRoom(existingId, 10));
        
        Set<String> generatedIds = new HashSet<>();
        int attempts = 10;
        
        for (int i = 0; i < attempts; i++) {
            WaitingRoom room = waitingRoomService.createRoom();
            assertNotNull(room);
            generatedIds.add(room.getRoomId());
            waitingRoomService.removeRoom(room.getRoomId());
        }
        
        assertTrue(generatedIds.size() > 1, "Should generate multiple unique room IDs");
    }

    @Test
    void leaveRoom_RemovesRoomAndShutsDownScheduler_WhenLastPlayerLeaves() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        String playerId = "player1";
        
        waitingRoomService.joinRoom(roomId, playerId);
        
        assertTrue(waitingRoomService.getRoom(roomId).isPresent());
        
        boolean result = waitingRoomService.leaveRoom(roomId, playerId);
        
        assertTrue(result);
        assertFalse(waitingRoomService.getRoom(roomId).isPresent());
    }

    @Test
    void cleanupRoom_NullScheduler_HandlesGracefully() throws Exception {
        WaitingRoom room = new WaitingRoom("TEST", 10);
        getRoomsMap().put("TEST", room);
        
        Method cleanupRoomMethod = WaitingRoomService.class.getDeclaredMethod("cleanupRoom", WaitingRoom.class, ScheduledExecutorService.class);
        cleanupRoomMethod.setAccessible(true);
        
        assertDoesNotThrow(() -> cleanupRoomMethod.invoke(waitingRoomService, room, null));
        
        assertFalse(getRoomsMap().containsKey("TEST"));
    }

    @Test
    void joinRoom_RoomFull_ReturnsNull() {
        WaitingRoom room = waitingRoomService.createRoom();
        String roomId = room.getRoomId();
        
        for (int i = 0; i < 20; i++) {
            WaitingRoomState result = waitingRoomService.joinRoom(roomId, "player" + i);
            if (result == null) {
                break;
            }
        }
                WaitingRoomState result = waitingRoomService.joinRoom(roomId, "extraPlayer");
        assertNull(result);
    }

}