package com.example.siomanager.view;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

public final class PdfDocumentView extends BorderPane implements AutoCloseable {
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.5;
    private static final double ZOOM_STEP = 0.25;

    private final PDDocument document;
    private final PDFRenderer renderer;
    private final ImageView pageImage = new ImageView();
    private final Label pageLabel = new Label();
    private final Label zoomLabel = new Label();
    private final Label loadingLabel = new Label("Chargement…");
    private final Button previousButton = new Button("‹");
    private final Button nextButton = new Button("›");
    private final AtomicLong renderGeneration = new AtomicLong();

    private int pageIndex;
    private double zoom = 1.0;
    private volatile boolean closed;

    public PdfDocumentView(Path path) throws IOException {
        document = Loader.loadPDF(path.toFile());
        if (document.getNumberOfPages() == 0) {
            document.close();
            throw new IOException("Le document PDF ne contient aucune page.");
        }
        renderer = new PDFRenderer(document);

        configureToolbar();
        configurePageArea();
        getStyleClass().add("pdf-viewer");
        renderCurrentPage();
    }

    private void configureToolbar() {
        previousButton.getStyleClass().add("pdf-toolbar-button");
        nextButton.getStyleClass().add("pdf-toolbar-button");
        previousButton.setOnAction(event -> showPage(pageIndex - 1));
        nextButton.setOnAction(event -> showPage(pageIndex + 1));

        Button zoomOutButton = new Button("−");
        Button zoomInButton = new Button("+");
        zoomOutButton.getStyleClass().add("pdf-toolbar-button");
        zoomInButton.getStyleClass().add("pdf-toolbar-button");
        zoomOutButton.setOnAction(event -> changeZoom(-ZOOM_STEP));
        zoomInButton.setOnAction(event -> changeZoom(ZOOM_STEP));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(7,
                previousButton, nextButton, pageLabel, spacer,
                zoomOutButton, zoomLabel, zoomInButton
        );
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().addAll("document-toolbar", "pdf-toolbar");
        setTop(toolbar);
        updateToolbar();
    }

    private void configurePageArea() {
        pageImage.setPreserveRatio(true);
        pageImage.setSmooth(true);
        loadingLabel.getStyleClass().add("muted-label");

        StackPane pageContainer = new StackPane(pageImage, loadingLabel);
        pageContainer.getStyleClass().add("pdf-page-container");

        ScrollPane scrollPane = new ScrollPane(pageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("pdf-scroll-pane");
        setCenter(scrollPane);
    }

    private void showPage(int requestedPage) {
        if (requestedPage < 0 || requestedPage >= document.getNumberOfPages() || requestedPage == pageIndex) {
            return;
        }
        pageIndex = requestedPage;
        updateToolbar();
        renderCurrentPage();
    }

    private void changeZoom(double delta) {
        double requestedZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + delta));
        if (Double.compare(requestedZoom, zoom) == 0) {
            return;
        }
        zoom = requestedZoom;
        updateToolbar();
        renderCurrentPage();
    }

    private void updateToolbar() {
        int pageCount = document.getNumberOfPages();
        pageLabel.setText("Page " + (pageIndex + 1) + " / " + pageCount);
        zoomLabel.setText(Math.round(zoom * 100) + " %");
        previousButton.setDisable(pageIndex == 0);
        nextButton.setDisable(pageIndex == pageCount - 1);
    }

    private void renderCurrentPage() {
        long generation = renderGeneration.incrementAndGet();
        int requestedPage = pageIndex;
        double requestedZoom = zoom;
        loadingLabel.setText("Chargement…");
        loadingLabel.setVisible(true);

        Thread.ofVirtual().name("pdf-render-" + requestedPage).start(() -> {
            try {
                BufferedImage bufferedImage;
                synchronized (document) {
                    if (closed) {
                        return;
                    }
                    bufferedImage = renderer.renderImage(requestedPage, (float) requestedZoom, ImageType.RGB);
                }
                WritableImage image = SwingFXUtils.toFXImage(bufferedImage, null);
                Platform.runLater(() -> {
                    if (!closed && generation == renderGeneration.get()) {
                        pageImage.setImage(image);
                        loadingLabel.setVisible(false);
                    }
                });
            } catch (IOException exception) {
                Platform.runLater(() -> {
                    if (!closed && generation == renderGeneration.get()) {
                        loadingLabel.setText("Impossible d’afficher cette page : " + exception.getMessage());
                        loadingLabel.setVisible(true);
                    }
                });
            }
        });
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        renderGeneration.incrementAndGet();
        synchronized (document) {
            try {
                document.close();
            } catch (IOException ignored) {
                // Closing a viewer must not prevent the tab or application from closing.
            }
        }
    }
}
