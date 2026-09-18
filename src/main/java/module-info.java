module com.example.siomanager {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.siomanager to javafx.fxml;
    exports com.example.siomanager;
}