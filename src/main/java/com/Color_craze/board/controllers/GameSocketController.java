package com.Color_craze.board.controllers;


import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.Color_craze.board.dtos.Requests.PlayerMoveMessage;
import com.Color_craze.board.dtos.Responses.MoveResult;
import com.Color_craze.board.services.BoardService;

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
                if (result.success()) {
                    messagingTemplate.convertAndSend("/topic/board." + gameId, result);
                } else {
                    messagingTemplate.convertAndSendToUser(
                        moveMessage.getPlayerId(),
                        "/queue/reply",
                        result
                    );
                }
            }
        });
    }


    @Scheduled(fixedDelay = 300)
    public void gravityTick() {
        System.out.println("Aplicando gravedad a todos los tableros");
        for (String gameId : boardService.getAllBoardIds()) {
            List<MoveResult> gravityResults = boardService.applyGravity(gameId);

            for (MoveResult result : gravityResults) {
                if (result.success()) {
                    messagingTemplate.convertAndSend("/topic/board." + gameId, result);
                }
            }
        }
    }


}
