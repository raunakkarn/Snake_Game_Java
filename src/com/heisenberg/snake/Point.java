package com.heisenberg.snake;

public record Point(int x, int y) {
    public Point translate(Direction direction) {
        return new Point(x + direction.dx(), y + direction.dy());
    }
}
