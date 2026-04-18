package com.heisenberg.snake;

import java.time.Instant;
import java.util.List;

public record GameSnapshot(
        List<SnakeView> snakes,
        Point food,
        boolean running,
        String statusText,
        Instant startedAt) {
    public record SnakeView(String playerName, java.awt.Color headColor, java.awt.Color bodyColor,
                            java.util.List<Point> segments, int score, boolean alive) {
    }
}
