package com.heisenberg.snake;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ScoreRepository {
    private final String jdbcUrl;

    public ScoreRepository(String databasePath) {
        this.jdbcUrl = "jdbc:sqlite:" + databasePath;
    }

    public void initialize() throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS match_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        winner TEXT NOT NULL,
                        player_one_score INTEGER NOT NULL,
                        player_two_score INTEGER NOT NULL,
                        started_at TEXT NOT NULL,
                        finished_at TEXT NOT NULL
                    )
                    """);
        }
    }

    public void saveMatch(MatchResult result, List<String> playerOrder) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO match_history(winner, player_one_score, player_two_score, started_at, finished_at)
                     VALUES (?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, result.winner());
            statement.setInt(2, result.scores().getOrDefault(playerOrder.get(0), 0));
            statement.setInt(3, result.scores().getOrDefault(playerOrder.get(1), 0));
            statement.setString(4, result.startedAt().toString());
            statement.setString(5, result.finishedAt().toString());
            statement.executeUpdate();
        }
    }

    public List<ScoreRecord> topScores() throws SQLException {
        List<ScoreRecord> records = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT winner, MAX(player_one_score, player_two_score) AS top_score, finished_at
                     FROM match_history
                     ORDER BY top_score DESC, finished_at DESC
                     LIMIT 5
                     """)) {
            while (resultSet.next()) {
                records.add(new ScoreRecord(
                        resultSet.getString("winner"),
                        resultSet.getInt("top_score"),
                        Instant.parse(resultSet.getString("finished_at"))));
            }
        }
        return records;
    }

    public record ScoreRecord(String winner, int topScore, Instant finishedAt) {
    }
}
