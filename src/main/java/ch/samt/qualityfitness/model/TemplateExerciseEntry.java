package ch.samt.qualityfitness.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TemplateExerciseEntry {

    private final int exerciseId;
    private final String exerciseName;
    private final List<Integer> repsPerSet;

    public TemplateExerciseEntry(int exerciseId, String exerciseName, List<Integer> repsPerSet) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.repsPerSet = new ArrayList<>(repsPerSet);
    }

    public int getExerciseId() { return exerciseId; }
    public String getExerciseName() { return exerciseName; }
    public List<Integer> getRepsPerSet() { return repsPerSet; }
    public int getSetCount() { return repsPerSet.size(); }

    public String getRepsSummary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repsPerSet.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(repsPerSet.get(i));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemplateExerciseEntry other)) return false;
        return exerciseId == other.exerciseId && repsPerSet.equals(other.repsPerSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exerciseId, repsPerSet);
    }
}