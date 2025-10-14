package com.Color_craze.board.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.Color_craze.board.models.Box;
import com.Color_craze.board.models.Player;
import com.Color_craze.utils.enums.ColorStatus;
import com.Color_craze.utils.enums.PlayerMove;
import com.Color_craze.board.dtos.Responses.MoveResult;
import com.Color_craze.board.models.Board;

@Service
public class BoardService {

    private final Board board;

    public BoardService() {
        this.board = new Board();

        Player p1 = new Player(UUID.randomUUID(),ColorStatus.PINK);
        Player p2 = new Player(UUID.randomUUID(),ColorStatus.YELLOW);

        board.addPlayer(p1);
        board.addPlayer(p2);
    }

    public Board getBoard() {
        return board;
    }

    public Box getBlock(int row, int col) {
        return board.getGrid()[row][col];
    }

    public void setBlock(int row, int col, Box block) {
        board.getGrid()[row][col] = block;
    }

    public MoveResult movePlayer(String playerId, PlayerMove playerMove) {
        UUID uuid = UUID.fromString(playerId);
        return board.movePlayer(uuid, playerMove);
    }
}