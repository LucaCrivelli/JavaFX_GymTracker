package ch.samt.qualityfitness.controller;

import ch.samt.qualityfitness.model.Exercise;
import ch.samt.qualityfitness.model.TemplateExerciseEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

public class TemplateExerciseEditController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<Exercise> exerciseComboBox;
    @FXML private Spinner<Integer> setsSpinner;
    @FXML private VBox repsContainer;
    @FXML private Label errorLabel;

    private final List<TextField> repsFields = new ArrayList<>();
    private TemplateExerciseEntry existingEntry; // null = aggiunta di un nuovo esercizio
    private boolean saved = false;
    private TemplateExerciseEntry result;

    @FXML
    public void initialize() {
        exerciseComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Exercise exercise) {
                return exercise == null ? "" : exercise.getName();
            }
            @Override
            public Exercise fromString(String string) {
                return null;
            }
        });

        setsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1));
        setsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> rebuildRepsFields(newVal, null));
    }

    /**
     * Da chiamare PRIMA di mostrare il modale (stage.showAndWait()).
     */
    public void setup(List<Exercise> allExercises, List<Integer> excludedExerciseIds, TemplateExerciseEntry existingEntry) {
        this.existingEntry = existingEntry;

        ObservableList<Exercise> available = FXCollections.observableArrayList();
        for (Exercise exercise : allExercises) {
            boolean excluded = excludedExerciseIds.contains(exercise.getId())
                    && (existingEntry == null || exercise.getId() != existingEntry.getExerciseId());
            if (!excluded) available.add(exercise);
        }
        exerciseComboBox.setItems(available);

        if (existingEntry == null) {
            titleLabel.setText("Add exercise");
            setsSpinner.getValueFactory().setValue(1);
            rebuildRepsFields(1, null);
        } else {
            titleLabel.setText("Edit exercise \"" + existingEntry.getExerciseName() + "\"");
            available.stream()
                    .filter(e -> e.getId() == existingEntry.getExerciseId())
                    .findFirst()
                    .ifPresent(exerciseComboBox::setValue);
            exerciseComboBox.setDisable(true); // non si cambia l'esercizio, solo sets/reps
            setsSpinner.getValueFactory().setValue(existingEntry.getSetCount());
            rebuildRepsFields(existingEntry.getSetCount(), existingEntry.getRepsPerSet());
        }
    }

    private void rebuildRepsFields(int setCount, List<Integer> existingValues) {
        List<String> currentValues = new ArrayList<>();
        if (existingValues != null) {
            for (int v : existingValues) currentValues.add(String.valueOf(v));
        } else {
            for (TextField field : repsFields) currentValues.add(field.getText());
        }

        repsContainer.getChildren().clear();
        repsFields.clear();

        for (int i = 0; i < setCount; i++) {
            Label label = new Label("Set " + (i + 1) + " reps:");
            label.setPrefWidth(80);
            TextField field = new TextField();
            field.setPrefWidth(80);
            if (i < currentValues.size()) field.setText(currentValues.get(i));
            repsFields.add(field);
            repsContainer.getChildren().add(new HBox(10, label, field));
        }
    }

    public boolean isSaved() { return saved; }
    public TemplateExerciseEntry getResult() { return result; }

    @FXML
    private void onSave() {
        Exercise selected = exerciseComboBox.getValue();
        if (selected == null) {
            showFieldError("Seleziona un esercizio.");
            return;
        }

        List<Integer> reps = new ArrayList<>();
        for (TextField field : repsFields) {
            String text = field.getText() == null ? "" : field.getText().trim();
            if (text.isEmpty()) {
                showFieldError("Inserisci il numero di reps per ogni set.");
                return;
            }
            try {
                int value = Integer.parseInt(text);
                if (value <= 0) {
                    showFieldError("Le reps devono essere un numero positivo.");
                    return;
                }
                reps.add(value);
            } catch (NumberFormatException e) {
                showFieldError("Le reps devono essere numeri interi.");
                return;
            }
        }

        result = new TemplateExerciseEntry(selected.getId(), selected.getName(), reps);
        saved = true;
        closeWindow();
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void showFieldError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void closeWindow() {
        Stage stage = (Stage) exerciseComboBox.getScene().getWindow();
        stage.close();
    }
}