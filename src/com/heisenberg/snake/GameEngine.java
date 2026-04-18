package com.heisenberg.snake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class GameEngine {
    private static final int TICK_MS = 140;

    private final int rows;
    private final int columns;
    private final List<Snake> snakes;
    private final Random random = new Random();
    private final ScheduledExecutorService loop = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "game-loop");
        thread.setDaemon(true);
        return thread;
    });

    private final Object lock = new Object();
    private final Consumer<GameSnapshot> snapshotConsumer;
    private final Consumer<MatchResult> matchResultConsumer;

    private Point food;
    private boolean running;
    private Instant startedAt;
    private String statusText = "Press Start";

    public GameEngine(int rows, int columns, List<PlayerProfile> profiles,
                      Consumer<GameSnapshot> snapshotConsumer,
                      Consumer<MatchResult> matchResultConsumer) {
        this.rows = rows;
        this.columns = columns;
        this.snapshotConsumer = snapshotConsumer;
        this.matchResultConsumer = matchResultConsumer;
        this.snakes = profiles.stream().map(Snake::new).toList();
        reset();
        loop.scheduleAtFixedRate(this::tickSafely, 0, TICK_MS, TimeUnit.MILLISECONDS);
    }

    public void start() {
        synchronized (lock) {
            running = true;
            startedAt = Instant.now();
            statusText = "Match running";
        }
        emitSnapshot();
    }

    public void reset() {
        synchronized (lock) {
            snakes.forEach(Snake::reset);
            food = randomFreeCell();
            running = false;
            startedAt = Instant.now();
            statusText = "Press Start";
        }
        emitSnapshot();
    }

    public void changeDirection(int keyCode) {
        synchronized (lock) {
            for (Snake snake : snakes) {
                PlayerProfile profile = snake.profile();
                if (keyCode == profile.upKey()) {
                    snake.queueDirection(Direction.UP);
                } else if (keyCode == profile.downKey()) {
                    snake.queueDirection(Direction.DOWN);
                } else if (keyCode == profile.leftKey()) {
                    snake.queueDirection(Direction.LEFT);
                } else if (keyCode == profile.rightKey()) {
                    snake.queueDirection(Direction.RIGHT);
                }
            }
        }
    }

    private void tickSafely() {
        synchronized (lock) {
            if (!running) {
                return;
            }

            List<Point> nextHeads = new ArrayList<>();
            List<Boolean> growFlags = new ArrayList<>();
            for (Snake snake : snakes) {
                if (!snake.alive()) {
                    nextHeads.add(null);
                    growFlags.add(false);
                    continue;
                }
                Point next = snake.nextHead();
                nextHeads.add(next);
                growFlags.add(next.equals(food));
            }

            HashMap<Point, Integer> nextHeadCounts = new HashMap<>();
            for (Point nextHead : nextHeads) {
                if (nextHead != null) {
                    nextHeadCounts.merge(nextHead, 1, Integer::sum);
                }
            }

            for (int i = 0; i < snakes.size(); i++) {
                Snake snake = snakes.get(i);
                Point next = nextHeads.get(i);
                if (!snake.alive() || next == null) {
                    continue;
                }
                if (hitsWall(next)
                        || nextHeadCounts.getOrDefault(next, 0) > 1
                        || collidesWithAny(i, next, growFlags.get(i))) {
                    snake.markDead();
                }
            }

            for (int i = 0; i < snakes.size(); i++) {
                Snake snake = snakes.get(i);
                if (snake.alive()) {
                    snake.step(growFlags.get(i));
                }
            }

            boolean foodEaten = false;
            for (int i = 0; i < snakes.size(); i++) {
                if (snakes.get(i).alive() && growFlags.get(i)) {
                    foodEaten = true;
                }
            }
            if (foodEaten) {
                food = randomFreeCell();
            }

            long aliveCount = snakes.stream().filter(Snake::alive).count();
            if (aliveCount <= 1) {
                running = false;
                Snake winner = snakes.stream().filter(Snake::alive).findFirst()
                        .orElse(snakes.stream().max((a, b) -> Integer.compare(a.score(), b.score())).orElse(snakes.getFirst()));
                statusText = "Winner: " + winner.profile().name();
                matchResultConsumer.accept(new MatchResult(
                        winner.profile().name(),
                        snakes.stream().collect(Collectors.toMap(s -> s.profile().name(), Snake::score)),
                        startedAt,
                        Instant.now()));
            }
        }
        emitSnapshot();
    }

    private boolean hitsWall(Point point) {
        return point.x() < 0 || point.y() < 0 || point.x() >= columns || point.y() >= rows;
    }

    private boolean collidesWithAny(int snakeIndex, Point nextHead, boolean grows) {
        for (int i = 0; i < snakes.size(); i++) {
            Snake other = snakes.get(i);
            List<Point> segments = new ArrayList<>(other.segments());
            if (!grows && i == snakeIndex && !segments.isEmpty()) {
                segments.removeLast();
            }
            for (Point segment : segments) {
                if (nextHead.equals(segment)) {
                    return true;
                }
            }
        }
        for (int i = 0; i < snakes.size(); i++) {
            if (i != snakeIndex && nextHead.equals(snakes.get(i).head()) && snakes.get(i).alive()) {
                return true;
            }
        }
        return false;
    }

    private Point randomFreeCell() {
        List<Point> available = new ArrayList<>();
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                Point candidate = new Point(x, y);
                boolean occupied = snakes.stream()
                        .flatMap(s -> s.segments().stream())
                        .anyMatch(candidate::equals);
                if (!occupied) {
                    available.add(candidate);
                }
            }
        }
        return available.get(random.nextInt(available.size()));
    }

    private void emitSnapshot() {
        GameSnapshot snapshot;
        synchronized (lock) {
            snapshot = new GameSnapshot(
                    snakes.stream().map(s -> new GameSnapshot.SnakeView(
                            s.profile().name(),
                            s.profile().headColor(),
                            s.profile().bodyColor(),
                            List.copyOf(s.segments()),
                            s.score(),
                            s.alive()))
                            .toList(),
                    food,
                    running,
                    statusText,
                    startedAt);
        }
        snapshotConsumer.accept(snapshot);
    }

    public List<String> playerNames() {
        return snakes.stream().map(s -> s.profile().name()).toList();
    }

    public void shutdown() {
        loop.shutdownNow();
    }
}
