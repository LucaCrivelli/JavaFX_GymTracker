package ch.samt.qualityfitness;

import ch.samt.qualityfitness.ExerciseDao;
import ch.samt.qualityfitness.Exercise;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ExerciseEditController {

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private Label errorLabel;

    private final ExerciseDao exerciseDao = new ExerciseDao();
    private Exercise existingExercise; // null = modalita' Add
    private boolean saved = false;

    // Stato iniziale, per rilevare modifiche non salvate alla chiusura
    private String initialName;
    private String initialDescription;
    private String initialType;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList(
                "Strength", "Cardio", "Flexibility", "Balance"));
    }

    public void setExercise(Exercise exercise) {
        this.existingExercise = exercise;

        if (exercise == null) {
            titleLabel.setText("Add exercise");
        } else {
            titleLabel.setText("Edit exercise \"" + exercise.getName() + "\"");
            nameField.setText(exercise.getName());
            descriptionField.setText(exercise.getDescription());
            typeComboBox.setValue(exercise.getType());
        }

        initialName = nameField.getText();
        initialDescription = descriptionField.getText();
        initialType = typeComboBox.getValue();
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void onSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
        String type = typeComboBox.getValue();

        if (name.isEmpty() || description.isEmpty() || type == null || type.isBlank()) {
            showFieldError("Tutti i campi sono obbligatori.");
            return;
        }

        try {
            if (existingExercise == null) {
                exerciseDao.insertExercise(name, description, type);
            } else {
                exerciseDao.updateExercise(existingExercise.getId(), name, description, type);
            }
            saved = true;
            closeWindow();
        } catch (RuntimeException e) {
            showFieldError("Errore durante il salvataggio: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        if (hasUnsavedChanges()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Ci sono modifiche non salvate. Vuoi comunque annullare?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Modifiche non salvate");
            confirm.setHeaderText(null);

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    closeWindow();
                }
            });
        } else {
            closeWindow();
        }
    }

    private boolean hasUnsavedChanges() {
        return !safeEquals(initialName, nameField.getText())
                || !safeEquals(initialDescription, descriptionField.getText())
                || !safeEquals(initialType, typeComboBox.getValue());
    }

    private boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private void showFieldError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}