package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.DSAKeyPairModel;
import model.DSASignatureModel;
import model.FileInfoModel;
import service.DSAParameterService;
import service.DSAVerifyService;
import service.FilePreviewService;
import service.FileService;
import service.LogService;
import util.AlertUtil;

import java.io.File;

public class VerifyController {
    @FXML private VBox root;
    @FXML private TextArea textInput;
    @FXML private Label textSignatureLabel;
    @FXML private Label textPublicKeyLabel;
    @FXML private Label textStatusMessage;
    @FXML private Label originalFileLabel;
    @FXML private Label fileSignatureLabel;
    @FXML private Label filePublicKeyLabel;
    @FXML private ScrollPane fileFormattedScroll;
    @FXML private VBox fileFormattedPages;
    @FXML private WebView fileDocxWebView;
    @FXML private TabPane fileWorkbookTabs;
    @FXML private TextArea filePlainTextArea;
    @FXML private TextArea fileInfoArea;
    @FXML private TextArea fileHashArea;
    @FXML private Label fileStatusMessage;
    
    @FXML private VBox emptyStatePreview;

    private final DSAVerifyService verifyService = new DSAVerifyService();
    private final FileService fileService = new FileService();
    private final FilePreviewService previewService = new FilePreviewService();
    private final DSAParameterService parameterService = new DSAParameterService();

    private DSASignatureModel textSignature;
    private DSASignatureModel fileSignature;
    private DSAKeyPairModel textPublicKey;
    private DSAKeyPairModel filePublicKey;
    private File originalFile;
    private DocumentPreviewManager filePreviewManager;

    @FXML
    private void initialize() {
        filePreviewManager = new DocumentPreviewManager(previewService, fileFormattedScroll, fileFormattedPages,
                fileDocxWebView, fileWorkbookTabs, filePlainTextArea, fileInfoArea, fileHashArea, fileStatusMessage);
        setTextStatus("Sẵn sàng xác thực văn bản", true);
        setFileStatus("Sẵn sàng xác thực tệp", true);
    }

    @FXML
    private void handleChooseTextSignature() {
        DSASignatureModel signature = chooseSignature("TEXT");
        if (signature != null) {
            textSignature = signature;
            textSignatureLabel.setText("Đã chọn chữ ký văn bản");
            setTextStatus("Chữ ký văn bản hợp lệ", true);
        }
    }

    @FXML
    private void handleChooseTextPublicKey() {
        DSAKeyPairModel key = choosePublicKey();
        if (key != null) {
            textPublicKey = key;
            textPublicKeyLabel.setText("Đã chọn khóa công khai");
            setTextStatus("Khóa công khai hợp lệ", true);
        }
    }

    @FXML
    private void handleVerifyText() {
        try {
            if (textInput.getText() == null || textInput.getText().isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập nội dung văn bản gốc cần xác thực.");
            }
            if (textSignature == null) {
                throw new IllegalArgumentException("Vui lòng chọn tệp chữ ký JSON.");
            }
            if (textPublicKey == null) {
                throw new IllegalArgumentException("Vui lòng chọn tệp khóa công khai JSON.");
            }
            DSAVerifyService.VerificationResult result =
                    verifyService.verifyText(textInput.getText(), textSignature, textPublicKey);
            setTextStatus(result.toDetailedMessage(), result.isValid());
            LogService.getInstance().addLog("VERIFY", result.getMessage(), result.isValid() ? "SUCCESS" : "FAIL");
        } catch (Exception ex) {
            LogService.getInstance().addLog("VERIFY", "Xác thực văn bản thất bại: " + ex.getMessage(), "FAIL");
            setTextStatus(ex.getMessage(), false);
            AlertUtil.error(window(), "Lỗi xác thực", ex.getMessage());
        }
    }

    @FXML
    private void handleClearText() {
        textInput.clear();
        textSignature = null;
        textPublicKey = null;
        textSignatureLabel.setText("Chưa chọn chữ ký");
        textPublicKeyLabel.setText("Chưa chọn khóa công khai");
        setTextStatus("Đã làm mới phần xác thực văn bản", true);
    }

