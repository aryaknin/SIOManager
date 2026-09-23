module com.example.siomanager {
    requires java.prefs;
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires javafx.web;
    requires org.apache.pdfbox;
    requires org.commonmark;
    requires org.fxmisc.flowless;
    requires org.fxmisc.richtext;
    requires org.xerial.sqlitejdbc;

    opens com.example.siomanager to javafx.fxml;
    exports com.example.siomanager;
}
