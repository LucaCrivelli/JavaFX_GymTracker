package ch.samt.qualityfitness;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

public class WorkoutSetEntry {
    private final ObjectProperty<Integer> weight = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Integer> reps = new SimpleObjectProperty<>(null);

    public ObjectProperty<Integer> weightProperty() { return weight; }
    public ObjectProperty<Integer> repsProperty() { return reps; }

    public Integer getWeight() { return weight.get(); }
    public void setWeight(Integer value) { weight.set(value); }
    public Integer getReps() { return reps.get(); }
    public void setReps(Integer value) { reps.set(value); }

    public boolean isEmpty() { return getWeight() == null && getReps() == null; }
    public boolean isComplete() { return getWeight() != null && getReps() != null; }
}