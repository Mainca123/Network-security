package controller;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import model.SystemLog;
import service.LogService;
import util.AlertUtil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_NOTIFICATION_ITEMS = 6;

    @FXML private StackPane contentPane;
    @FXML private Label statusLabel;
    @FXML private Label clockLabel;
    @FXML private Label currentPageLabel;
    @FXML private Button notificationButton;
    @FXML private Button manualButton;
    @FXML private Button autoKeyButton;
    @FXML private Button signButton;
    @FXML private Button verifyButton;
    @FXML private Button logButton;

    private final LogService logService = LogService.getInstance();
    private Timeline clockTimeline;
    private Timeline notificationTimeline;

    @FXML
    private void initialize() {
        startClock();
        startNotificationRefresh();
        updateNotificationButton();
        loadView("/fxml/manual-parameter.fxml", manualButton, "Tạo tham số DSA");
    }

    @FXML
    private void showNotifications() {
        logService.reload();
        List<SystemLog> logs = logService.getLogs();
        updateNotificationButton();

        if (logs.isEmpty()) {
            AlertUtil.info(window(), "Thông báo hệ thống",
                    "Chưa có thông báo nào.\n\nCác thao tác tạo khóa, ký dữ liệu, xác thực và lưu tham số sẽ được ghi nhận tại đây.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Thông báo gần đây\n\n");
        logs.stream()
                .limit(MAX_NOTIFICATION_ITEMS)
                .forEach(log -> builder.append(logService.format(log))
                        .append(" (").append(logService.displayStatus(log.getStatus())).append(")")
                        .append("\n\n"));
        builder.append("Tổng số nhật ký hiện có: ").append(logs.size()).append(".");
        AlertUtil.info(window(), "Thông báo hệ thống", builder.toString().trim());
    }

    @FXML
    private void showManualParameter() {
        loadView("/fxml/manual-parameter.fxml", manualButton, "Tạo tham số DSA");
    }

    @FXML
    private void showAutoKey() {
        loadView("/fxml/auto-key.fxml", autoKeyButton, "Quản lý khóa DSA");
    }

    @FXML
    private void showSign() {
        loadView("/fxml/sign.fxml", signButton, "Ký dữ liệu");
    }

    @FXML
    private void showVerify() {
        loadView("/fxml/verify.fxml", verifyButton, "Xác thực chữ ký");
    }

    @FXML
    private void showLog() {
        loadView("/fxml/log.fxml", logButton, "Nhật ký hệ thống");
    }

    private void loadView(String resource, Button selectedButton, String pageTitle) {
        try {
            Parent view = FXMLLoader.load(MainController.class.getResource(resource));
            contentPane.getChildren().setAll(view);
            animateView(view);
            setActiveButton(selectedButton);
            currentPageLabel.setText(pageTitle);
            statusLabel.setText("Sẵn sàng");
            updateNotificationButton();
        } catch (IOException ex) {
            AlertUtil.error(window(), "Lỗi giao diện", "Không thể tải màn hình: " + ex.getMessage());
        }
    }

    private void setActiveButton(Button selectedButton) {
        Button[] buttons = {manualButton, autoKeyButton, signButton, verifyButton, logButton};
        for (Button button : buttons) {
            button.getStyleClass().remove("nav-button-active");
        }
        if (!selectedButton.getStyleClass().contains("nav-button-active")) {
            selectedButton.getStyleClass().add("nav-button-active");
        }
    }

    private void animateView(Parent view) {
        view.setOpacity(0);
        view.setTranslateY(8);

        FadeTransition fade = new FadeTransition(Duration.millis(140), view);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(150), view);
        slide.setFromY(8);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void startClock() {
        updateClock();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateClock()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    private void startNotificationRefresh() {
        notificationTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> updateNotificationButton()));
        notificationTimeline.setCycleCount(Timeline.INDEFINITE);
        notificationTimeline.play();
    }

    private void updateClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
    }

    private void updateNotificationButton() {
        logService.reload();
        int count = logService.getLogs().size();
        notificationButton.setText("Thông báo (" + count + ")");
    }

    private javafx.stage.Window window() {
        return contentPane.getScene() == null ? null : contentPane.getScene().getWindow();
    }
}
