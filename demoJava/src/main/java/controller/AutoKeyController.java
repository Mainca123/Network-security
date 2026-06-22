package controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.DSAKeyPairModel;
import service.DSAKeyService;
import service.FileService;
import service.LogService;
import util.AlertUtil;

import java.io.File;

public class AutoKeyController {
    @FXML private VBox root;
    @FXML private ToggleButton security1024Button;
    @FXML private ToggleButton security2048N224Button;
    @FXML private ToggleButton security2048N256Button;
    @FXML private ToggleButton security3072Button;
    @FXML private ComboBox<String> hashCombo;
    @FXML private TextArea pOutput;
    @FXML private TextArea qOutput;
    @FXML private TextArea gOutput;
    @FXML private TextArea xOutput;
    @FXML private TextArea yOutput;
    @FXML private Label statusMessage;
    @FXML private Button generateParameterButton;
    @FXML private Button generateKeyButton;
    @FXML private ProgressIndicator loadingIndicator;

    private final DSAKeyService keyService = new DSAKeyService();
    private final FileService fileService = new FileService();
    private DSAKeyPairModel currentParameters;
    private DSAKeyPairModel currentKeyPair;
    private boolean showPrivateKey;

    @FXML
    private void initialize() {
        ToggleGroup securityGroup = new ToggleGroup();
        configureSecurityButton(security1024Button, securityGroup, "L = 1024, N = 160");
        configureSecurityButton(security2048N224Button, securityGroup, "L = 2048, N = 224");
        configureSecurityButton(security2048N256Button, securityGroup, "L = 2048, N = 256");
        configureSecurityButton(security3072Button, securityGroup, "L = 3072, N = 256");
        security1024Button.setSelected(true);
        hashCombo.getItems().setAll("SHA-1", "SHA-256", "SHA-512");
        hashCombo.getSelectionModel().select("SHA-256");
        setStatus("Chọn mức bảo mật rồi tạo tham số", true);
    }

