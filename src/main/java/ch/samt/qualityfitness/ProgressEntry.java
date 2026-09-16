package ch.samt.qualityfitness;

import java.time.LocalDateTime;

public class ProgressEntry {
    private final LocalDateTime executionDateTime;
    private final int totalVolume;

    public ProgressEntry(LocalDateTime executionDateTime, int totalVolume) {
        this.executionDateTime = executionDateTime;
        this.totalVolume = totalVolume;
    }

    public LocalDateTime getExecutionDateTime() { return executionDateTime; }
    public int getTotalVolume() { return totalVolume; }
}