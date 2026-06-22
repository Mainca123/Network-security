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
        DSAParameters parameters = readParameters();
        
        String pStr = parameters.getP();
        String qStr = parameters.getQ();
        String gStr = parameters.getG();
        String xStr = parameters.getX();
        String yStr = parameters.getY();
        
        if (pStr != null && !pStr.trim().isEmpty() && qStr != null && !qStr.trim().isEmpty()
            && (gStr == null || gStr.trim().isEmpty())
            && (xStr == null || xStr.trim().isEmpty())
            && (yStr == null || yStr.trim().isEmpty())) {
            
            try {
                java.math.BigInteger p = util.BigIntegerUtil.parseRequired(pStr, "P");
                java.math.BigInteger q = util.BigIntegerUtil.parseRequired(qStr, "Q");
                
                if (p.isProbablePrime(80) && q.isProbablePrime(80) && p.subtract(java.math.BigInteger.ONE).mod(q).equals(java.math.BigInteger.ZERO)) {
                    java.security.SecureRandom secureRandom = new java.security.SecureRandom();
                    java.math.BigInteger exponent = p.subtract(java.math.BigInteger.ONE).divide(q);
                    java.math.BigInteger g;
                    do {
                        java.math.BigInteger h = util.BigIntegerUtil.randomBetween(java.math.BigInteger.TWO, p.subtract(java.math.BigInteger.TWO), secureRandom);
                        g = h.modPow(exponent, p);
                    } while (g.compareTo(java.math.BigInteger.ONE) <= 0);
                    
                    java.math.BigInteger x = util.BigIntegerUtil.randomBetween(java.math.BigInteger.ONE, q.subtract(java.math.BigInteger.ONE), secureRandom);
                    java.math.BigInteger y = g.modPow(x, p);
                    
                    gField.setText(g.toString());
                    xField.setText(x.toString());
                    yField.setText(y.toString());
                    parameters = readParameters();
                }
            } catch (Exception e) {
                // Ignore and let standard validation handle it
            }
        }
        
        DSAParameterService.ParameterValidationResult result = parameterService.validate(parameters);
        if (result.isValid()) {
            parameters.setCreatedAt(DateTimeUtil.nowStorage());
            currentValidParameters = parameters;
            setStatus(formatValidationStatus(result), true);
        } else {
            currentValidParameters = null;
            setStatus(formatValidationStatus(result), false);
        }
    }

    @FXML
    private void handleSave() {
        if (currentValidParameters == null) {
            handleValidate();
        }
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
                        + "3. Nhập g là phần tử sinh hợp lệ trong miền tham số DSA.\n\n"
                        + "4. Nhập x là khóa bí mật, giá trị phải nằm trong khoảng hợp lệ.\n\n"
                        + "5. Nhập y là khóa công khai, phải thỏa y = g^x mod p.\n\n"
                        + "6. Nhấn Kiểm tra tham số. Nếu toàn bộ hợp lệ, có thể nhấn Lưu tham số để xuất ra tệp JSON.\n\n"
                        + "Gợi ý: nếu chưa có bộ tham số sẵn, nên dùng màn Tạo khóa để hệ thống tự sinh p, q, g, x, y nhằm tránh nhập sai.");
    }

    private DSAParameters readParameters() {
        return new DSAParameters(
                pField.getText(),
                qField.getText(),
                gField.getText(),
                xField.getText(),
                yField.getText(),
                DateTimeUtil.nowStorage()
        );
    }

    private void setStatus(String message, boolean success) {
        statusMessage.setText(message);
        statusMessage.getStyleClass().removeAll("success-text", "error-text");
        statusMessage.getStyleClass().add(success ? "success-text" : "error-text");
    }

    private String formatValidationStatus(DSAParameterService.ParameterValidationResult result) {
        if (result.isValid()) {
            return "Tất cả tham số hợp lệ – có thể lưu tệp";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Phát hiện ").append(result.getErrors().size()).append(" lỗi:\n");
        for (String error : result.getErrors()) {
            sb.append("  • ").append(error).append("\n");
        }
        return sb.toString().trim();
    }

    private Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }
}
