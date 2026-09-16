module ch.samt.qualityfitness {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires mysql.connector.j;


    opens ch.samt.qualityfitness to javafx.fxml;
    exports ch.samt.qualityfitness;
}