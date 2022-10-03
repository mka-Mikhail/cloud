module com.mka.client {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.mka.client to javafx.fxml;
    exports com.mka.client;
}