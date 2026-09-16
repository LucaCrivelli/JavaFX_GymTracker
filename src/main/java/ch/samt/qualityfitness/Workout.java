package ch.samt.qualityfitness;

import java.time.Duration;
import java.time.LocalDateTime;

public class Workout {

    private final int id;
    private final String name;
    private final LocalDateTime start;
    private final LocalDateTime end;

    public Workout(int id, String name, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.name = name;
        this.start = start;
        this.end = end;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }

    public String getDurationFormatted() {
        Duration d = Duration.between(start, end);
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        return String.format("%dh %02dm", hours, minutes);
    }
}