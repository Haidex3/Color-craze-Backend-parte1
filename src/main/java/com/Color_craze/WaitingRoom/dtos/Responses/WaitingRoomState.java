package com.Color_craze.WaitingRoom.dtos.Responses;

import com.Color_craze.utils.enums.ColorStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
public class WaitingRoomState {
    private String roomId;
    private Set<String> players;
    private Map<String, ColorStatus> playerColors;
    private boolean isFull;
}
