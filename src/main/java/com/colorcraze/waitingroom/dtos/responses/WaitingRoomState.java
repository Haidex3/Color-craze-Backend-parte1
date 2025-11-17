package com.colorcraze.waitingroom.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Set;

import com.colorcraze.utils.enums.ColorStatus;

@Data
@AllArgsConstructor
public class WaitingRoomState {
    private String roomId;
    private Set<String> players;
    private Map<String, ColorStatus> playerColors;
    private boolean isFull;
    private int seconds; // tiempo restante en segundos --- IGNORE ---
}
