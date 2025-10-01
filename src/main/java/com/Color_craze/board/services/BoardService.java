package com.Color_craze.board.services;

import org.springframework.stereotype.Service;

import com.Color_craze.board.models.Box;
import com.Color_craze.board.models.Player;
import com.Color_craze.utils.enums.ColorStatus;
import com.Color_craze.utils.enums.PlayerMove;
import com.Color_craze.board.models.Board;

@Service
public class BoardService {

    private final Board board;

    public BoardService() {
        Player[] players = {
            new Player("P1", ColorStatus.PINK),
            new Player("P2", ColorStatus.YELLOW)
        };
        this.board = new Board(players);
    }

    public Board getBoard() {
        return board;
    }

    public Box getBlock(int row, int col) {
        return board.getBlock(row, col);
    }

    public void setBlock(int row, int col, Box block) {
        board.setBlock(row, col, block);
    }

    public Board movePlayer(Player player, PlayerMove playerMove){
        return board.movePlayer(player, playerMove);
    }
}
