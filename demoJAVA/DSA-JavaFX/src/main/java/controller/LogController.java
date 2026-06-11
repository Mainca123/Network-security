package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import model.SystemLog;
import service.LogService;

import java.util.List;
import java.util.Locale;

public class LogController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private ListView<SystemLog> logListView;
    @FXML private Label summaryLabel;

    private final LogService logService = LogService.getInstance();
    private final ObservableList<SystemLog> visibleLogs = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        filterCombo.getItems().setAll(
                "Tất cả",
                "Tạo khóa",
                "Ký thành công",
                "Ký thất bại",
                "Xác thực thành công",
                "Xác thực thất bại"
        );
        filterCombo.getSelectionModel().selectFirst();
        logListView.setItems(visibleLogs);
        logListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(SystemLog item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("log-success", "log-fail");
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(logService.format(item) + " (" + logService.displayStatus(item.getStatus()) + ")");
                    getStyleClass().add("SUCCESS".equalsIgnoreCase(item.getStatus()) ? "log-success" : "log-fail");
                }
            }
        });
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        filterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        logService.reload();
        applyFilters();
    }

    @FXML
    private void handleClearLogs() {
        logService.clearLogs();
        applyFilters();
    }

    private void applyFilters() {
        List<SystemLog> allLogs = logService.getLogs();
        String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase(Locale.ROOT).trim();
        String filter = filterCombo.getValue() == null ? "Tất cả" : filterCombo.getValue();

        visibleLogs.setAll(allLogs.stream()
                .filter(log -> matchesSearch(log, keyword))
                .filter(log -> matchesFilter(log, filter))
                .toList());

        if (allLogs.isEmpty()) {
            summaryLabel.setText("Chưa có nhật ký hệ thống.");
        } else {
            summaryLabel.setText("Hiển thị " + visibleLogs.size() + " / " + allLogs.size()
                    + " nhật ký. Tệp: " + logService.getLogPath());
        }
    }

    private boolean matchesSearch(SystemLog log, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        String source = (log.getType() + " " + log.getMessage() + " " + log.getStatus()).toLowerCase(Locale.ROOT);
        return source.contains(keyword);
    }

    private boolean matchesFilter(SystemLog log, String filter) {
        return switch (filter) {
            case "Tạo khóa" -> "KEY".equalsIgnoreCase(log.getType());
            case "Ký thành công" -> "SIGN".equalsIgnoreCase(log.getType()) && "SUCCESS".equalsIgnoreCase(log.getStatus());
            case "Ký thất bại" -> "SIGN".equalsIgnoreCase(log.getType()) && "FAIL".equalsIgnoreCase(log.getStatus());
            case "Xác thực thành công" -> "VERIFY".equalsIgnoreCase(log.getType()) && "SUCCESS".equalsIgnoreCase(log.getStatus());
            case "Xác thực thất bại" -> "VERIFY".equalsIgnoreCase(log.getType()) && "FAIL".equalsIgnoreCase(log.getStatus());
            default -> true;
        };
    }
}
