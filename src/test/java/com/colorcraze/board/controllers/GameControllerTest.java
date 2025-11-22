package com.colorcraze.board.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.colorcraze.board.models.Board;
import com.colorcraze.board.services.BoardService;
import com.colorcraze.waitingroom.dtos.responses.WaitingRoomState;
import com.colorcraze.waitingroom.services.WaitingRoomService;
import com.colorcraze.utils.enums.ColorStatus;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private BoardService boardService;

    @Mock
    private WaitingRoomService waitingRoomService;

    @InjectMocks
    private GameController gameController;

    @Test
    void createGameFromRoom_Success() {
        String roomId = "test-room";
        Map<String, ColorStatus> playerColors = new HashMap<>();
        playerColors.put(UUID.randomUUID().toString(), ColorStatus.RED);
        playerColors.put(UUID.randomUUID().toString(), ColorStatus.YELLOW);
        
        WaitingRoomState roomState = new WaitingRoomState(
            roomId, 
            playerColors.keySet(), 
            playerColors, 
            false, 
            60
        );
        
        Board mockBoard = new Board("game-123", playerColors);
        
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);
        when(boardService.createBoardWithPlayers(roomId, playerColors)).thenReturn(mockBoard);
        
        ResponseEntity<Map<String, Object>> response = gameController.createGameFromRoom(roomId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("game-123", response.getBody().get("gameId"));
        assertNotNull(response.getBody().get("players"));
        
        verify(waitingRoomService).getRoomState(roomId);
        verify(boardService).createBoardWithPlayers(roomId, playerColors);
        verify(waitingRoomService).removeRoom(roomId);
    }

    @Test
    void createGameFromRoom_RoomNotFound() {
        String roomId = "non-existent-room";
        
        when(waitingRoomService.getRoomState(roomId)).thenReturn(null);
        
        ResponseEntity<Map<String, Object>> response = gameController.createGameFromRoom(roomId);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Sala no existe o está vacía", response.getBody().get("error"));
        
        verify(waitingRoomService).getRoomState(roomId);
        verify(boardService, never()).createBoardWithPlayers(anyString(), any());
        verify(waitingRoomService, never()).removeRoom(anyString());
    }

    @Test
    void createGameFromRoom_RoomEmpty() {
        String roomId = "empty-room";
        WaitingRoomState roomState = new WaitingRoomState(
            roomId, 
            Collections.emptySet(), 
            Collections.emptyMap(), 
            false, 
            60
        );
        
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);
        
        ResponseEntity<Map<String, Object>> response = gameController.createGameFromRoom(roomId);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Sala no existe o está vacía", response.getBody().get("error"));
        
        verify(waitingRoomService).getRoomState(roomId);
        verify(boardService, never()).createBoardWithPlayers(anyString(), any());
        verify(waitingRoomService, never()).removeRoom(anyString());
    }

    @Test
    void createGameFromRoom_RoomStateNullPlayers() {
        String roomId = "null-players-room";
        
        WaitingRoomState roomState = new WaitingRoomState(
            roomId, 
            null,
            Collections.emptyMap(), 
            false, 
            60
        );
        
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);
        
        ResponseEntity<Map<String, Object>> response = gameController.createGameFromRoom(roomId);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Sala no existe o está vacía", response.getBody().get("error"));
        
        verify(waitingRoomService).getRoomState(roomId);
        verify(boardService, never()).createBoardWithPlayers(anyString(), any());
        verify(waitingRoomService, never()).removeRoom(anyString());
    }

    @Test
    void createGameFromRoom_RoomStateEmptyPlayerColors() {
        String roomId = "empty-colors-room";
        Set<String> players = Set.of(UUID.randomUUID().toString());
        
        WaitingRoomState roomState = new WaitingRoomState(
            roomId, 
            players, 
            Collections.emptyMap(),
            false, 
            60
        );
        
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);
        
        ResponseEntity<Map<String, Object>> response = gameController.createGameFromRoom(roomId);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Sala no existe o está vacía", response.getBody().get("error"));
        
        verify(waitingRoomService).getRoomState(roomId);
        verify(boardService, never()).createBoardWithPlayers(anyString(), any());
        verify(waitingRoomService, never()).removeRoom(anyString());
    }

    @Test
    void createGameFromRoom_RoomStateNullPlayerColors() {
        String roomId = "null-colors-room";
        Set<String> players = Set.of(UUID.randomUUID().toString());
        
        WaitingRoomState roomState = new WaitingRoomState(
            roomId, 
            players, 
            null,
            false, 
            60
        );
        
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);
        
        ResponseEntity<Map<String, Object>> response = gameController.createGameFromRoom(roomId);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Sala no existe o está vacía", response.getBody().get("error"));
        
        verify(waitingRoomService).getRoomState(roomId);
        verify(boardService, never()).createBoardWithPlayers(anyString(), any());
        verify(waitingRoomService, never()).removeRoom(anyString());
    }

    @Test
    void createGameFromRoom_BoardServiceReturnsNull() {
        String roomId = "board-service-null-room";
        Map<String, ColorStatus> playerColors = new HashMap<>();
        playerColors.put(UUID.randomUUID().toString(), ColorStatus.RED);
        
        WaitingRoomState roomState = new WaitingRoomState(
            roomId, 
            playerColors.keySet(), 
            playerColors, 
            false, 
            60
        );
        
        when(waitingRoomService.getRoomState(roomId)).thenReturn(roomState);
        when(boardService.createBoardWithPlayers(roomId, playerColors)).thenReturn(null);
        
        ResponseEntity<Map<String, Object>> response = gameController.createGameFromRoom(roomId);
        
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error al crear el juego", response.getBody().get("error"));
        
        verify(waitingRoomService).getRoomState(roomId);
        verify(boardService).createBoardWithPlayers(roomId, playerColors);
        verify(waitingRoomService, never()).removeRoom(anyString());
    }

    @Test
    void getBoardState_Success() {
        String gameId = "test-game";
        Map<String, ColorStatus> playerColors = new HashMap<>();
        playerColors.put(UUID.randomUUID().toString(), ColorStatus.PURPLE);
        
        Board mockBoard = new Board(gameId, playerColors);
        
        when(boardService.getBoard(gameId)).thenReturn(mockBoard);
        
        ResponseEntity<Object> response = gameController.getBoardState(gameId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(mockBoard, response.getBody());
        
        verify(boardService).getBoard(gameId);
    }

    @Test
    void getBoardState_NotFound() {
        String gameId = "non-existent-game";
        when(boardService.getBoard(gameId)).thenReturn(null);
        ResponseEntity<Object> response = gameController.getBoardState(gameId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        assertTrue(response.getBody() instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Juego no encontrado", responseBody.get("error"));
        verify(boardService).getBoard(gameId);
    }

}