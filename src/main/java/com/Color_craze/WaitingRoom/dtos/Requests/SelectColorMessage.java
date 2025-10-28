package com.Color_craze.WaitingRoom.dtos.Requests;

import com.Color_craze.utils.enums.ColorStatus;
import lombok.Data;

@Data
public class SelectColorMessage {
    private String playerId;
    private ColorStatus color;
}
