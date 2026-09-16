package ch.samt.qualityfitness;

import java.util.List;

public class WorkoutExerciseData {
    private final int exerciseId;
    private final String exerciseName;
    private final List<SetData> sets;

    public record SetData(int weight, int reps) {}

    public WorkoutExerciseData(int exerciseId, String exerciseName, List<SetData> sets) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.sets = sets;
    }

    public int getExerciseId() { return exerciseId; }
    public String getExerciseName() { return exerciseName; }
    public List<SetData> getSets() { return sets; }
}