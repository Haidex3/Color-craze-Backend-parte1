package com.colorcraze.board.models;

import java.util.UUID;

import com.colorcraze.utils.enums.ColorStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player extends Box{
    private UUID id;
    private int col;
    private int row;
    private int score;
    private boolean isUp;

    public Player(UUID id, ColorStatus color) {
        super(color);
        this.color = color;
        this.id = id;
        this.score = 0;
        this.isUp = false;
        switch (color) {
        case RED -> {row = 13;
            col = 29;}
        case YELLOW -> {row = 13;
            col = 1;}
        case PURPLE -> {row = 6;
            col = 8;}
        case GREEN -> {row = 6;
            col = 22;}
        default -> {row = -1;
            col = -1;}
    }}

}
