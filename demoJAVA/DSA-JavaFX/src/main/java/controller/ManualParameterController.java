package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.DSAParameters;
import service.DSAParameterService;
import service.FileService;
import service.LogService;
import util.AlertUtil;
import util.DateTimeUtil;

import java.io.File;

public class ManualParameterController {
    @FXML private VBox root;
    @FXML private TextArea pField;
    @FXML private TextArea qField;
    @FXML private TextArea gField;
    @FXML private TextArea xField;
    @FXML private TextArea yField;
    @FXML private Label statusMessage;

    private final DSAParameterService parameterService = new DSAParameterService();
    private final FileService fileService = new FileService();
    private DSAParameters currentValidParameters;

    @FXML
    private void handleValidate() {
        try {
            DSAParameters parameters = readParameters();
            fillGeneratedParameters(parameters);
            DSAParameterService.ParameterValidationResult result = parameterService.validate(parameters);
            if (result.isValid()) {
                parameters.setCreatedAt(DateTimeUtil.nowStorage());
                currentValidParameters = parameters;
                setStatus(formatValidationStatus(result), true);
            } else {
                currentValidParameters = null;
                setStatus(formatValidationStatus(result), false);
            }
        } catch (IllegalArgumentException ex) {
            currentValidParameters = null;
            gField.clear();
            yField.clear();
            setStatus("Không thể tự sinh tham số:\n" + ex.getMessage(), false);
        }
    }

    @FXML
    private void handleSave() {
        handleValidate();
        if (currentValidParameters == null) {
            AlertUtil.warning(window(), "Chưa thể lưu", "Vui lòng sửa các lỗi tham số trước khi lưu.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu tham số DSA");
        chooser.setInitialFileName("tham-so-dsa.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp JSON", "*.json"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }

        try {
            currentValidParameters.setCreatedAt(DateTimeUtil.nowStorage());
            fileService.writeJson(file, currentValidParameters);
            LogService.getInstance().addLog("PARAMETER", "Lưu tham số thủ công thành công", "SUCCESS");
            setStatus("Đã lưu tham số: " + file.getAbsolutePath(), true);
        } catch (Exception ex) {
            LogService.getInstance().addLog("PARAMETER", "Lưu tham số thủ công thất bại", "FAIL");
            AlertUtil.error(window(), "Lỗi lưu tệp", ex.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        pField.clear();
        qField.clear();
        gField.clear();
        xField.clear();
        yField.clear();
        currentValidParameters = null;
        setStatus("Sẵn sàng nhập tham số", true);
    }

    @FXML
    private void handleShowGuide() {
        AlertUtil.info(window(), "Hướng dẫn nhập tham số DSA",
                "Cách nhập tham số thủ công:\n\n"
                        + "1. Nhập p là số nguyên tố lớn.\n\n"
                        + "2. Nhập q là số nguyên tố và q phải chia hết p - 1.\n\n"
                        + "3. Nhập x là khóa bí mật, giá trị phải nằm trong khoảng 0 < x < q.\n\n"
                        + "4. Nhấn Kiểm tra tham số để hệ thống tự sinh g theo công thức g = h^((p - 1) / q) mod p và tự tính y = g^x mod p.\n\n"
                        + "5. Nếu toàn bộ hợp lệ, có thể nhấn Lưu tham số để xuất ra tệp JSON.\n\n"
                        + "Gợi ý: nếu chưa có p và q phù hợp, nên dùng màn Tạo khóa để hệ thống tự sinh bộ tham số DSA.");
    }

    private DSAParameters readParameters() {
        return parameterService.generateFromManualInput(
                pField.getText(),
                qField.getText(),
                xField.getText()
        );
    }

    private void fillGeneratedParameters(DSAParameters parameters) {
        pField.setText(parameters.getP());
        qField.setText(parameters.getQ());
        gField.setText(parameters.getG());
        xField.setText(parameters.getX());
        yField.setText(parameters.getY());
    }

    private void setStatus(String message, boolean success) {
        statusMessage.setText(message);
        statusMessage.getStyleClass().removeAll("success-text", "error-text");
        statusMessage.getStyleClass().add(success ? "success-text" : "error-text");
    }

    private String formatValidationStatus(DSAParameterService.ParameterValidationResult result) {
        return statusLine("P", hasError(result, "P"))
                + "\n" + statusLine("Q", hasError(result, "Q"))
                + "\n" + statusLine("G", hasError(result, "G"))
                + "\n" + statusLine("X", hasError(result, "X"))
                + "\n" + statusLine("Y", hasError(result, "Y"))
                + (result.isValid() ? "\nTham số hợp lệ" : "\n\n" + String.join("\n", result.getErrors()));
    }

    private boolean hasError(DSAParameterService.ParameterValidationResult result, String field) {
        return result.getErrors().stream().anyMatch(error -> error.toUpperCase().startsWith(field)
                || error.toUpperCase().contains(field + " "));
    }

    private String statusLine(String field, boolean hasError) {
        return field + ": " + (hasError ? "không hợp lệ" : "hợp lệ");
    }

    private Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }
}
