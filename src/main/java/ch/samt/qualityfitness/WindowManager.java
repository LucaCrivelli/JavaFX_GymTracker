package ch.samt.qualityfitness;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class WindowManager {

    /**
     * Contenitore che espone sia il controller della FXML caricata sia lo Stage
     * non ancora mostrato, cosi il chiamante puo' configurare il controller
     * PRIMA di mostrare la finestra (fondamentale per i modali tipo edit).
     */
    public record LoadResult<T>(T controller, Stage stage) { }

    /**
     * Carica una FXML in un nuovo Stage senza mostrarla.
     * Usalo quando devi configurare il controller prima della visualizzazione
     * (es. passare l'entita' da modificare a un modale).
     */
    public static <T> LoadResult<T> load(String fxml, String title, boolean resizable) throws IOException {
        FXMLLoader loader = new FXMLLoader(WindowManager.class.getResource(fxml));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.setResizable(resizable);
        if (resizable) {
            stage.setMinWidth(600);
            stage.setMinHeight(400);
        }

        return new LoadResult<>(loader.getController(), stage);
    }

    /**
     * Apre una Window "di navigazione" del diagramma (Main <-> Exercises, ecc.):
     * nasconde la finestra corrente e la ripristina quando quella nuova si chiude.
     * Ritorna subito il controller, gia' mostrato sullo schermo.
     */
    public static <T> T openWindow(Stage currentStage, String fxml, String title) throws IOException {
        LoadResult<T> result = load(fxml, title, true);

        if (currentStage != null) {
            currentStage.hide();
            result.stage().setOnHidden(e -> currentStage.show());
        }

        result.stage().show();
        return result.controller();
    }

    /**
     * Carica un dialog modale SENZA mostrarlo: il chiamante configura il
     * controller restituito, poi chiama result.stage().showAndWait() quando pronto.
     * Esempio:
     *   LoadResult<ExerciseEditController> result =
     *           WindowManager.loadModal(ownerStage, "exercise-edit-view.fxml", "Add exercise");
     *   result.controller().setExercise(existing);
     *   result.stage().showAndWait();
     *   if (result.controller().isSaved()) refresh();
     */
    public static <T> LoadResult<T> loadModal(Stage ownerStage, String fxml, String title) throws IOException {
        LoadResult<T> result = load(fxml, title, false);

        result.stage().initModality(Modality.WINDOW_MODAL);
        result.stage().initOwner(ownerStage);

        return result;
    }
}