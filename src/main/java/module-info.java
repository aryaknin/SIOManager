module com.example.siomanager {
    requires java.prefs;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires org.apache.pdfbox;
    requires org.commonmark;
    requires org.fxmisc.flowless;
    requires org.fxmisc.richtext;

    opens com.example.siomanager to javafx.fxml;
    exports com.example.siomanager;
}
