package com.colorcraze.board.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.colorcraze.board.dtos.requests.PlayerMoveMessage;
import com.colorcraze.board.dtos.responses.MoveResult;
import com.colorcraze.board.dtos.responses.PlatformUpdate;
import com.colorcraze.board.dtos.responses.PlayerUpdate;
import com.colorcraze.board.services.BoardService;
import com.colorcraze.utils.enums.ColorStatus;
import com.colorcraze.utils.enums.PlayerMove;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameSocketControllerTest {

    @Mock
    private BoardService boardService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameSocketController gameSocketController;

    @Test
    void handlePlayerMove_SuccessWithGravityChain() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.RIGHT);

        List<MoveResult> initialResults = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 2, true, true)
        );

        List<MoveResult> gravityResults = List.of(
            createMoveResult(UUID.fromString(playerId), 2, 2, true, false)
        );

        when(boardService.movePlayer(gameId, playerId, PlayerMove.RIGHT)).thenReturn(initialResults);
        when(boardService.movePlayer(gameId, playerId, PlayerMove.DOWN)).thenReturn(gravityResults);

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.RIGHT);
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.DOWN);
            verify(messagingTemplate, atLeast(1)).convertAndSend(eq("/topic/board." + gameId), any(MoveResult.class));
        });
    }

    @Test
    void handlePlayerMove_SuccessWithoutGravity() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.LEFT);

        List<MoveResult> results = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 0, true, false)
        );
        
        when(boardService.movePlayer(gameId, playerId, PlayerMove.LEFT)).thenReturn(results);

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.LEFT);
            verify(messagingTemplate).convertAndSend("/topic/board." + gameId, results.get(0));
        });
    }

    @Test
    void handlePlayerMove_NullOrEmptyResults() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.UP);

        when(boardService.movePlayer(gameId, playerId, PlayerMove.UP)).thenReturn(null);

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        verify(boardService).movePlayer(gameId, playerId, PlayerMove.UP);
        verifyNoInteractions(messagingTemplate);

        when(boardService.movePlayer(gameId, playerId, PlayerMove.DOWN)).thenReturn(List.of());
        moveMessage.setDirection(PlayerMove.DOWN);

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        verify(boardService).movePlayer(gameId, playerId, PlayerMove.DOWN);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void handlePlayerMove_MultipleGravitySteps() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.RIGHT);

        List<MoveResult> initialResults = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 2, true, true)
        );

        List<MoveResult> firstGravityResults = List.of(
            createMoveResult(UUID.fromString(playerId), 2, 2, true, true)
        );
        
        List<MoveResult> secondGravityResults = List.of(
            createMoveResult(UUID.fromString(playerId), 3, 2, true, false)
        );

        when(boardService.movePlayer(gameId, playerId, PlayerMove.RIGHT)).thenReturn(initialResults);
        when(boardService.movePlayer(gameId, playerId, PlayerMove.DOWN))
            .thenReturn(firstGravityResults)
            .thenReturn(secondGravityResults);

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.RIGHT);
            verify(boardService, times(2)).movePlayer(gameId, playerId, PlayerMove.DOWN);
        });
    }

    @Test
    void handlePlayerMove_GravityWithNullResults() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.RIGHT);

        List<MoveResult> initialResults = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 2, true, true)
        );

        when(boardService.movePlayer(gameId, playerId, PlayerMove.RIGHT)).thenReturn(initialResults);
        when(boardService.movePlayer(gameId, playerId, PlayerMove.DOWN)).thenReturn(null);

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.RIGHT);
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.DOWN);
        });
    }

    @Test
    void handlePlayerMove_GravityWithEmptyResults() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.RIGHT);

        List<MoveResult> initialResults = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 2, true, true)
        );

        when(boardService.movePlayer(gameId, playerId, PlayerMove.RIGHT)).thenReturn(initialResults);
        when(boardService.movePlayer(gameId, playerId, PlayerMove.DOWN)).thenReturn(List.of());

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.RIGHT);
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.DOWN);
        });
    }

    @Test
    void handlePlayerMove_ThreadInterruptionDuringSleep() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.RIGHT);

        List<MoveResult> results = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 2, true, true)
        );

        when(boardService.movePlayer(gameId, playerId, PlayerMove.RIGHT)).thenReturn(results);

        Thread.currentThread().interrupt();

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        Thread.interrupted();

        verify(boardService).movePlayer(gameId, playerId, PlayerMove.RIGHT);
    }

    @Test
    void handlePlayerMove_GravityWithPartialNullResults() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.RIGHT);

        List<MoveResult> initialResults = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 2, true, true)
        );

        List<MoveResult> gravityResults = new ArrayList<>();
        gravityResults.add(createMoveResult(UUID.fromString(playerId), 2, 2, true, true));
        gravityResults.add(null);

        when(boardService.movePlayer(gameId, playerId, PlayerMove.RIGHT)).thenReturn(initialResults);
        when(boardService.movePlayer(gameId, playerId, PlayerMove.DOWN)).thenReturn(gravityResults);

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.RIGHT);
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.DOWN);
        });
    }

    @Test
    void handlePlayerMove_FinalResultWithAccumulatedUpdates() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.RIGHT);

        List<PlatformUpdate> platformUpdates = List.of(new PlatformUpdate(1, 2, ColorStatus.GREEN));
        List<PlayerUpdate> playerUpdates = List.of(new PlayerUpdate(UUID.randomUUID(), ColorStatus.GREEN, 3));

        List<MoveResult> initialResults = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 2, true, true)
        );

        List<MoveResult> gravityResults = List.of(
            new MoveResult(
                UUID.fromString(playerId),
                2, 
                2,
                platformUpdates,
                playerUpdates,
                true,
                false
            )
        );

        when(boardService.movePlayer(gameId, playerId, PlayerMove.RIGHT)).thenReturn(initialResults);
        when(boardService.movePlayer(gameId, playerId, PlayerMove.DOWN)).thenReturn(gravityResults);

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.RIGHT);
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.DOWN);
            verify(messagingTemplate, atLeast(1)).convertAndSend(eq("/topic/board." + gameId), any(MoveResult.class));
        });
    }

    @Test
    void handlePlayerMove_SingleResultWithGravityButNoFurtherMovement() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.RIGHT);

        List<MoveResult> results = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 2, true, true)
        );

        when(boardService.movePlayer(gameId, playerId, PlayerMove.RIGHT)).thenReturn(results);
        when(boardService.movePlayer(gameId, playerId, PlayerMove.DOWN)).thenReturn(null);

        gameSocketController.handlePlayerMove(gameId, moveMessage);

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.RIGHT);
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.DOWN);
        });
    }

    @Test
    void handlePlayerMove_InterruptedExceptionInMainLoop() {
        String gameId = "test-game";
        String playerId = UUID.randomUUID().toString();
        PlayerMoveMessage moveMessage = new PlayerMoveMessage();
        moveMessage.setPlayerId(playerId);
        moveMessage.setDirection(PlayerMove.RIGHT);

        List<MoveResult> results = List.of(
            createMoveResult(UUID.fromString(playerId), 1, 2, true, true),
            createMoveResult(UUID.fromString(playerId), 1, 3, true, false)
        );

        when(boardService.movePlayer(gameId, playerId, PlayerMove.RIGHT)).thenReturn(results);

        gameSocketController.handlePlayerMove(gameId, moveMessage);
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(boardService).movePlayer(gameId, playerId, PlayerMove.RIGHT);
        });
    }

    private MoveResult createMoveResult(UUID playerId, int row, int col, boolean success, boolean gravity) {
        return new MoveResult(
            playerId,
            row,
            col,
            new ArrayList<>(),
            new ArrayList<>(),
            success,
            gravity
        );
    }
}