package ch.samt.qualityfitness.controller;

import ch.samt.qualityfitness.WindowManager;
import ch.samt.qualityfitness.dao.ExerciseDao;
import ch.samt.qualityfitness.dao.TemplateDao;
import ch.samt.qualityfitness.model.Exercise;
import ch.samt.qualityfitness.model.Template;
import ch.samt.qualityfitness.model.TemplateExerciseEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TemplateEditController {

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private Label errorLabel;
    @FXML private TableView<TemplateExerciseEntry> exercisesTable;
    @FXML private TableColumn<TemplateExerciseEntry, String> exerciseNameColumn;
    @FXML private TableColumn<TemplateExerciseEntry, Number> setsColumn;
    @FXML private TableColumn<TemplateExerciseEntry, String> repsColumn;

    private final TemplateDao templateDao = new TemplateDao();
    private final ExerciseDao exerciseDao = new ExerciseDao();
    private final ObservableList<TemplateExerciseEntry> entries = FXCollections.observableArrayList();

    private Template existingTemplate; // null = modalita' Add
    private String initialName = "";
    private List<TemplateExerciseEntry> initialEntries = new ArrayList<>();

    @FXML
    public void initialize() {
        exerciseNameColumn.setCellValueFactory(new PropertyValueFactory<>("exerciseName"));
        setsColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getSetCount()));
        repsColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getRepsSummary()));

        exercisesTable.setItems(entries);
    }

    public void setTemplate(Template template) {
        this.existingTemplate = template;

        if (template == null) {
            titleLabel.setText("Add template");
        } else {
            titleLabel.setText("Edit template \"" + template.getName() + "\"");
            nameField.setText(template.getName());
            entries.setAll(templateDao.getTemplateExercises(template.getId()));
        }

        initialName = nameField.getText();
        initialEntries = new ArrayList<>(entries);
    }

    @FXML
    private void onAddExercise() {
        openExerciseEditor(null);
    }

    @FXML
    private void onEditExercise() {
        TemplateExerciseEntry selected = exercisesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Nessun esercizio selezionato", "Seleziona un esercizio dalla tabella per modificarlo.");
            return;
        }
        openExerciseEditor(selected);
    }

    private void openExerciseEditor(TemplateExerciseEntry existing) {
        try {
            List<Exercise> allExercises = exerciseDao.getAllExercises();
            List<Integer> excludedIds = entries.stream().map(TemplateExerciseEntry::getExerciseId).toList();

            Stage currentStage = (Stage) exercisesTable.getScene().getWindow();
            String title = existing == null ? "Add exercise" : "Edit exercise";

            WindowManager.LoadResult<TemplateExerciseEditController> result =
                    WindowManager.loadModal(currentStage, "template-exercise-edit-view.fxml", title);

            result.controller().setup(allExercises, excludedIds, existing);
            result.stage().showAndWait();

            if (result.controller().isSaved()) {
                TemplateExerciseEntry newEntry = result.controller().getResult();
                if (existing == null) {
                    entries.add(newEntry);
                } else {
                    entries.set(entries.indexOf(existing), newEntry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore", "Impossibile aprire la finestra esercizio.", e);
        }
    }

    @FXML
    private void onRemoveExercise() {
        TemplateExerciseEntry selected = exercisesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Nessun esercizio selezionato", "Seleziona un esercizio dalla tabella per rimuoverlo.");
            return;
        }
        entries.remove(selected);
    }

    @FXML
    private void onMoveUp() {
        int index = exercisesTable.getSelectionModel().getSelectedIndex();
        if (index > 0) {
            TemplateExerciseEntry item = entries.remove(index);
            entries.add(index - 1, item);
            exercisesTable.getSelectionModel().select(index - 1);
        }
    }

    @FXML
    private void onMoveDown() {
        int index = exercisesTable.getSelectionModel().getSelectedIndex();
        if (index >= 0 && index < entries.size() - 1) {
            TemplateExerciseEntry item = entries.remove(index);
            entries.add(index + 1, item);
            exercisesTable.getSelectionModel().select(index + 1);
        }
    }

    @FXML
    private void onSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();

        if (name.isEmpty()) {
            showFieldError("Il nome del template è obbligatorio.");
            return;
        }
        Integer excludeId = existingTemplate == null ? null : existingTemplate.getId();
        if (templateDao.isNameTaken(name, excludeId)) {
            showFieldError("Esiste già un template con questo nome.");
            return;
        }
        if (entries.isEmpty()) {
            showFieldError("Il template deve contenere almeno un esercizio.");
            return;
        }

        try {
            if (existingTemplate == null) {
                templateDao.insertTemplate(name, entries);
            } else {
                templateDao.updateTemplate(existingTemplate.getId(), name, entries);
            }
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
                if (response == ButtonType.YES) closeWindow();
            });
        } else {
            closeWindow();
        }
    }

    private boolean hasUnsavedChanges() {
        String currentName = nameField.getText() == null ? "" : nameField.getText();
        return !currentName.equals(initialName) || !entries.equals(initialEntries);
    }

    private void showFieldError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showError(String title, String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message + "\n" + e.getMessage());
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}