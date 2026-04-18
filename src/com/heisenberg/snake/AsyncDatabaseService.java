package com.heisenberg.snake;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class AsyncDatabaseService {
    private final ScoreRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "sqlite-worker");
        thread.setDaemon(true);
        return thread;
    });

    public AsyncDatabaseService(ScoreRepository repository) {
        this.repository = repository;
    }

    public void initialize(Consumer<String> errorHandler) {
        executor.execute(() -> {
            try {
                repository.initialize();
            } catch (ClassNotFoundException e) {
                errorHandler.accept("SQLite JDBC driver not found. Add sqlite-jdbc.jar to lib/ to enable persistence.");
            } catch (SQLException e) {
                errorHandler.accept("Failed to initialize SQLite: " + e.getMessage());
            }
        });
    }

    public void saveMatch(MatchResult result, List<String> playerOrder, Consumer<String> errorHandler) {
        executor.execute(() -> {
            try {
                repository.saveMatch(result, playerOrder);
            } catch (SQLException e) {
                errorHandler.accept("Failed to save match: " + e.getMessage());
            }
        });
    }

    public void loadLeaderboard(Consumer<List<ScoreRepository.ScoreRecord>> onSuccess, Consumer<String> onFailure) {
        executor.execute(() -> {
            try {
                onSuccess.accept(repository.topScores());
            } catch (SQLException e) {
                onFailure.accept("Failed to load leaderboard: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
