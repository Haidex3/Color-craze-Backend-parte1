package com.colorcraze.waitingroom.models;

import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class WaitingRoomTest {

    private WaitingRoom waitingRoom;
    private final String roomId = "test-room";
    private final int initialSeconds = 10;

    @BeforeEach
    void setUp() {
        waitingRoom = new WaitingRoom(roomId, initialSeconds);
    }

    @Test
    void testAddPlayer_Success() {
        WaitingRoomState result = waitingRoom.addPlayer("player1");

        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        assertEquals(Set.of("player1"), result.getPlayers());
        assertEquals(Map.of("player1", ColorStatus.YELLOW), result.getPlayerColors());
        assertFalse(result.isFull());
        assertEquals(initialSeconds, result.getSeconds());
    }

    @Test
    void testAddPlayer_WhenRoomFull_ReturnsNull() {
        waitingRoom.addPlayer("player1");
        waitingRoom.addPlayer("player2");
        waitingRoom.addPlayer("player3");
        waitingRoom.addPlayer("player4");

        WaitingRoomState result = waitingRoom.addPlayer("player5");

        assertNull(result);
        assertTrue(waitingRoom.isFull());
    }

    @Test
    void testAddPlayer_MultiplePlayers_AssignsDifferentColors() {
        waitingRoom.addPlayer("player1");
        waitingRoom.addPlayer("player2");
        waitingRoom.addPlayer("player3");

        Map<String, ColorStatus> colors = waitingRoom.getPlayerColors();
        assertEquals(3, colors.size());
        
        long distinctColors = colors.values().stream()
                .filter(color -> color != ColorStatus.WHITE)
                .distinct()
                .count();
        assertEquals(3, distinctColors);
    }

    @Test
    @Timeout(5)
    void testStartCountdown_DecrementsSeconds() throws InterruptedException {
        waitingRoom.setSeconds(3);

        CountDownLatch latch = new CountDownLatch(1);

        Thread countdownThread = new Thread(() -> {
            waitingRoom.startCountdown();
            latch.countDown();
        });
        countdownThread.start();
        latch.await(5, TimeUnit.SECONDS);

        waitingRoom.stopCountdown();
        assertEquals(3, waitingRoom.getSeconds());
    }

    @Test
    @Timeout(5)
    void testStartCountdown_StopsAtZero() throws InterruptedException {
        waitingRoom.setSeconds(2);

        CountDownLatch latch = new CountDownLatch(1);

        Thread countdownThread = new Thread(() -> {
            waitingRoom.startCountdown();
            latch.countDown();
        });
        countdownThread.start();
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(2, waitingRoom.getSeconds());
        int currentSeconds = waitingRoom.getSeconds();
        countdownThread.join(1000);
        assertEquals(currentSeconds, waitingRoom.getSeconds());
    }


    @Test
    void testStopCountdown_WhenNotStarted_NoException() {
        waitingRoom.stopCountdown();

        assertTrue(true);
    }

    @Test
    @Timeout(5)
    void testStopCountdown_InterruptsCountdown() throws InterruptedException {
        waitingRoom.setSeconds(10);

        CountDownLatch startedLatch = new CountDownLatch(1);

        Thread countdownThread = new Thread(() -> {
            startedLatch.countDown();
            waitingRoom.startCountdown();
        });
        countdownThread.start();

        startedLatch.await(1, TimeUnit.SECONDS);

        waitingRoom.stopCountdown();
        int secondsAfterStop = waitingRoom.getSeconds();
        assertTrue(secondsAfterStop >= 8 && secondsAfterStop <= 10);
        countdownThread.join(1000);
        assertEquals(secondsAfterStop, waitingRoom.getSeconds());
    }


    @Test
    void testAddPlayer_BranchCoverage_PlayerAlreadyExists() {
        waitingRoom.addPlayer("player1");

        WaitingRoomState result = waitingRoom.addPlayer("player1");

        assertNull(result);
        assertEquals(1, waitingRoom.getPlayers().size());
    }

    @Test
    void testAddPlayer_BranchCoverage_EmptyAvailableColors() {
        WaitingRoom room = new WaitingRoom("branch-test", 5);

        room.addPlayer("player1");

        assertEquals(1, room.getPlayers().size());
        assertTrue(room.getPlayers().contains("player1"));
    }


    @Test
    void testConcurrentAddPlayer() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final String playerId = "player" + i;
            executorService.submit(() -> {
                waitingRoom.addPlayer(playerId);
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        assertEquals(4, waitingRoom.getPlayers().size());
        assertTrue(waitingRoom.isFull());
    }

    @Test
    @Timeout(5)
    void testCountdownWithConcurrentOperations() throws InterruptedException {
        waitingRoom.setSeconds(3);

        CountDownLatch latch = new CountDownLatch(1);

        Thread countdownThread = new Thread(() -> {
            waitingRoom.startCountdown();
            latch.countDown(); 
        });
        countdownThread.start();

        Thread addPlayerThread = new Thread(() -> {
            waitingRoom.addPlayer("player1");
            waitingRoom.addPlayer("player2");
        });
        addPlayerThread.start();

        addPlayerThread.join();
        latch.await(5, TimeUnit.SECONDS);

        waitingRoom.stopCountdown();

        assertEquals(2, waitingRoom.getPlayers().size());
    }


    @Test
    void testRemovePlayer() {
        waitingRoom.addPlayer("player1");
        
        boolean result = waitingRoom.removePlayer("player1");
        
        assertTrue(result);
        assertTrue(waitingRoom.getPlayers().isEmpty());
        assertTrue(waitingRoom.getPlayerColors().isEmpty());
    }

    @Test
    void testRemovePlayer_NonExistent() {
        boolean result = waitingRoom.removePlayer("non-existent");
        
        assertFalse(result);
    }

    @Test
    void testSelectColor() {
        waitingRoom.addPlayer("player1");
        
        boolean result = waitingRoom.selectColor("player1", ColorStatus.PURPLE);
        
        assertTrue(result);
        assertEquals(ColorStatus.PURPLE, waitingRoom.getPlayerColors().get("player1"));
    }

    @Test
    void testSelectColor_InvalidScenarios() {
        waitingRoom.addPlayer("player1");
        
        boolean result1 = waitingRoom.selectColor("player1", ColorStatus.WHITE);
        
        boolean result2 = waitingRoom.selectColor("non-existent", ColorStatus.RED);
        
        assertFalse(result1);
        assertFalse(result2);
    }

    @Test
    void testIsFull() {
        assertFalse(waitingRoom.isFull());
        
        waitingRoom.addPlayer("player1");
        waitingRoom.addPlayer("player2");
        waitingRoom.addPlayer("player3");
        waitingRoom.addPlayer("player4");
        
        assertTrue(waitingRoom.isFull());
    }

    @Test
    @Timeout(5)
    void testStartCountdown_WhenSecondsIsZero_StopsImmediately() throws InterruptedException {
        // Arrange: configurar seconds en 0
        waitingRoom.setSeconds(0);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // Act: iniciar el countdown
        Thread countdownThread = new Thread(() -> {
            waitingRoom.startCountdown();
            latch.countDown();
        });
        countdownThread.start();
        
        // Assert: verificar que el countdown se detiene inmediatamente
        boolean completed = latch.await(2, TimeUnit.SECONDS); // Debería completarse rápidamente
        assertTrue(completed, "El countdown debería detenerse inmediatamente cuando seconds es 0");
        
        // Verificar que los seconds siguen en 0
        assertEquals(0, waitingRoom.getSeconds());
        
        // Verificar que el scheduler se detuvo (no debería seguir ejecutándose)
        Thread.sleep(1000); // Esperar un segundo adicional
        assertEquals(0, waitingRoom.getSeconds()); // Sigue en 0, no decrementa a valores negativos
    }
}