package ch.samt.qualityfitness.controller;

import ch.samt.qualityfitness.dao.TemplateDao;
import ch.samt.qualityfitness.model.Template;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.List;

public class TemplatePickerController {

    @FXML private ListView<Template> templateList;
    @FXML private Label emptyLabel;

    private Template selectedTemplate; // null = "senza template"
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        templateList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Template item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        List<Template> templates = new TemplateDao().getAllTemplates();

        if (templates.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            templateList.setVisible(false);
            templateList.setManaged(false);
        } else {
            templateList.setItems(FXCollections.observableArrayList(templates));
        }
    }

    public boolean isConfirmed() { return confirmed; }
    public Template getSelectedTemplate() { return selectedTemplate; }

    @FXML
    private void onUseTemplate() {
        Template selected = templateList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Seleziona un template dalla lista, oppure premi \"Inizia senza template\".");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }
        selectedTemplate = selected;
        confirmed = true;
        closeWindow();
    }

    @FXML
    private void onStartWithoutTemplate() {
        selectedTemplate = null;
        confirmed = true;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) templateList.getScene().getWindow();
        stage.close();
    }
}