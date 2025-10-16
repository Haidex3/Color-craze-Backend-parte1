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
                messagingTemplate.convertAndSend("/topic/board." + gameId, result);
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }


}
