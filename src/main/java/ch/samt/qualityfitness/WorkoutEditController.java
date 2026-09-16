package ch.samt.qualityfitness;

import ch.samt.qualityfitness.ExerciseDao;
import ch.samt.qualityfitness.WorkoutDao;
import ch.samt.qualityfitness.*;
import ch.samt.qualityfitness.WorkoutExerciseData.SetData;
import ch.samt.qualityfitness.WindowManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class WorkoutEditController {

    @FXML private Label titleLabel;
    @FXML private TextField nameField;
    @FXML private DatePicker startDatePicker;
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private Spinner<Integer> startMinuteSpinner;
    @FXML private DatePicker endDatePicker;
    @FXML private Spinner<Integer> endHourSpinner;
    @FXML private Spinner<Integer> endMinuteSpinner;
    @FXML private Label errorLabel;
    @FXML private Button addExerciseButton;
    @FXML private VBox exercisesContainer;

    private final WorkoutDao workoutDao = new WorkoutDao();
    private final ExerciseDao exerciseDao = new ExerciseDao();

    // Ogni "blocco" rappresenta un esercizio aggiunto al workout, con la sua tabella di set
    private final List<ExerciseBlock> exerciseBlocks = new ArrayList<>();

    private Workout existingWorkout; // null = modalita' Add
    private boolean initializedWithTemplateChoice = false;

    private String initialName = "";
    private LocalDateTime initialStart;
    private LocalDateTime initialEnd;

    private static class ExerciseBlock {
        final int exerciseId;
        final String exerciseName;
        final ObservableList<WorkoutSetEntry> sets = FXCollections.observableArrayList();
        final VBox pane;

        ExerciseBlock(int exerciseId, String exerciseName, VBox pane) {
            this.exerciseId = exerciseId;
            this.exerciseName = exerciseName;
            this.pane = pane;
        }
    }

    @FXML
    public void initialize() {
        startHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 8));
        startMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        endHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
        endMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
    }

    /**
     * Da chiamare PRIMA di mostrare la finestra (stage.show()).
     * Se existingWorkout == null siamo in modalita' Add e verra' proposta la scelta del template
     * al primo "onShown" della finestra (serve lo Stage gia' assegnato, quindi vedi showTemplatePromptIfNeeded()).
     */
    public void setWorkout(Workout workout) {
        this.existingWorkout = workout;

        if (workout == null) {
            titleLabel.setText("Add workout");
        } else {
            titleLabel.setText("Edit workout \"" + workout.getName() + "\"");
            nameField.setText(workout.getName());
            setDateTimeFields(workout.getStart(), startDatePicker, startHourSpinner, startMinuteSpinner);
            setDateTimeFields(workout.getEnd(), endDatePicker, endHourSpinner, endMinuteSpinner);

            for (WorkoutExerciseData data : workoutDao.getWorkoutExercises(workout.getId())) {
                ExerciseBlock block = addExerciseBlock(data.getExerciseId(), data.getExerciseName());
                for (SetData set : data.getSets()) {
                    WorkoutSetEntry entry = new WorkoutSetEntry();
                    entry.setWeight(set.weight());
                    entry.setReps(set.reps());
                    block.sets.add(entry);
                }
                ensureTrailingEmptyRow(block);
            }
        }

        initialName = nameField.getText();
        initialStart = readDateTime(startDatePicker, startHourSpinner, startMinuteSpinner);
        initialEnd = readDateTime(endDatePicker, endHourSpinner, endMinuteSpinner);
    }

    /**
     * Da chiamare subito dopo stage.show() per la modalita' Add: mostra il modale
     * di scelta template (bloccante) e prepopola gli esercizi se scelto.
     */
    public void promptForTemplateIfNeeded(Stage ownerStage) {
        if (existingWorkout != null || initializedWithTemplateChoice) return;
        initializedWithTemplateChoice = true;

        try {
            WindowManager.LoadResult<TemplatePickerController> result =
                    WindowManager.loadModal(ownerStage, "template-picker-view.fxml", "Seleziona template");
            result.stage().showAndWait();

            if (result.controller().isConfirmed() && result.controller().getSelectedTemplate() != null) {
                applyTemplate(result.controller().getSelectedTemplate());
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore", "Impossibile aprire la scelta del template.", e);
        }
    }

    private void applyTemplate(Template template) {
        var templateDao = new ch.samt.qualityfitness.TemplateDao();
        for (TemplateExerciseEntry entry : templateDao.getTemplateExercises(template.getId())) {
            ExerciseBlock block = addExerciseBlock(entry.getExerciseId(), entry.getExerciseName());
            for (int reps : entry.getRepsPerSet()) {
                WorkoutSetEntry setEntry = new WorkoutSetEntry();
                setEntry.setReps(reps); // weight resta null: va inserito manualmente
                block.sets.add(setEntry);
            }
            ensureTrailingEmptyRow(block);
        }
    }

    @FXML
    private void onAddExercise() {
        try {
            List<Exercise> allExercises = exerciseDao.getAllExercises();
            Set<Integer> usedIds = exerciseBlocks.stream().map(b -> b.exerciseId).collect(Collectors.toSet());
            List<Exercise> available = allExercises.stream().filter(e -> !usedIds.contains(e.getId())).toList();

            if (available.isEmpty()) {
                showInfo("Nessun esercizio disponibile", "Tutti gli esercizi sono gia' stati aggiunti a questo workout, oppure non ci sono esercizi nel database.");
                return;
            }

            Stage currentStage = (Stage) addExerciseButton.getScene().getWindow();
            WindowManager.LoadResult<ExercisePickerController> result =
                    WindowManager.loadModal(currentStage, "exercise-picker-view.fxml", "Add exercise");
            result.controller().setAvailableExercises(available);
            result.stage().showAndWait();

            if (result.controller().isConfirmed()) {
                Exercise selected = result.controller().getSelectedExercise();
                ExerciseBlock block = addExerciseBlock(selected.getId(), selected.getName());
                ensureTrailingEmptyRow(block);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore", "Impossibile aprire la scelta dell'esercizio.", e);
        }
    }

    private ExerciseBlock addExerciseBlock(int exerciseId, String exerciseName) {
        Label nameLabel = new Label(exerciseName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Button removeButton = new Button("Remove exercise");

        HBox header = new HBox(15, nameLabel, removeButton);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        TableView<WorkoutSetEntry> table = new TableView<>();
        table.setEditable(true);
        table.setPrefHeight(180);

        TableColumn<WorkoutSetEntry, Number> setColumn = new TableColumn<>("Set");
        setColumn.setCellValueFactory(data -> new SimpleIntegerProperty(table.getItems().indexOf(data.getValue()) + 1));
        setColumn.setSortable(false);
        setColumn.setPrefWidth(60);

        TableColumn<WorkoutSetEntry, Integer> weightColumn = new TableColumn<>("Weight (kg)");
        weightColumn.setCellValueFactory(data -> data.getValue().weightProperty());
        weightColumn.setCellFactory(col -> new TextFieldTableCell<>(new IntegerStringConverter()) {
            @Override
            public void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString());
            }
        });
        weightColumn.setPrefWidth(120);

        TableColumn<WorkoutSetEntry, Integer> repsColumn = new TableColumn<>("Reps");
        repsColumn.setCellValueFactory(data -> data.getValue().repsProperty());
        repsColumn.setCellFactory(col -> new TextFieldTableCell<>(new IntegerStringConverter()) {
            @Override
            public void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.toString());
            }
        });
        repsColumn.setPrefWidth(100);

        TableColumn<WorkoutSetEntry, Void> deleteColumn = new TableColumn<>("");
        deleteColumn.setPrefWidth(90);

        VBox pane = new VBox(8, header, table);
        ExerciseBlock block = new ExerciseBlock(exerciseId, exerciseName, pane);

        weightColumn.setOnEditCommit(e -> {
            e.getRowValue().setWeight(e.getNewValue());
            ensureTrailingEmptyRow(block);
        });
        repsColumn.setOnEditCommit(e -> {
            e.getRowValue().setReps(e.getNewValue());
            ensureTrailingEmptyRow(block);
        });

        deleteColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            {
                deleteButton.setOnAction(e -> {
                    WorkoutSetEntry entry = getTableView().getItems().get(getIndex());
                    if (!entry.isEmpty()) { // non elimina la riga vuota finale, non serve
                        block.sets.remove(entry);
                        table.refresh();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                WorkoutSetEntry entry = empty ? null : getTableView().getItems().get(getIndex());
                setGraphic(entry != null && !entry.isEmpty() ? deleteButton : null);
            }
        });

        table.getColumns().addAll(List.of(setColumn, weightColumn, repsColumn, deleteColumn));
        table.setItems(block.sets);

        removeButton.setOnAction(e -> {
            exerciseBlocks.remove(block);
            exercisesContainer.getChildren().remove(pane);
        });

        exerciseBlocks.add(block);
        exercisesContainer.getChildren().add(pane);
        return block;
    }

    /** Garantisce che ci sia sempre esattamente una riga vuota in coda per aggiungere nuovi set. */
    private void ensureTrailingEmptyRow(ExerciseBlock block) {
        // Rimuove eventuali righe vuote intermedie/multiple in coda
        while (block.sets.size() > 0 && block.sets.get(block.sets.size() - 1).isEmpty()
                && block.sets.size() > 1 && block.sets.get(block.sets.size() - 2).isEmpty()) {
            block.sets.remove(block.sets.size() - 1);
        }
        if (block.sets.isEmpty() || !block.sets.get(block.sets.size() - 1).isEmpty()) {
            block.sets.add(new WorkoutSetEntry());
        }
    }

    @FXML
    private void onSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        LocalDateTime start = readDateTime(startDatePicker, startHourSpinner, startMinuteSpinner);
        LocalDateTime end = readDateTime(endDatePicker, endHourSpinner, endMinuteSpinner);

        if (name.isEmpty()) { showFieldError("Il nome del workout è obbligatorio."); return; }
        if (start == null || end == null) { showFieldError("Data/ora di inizio e fine sono obbligatorie."); return; }
        if (!end.isAfter(start)) { showFieldError("La data/ora di fine deve essere successiva all'inizio."); return; }

        Integer excludeId = existingWorkout == null ? null : existingWorkout.getId();
        if (workoutDao.isNameTaken(name, excludeId)) { showFieldError("Esiste già un workout con questo nome."); return; }
        if (workoutDao.hasOverlap(start, end, excludeId)) { showFieldError("L'orario si sovrappone a un altro workout esistente."); return; }

        if (exerciseBlocks.isEmpty()) { showFieldError("Il workout deve contenere almeno un esercizio."); return; }

        List<WorkoutExerciseData> exercisesData = new ArrayList<>();
        for (ExerciseBlock block : exerciseBlocks) {
            List<SetData> sets = new ArrayList<>();
            for (WorkoutSetEntry entry : block.sets) {
                if (entry.isEmpty()) continue; // riga vuota finale, ignorata
                if (!entry.isComplete()) {
                    showFieldError("Completa weight e reps per tutti i set dell'esercizio \"" + block.exerciseName + "\".");
                    return;
                }
                sets.add(new SetData(entry.getWeight(), entry.getReps()));
            }
            if (sets.isEmpty()) {
                showFieldError("L'esercizio \"" + block.exerciseName + "\" deve avere almeno un set.");
                return;
            }
            exercisesData.add(new WorkoutExerciseData(block.exerciseId, block.exerciseName, sets));
        }

        try {
            if (existingWorkout == null) {
                workoutDao.insertWorkout(name, start, end, exercisesData);
            } else {
                workoutDao.updateWorkout(existingWorkout.getId(), name, start, end, exercisesData);
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
        LocalDateTime currentStart = readDateTime(startDatePicker, startHourSpinner, startMinuteSpinner);
        LocalDateTime currentEnd = readDateTime(endDatePicker, endHourSpinner, endMinuteSpinner);

        boolean basicChanged = !currentName.equals(initialName)
                || !Objects.equals(currentStart, initialStart)
                || !Objects.equals(currentEnd, initialEnd);

        boolean exercisesChanged = exerciseBlocks.stream()
                .anyMatch(b -> b.sets.stream().anyMatch(s -> !s.isEmpty()));
        // Nota: per semplicita' consideriamo "modificato" se in modalita' Add ci sono esercizi con dati,
        // o se in modalita' Edit sono stati aggiunti/rimossi blocchi rispetto al caricamento iniziale.

        return basicChanged || (existingWorkout == null && exercisesChanged) || (existingWorkout != null);
    }

    private LocalDateTime readDateTime(DatePicker datePicker, Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner) {
        LocalDate date = datePicker.getValue();
        if (date == null) return null;
        return LocalDateTime.of(date, LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue()));
    }

    private void setDateTimeFields(LocalDateTime dt, DatePicker datePicker, Spinner<Integer> hourSpinner, Spinner<Integer> minuteSpinner) {
        datePicker.setValue(dt.toLocalDate());
        hourSpinner.getValueFactory().setValue(dt.getHour());
        minuteSpinner.getValueFactory().setValue(dt.getMinute());
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