    @FXML
    private void handleGenerateParameters() {
        DSAKeyService.SecurityLevel level = DSAKeyService.parseSecurityLevel(selectedSecurityLevelValue());
        setWorking(true, "Đang sinh tham số DSA, vui lòng chờ...");

        Task<DSAKeyPairModel> task = new Task<>() {
            @Override
            protected DSAKeyPairModel call() {
                return keyService.generateParameters(level);
            }
        };
        task.setOnSucceeded(event -> {
            currentParameters = task.getValue();
            currentKeyPair = null;
            showPrivateKey = false;
            fillOutputs(currentParameters);
            LogService.getInstance().addLog("KEY", "Tạo tham số tự động thành công", "SUCCESS");
            setWorking(false, "Tạo tham số thành công. Có thể tạo khóa.");
        });
        task.setOnFailed(event -> {
            LogService.getInstance().addLog("KEY", "Tạo tham số tự động thất bại", "FAIL");
            setWorking(false, "Tạo tham số thất bại: " + task.getException().getMessage());
        });
        Thread worker = new Thread(task, "dsa-parameter-generator");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleGenerateKey() {
        if (currentParameters == null) {
            AlertUtil.warning(window(), "Thiếu tham số", "Vui lòng tạo tham số trước khi tạo khóa.");
            return;
        }
        try {
            currentKeyPair = keyService.generateKeyPair(currentParameters);
            fillOutputs(currentKeyPair);
            LogService.getInstance().addLog("KEY", "Tạo khóa tự động thành công", "SUCCESS");
            setStatus("Tạo khóa thành công bằng " + hashCombo.getValue(), true);
        } catch (Exception ex) {
            LogService.getInstance().addLog("KEY", "Tạo khóa tự động thất bại", "FAIL");
            AlertUtil.error(window(), "Lỗi tạo khóa", ex.getMessage());
        }
    }

    @FXML
    private void handleTogglePrivateKey() {
        showPrivateKey = !showPrivateKey;
        updatePrivateKeyDisplay();
    }

    @FXML
    private void handleSavePrivateKey() {
        if (ensureKeyPair()) {
            saveKey(keyService.toPrivateKey(currentKeyPair), "khoa-bi-mat-dsa.json");
        }
    }

    @FXML
    private void handleSavePublicKey() {
        if (ensureKeyPair()) {
            saveKey(keyService.toPublicKey(currentKeyPair), "khoa-cong-khai-dsa.json");
        }
    }

    @FXML
    private void handleSaveFullKeyPair() {
        if (ensureKeyPair()) {
            saveKey(keyService.toFullKeyPair(currentKeyPair), "cap-khoa-dsa.json");
        }
    }

    @FXML
    private void handleClear() {
        currentParameters = null;
        currentKeyPair = null;
        showPrivateKey = false;
        pOutput.clear();
        qOutput.clear();
        gOutput.clear();
        xOutput.clear();
        yOutput.clear();
        setStatus("Đã làm mới màn hình tạo khóa", true);
    }

    @FXML
    private void handleShowGuide() {
        AlertUtil.info(window(), "Hướng dẫn tạo khóa DSA",
                "Quy trình tạo khóa:\n\n"
                        + "1. Chọn mức bảo mật phù hợp. Mức 1024 / 160 dùng cho thử nghiệm nhanh, các mức 2048 hoặc 3072 phù hợp hơn khi cần độ an toàn cao.\n\n"
                        + "2. Chọn thuật toán băm. SHA-256 là lựa chọn khuyến nghị cho đồ án và kiểm thử thông thường.\n\n"
                        + "3. Nhấn Tạo tham số để sinh p, q, g. Bước này có thể mất vài giây tùy mức bảo mật.\n\n"
                        + "4. Nhấn Tạo khóa để sinh khóa bí mật x và khóa công khai y.\n\n"
                        + "5. Có thể dùng Hiện/Ẩn khóa bí mật để kiểm tra giá trị x khi cần.\n\n"
                        + "6. Lưu khóa bí mật, khóa công khai hoặc cả cặp khóa ra tệp JSON để dùng ở màn Ký dữ liệu và Xác thực chữ ký.\n\n"
                        + "Lưu ý bảo mật: không chia sẻ khóa bí mật. Chữ ký số phải được tạo bằng khóa bí mật và xác thực bằng khóa công khai tương ứng.");
    }

    private void fillOutputs(DSAKeyPairModel model) {
        pOutput.setText(model.getP());
        qOutput.setText(model.getQ());
        gOutput.setText(model.getG());
        yOutput.setText(model.getY() == null ? "" : model.getY());
        updatePrivateKeyDisplay();
    }

    private void updatePrivateKeyDisplay() {
        if (currentKeyPair == null || currentKeyPair.getX() == null) {
            xOutput.clear();
            return;
        }
        xOutput.setText(showPrivateKey ? currentKeyPair.getX() : "******** (khóa bí mật đang ẩn)");
    }

    private boolean ensureKeyPair() {
        if (currentKeyPair == null || !currentKeyPair.hasPrivateKey() || !currentKeyPair.hasPublicKey()) {
            AlertUtil.warning(window(), "Chưa có khóa", "Vui lòng tạo khóa trước khi lưu.");
            return false;
        }
        return true;
    }

    private void saveKey(DSAKeyPairModel key, String initialName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu khóa DSA");
        chooser.setInitialFileName(initialName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp JSON", "*.json"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            fileService.writeJson(file, key);
            LogService.getInstance().addLog("KEY", "Lưu " + displayKeyType(key.getType()) + " thành công", "SUCCESS");
            setStatus("Đã lưu khóa: " + file.getAbsolutePath(), true);
        } catch (Exception ex) {
            LogService.getInstance().addLog("KEY", "Lưu khóa thất bại", "FAIL");
            AlertUtil.error(window(), "Lỗi lưu khóa", ex.getMessage());
        }
    }

    private String displayKeyType(String type) {
        return switch (type == null ? "" : type) {
            case "PRIVATE_KEY" -> "khóa bí mật";
            case "PUBLIC_KEY" -> "khóa công khai";
            case "KEY_PAIR" -> "cặp khóa";
            default -> "khóa";
        };
    }

    private void setWorking(boolean working, String message) {
        generateParameterButton.setDisable(working);
        generateKeyButton.setDisable(working);
        loadingIndicator.setVisible(working);
        loadingIndicator.setManaged(working);
        setStatus(message, !message.toLowerCase().contains("thất bại"));
    }

    private void configureSecurityButton(ToggleButton button, ToggleGroup group, String value) {
        button.setToggleGroup(group);
        button.setUserData(value);
    }

    private String selectedSecurityLevelValue() {
        ToggleButton[] buttons = {
                security1024Button,
                security2048N224Button,
                security2048N256Button,
                security3072Button
        };
        for (ToggleButton button : buttons) {
            if (button.isSelected()) {
                return button.getUserData().toString();
            }
        }
        return "L = 1024, N = 160";
    }

    private void setStatus(String message, boolean success) {
        statusMessage.setText(message);
        statusMessage.getStyleClass().removeAll("success-text", "error-text");
        statusMessage.getStyleClass().add(success ? "success-text" : "error-text");
    }

    private Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }
}
