package com.heisenberg.snake;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public final class Snake {
    private final PlayerProfile profile;
    private final Deque<Point> segments = new ArrayDeque<>();
    private Direction direction;
    private Direction pendingDirection;
    private boolean alive;
    private int score;

    public Snake(PlayerProfile profile) {
        this.profile = profile;
        reset();
    }

    public void reset() {
        segments.clear();
        Point start = profile.startPosition();
        segments.addFirst(start);
        segments.addLast(new Point(start.x() - profile.initialDirection().dx(), start.y() - profile.initialDirection().dy()));
        segments.addLast(new Point(start.x() - (profile.initialDirection().dx() * 2), start.y() - (profile.initialDirection().dy() * 2)));
        direction = profile.initialDirection();
        pendingDirection = direction;
        alive = true;
        score = 0;
    }

    public void queueDirection(Direction candidate) {
        if (candidate != null && !candidate.isOpposite(direction)) {
            pendingDirection = candidate;
        }
    }

    public Point step(boolean grow) {
        direction = pendingDirection;
        Point next = head().translate(direction);
        segments.addFirst(next);
        if (!grow) {
            segments.removeLast();
        } else {
            score += 10;
        }
        return next;
    }

    public Point nextHead() {
        return head().translate(pendingDirection);
    }

    public Point head() {
        return segments.peekFirst();
    }

    public Deque<Point> segments() {
        return segments;
    }

    public Set<Point> occupiedCells() {
        return new HashSet<>(segments);
    }

    public PlayerProfile profile() {
        return profile;
    }

    public Direction direction() {
        return direction;
    }

    public boolean alive() {
        return alive;
    }

    public void markDead() {
        alive = false;
    }

    public int score() {
        return score;
    }
}
