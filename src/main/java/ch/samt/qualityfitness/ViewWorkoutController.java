package ch.samt.qualityfitness;

import ch.samt.qualityfitness.ExerciseDao;
import ch.samt.qualityfitness.WorkoutDao;
import ch.samt.qualityfitness.*;
import ch.samt.qualityfitness.WorkoutExerciseData.SetData;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ViewWorkoutController {

    @FXML private Label titleLabel;
    @FXML private Label totalVolumeLabel;
    @FXML private Label totalSetsLabel;
    @FXML private Label totalRepsLabel;
    @FXML private TableView<SetRow> tableView;
    @FXML private TableColumn<SetRow, String> exerciseNameColumn;
    @FXML private TableColumn<SetRow, Number> setsColumn;
    @FXML private TableColumn<SetRow, Number> weightColumn;
    @FXML private TableColumn<SetRow, Number> repsColumn;
    @FXML private TableColumn<SetRow, Void> progressColumn;

    private final WorkoutDao workoutDao = new WorkoutDao();
    private final ExerciseDao exerciseDao = new ExerciseDao();

    /** Riga di appoggio: un set di un esercizio del workout. */
    public static class SetRow {
        final int exerciseId;
        final String exerciseName;
        final int setNumber;
        final int weight;
        final int reps;

        SetRow(int exerciseId, String exerciseName, int setNumber, int weight, int reps) {
            this.exerciseId = exerciseId;
            this.exerciseName = exerciseName;
            this.setNumber = setNumber;
            this.weight = weight;
            this.reps = reps;
        }

        public String getExerciseName() { return exerciseName; }
        public int getSetNumber() { return setNumber; }
        public int getWeight() { return weight; }
        public int getReps() { return reps; }
    }

    @FXML
    public void initialize() {
        exerciseNameColumn.setCellValueFactory(new PropertyValueFactory<>("exerciseName"));
        setsColumn.setCellValueFactory(new PropertyValueFactory<>("setNumber"));
        weightColumn.setCellValueFactory(new PropertyValueFactory<>("weight"));
        repsColumn.setCellValueFactory(new PropertyValueFactory<>("reps"));

        progressColumn.setCellFactory(col -> new TableCell<>() {
            private final Button button = new Button("My Progress");
            {
                button.setOnAction(e -> openProgress(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });
    }

    public void setWorkout(Workout workout) {
        titleLabel.setText("Workout \"" + workout.getName() + "\"");

        List<WorkoutExerciseData> exercises = workoutDao.getWorkoutExercises(workout.getId());

        ObservableList<SetRow> rows = FXCollections.observableArrayList();
        int totalVolume = 0;
        int totalSets = 0;
        int totalReps = 0;

        for (WorkoutExerciseData exercise : exercises) {
            int setNumber = 1;
            for (SetData set : exercise.getSets()) {
                rows.add(new SetRow(exercise.getExerciseId(), exercise.getExerciseName(),
                        setNumber++, set.weight(), set.reps()));
                totalVolume += set.weight() * set.reps();
                totalSets++;
                totalReps += set.reps();
            }
        }

        tableView.setItems(rows);
        totalVolumeLabel.setText("Total volume: " + totalVolume);
        totalSetsLabel.setText("Total sets: " + totalSets);
        totalRepsLabel.setText("Total reps: " + totalReps);
    }

    private void openProgress(SetRow row) {
        try {
            List<Exercise> allExercises = exerciseDao.getAllExercises();
            Exercise exercise = allExercises.stream()
                    .filter(e -> e.getId() == row.exerciseId)
                    .findFirst()
                    .orElse(null);
            if (exercise == null) return;

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
}