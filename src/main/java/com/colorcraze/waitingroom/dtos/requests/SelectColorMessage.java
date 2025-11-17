package com.colorcraze.waitingroom.dtos.requests;

import com.colorcraze.utils.enums.ColorStatus;

import lombok.Data;

@Data
public class SelectColorMessage {
    private String playerId;
    private ColorStatus color;
}
