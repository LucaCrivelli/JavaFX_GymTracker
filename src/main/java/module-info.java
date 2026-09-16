module ch.samt.qualityfitness {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;


    opens ch.samt.qualityfitness to javafx.fxml;
    exports ch.samt.qualityfitness;
    exports ch.samt.qualityfitness.dao;
    opens ch.samt.qualityfitness.dao to javafx.fxml;
    exports ch.samt.qualityfitness.model;
    opens ch.samt.qualityfitness.model to javafx.fxml;
    exports ch.samt.qualityfitness.db;
    opens ch.samt.qualityfitness.db to javafx.fxml;
    exports ch.samt.qualityfitness.controller;
    opens ch.samt.qualityfitness.controller to javafx.fxml;
}