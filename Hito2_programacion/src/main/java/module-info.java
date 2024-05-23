module com.empresa.hito2_programacion {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.xml;

    requires java.desktop;
    requires org.mongodb.driver.core;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;


    opens com.empresa.hito2_programacion to javafx.fxml;
    exports com.empresa.hito2_programacion;
}