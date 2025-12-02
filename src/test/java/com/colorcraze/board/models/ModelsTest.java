package com.colorcraze.board.models;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import com.colorcraze.utils.enums.ColorStatus;

class ModelsTest {

    @Test
    void testEmptyConstructor() {
        Player player = new Player();

        assertNotNull(player.getId(), "El ID no debe ser null");
        assertInstanceOf(UUID.class, player.getId(), "El ID debe ser un UUID");
        assertEquals(ColorStatus.WHITE, player.getColor(), "El color por defecto debe ser WHITE");
        assertEquals(0, player.getCol(), "La columna inicial debe ser 0");
        assertEquals(0, player.getRow(), "La fila inicial debe ser 0");
        assertEquals(0, player.getScore(), "El score inicial debe ser 0");
        assertFalse(player.isUp(), "isUp debe iniciar en false");
    }

    @Test
    void testConstructorWithDefaultPosition() {
        UUID playerId = UUID.randomUUID();

        Player player = new Player(playerId, ColorStatus.WHITE);

        assertEquals(playerId, player.getId(), "El ID debe ser el pasado al constructor");
        assertEquals(ColorStatus.WHITE, player.getColor(), "El color debe ser el pasado al constructor");
        assertEquals(0, player.getScore(), "El score inicial debe ser 0");
        assertFalse(player.isUp(), "isUp debe iniciar en false");

        assertEquals(-1, player.getRow(), "Fila debe ser -1 en el default");
        assertEquals(-1, player.getCol(), "Columna debe ser -1 en el default");
    }

    @Test
    void testPlatformEmptyConstructor() {
        Platform platform = new Platform();

        assertNotNull(platform.getColor(), "El color no debe ser null");
        assertEquals(ColorStatus.WHITE, platform.getColor(), "El color por defecto debe ser WHITE");
    }

    @Test
    void testEmptyConstructorBox() {
        Box box = new Box();

        assertNotNull(box.getColor(), "El color no debe ser null");
        assertEquals(ColorStatus.WHITE, box.getColor(), "El color por defecto debe ser WHITE");
    }

    @Test
    void testTpPlatformEmptyConstructor() {
        TpPlatform tpPlatform = new TpPlatform();

        assertNull(tpPlatform.getColor(), "El color debe ser null al usar el constructor vacío");

        assertEquals(0, tpPlatform.getNewCol(), "newCol debe inicializarse en 0");
        assertEquals(0, tpPlatform.getNewRow(), "newRow debe inicializarse en 0");
    }
}
