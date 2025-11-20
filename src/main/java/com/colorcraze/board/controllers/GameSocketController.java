package com.colorcraze.board.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.colorcraze.board.dtos.requests.PlayerMoveMessage;
import com.colorcraze.board.dtos.responses.MoveResult;
import com.colorcraze.board.dtos.responses.PlatformUpdate;
import com.colorcraze.board.dtos.responses.PlayerUpdate;
import com.colorcraze.board.services.BoardService;
import com.colorcraze.utils.enums.PlayerMove;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final BoardService boardService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/move.{gameId}")
    public void handlePlayerMove(@DestinationVariable String gameId, @Payload PlayerMoveMessage moveMessage) {
        List<MoveResult> results = boardService.movePlayer(gameId, moveMessage.getPlayerId(), moveMessage.getDirection());

        CompletableFuture.runAsync(() -> {
            if (results == null || results.isEmpty()) return;

            for (MoveResult result : results) {
                try {
                    Thread.sleep(110);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                messagingTemplate.convertAndSend("/topic/board." + gameId, result);

                if (result.gravity()) {
                    applyGravity(gameId, moveMessage.getPlayerId());
                }
            }
        });
    }

    private void applyGravity(String gameId, String playerId) {
        List<PlatformUpdate> totalPlatformUpdates = new ArrayList<>();
        List<PlayerUpdate> totalPlayerUpdates = new ArrayList<>();
        MoveResult lastStep = null;

        while (shouldContinueFalling(lastStep)) {
            if (!sleepSafely()) {
                return;
            }

            List<MoveResult> gravityResults = boardService.movePlayer(gameId, playerId, PlayerMove.DOWN);
            if (!processGravityResults(gravityResults, totalPlatformUpdates, totalPlayerUpdates)) {
                break;
            }
            
            lastStep = getLastStepFromResults(gravityResults);
        }

        sendFinalResult(gameId, lastStep, totalPlatformUpdates, totalPlayerUpdates);
    }

    private boolean shouldContinueFalling(MoveResult lastStep) {
        return lastStep == null || lastStep.gravity();
    }

    private boolean sleepSafely() {
        try {
            Thread.sleep(100);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean processGravityResults(List<MoveResult> gravityResults, 
                                        List<PlatformUpdate> platformUpdates,
                                        List<PlayerUpdate> playerUpdates) {
        if (gravityResults == null || gravityResults.isEmpty()) {
            return false;
        }

        for (MoveResult gravityStep : gravityResults) {
            if (gravityStep != null) {
                platformUpdates.addAll(gravityStep.platforms());
                playerUpdates.addAll(gravityStep.affectedPlayers());
                
                if (!gravityStep.gravity()) {
                    return false;
                }
            }
        }
        return true;
    }

    private MoveResult getLastStepFromResults(List<MoveResult> gravityResults) {
        return gravityResults.stream()
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private void sendFinalResult(String gameId, MoveResult lastStep, 
                            List<PlatformUpdate> platformUpdates,
                            List<PlayerUpdate> playerUpdates) {
        if (lastStep != null) {
            MoveResult finalResult = new MoveResult(
                lastStep.playerId(),
                lastStep.newRow(),
                lastStep.newCol(),
                platformUpdates,
                playerUpdates,
                lastStep.success(),
                false
            );
            messagingTemplate.convertAndSend("/topic/board." + gameId, finalResult);
        }
    }
}
