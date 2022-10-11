module cloud.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires cloud.common;

    opens com.mka.client to javafx.fxml;
    exports com.mka.client;
}