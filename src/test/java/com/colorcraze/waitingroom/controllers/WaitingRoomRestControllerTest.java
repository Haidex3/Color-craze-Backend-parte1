package com.colorcraze.waitingroom.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.models.WaitingRoom;
import com.colorcraze.waitingroom.services.WaitingRoomService;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingRoomRestControllerTest {

    @Mock
    private WaitingRoomService waitingRoomService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WaitingRoomRestController waitingRoomRestController;

    @Test
    void createRoom_Success() {
        String playerId = "player-123";
        String roomId = "room-456";
        
        WaitingRoom mockRoom = new WaitingRoom(roomId, 2);
        WaitingRoomState mockState = new WaitingRoomState(
            roomId, 
            Set.of(playerId), 
            null, 
            false, 
            60
        );
        
        when(waitingRoomService.createRoom()).thenReturn(mockRoom);
        when(waitingRoomService.joinRoom(roomId, playerId)).thenReturn(mockState);
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.createRoom(playerId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(roomId, response.getBody().getRoomId());
        assertTrue(response.getBody().getPlayers().contains(playerId));
        
        verify(waitingRoomService).createRoom();
        verify(waitingRoomService).joinRoom(roomId, playerId);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void createRoom_ServiceReturnsNullRoom() {
        String playerId = "player-123";
        
        when(waitingRoomService.createRoom()).thenReturn(null);
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.createRoom(playerId);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        verify(waitingRoomService).createRoom();
        verify(waitingRoomService, never()).joinRoom(anyString(), anyString());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void createRoom_JoinRoomReturnsNull() {
        String playerId = "player-123";
        String roomId = "room-456";
        
        WaitingRoom mockRoom = new WaitingRoom(roomId, 2);
        
        when(waitingRoomService.createRoom()).thenReturn(mockRoom);
        when(waitingRoomService.joinRoom(roomId, playerId)).thenReturn(null);
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.createRoom(playerId);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        verify(waitingRoomService).createRoom();
        verify(waitingRoomService).joinRoom(roomId, playerId);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void createRoom_BlankPlayerId() {
        String playerId = "   ";
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.createRoom(playerId);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(waitingRoomService, never()).createRoom();
        verify(waitingRoomService, never()).joinRoom(anyString(), anyString());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void createRoom_NullPlayerId() {
        String playerId = null;
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.createRoom(playerId);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(waitingRoomService, never()).createRoom();
        verify(waitingRoomService, never()).joinRoom(anyString(), anyString());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void createRoom_ServiceThrowsException() {
        String playerId = "player-123";
        
        when(waitingRoomService.createRoom()).thenThrow(new RuntimeException("Database error"));
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.createRoom(playerId);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        verify(waitingRoomService).createRoom();
        verify(waitingRoomService, never()).joinRoom(anyString(), anyString());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void joinRoom_Success() {
        String roomId = "room-123";
        String playerId = "player-456";
        
        WaitingRoomState mockState = new WaitingRoomState(
            roomId, 
            Set.of("player-123", playerId), 
            null, 
            false, 
            60
        );
        
        when(waitingRoomService.joinRoom(roomId, playerId)).thenReturn(mockState);
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.joinRoom(roomId, playerId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(roomId, response.getBody().getRoomId());
        assertEquals(2, response.getBody().getPlayers().size());
        
        verify(waitingRoomService).joinRoom(roomId, playerId);
        verify(messagingTemplate).convertAndSend("/topic/waiting-room/" + roomId, mockState);
    }

    @Test
    void joinRoom_RoomNotFound() {
        String roomId = "non-existent-room";
        String playerId = "player-123";
        
        when(waitingRoomService.joinRoom(roomId, playerId)).thenReturn(null);
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.joinRoom(roomId, playerId);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(waitingRoomService).joinRoom(roomId, playerId);
        verifyNoInteractions(messagingTemplate);
    }

    @ParameterizedTest
    @MethodSource("invalidJoinRoomParams")
    void joinRoom_InvalidParams_ShouldReturnBadRequest(String roomId, String playerId) {

        ResponseEntity<WaitingRoomState> response = 
                waitingRoomRestController.joinRoom(roomId, playerId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verify(waitingRoomService, never()).joinRoom(anyString(), anyString());
        verifyNoInteractions(messagingTemplate);
    }

    private static Stream<Arguments> invalidJoinRoomParams() {
        return Stream.of(
            Arguments.of("   ", "player-123"),   // roomId blank
            Arguments.of(null, "player-123"),    // roomId null
            Arguments.of("room-123", "   "),     // playerId blank
            Arguments.of("room-123", null)       // playerId null
        );
    }

        @Test
        void joinRoom_MessagingFailsButStillReturnsSuccess() {
            String roomId = "room-123";
            String playerId = "player-456";
            
            WaitingRoomState mockState = new WaitingRoomState(
                roomId, 
                Set.of(playerId), 
                null, 
                false, 
                60
            );
            
            when(waitingRoomService.joinRoom(roomId, playerId)).thenReturn(mockState);
            doThrow(new RuntimeException("WebSocket error")).when(messagingTemplate)
                .convertAndSend("/topic/waiting-room/" + roomId, mockState);
            
            ResponseEntity<WaitingRoomState> response = waitingRoomRestController.joinRoom(roomId, playerId);
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            
            verify(waitingRoomService).joinRoom(roomId, playerId);
            verify(messagingTemplate).convertAndSend("/topic/waiting-room/" + roomId, mockState);
        }

    @Test
    void joinRoom_ServiceThrowsException() {
        String roomId = "room-123";
        String playerId = "player-456";
        
        when(waitingRoomService.joinRoom(roomId, playerId)).thenThrow(new RuntimeException("Service error"));
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.joinRoom(roomId, playerId);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        verify(waitingRoomService).joinRoom(roomId, playerId);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void createRoom_WithValidUUIDs() {
        String playerId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        
        WaitingRoom mockRoom = new WaitingRoom(roomId, 2);
        WaitingRoomState mockState = new WaitingRoomState(
            roomId, 
            Set.of(playerId), 
            null, 
            false, 
            60
        );
        
        when(waitingRoomService.createRoom()).thenReturn(mockRoom);
        when(waitingRoomService.joinRoom(roomId, playerId)).thenReturn(mockState);
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.createRoom(playerId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        verify(waitingRoomService).createRoom();
        verify(waitingRoomService).joinRoom(roomId, playerId);
    }

    @Test
    void sendRoomStateUpdate_WebSocketSuccess() {
        String roomId = "room-123";
        WaitingRoomState state = new WaitingRoomState(roomId, Set.of("player1"), null, false, 60);
        
        waitingRoomRestController.joinRoom(roomId, "player1");
        
        verify(messagingTemplate, never()).convertAndSend("/topic/waiting-room/" + roomId, state);
    }

    @Test
    void sendRoomStateUpdate_WebSocketException() {
        String roomId = "room-123";
        String playerId = "player-456";
        
        WaitingRoomState mockState = new WaitingRoomState(roomId, Set.of(playerId), null, false, 60);
        
        when(waitingRoomService.joinRoom(roomId, playerId)).thenReturn(mockState);
        doThrow(new RuntimeException("Connection failed")).when(messagingTemplate)
            .convertAndSend("/topic/waiting-room/" + roomId, mockState);
        
        ResponseEntity<WaitingRoomState> response = waitingRoomRestController.joinRoom(roomId, playerId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(messagingTemplate).convertAndSend("/topic/waiting-room/" + roomId, mockState);
    }
    
}