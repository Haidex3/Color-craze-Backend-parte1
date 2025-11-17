package com.colorcraze.waitingroom.dtos.responses;

import com.colorcraze.utils.enums.ColorStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerColorState {
    private String playerId;
    private ColorStatus color;
    private boolean success;
}
