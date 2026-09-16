package ch.samt.qualityfitness;

import ch.samt.qualityfitness.ProgressDao;
import ch.samt.qualityfitness.Exercise;
import ch.samt.qualityfitness.ProgressEntry;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProgressController {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private Label titleLabel;
    @FXML private Label emptyLabel;
    @FXML private LineChart<Number, Number> lineChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private TableView<TableRow> tableView;
    @FXML private TableColumn<TableRow, String> dateColumn;
    @FXML private TableColumn<TableRow, Number> volumeColumn;

    private final ProgressDao progressDao = new ProgressDao();

    /** Riga di appoggio per la TableView (data formattata + volume). */
    public static class TableRow {
        private final String date;
        private final int volume;
        public TableRow(String date, int volume) { this.date = date; this.volume = volume; }
        public String getDate() { return date; }
        public int getVolume() { return volume; }
    }

    @FXML
    public void initialize() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        volumeColumn.setCellValueFactory(new PropertyValueFactory<>("volume"));
    }

    public void setExercise(Exercise exercise) {
        titleLabel.setText("Progress of " + exercise.getName());

        List<ProgressEntry> allTime = progressDao.getExecutions(exercise.getId());

        if (allTime.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            lineChart.setVisible(false);
            lineChart.setManaged(false);
            tableView.setVisible(false);
            tableView.setManaged(false);
            return;
        }

        // --- Tabella: tutti i dati, sempre ---
        var rows = allTime.stream()
                .map(e -> new TableRow(e.getExecutionDateTime().format(DATETIME_FORMAT), e.getTotalVolume()))
                .toList();
        tableView.setItems(FXCollections.observableArrayList(rows));

        // --- Prossimo valore atteso, calcolato sempre su tutto lo storico ---
        int nextExpected = computeNextExpectedVolume(allTime);

        // --- Grafico: solo ultimi 3 mesi, + il valore atteso come punto successivo ---
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<ProgressEntry> lastThreeMonths = allTime.stream()
                .filter(e -> !e.getExecutionDateTime().isBefore(threeMonthsAgo))
                .toList();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(exercise.getName());

        int index = 1;
        for (ProgressEntry entry : lastThreeMonths) {
            series.getData().add(new XYChart.Data<>(index++, entry.getTotalVolume()));
        }
        // Punto finale: prossimo valore atteso
        series.getData().add(new XYChart.Data<>(index, nextExpected));

        lineChart.getData().clear();
        lineChart.getData().add(series);
    }

    /**
     * 1. Calcola l'incremento tra ogni coppia consecutiva (in ordine cronologico)
     * 2. Fa la media degli incrementi
     * 3. La somma all'ultimo valore per stimare il prossimo
     * Con un solo dato disponibile, l'incremento medio è 0 (nessuna tendenza calcolabile).
     */
    private int computeNextExpectedVolume(List<ProgressEntry> allTimeChronological) {
        if (allTimeChronological.size() == 1) {
            return allTimeChronological.get(0).getTotalVolume();
        }
        int totalIncrease = 0;
        for (int i = 1; i < allTimeChronological.size(); i++) {
            totalIncrease += allTimeChronological.get(i).getTotalVolume()
                    - allTimeChronological.get(i - 1).getTotalVolume();
        }
        double averageIncrease = (double) totalIncrease / (allTimeChronological.size() - 1);
        int lastVolume = allTimeChronological.get(allTimeChronological.size() - 1).getTotalVolume();
        return (int) Math.round(lastVolume + averageIncrease);
    }
}