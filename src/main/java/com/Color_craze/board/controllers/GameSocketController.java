package com.Color_craze.board.controllers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.Color_craze.board.dtos.Requests.PlayerMoveMessage;
import com.Color_craze.board.dtos.Responses.MoveResult;
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
        List<MoveResult> results = boardService.movePlayer(gameId, moveMessage.getPlayerId(), moveMessage.getDirection());

        CompletableFuture.runAsync(() -> {
            for (MoveResult result : results) {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                messagingTemplate.convertAndSend("/topic/board." + gameId, result);
                System.out.println("Sent move result: " + result.gravity());
                if (result.gravity()) {
                    applyGravity(gameId, moveMessage.getPlayerId());
                }
            }
        });
    }

    /**
     * Aplica gravedad al jugador: sigue moviéndolo hacia abajo hasta que no pueda seguir cayendo.
     */
    private void applyGravity(String gameId, String playerId) {
        boolean continueFalling = true;

        while (continueFalling) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            List<MoveResult> gravityResults = boardService.movePlayer(gameId, playerId, PlayerMove.DOWN);
            if (gravityResults == null || gravityResults.isEmpty()) break;

            for (MoveResult gravityStep : gravityResults) {
                messagingTemplate.convertAndSend("/topic/board." + gameId, gravityStep);

                if (!gravityStep.gravity()) {
                    continueFalling = false;
                    break;
                }
            }
        }
    }
}
