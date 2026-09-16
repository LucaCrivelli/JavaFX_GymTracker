package ch.samt.qualityfitness;

import ch.samt.qualityfitness.TemplateDao;
import ch.samt.qualityfitness.Template;
import ch.samt.qualityfitness.WindowManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

public class TemplatesController {

    @FXML private TableView<Template> tableView;
    @FXML private TableColumn<Template, String> nameColumn;
    @FXML private TableColumn<Template, Void> deleteColumn;

    private final TemplateDao templateDao = new TemplateDao();
    private final ObservableList<Template> templateList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        deleteColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            {
                deleteButton.setOnAction(e -> confirmAndDelete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });

        tableView.setItems(templateList);
        refresh();
    }

    private void refresh() {
        try {
            templateList.setAll(templateDao.getAllTemplates());
        } catch (RuntimeException e) {
            e.printStackTrace();
            showError("Errore di caricamento", "Impossibile caricare i template.", e);
        }
    }

    @FXML
    private void onAdd() {
        openTemplateWindow(null);
    }

    @FXML
    private void onEdit() {
        Template selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Nessun template selezionato", "Seleziona un template dalla tabella per modificarlo.");
            return;
        }
        openTemplateWindow(selected);
    }

    private void openTemplateWindow(Template existing) {
        try {
            Stage currentStage = (Stage) tableView.getScene().getWindow();
            String title = existing == null ? "Add template" : "Edit template \"" + existing.getName() + "\"";

            TemplateEditController controller =
                    WindowManager.openWindow(currentStage, "template-edit-view.fxml", title);
            controller.setTemplate(existing);

            // Quando l'utente torna qui (chiude la finestra di edit), ricarica la tabella
            currentStage.setOnShown(e -> refresh());
        } catch (IOException e) {
            e.printStackTrace();
            showError("Errore di navigazione", "Impossibile aprire la finestra template.", e);
        }
    }

    private void confirmAndDelete(Template template) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Sei sicuro di voler eliminare il template \"" + template.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Conferma eliminazione");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    templateDao.deleteTemplate(template.getId());
                    refresh();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    showError("Errore", "Impossibile eliminare il template.", e);
                }
            }
        });
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