package com.colorcraze.waitingroom.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.colorcraze.waitingroom.dtos.requests.SelectColorMessage;
import com.colorcraze.waitingroom.dtos.responses.PlayerColorState;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.services.WaitingRoomService;
import com.colorcraze.utils.enums.ColorStatus;

import java.util.UUID;
import java.util.Set;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingRoomWebSocketControllerTest {

    @Mock
    private WaitingRoomService waitingRoomService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WaitingRoomWebSocketController waitingRoomWebSocketController;

    @Test
    void selectColor_SuccessfulSelection_BroadcastsRoomState() {
        String roomId = "test-room";
        String playerId = UUID.randomUUID().toString();
        ColorStatus color = ColorStatus.RED;
        
        SelectColorMessage message = new SelectColorMessage();
        message.setPlayerId(playerId);
        message.setColor(color);
        
        Set<String> players = Set.of(playerId);
        Map<String, ColorStatus> playerColors = Map.of(playerId, color);
        WaitingRoomState roomState = new WaitingRoomState(roomId, players, playerColors, false, 10);
        
        when(waitingRoomService.selectColor(roomId, playerId, color)).thenReturn(true);
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);

        waitingRoomWebSocketController.selectColor(roomId, message);

        verify(waitingRoomService).selectColor(roomId, playerId, color);
        verify(waitingRoomService).getRoomState(roomId);
        verify(messagingTemplate).convertAndSend("/topic/waiting-room/" + roomId, roomState);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void selectColor_FailedSelection_SendsPrivateError() {
        String roomId = "test-room";
        String playerId = UUID.randomUUID().toString();
        ColorStatus color = ColorStatus.GREEN;
        
        SelectColorMessage message = new SelectColorMessage();
        message.setPlayerId(playerId);
        message.setColor(color);
        
        when(waitingRoomService.selectColor(roomId, playerId, color)).thenReturn(false);

        waitingRoomWebSocketController.selectColor(roomId, message);

        verify(waitingRoomService).selectColor(roomId, playerId, color);
        verify(waitingRoomService, never()).getRoomState(any());
        verify(messagingTemplate).convertAndSendToUser(
            eq(playerId),
            eq("/queue/waiting-room/color-error"),
            any(PlayerColorState.class)
        );
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void selectColor_FailedSelection_PlayerColorStateHasCorrectValues() {
        String roomId = "test-room";
        String playerId = UUID.randomUUID().toString();
        ColorStatus color = ColorStatus.GREEN;
        
        SelectColorMessage message = new SelectColorMessage();
        message.setPlayerId(playerId);
        message.setColor(color);
        
        when(waitingRoomService.selectColor(roomId, playerId, color)).thenReturn(false);

        waitingRoomWebSocketController.selectColor(roomId, message);

        verify(messagingTemplate).convertAndSendToUser(
            eq(playerId),
            eq("/queue/waiting-room/color-error"),
            argThat((PlayerColorState state) -> 
                state.getPlayerId().equals(playerId) &&
                state.getColor().equals(color) &&
                !state.isSuccess()
            )
        );
    }

    @Test
    void selectColor_SuccessfulSelection_NeverSendsError() {
        String roomId = "test-room";
        String playerId = UUID.randomUUID().toString();
        ColorStatus color = ColorStatus.YELLOW;
        
        SelectColorMessage message = new SelectColorMessage();
        message.setPlayerId(playerId);
        message.setColor(color);
        
        Set<String> players = Set.of(playerId);
        Map<String, ColorStatus> playerColors = Map.of(playerId, color);
        WaitingRoomState roomState = new WaitingRoomState(roomId, players, playerColors, false, 10);
        
        when(waitingRoomService.selectColor(roomId, playerId, color)).thenReturn(true);
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);

        waitingRoomWebSocketController.selectColor(roomId, message);

        verify(waitingRoomService).selectColor(roomId, playerId, color);
        verify(waitingRoomService).getRoomState(roomId);
        verify(messagingTemplate).convertAndSend("/topic/waiting-room/" + roomId, roomState);
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void selectColor_FailedSelection_NeverBroadcastsRoomState() {
        String roomId = "test-room";
        String playerId = UUID.randomUUID().toString();
        ColorStatus color = ColorStatus.PURPLE;

        SelectColorMessage message = new SelectColorMessage();
        message.setPlayerId(playerId);
        message.setColor(color);

        when(waitingRoomService.selectColor(roomId, playerId, color)).thenReturn(false);

        waitingRoomWebSocketController.selectColor(roomId, message);

        verify(waitingRoomService).selectColor(roomId, playerId, color);
        verify(waitingRoomService, never()).getRoomState(any());
        verify(messagingTemplate, never())
                .convertAndSend(any(String.class), any(Object.class));
        verify(messagingTemplate).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void selectColor_RoomDoesNotExist_SendsError() {
        String roomId = "non-existent-room";
        String playerId = UUID.randomUUID().toString();
        ColorStatus color = ColorStatus.RED;
        
        SelectColorMessage message = new SelectColorMessage();
        message.setPlayerId(playerId);
        message.setColor(color);
        
        when(waitingRoomService.selectColor(roomId, playerId, color)).thenReturn(false);

        waitingRoomWebSocketController.selectColor(roomId, message);

        verify(waitingRoomService).selectColor(roomId, playerId, color);
        verify(messagingTemplate).convertAndSendToUser(eq(playerId), eq("/queue/waiting-room/color-error"), any(PlayerColorState.class));
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void selectColor_ColorAlreadyTaken_SendsError() {
        String roomId = "test-room";
        String playerId = UUID.randomUUID().toString();
        ColorStatus color = ColorStatus.GREEN;
        
        SelectColorMessage message = new SelectColorMessage();
        message.setPlayerId(playerId);
        message.setColor(color);
        
        when(waitingRoomService.selectColor(roomId, playerId, color)).thenReturn(false);

        waitingRoomWebSocketController.selectColor(roomId, message);

        verify(waitingRoomService).selectColor(roomId, playerId, color);
        verify(messagingTemplate).convertAndSendToUser(eq(playerId), eq("/queue/waiting-room/color-error"), any(PlayerColorState.class));
    }

    @Test
    void selectColor_MultipleSuccessfulSelections() {
        String roomId = "test-room";
        String playerId1 = UUID.randomUUID().toString();
        String playerId2 = UUID.randomUUID().toString();
        ColorStatus color1 = ColorStatus.RED;
        ColorStatus color2 = ColorStatus.GREEN;
        
        SelectColorMessage message1 = new SelectColorMessage();
        message1.setPlayerId(playerId1);
        message1.setColor(color1);
        
        SelectColorMessage message2 = new SelectColorMessage();
        message2.setPlayerId(playerId2);
        message2.setColor(color2);
        
        Set<String> players = Set.of(playerId1, playerId2);
        Map<String, ColorStatus> playerColors = Map.of(playerId1, color1, playerId2, color2);
        WaitingRoomState roomState = new WaitingRoomState(roomId, players, playerColors, false, 10);
        
        when(waitingRoomService.selectColor(roomId, playerId1, color1)).thenReturn(true);
        when(waitingRoomService.selectColor(roomId, playerId2, color2)).thenReturn(true);
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);

        waitingRoomWebSocketController.selectColor(roomId, message1);
        waitingRoomWebSocketController.selectColor(roomId, message2);

        verify(waitingRoomService).selectColor(roomId, playerId1, color1);
        verify(waitingRoomService).selectColor(roomId, playerId2, color2);
        verify(waitingRoomService, times(2)).getRoomState(roomId);
        verify(messagingTemplate, times(2)).convertAndSend("/topic/waiting-room/" + roomId, roomState);
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void selectColor_MixedSuccessAndFailure() {
        String roomId = "test-room";
        String playerId1 = UUID.randomUUID().toString();
        String playerId2 = UUID.randomUUID().toString();
        ColorStatus color1 = ColorStatus.RED;
        ColorStatus color2 = ColorStatus.GREEN;
        
        SelectColorMessage message1 = new SelectColorMessage();
        message1.setPlayerId(playerId1);
        message1.setColor(color1);
        
        SelectColorMessage message2 = new SelectColorMessage();
        message2.setPlayerId(playerId2);
        message2.setColor(color2);
        
        Set<String> players = Set.of(playerId1, playerId2);
        Map<String, ColorStatus> playerColors = Map.of(playerId1, color1, playerId2, color2);
        WaitingRoomState roomState = new WaitingRoomState(roomId, players, playerColors, false, 10);
        
        when(waitingRoomService.selectColor(roomId, playerId1, color1)).thenReturn(true);
        when(waitingRoomService.selectColor(roomId, playerId2, color2)).thenReturn(false);
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);

        waitingRoomWebSocketController.selectColor(roomId, message1);
        waitingRoomWebSocketController.selectColor(roomId, message2);

        verify(waitingRoomService).selectColor(roomId, playerId1, color1);
        verify(waitingRoomService).selectColor(roomId, playerId2, color2);
        verify(waitingRoomService).getRoomState(roomId);
        verify(messagingTemplate).convertAndSend("/topic/waiting-room/" + roomId, roomState);
        verify(messagingTemplate).convertAndSendToUser(eq(playerId2), eq("/queue/waiting-room/color-error"), any(PlayerColorState.class));
    }

    @Test
    void selectColor_SuccessfulSelectionWithFullRoom() {
        String roomId = "test-room";
        String playerId = UUID.randomUUID().toString();
        ColorStatus color = ColorStatus.GREEN;
        
        SelectColorMessage message = new SelectColorMessage();
        message.setPlayerId(playerId);
        message.setColor(color);
        
        Set<String> players = Set.of(playerId, "player2", "player3", "player4");
        Map<String, ColorStatus> playerColors = Map.of(
            playerId, color,
            "player2", ColorStatus.RED,
            "player3", ColorStatus.GREEN,
            "player4", ColorStatus.YELLOW
        );
        WaitingRoomState roomState = new WaitingRoomState(roomId, players, playerColors, true, 5);
        
        when(waitingRoomService.selectColor(roomId, playerId, color)).thenReturn(true);
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);

        waitingRoomWebSocketController.selectColor(roomId, message);

        verify(waitingRoomService).selectColor(roomId, playerId, color);
        verify(waitingRoomService).getRoomState(roomId);
        verify(messagingTemplate).convertAndSend("/topic/waiting-room/" + roomId, roomState);
    }
}