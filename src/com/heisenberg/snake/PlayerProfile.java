package com.heisenberg.snake;

import java.awt.Color;

public record PlayerProfile(
        String name,
        Color headColor,
        Color bodyColor,
        Direction initialDirection,
        Point startPosition,
        int upKey,
        int downKey,
        int leftKey,
        int rightKey) {
}