    @FXML
    private void handleChooseOriginalFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn tệp gốc");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tệp được hỗ trợ", "*.txt", "*.docx", "*.pdf", "*.xlsx", "*.json",
                        "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Tất cả tệp", "*.*")
        );
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return;
        }
        try {
            originalFile = file;
            FileInfoModel info = previewService.readFileInfo(file.toPath());
            originalFileLabel.setText(file.getAbsolutePath());
            setFileStatus("Đã đọc tệp gốc", true);
            filePreviewManager.render(file, info);
            
            if(emptyStatePreview != null) {
                emptyStatePreview.setVisible(false); emptyStatePreview.setManaged(false);
            }
        } catch (Exception ex) {
            setFileStatus("Không thể đọc tệp gốc: " + ex.getMessage(), false);
            AlertUtil.error(window(), "Lỗi đọc tệp", ex.getMessage());
        }
    }

    @FXML
    private void handleChooseFileSignature() {
        DSASignatureModel signature = chooseSignature("FILE");
        if (signature != null) {
            fileSignature = signature;
            fileSignatureLabel.setText("Đã chọn chữ ký tệp");
            setFileStatus("Chữ ký tệp hợp lệ", true);
        }
    }

    @FXML
    private void handleChooseFilePublicKey() {
        DSAKeyPairModel key = choosePublicKey();
        if (key != null) {
            filePublicKey = key;
            filePublicKeyLabel.setText("Đã chọn khóa công khai");
            setFileStatus("Khóa công khai hợp lệ", true);
        }
    }

    @FXML
    private void handleVerifyFile() {
        try {
            if (originalFile == null) {
                throw new IllegalArgumentException("Vui lòng chọn tệp gốc cần xác thực.");
            }
            if (fileSignature == null) {
                throw new IllegalArgumentException("Vui lòng chọn tệp chữ ký JSON.");
            }
            if (filePublicKey == null) {
                throw new IllegalArgumentException("Vui lòng chọn tệp khóa công khai JSON.");
            }
            DSAVerifyService.VerificationResult result =
                    verifyService.verifyFile(originalFile.toPath(), fileSignature, filePublicKey);
            setFileStatus(result.toDetailedMessage(), result.isValid());
            if (result.getInitialHash() != null || result.getCurrentHash() != null) {
                fileHashArea.setText(formatHashComparison(result));
            }
            LogService.getInstance().addLog("VERIFY", result.getMessage(), result.isValid() ? "SUCCESS" : "FAIL");
        } catch (Exception ex) {
            LogService.getInstance().addLog("VERIFY", "Xác thực tệp thất bại: " + ex.getMessage(), "FAIL");
            setFileStatus(ex.getMessage(), false);
            AlertUtil.error(window(), "Lỗi xác thực", ex.getMessage());
        }
    }

    @FXML
    private void handleClearFile() {
        originalFile = null;
        fileSignature = null;
        filePublicKey = null;
        originalFileLabel.setText("Chưa chọn tệp gốc");
        fileSignatureLabel.setText("Chưa chọn chữ ký");
        filePublicKeyLabel.setText("Chưa chọn khóa công khai");
        filePreviewManager.clear();
        setFileStatus("Đã làm mới phần xác thực tệp", true);
        
        if(emptyStatePreview != null) {
            emptyStatePreview.setVisible(true); emptyStatePreview.setManaged(true);
        }
    }

    @FXML
    private void handleShowGuide() {
        AlertUtil.info(window(), "Hướng dẫn xác thực chữ ký",
                "Xác thực văn bản:\n\n"
                        + "1. Mở thẻ Xác thực văn bản và nhập đúng nội dung gốc cần kiểm tra.\n\n"
                        + "2. Chọn tệp chữ ký JSON đã được tạo ở màn Ký dữ liệu.\n\n"
                        + "3. Chọn khóa công khai tương ứng với khóa bí mật đã dùng để ký.\n\n"
                        + "4. Nhấn Xác thực. Hệ thống sẽ tính lại mã băm và kiểm tra chữ ký r, s.\n\n"
                        + "Xác thực tệp:\n\n"
                        + "1. Mở thẻ Xác thực tệp và chọn tệp gốc.\n\n"
                        + "2. Chọn tệp chữ ký của tài liệu đó và khóa công khai tương ứng.\n\n"
                        + "3. Nhấn Xác thực ngay. Nếu tệp bị chỉnh sửa sau khi ký, mã băm hiện tại sẽ khác mã băm ban đầu.\n\n"
                        + "Ý nghĩa kết quả:\n\n"
                        + "Xác thực thành công nghĩa là dữ liệu còn nguyên vẹn và chữ ký khớp với khóa công khai. Xác thực thất bại có thể do nội dung bị chỉnh sửa, chữ ký sai định dạng hoặc khóa công khai không đúng cặp.");
    }

    private DSASignatureModel chooseSignature(String expectedType) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn tệp chữ ký");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp JSON", "*.json"));
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return null;
        }

        try {
            DSASignatureModel signature = fileService.readJson(file, DSASignatureModel.class);
            if (signature == null || !"DSA".equalsIgnoreCase(signature.getAlgorithm())) {
                throw new IllegalArgumentException("Tệp này không phải chữ ký DSA hợp lệ.");
            }
            return signature;
        } catch (Exception ex) {
            LogService.getInstance().addLog("VERIFY", "Lỗi tệp chữ ký: " + ex.getMessage(), "FAIL");
            AlertUtil.error(window(), "Tệp chữ ký không hợp lệ", ex.getMessage());
            return null;
        }
    }

    private DSAKeyPairModel choosePublicKey() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn khóa công khai");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp JSON", "*.json"));
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return null;
        }

        try {
            DSAKeyPairModel key = fileService.readJson(file, DSAKeyPairModel.class);
            parameterService.validatePublicKey(key);
            return key;
        } catch (Exception ex) {
            LogService.getInstance().addLog("VERIFY", "Lỗi định dạng khóa công khai: " + ex.getMessage(), "FAIL");
            AlertUtil.error(window(), "Khóa công khai không hợp lệ", ex.getMessage());
            return null;
        }
    }

    @FXML
    private void handleFileZoomIn() {
        filePreviewManager.zoomIn();
    }

    @FXML
    private void handleFileZoomOut() {
        filePreviewManager.zoomOut();
    }

    @FXML
    private void handleFileFitWidth() {
        filePreviewManager.fitWidth();
    }

    private String formatHashComparison(DSAVerifyService.VerificationResult result) {
        return "Mã băm ban đầu\n" + nullToEmpty(result.getInitialHash())
                + "\n\nMã băm hiện tại\n" + nullToEmpty(result.getCurrentHash());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private void setTextStatus(String message, boolean success) {
        setStatus(textStatusMessage, message, success);
    }

    private void setFileStatus(String message, boolean success) {
        setStatus(fileStatusMessage, message, success);
    }

    private void setStatus(Label label, String message, boolean success) {
        label.setText(message);
        label.getStyleClass().removeAll("success-text", "error-text");
        label.getStyleClass().add(success ? "success-text" : "error-text");
    }

    private Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }
}
