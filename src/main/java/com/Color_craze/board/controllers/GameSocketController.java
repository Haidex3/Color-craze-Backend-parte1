package com.Color_craze.board.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.Color_craze.board.dtos.Requests.PlayerMoveMessage;
import com.Color_craze.board.dtos.Responses.MoveResult;
import com.Color_craze.board.dtos.Responses.PlatformUpdate;
import com.Color_craze.board.dtos.Responses.PlayerUpdate;
import com.Color_craze.board.services.BoardService;
import com.Color_craze.utils.enums.PlayerMove;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class GameSocketController {

    private final BoardService boardService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/move.{gameId}")
    public void handlePlayerMove(@DestinationVariable String gameId, @Payload PlayerMoveMessage moveMessage) {
        System.out.println("Received move from player " + moveMessage.getPlayerId() + " in game " + gameId + " to " + moveMessage.getDirection());
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
                System.out.println("si salio el mensaje " + gameId);
                messagingTemplate.convertAndSend("/topic/board." + gameId, result);

                if (result.gravity()) {
                    applyGravity(gameId, moveMessage.getPlayerId());
                }
            }
        });
    }

    /**
     * Aplica gravedad acumulada y envía un único resultado final.
     */
    private void applyGravity(String gameId, String playerId) {
        boolean continueFalling = true;
        List<PlatformUpdate> totalPlatformUpdates = new ArrayList<>();
        List<PlayerUpdate> totalPlayerUpdates = new ArrayList<>();
        MoveResult lastStep = null;

        while (continueFalling) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            List<MoveResult> gravityResults = boardService.movePlayer(gameId, playerId, PlayerMove.DOWN);
            if (gravityResults == null || gravityResults.isEmpty()) break;

            for (MoveResult gravityStep : gravityResults) {
                if (gravityStep == null) continue;
                totalPlatformUpdates.addAll(gravityStep.platforms());
                totalPlayerUpdates.addAll(gravityStep.affectedPlayers());
                lastStep = gravityStep;

                if (!gravityStep.gravity()) {
                    continueFalling = false;
                    break;
                }
            }
        }

        if (lastStep != null) {
            MoveResult finalResult = new MoveResult(
                lastStep.playerId(),
                lastStep.newRow(),
                lastStep.newCol(),
                totalPlatformUpdates,
                totalPlayerUpdates,
                lastStep.success(),
                false 
            );

            messagingTemplate.convertAndSend("/topic/board." + gameId, finalResult);
        }
    }
}
