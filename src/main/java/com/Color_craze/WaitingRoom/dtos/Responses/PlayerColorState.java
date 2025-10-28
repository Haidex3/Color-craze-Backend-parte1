package com.Color_craze.WaitingRoom.dtos.Responses;

import com.Color_craze.utils.enums.ColorStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerColorState {
    private String playerId;
    private ColorStatus color;
    private boolean success;
}
