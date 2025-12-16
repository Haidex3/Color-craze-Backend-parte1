package com.colorcraze.board.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * Controller responsible for handling WebSocket messages for game moves.
 * Receives player move messages and broadcasts board updates to clients.
 */
@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final BoardService boardService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GameSocketController.class);

    /**
     * Handles a player move received via WebSocket.
     * Processes the move, applies gravity effects if necessary, and broadcasts updates.
     *
     * @param gameId the ID of the game
     * @param moveMessage the message containing player ID and move direction
     */
    @MessageMapping("/move.{gameId}")
    public void handlePlayerMove(
        @DestinationVariable @NotBlank @Pattern(regexp = "^[a-zA-Z0-9-]+$") String gameId,
        @Valid @Payload PlayerMoveMessage moveMessage) {
        try {
            List<MoveResult> results =
                    boardService.movePlayer(gameId, moveMessage.getPlayerId(), moveMessage.getDirection());

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

        } catch (IllegalStateException e) {
            log.warn("Move ignored: {}", e.getMessage());

            messagingTemplate.convertAndSend(
                "/topic/errors." + gameId,
                Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            log.error("Unexpected error in WebSocket move handler: {}", e.getMessage());
        }
    }


    /**
     * Applies gravity logic for a player, updating the board state and sending final results.
     *
     * @param gameId the ID of the game
     * @param playerId the ID of the player affected by gravity
     */
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
                lastStep = getLastStepFromResults(gravityResults);
                break;
            }
            
            lastStep = getLastStepFromResults(gravityResults);
        }

        sendFinalResult(gameId, lastStep, totalPlatformUpdates, totalPlayerUpdates);
    }

    /**
     * Determines if the player should continue falling based on the last move result.
     *
     * @param lastStep the previous move result
     * @return true if the player should keep falling, false otherwise
     */
    private boolean shouldContinueFalling(MoveResult lastStep) {
        return lastStep == null || lastStep.gravity();
    }

    /**
     * Sleeps the current thread safely for a short interval to pace gravity steps.
     *
     * @return true if sleep completed successfully, false if interrupted
     */
    private boolean sleepSafely() {
        try {
            Thread.sleep(100);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Processes gravity move results, collecting platform and player updates.
     *
     * @param gravityResults the list of gravity move results
     * @param platformUpdates accumulator for platform updates
     * @param playerUpdates accumulator for player updates
     * @return true if the player should continue falling, false otherwise
     */
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

    /**
     * Retrieves the last step from a list of move results.
     *
     * @param gravityResults the list of gravity move results
     * @return the last non-null move result, or null if none exist
     */
    private MoveResult getLastStepFromResults(List<MoveResult> gravityResults) {
        return gravityResults.stream()
                .filter(Objects::nonNull)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
    * Sends the final gravity result to all subscribed clients for the game.
    *
    * @param gameId the ID of the game
    * @param lastStep the last move result after gravity
    * @param platformUpdates the accumulated platform updates
    * @param playerUpdates the accumulated player updates
    */
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
