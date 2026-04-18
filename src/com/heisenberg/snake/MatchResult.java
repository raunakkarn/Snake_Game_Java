package com.heisenberg.snake;

import java.time.Instant;
import java.util.Map;

public record MatchResult(String winner, Map<String, Integer> scores, Instant startedAt, Instant finishedAt) {
}
