package ch.samt.qualityfitness.controller;

import ch.samt.qualityfitness.model.Exercise;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.List;

public class ExercisePickerController {

    @FXML private ComboBox<Exercise> exerciseComboBox;
    @FXML private Label errorLabel;

    private Exercise selectedExercise;
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        exerciseComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Exercise e) { return e == null ? "" : e.getName(); }
            @Override public Exercise fromString(String s) { return null; }
        });
    }

    public void setAvailableExercises(List<Exercise> exercises) {
        exerciseComboBox.setItems(FXCollections.observableArrayList(exercises));
    }

    public boolean isConfirmed() { return confirmed; }
    public Exercise getSelectedExercise() { return selectedExercise; }

    @FXML
    private void onAdd() {
        Exercise selected = exerciseComboBox.getValue();
        if (selected == null) {
            errorLabel.setText("Seleziona un esercizio.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }
        selectedExercise = selected;
        confirmed = true;
        closeWindow();
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) exerciseComboBox.getScene().getWindow();
        stage.close();
    }
}