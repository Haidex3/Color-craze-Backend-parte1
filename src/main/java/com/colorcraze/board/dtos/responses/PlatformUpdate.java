package com.colorcraze.board.dtos.responses;

import com.colorcraze.utils.enums.ColorStatus;

public record PlatformUpdate(int row, int col, ColorStatus color) {}