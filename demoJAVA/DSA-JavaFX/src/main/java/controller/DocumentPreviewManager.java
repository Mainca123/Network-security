package controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import model.FileInfoModel;
import service.FilePreviewService;
import util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocumentPreviewManager {
    private static final float PDF_RENDER_DPI = 120f;
    private static final double ZOOM_STEP = 0.15;
    private static final double MIN_ZOOM = 0.35;
    private static final double MAX_ZOOM = 2.5;

    private final FilePreviewService previewService;
    private final ScrollPane formattedScroll;
    private final VBox formattedPages;
    private final WebView docxWebView;
    private final TabPane workbookTabs;
    private final TextArea plainTextArea;
    private final TextArea metadataArea;
    private final TextArea hashArea;
    private final Label statusLabel;

    private final List<ImageView> imageViews = new ArrayList<>();
    private double zoom = 1.0;
    private boolean fitWidth = true;

    public DocumentPreviewManager(
            FilePreviewService previewService,
            ScrollPane formattedScroll,
            VBox formattedPages,
            WebView docxWebView,
            TabPane workbookTabs,
            TextArea plainTextArea,
            TextArea metadataArea,
            TextArea hashArea,
            Label statusLabel
    ) {
        this.previewService = previewService;
        this.formattedScroll = formattedScroll;
        this.formattedPages = formattedPages;
        this.docxWebView = docxWebView;
        this.workbookTabs = workbookTabs;
        this.plainTextArea = plainTextArea;
        this.metadataArea = metadataArea;
        this.hashArea = hashArea;
        this.statusLabel = statusLabel;
        this.formattedScroll.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> {
            if (fitWidth) {
                applyFitWidth();
            }
        });
    }

    public void render(File file, FileInfoModel info) {
        clear();
        plainTextArea.setText(info.getPreviewContent());
        metadataArea.setText(formatFileInfo(info));
        hashArea.setText("Mã băm SHA-256\n" + info.getSha256Hash());

        String extension = FileUtil.extension(file.toPath());
        try {
            switch (extension) {
                case "pdf" -> renderPdf(file);
                case "docx" -> renderDocx(file);
                case "xlsx" -> renderWorkbook(file);
                case "png", "jpg", "jpeg", "gif", "bmp" -> renderImage(file);
                case "txt", "json", "csv", "log" -> renderText(info.getPreviewContent());
                default -> renderMessage("Không hỗ trợ chế độ xem định dạng cho loại tệp này. Vui lòng xem Văn bản thuần, Thông tin tệp và Mã băm.");
            }
        } catch (Exception ex) {
            renderMessage("Không thể hiển thị đầy đủ định dạng, đang hiển thị dạng văn bản thuần.\n" + ex.getMessage());
            showTextStatus("Không thể hiển thị đầy đủ định dạng, đang hiển thị dạng văn bản thuần.", false);
        }
    }

    public void clear() {
        formattedPages.getChildren().clear();
        imageViews.clear();
        plainTextArea.clear();
        metadataArea.clear();
        hashArea.clear();
        workbookTabs.getTabs().clear();
        setVisible(formattedScroll, true);
        setVisible(docxWebView, false);
        setVisible(workbookTabs, false);
        docxWebView.getEngine().loadContent("");
        zoom = 1.0;
        fitWidth = true;
    }

    public void zoomIn() {
        fitWidth = false;
        zoom = Math.min(MAX_ZOOM, zoom + ZOOM_STEP);
        applyZoom();
    }

    public void zoomOut() {
        fitWidth = false;
        zoom = Math.max(MIN_ZOOM, zoom - ZOOM_STEP);
        applyZoom();
    }

    public void fitWidth() {
        fitWidth = true;
        zoom = 1.0;
        docxWebView.setZoom(1.0);
        applyFitWidth();
    }

    private void renderPdf(File file) throws Exception {
        showScrollContent();
        List<Image> pages = previewService.renderPdfPages(file.toPath(), PDF_RENDER_DPI);
        if (pages.isEmpty()) {
            renderMessage("PDF không có trang để hiển thị.");
            return;
        }
        int index = 1;
        for (Image page : pages) {
            Label pageLabel = new Label("Trang " + index++);
            pageLabel.getStyleClass().add("page-label");
            ImageView imageView = createImageView(page);
            VBox pageBox = new VBox(8, pageLabel, imageView);
            pageBox.getStyleClass().add("pdf-page");
            formattedPages.getChildren().add(pageBox);
            imageViews.add(imageView);
        }
        applyFitWidth();
    }

    private void renderDocx(File file) {
        FilePreviewService.DocxHtmlPreview preview = previewService.readDocxHtml(file.toPath());
        setVisible(formattedScroll, false);
        setVisible(workbookTabs, false);
        setVisible(docxWebView, true);
        docxWebView.setZoom(1.0);
        docxWebView.getEngine().loadContent(preview.html());
        if (preview.fallback()) {
            showTextStatus("Không thể hiển thị đầy đủ định dạng, đang hiển thị dạng văn bản thuần.", false);
        }
    }

    private void renderWorkbook(File file) throws Exception {
        setVisible(formattedScroll, false);
        setVisible(docxWebView, false);
        setVisible(workbookTabs, true);
        workbookTabs.getTabs().clear();
        for (FilePreviewService.SheetPreview sheet : previewService.readXlsxSheets(file.toPath())) {
            TableView<ObservableList<String>> tableView = createTable(sheet.rows());
            Tab tab = new Tab(sheet.name(), tableView);
            tab.setClosable(false);
            workbookTabs.getTabs().add(tab);
        }
        if (!workbookTabs.getTabs().isEmpty()) {
            workbookTabs.getSelectionModel().selectFirst();
        }
    }

    private void renderImage(File file) {
        showScrollContent();
        Image image = new Image(file.toURI().toString(), true);
        ImageView imageView = createImageView(image);
        formattedPages.getChildren().add(imageView);
        imageViews.add(imageView);
        applyFitWidth();
    }

    private void renderText(String text) {
        showScrollContent();
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(false);
        area.getStyleClass().add("document-viewer");
        area.setPrefRowCount(40);
        formattedPages.getChildren().add(area);
    }

    private void renderMessage(String message) {
        showScrollContent();
        TextArea area = new TextArea(message);
        area.setEditable(false);
        area.setWrapText(true);
        area.getStyleClass().add("document-viewer");
        formattedPages.getChildren().setAll(area);
    }

    private ImageView createImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        return imageView;
    }

    private TableView<ObservableList<String>> createTable(List<List<String>> rows) {
        TableView<ObservableList<String>> tableView = new TableView<>();
        tableView.getStyleClass().add("excel-table");
        int columnCount = rows.stream().mapToInt(List::size).max().orElse(1);
        for (int i = 0; i < columnCount; i++) {
            final int columnIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>("Cột " + (i + 1));
            column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(
                    columnIndex < data.getValue().size() ? data.getValue().get(columnIndex) : ""));
            column.setPrefWidth(140);
            tableView.getColumns().add(column);
        }
        tableView.setItems(FXCollections.observableArrayList(rows.stream()
                .map(row -> FXCollections.observableArrayList(row))
                .toList()));
        return tableView;
    }

    private void showScrollContent() {
        setVisible(formattedScroll, true);
        setVisible(docxWebView, false);
        setVisible(workbookTabs, false);
    }

    private void applyZoom() {
        for (ImageView imageView : imageViews) {
            imageView.setScaleX(1.0);
            imageView.setScaleY(1.0);
            double sourceWidth = imageView.getImage() == null || imageView.getImage().getWidth() <= 0
                    ? 760
                    : imageView.getImage().getWidth();
            imageView.setFitWidth(sourceWidth * zoom);
        }
        docxWebView.setZoom(zoom);
    }

    private void applyFitWidth() {
        double width = Math.max(260, formattedScroll.getViewportBounds().getWidth() - 32);
        for (ImageView imageView : imageViews) {
            imageView.setScaleX(1.0);
            imageView.setScaleY(1.0);
            imageView.setFitWidth(width);
        }
    }

    private void setVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void showTextStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-text", "error-text");
        statusLabel.getStyleClass().add(success ? "success-text" : "error-text");
    }

    private String formatFileInfo(FileInfoModel info) {
        return "Tên tệp: " + info.getFileName()
                + "\nĐường dẫn: " + info.getFilePath()
                + "\nLoại tệp: " + info.getFileType()
                + "\nKích thước: " + info.getFileSize()
                + "\nNgày tạo: " + info.getCreatedAt()
                + "\nNgày chỉnh sửa: " + info.getLastModified();
    }
}
