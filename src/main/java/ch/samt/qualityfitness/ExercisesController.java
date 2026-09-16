package ch.samt.qualityfitness;

import ch.samt.qualityfitness.ExerciseDao;
import ch.samt.qualityfitness.Exercise;
import ch.samt.qualityfitness.WindowManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

public class ExercisesController {

    @FXML private TableView<Exercise> tableView;
    @FXML private TableColumn<Exercise, String> nameColumn;
    @FXML private TableColumn<Exercise, String> descriptionColumn;
    @FXML private TableColumn<Exercise, String> typeColumn;
    @FXML private TableColumn<Exercise, Void> progressColumn;
    @FXML private TableColumn<Exercise, Void> deleteColumn;
    @FXML private Button addButton;
    @FXML private Button editButton;

    private final ExerciseDao exerciseDao = new ExerciseDao();
    private final ObservableList<Exercise> exerciseList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));

        addActionButton(progressColumn, "My Progress", this::openProgress);
        addActionButton(deleteColumn, "Delete", this::confirmAndDelete);

        tableView.setItems(exerciseList);
        refresh();
    }

    private void refresh() {
        try {
            exerciseList.setAll(exerciseDao.getAllExercises());
        } catch (RuntimeException e) {
            e.printStackTrace();
            showError("Errore di caricamento", "Impossibile caricare gli esercizi.", e);
        }
    }

    private void addActionButton(TableColumn<Exercise, Void> column, String label,
                                 java.util.function.Consumer<Exercise> action) {
        column.setCellFactory(col -> new TableCell<>() {
            private final Button button = new Button(label);
            {
                button.setOnAction(e -> action.accept(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });
    }

    @FXML
    private void onAdd() {
        openEditDialog(null);
    }

    @FXML
    private void onEdit() {
        Exercise selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Nessun esercizio selezionato", "Seleziona un esercizio dalla tabella per modificarlo.");
            return;
        }
        openEditDialog(selected);
    }

    private void openEditDialog(Exercise existing) {
        try {
            Stage currentStage = (Stage) tableView.getScene().getWindow();
            String title = existing == null ? "Add exercise" : "Edit exercise \"" + existing.getName() + "\"";

            WindowManager.LoadResult<ExerciseEditController> result =
                    WindowManager.loadModal(currentStage, "exercise-edit-view.fxml", title);

            result.controller().setExercise(existing);
            result.stage().showAndWait();

            if (result.controller().isSaved()) {
                refresh();
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore", "Impossibile aprire la finestra di modifica.", e);
        }
    }

    private void confirmAndDelete(Exercise exercise) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Sei sicuro di voler eliminare l'esercizio \"" + exercise.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Conferma eliminazione");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    exerciseDao.deleteExercise(exercise.getId());
                    refresh();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    showError("Errore", "Impossibile eliminare l'esercizio.", e);
                }
            }
        });
    }

    private void openProgress(Exercise exercise) {
        try {
            Stage currentStage = (Stage) tableView.getScene().getWindow();
            ProgressController controller =
                    WindowManager.openWindow(currentStage, "progress-view.fxml", "Progress");
            controller.setExercise(exercise);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore di navigazione", "Impossibile aprire la finestra Progress.", e);
        }
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
}