package com.Color_craze.board.models;

import java.util.UUID;

import com.Color_craze.utils.enums.ColorStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Player extends Box{
    private UUID id;
    private int col, Row;
    private int score;
    private boolean isUp;

    public Player(UUID id, ColorStatus color) {
        super(color);
        this.color = color;
        this.id = id;
        this.score = 0;
        this.isUp = false;
        switch (color) {
        case RED -> {Row = 13;
            col = 29;}
        case YELLOW -> {Row = 13;
            col = 1;}
        case PURPLE -> {Row = 6;
            col = 8;}
        case GREEN -> {Row = 6;
            col = 22;}
        default -> {Row = -1;
            col = -1;}
    }}

}
