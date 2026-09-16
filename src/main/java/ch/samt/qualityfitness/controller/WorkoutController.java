package ch.samt.qualityfitness.controller;

import ch.samt.qualityfitness.WindowManager;
import ch.samt.qualityfitness.dao.WorkoutDao;
import ch.samt.qualityfitness.model.Workout;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class WorkoutController {

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    private TableView<Workout> tableView;
    @FXML
    private TableColumn<Workout, String> workoutNameColumn;
    @FXML
    private TableColumn<Workout, String> startColumn;
    @FXML
    private TableColumn<Workout, String> endColumn;
    @FXML
    private TableColumn<Workout, String> durationColumn;
    @FXML
    private TableColumn<Workout, Void> actionColumn;

    @FXML
    private ToggleButton exerciesId;
    @FXML
    private ToggleButton templatesId;

    @FXML
    private Button addWorkoutId;
    @FXML
    private Button viewWorkoutId;
    @FXML
    private Button editWorkoutId;

    private final WorkoutDao workoutDao = new WorkoutDao();
    private final ObservableList<Workout> workoutList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        workoutNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        startColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStart().format(DATETIME_FORMAT)));
        endColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getEnd().format(DATETIME_FORMAT)));
        durationColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getDurationFormatted()));

        addDeleteButtonToTable();

        tableView.setItems(workoutList);
        refreshWorkouts();
    }

    private void refreshWorkouts() {
        try {
            workoutList.setAll(workoutDao.getAllWorkouts());
        } catch (RuntimeException e) {
            e.printStackTrace();
            showError("Errore di caricamento", "Impossibile caricare i workout dal database.", e);
        }
    }

    private void addDeleteButtonToTable() {
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");

            {
                deleteButton.setOnAction(event -> {
                    Workout workout = getTableView().getItems().get(getIndex());
                    confirmAndDelete(workout);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });
    }

    private void confirmAndDelete(Workout workout) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Sei sicuro di voler eliminare il workout \"" + workout.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Conferma eliminazione");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    workoutDao.deleteWorkout(workout.getId());
                    refreshWorkouts();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    showError("Errore", "Impossibile eliminare il workout.", e);
                }
            }
        });
    }

    private void showError(String title, String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message + "\n" + e.getMessage());
        alert.showAndWait();
    }

    // --- Navigazione (da implementare quando le altre finestre saranno pronte) ---

    @FXML
    protected void exercies() {
        try {
            Stage currentStage = (Stage) exerciesId.getScene().getWindow();
            WindowManager.openWindow(currentStage, "exercises-view.fxml", "Exercises");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore di navigazione", "Impossibile aprire la finestra Exercises.", e);
        }
    }

    @FXML
    protected void templates() {
        try {
            Stage currentStage = (Stage) templatesId.getScene().getWindow();
            WindowManager.openWindow(currentStage, "templates-view.fxml", "Templates");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore di navigazione", "Impossibile aprire la finestra Templates.", e);
        }
    }

    @FXML
    protected void addWorkout() {
        openWorkoutEditWindow(null);
    }

    @FXML
    protected void viewWorkout() {
        Workout selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Nessun workout selezionato", "Seleziona un workout dalla tabella per visualizzarlo.");
            return;
        }
        try {
            Stage currentStage = (Stage) viewWorkoutId.getScene().getWindow();
            ViewWorkoutController controller =
                    WindowManager.openWindow(currentStage, "view-workout-view.fxml", "View Workout");
            controller.setWorkout(selected);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore di navigazione", "Impossibile aprire la finestra View Workout.", e);
        }
    }

    @FXML
    protected void editWorkout() {
        Workout selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Nessun workout selezionato", "Seleziona un workout dalla tabella per modificarlo.");
            return;
        }
        openWorkoutEditWindow(selected);
    }

    private void showNotImplemented(String windowName) {
        showInfo("Non ancora implementato", windowName + " sarà disponibile a breve.");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void openWorkoutEditWindow(Workout existing) {
        try {
            Stage currentStage = (Stage) addWorkoutId.getScene().getWindow();
            String title = existing == null ? "Add workout" : "Edit workout \"" + existing.getName() + "\"";

            WindowManager.LoadResult<WorkoutEditController> result =
                    WindowManager.load("workout-edit-view.fxml", title, true);
            result.controller().setWorkout(existing);

            if (existing != null) {
                currentStage.hide();
                result.stage().setOnHidden(e -> {
                    currentStage.show();
                    refreshWorkouts();
                });
                result.stage().show();
            } else {
                currentStage.hide();
                result.stage().setOnHidden(e -> {
                    currentStage.show();
                    refreshWorkouts();
                });
                result.stage().show();
                result.controller().promptForTemplateIfNeeded(result.stage());
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore di navigazione", "Impossibile aprire la finestra workout.", e);
        }
    }
}