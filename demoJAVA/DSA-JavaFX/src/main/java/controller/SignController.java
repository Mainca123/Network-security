package controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import model.DSAKeyPairModel;
import model.DSASignatureModel;
import model.FileInfoModel;
import service.DSAParameterService;
import service.DSASignService;
import service.FilePreviewService;
import service.FileService;
import service.LogService;
import util.AlertUtil;

import java.io.File;

public class SignController {
    @FXML private VBox root;
    @FXML private TextArea textInput;
    @FXML private ComboBox<String> textHashCombo;
    @FXML private Label textKeyLabel;
    @FXML private TextField textRField;
    @FXML private TextField textSField;
    @FXML private TextArea textHashArea;
    @FXML private Label textStatusMessage;
    @FXML private Label fileLabel;
    @FXML private ScrollPane fileFormattedScroll;
    @FXML private VBox fileFormattedPages;
    @FXML private WebView fileDocxWebView;
    @FXML private TabPane fileWorkbookTabs;
    @FXML private TextArea filePlainTextArea;
    @FXML private TextArea fileInfoArea;
    @FXML private TextArea fileHashArea;
    @FXML private ComboBox<String> fileHashCombo;
    @FXML private Label fileKeyLabel;
    @FXML private TextField fileRField;
    @FXML private TextField fileSField;
    @FXML private Label fileStatusMessage;

    private final DSASignService signService = new DSASignService();
    private final FilePreviewService previewService = new FilePreviewService();
    private final FileService fileService = new FileService();
    private final DSAParameterService parameterService = new DSAParameterService();
    private DSAKeyPairModel textPrivateKey;
    private DSAKeyPairModel filePrivateKey;
    private DSASignatureModel textSignature;
    private DSASignatureModel fileSignature;
    private File selectedFile;
    private DocumentPreviewManager filePreviewManager;

    @FXML
    private void initialize() {
        textHashCombo.getItems().setAll("SHA-1", "SHA-256", "SHA-512");
        fileHashCombo.getItems().setAll("SHA-1", "SHA-256", "SHA-512");
        textHashCombo.getSelectionModel().select("SHA-256");
        fileHashCombo.getSelectionModel().select("SHA-256");
        filePreviewManager = new DocumentPreviewManager(previewService, fileFormattedScroll, fileFormattedPages,
                fileDocxWebView, fileWorkbookTabs, filePlainTextArea, fileInfoArea, fileHashArea, fileStatusMessage);
        setTextStatus("Sẵn sàng ký văn bản", true);
        setFileStatus("Sẵn sàng ký tệp", true);
    }

    @FXML
    private void handleChooseTextPrivateKey() {
        DSAKeyPairModel key = choosePrivateKey();
        if (key != null) {
            textPrivateKey = key;
            textKeyLabel.setText("Đã chọn khóa bí mật");
            setTextStatus("Khóa bí mật hợp lệ", true);
        }
    }

    @FXML
    private void handleSignText() {
        try {
            if (textPrivateKey == null) {
                throw new IllegalArgumentException("Vui lòng chọn khóa bí mật");
            }
            textSignature = signService.signText(textInput.getText(), textPrivateKey, textHashCombo.getValue());
            textRField.setText(textSignature.getR());
            textSField.setText(textSignature.getS());
            textHashArea.setText(textSignature.getHash());
            LogService.getInstance().addLog("SIGN", "Ký văn bản thành công", "SUCCESS");
            setTextStatus("Ký văn bản thành công", true);
        } catch (Exception ex) {
            LogService.getInstance().addLog("SIGN", "Ký văn bản thất bại: " + ex.getMessage(), "FAIL");
            setTextStatus(ex.getMessage(), false);
            AlertUtil.error(window(), "Lỗi ký văn bản", ex.getMessage());
        }
    }

    @FXML
    private void handleSaveTextSignature() {
        saveSignature(textSignature, "chu-ky-van-ban-dsa.json", textStatusMessage);
    }

    @FXML
    private void handleClearText() {
        textInput.clear();
        textRField.clear();
        textSField.clear();
        textHashArea.clear();
        textPrivateKey = null;
        textSignature = null;
        textKeyLabel.setText("Chưa chọn khóa bí mật");
        setTextStatus("Đã làm mới phần ký văn bản", true);
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn tệp cần ký");
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
            selectedFile = file;
            FileInfoModel info = previewService.readFileInfo(file.toPath());
            fileLabel.setText(file.getAbsolutePath());
            setFileStatus("Đã đọc thông tin tệp", true);
            filePreviewManager.render(file, info);
        } catch (Exception ex) {
            setFileStatus("Không thể đọc tệp: " + ex.getMessage(), false);
            AlertUtil.error(window(), "Lỗi đọc tệp", ex.getMessage());
        }
    }

    @FXML
    private void handleChooseFilePrivateKey() {
        DSAKeyPairModel key = choosePrivateKey();
        if (key != null) {
            filePrivateKey = key;
            fileKeyLabel.setText("Đã chọn khóa bí mật");
            setFileStatus("Khóa bí mật hợp lệ", true);
        }
    }

    @FXML
    private void handleSignFile() {
        try {
            if (selectedFile == null) {
                throw new IllegalArgumentException("Vui lòng chọn tệp cần ký");
            }
            if (filePrivateKey == null) {
                throw new IllegalArgumentException("Vui lòng chọn khóa bí mật");
            }
            fileSignature = signService.signFile(selectedFile.toPath(), filePrivateKey, fileHashCombo.getValue());
            fileRField.setText(fileSignature.getR());
            fileSField.setText(fileSignature.getS());
            LogService.getInstance().addLog("SIGN", "Ký tệp " + selectedFile.getName() + " thành công", "SUCCESS");
            setFileStatus("Ký tệp thành công", true);
        } catch (Exception ex) {
            LogService.getInstance().addLog("SIGN", "Ký tệp thất bại: " + ex.getMessage(), "FAIL");
            setFileStatus(ex.getMessage(), false);
            AlertUtil.error(window(), "Lỗi ký tệp", ex.getMessage());
        }
    }

    @FXML
    private void handleSaveFileSignature() {
        saveSignature(fileSignature, "chu-ky-tep-dsa.json", fileStatusMessage);
    }

    @FXML
    private void handleClearFile() {
        selectedFile = null;
        fileSignature = null;
        filePrivateKey = null;
        fileLabel.setText("Chưa chọn tệp");
        fileKeyLabel.setText("Chưa chọn khóa bí mật");
        filePreviewManager.clear();
        fileRField.clear();
        fileSField.clear();
        setFileStatus("Đã làm mới phần ký tệp", true);
    }

    @FXML
    private void handleShowGuide() {
        AlertUtil.info(window(), "Hướng dẫn ký dữ liệu",
                "Ký văn bản:\n\n"
                        + "1. Mở thẻ Ký văn bản và nhập nội dung cần ký.\n\n"
                        + "2. Chọn khóa bí mật DSA hợp lệ từ tệp JSON.\n\n"
                        + "3. Chọn thuật toán băm, nên dùng SHA-256 trong hầu hết trường hợp.\n\n"
                        + "4. Nhấn Ký dữ liệu. Hệ thống sẽ tạo chữ ký r, s và mã băm của nội dung.\n\n"
                        + "5. Nhấn Lưu chữ ký để xuất chữ ký ra tệp JSON phục vụ xác thực.\n\n"
                        + "Ký tệp:\n\n"
                        + "1. Mở thẻ Ký tệp, chọn tệp cần ký và kiểm tra nội dung xem trước.\n\n"
                        + "2. Chọn khóa bí mật, chọn thuật toán băm rồi nhấn Ký tệp.\n\n"
                        + "3. Lưu chữ ký tệp để gửi kèm tài liệu cho người xác thực.\n\n"
                        + "Lưu ý bảo mật: chức năng xem trước chỉ để quan sát. Hệ thống luôn ký trên byte gốc của tệp hoặc mã băm từ byte gốc, không ký nội dung văn bản/HTML đã trích xuất.");
    }

    private DSAKeyPairModel choosePrivateKey() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn khóa bí mật");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp JSON", "*.json"));
        File file = chooser.showOpenDialog(window());
        if (file == null) {
            return null;
        }

        try {
            DSAKeyPairModel key = fileService.readJson(file, DSAKeyPairModel.class);
            parameterService.validatePrivateKey(key);
            return key;
        } catch (Exception ex) {
            AlertUtil.error(window(), "Khóa bí mật không hợp lệ", ex.getMessage());
            return null;
        }
    }

    private void saveSignature(DSASignatureModel signature, String initialName, Label statusLabel) {
        if (signature == null) {
            AlertUtil.warning(window(), "Chưa có chữ ký", "Vui lòng ký dữ liệu trước khi lưu.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu chữ ký DSA");
        chooser.setInitialFileName(initialName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp JSON", "*.json"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }

        try {
            fileService.writeJson(file, signature);
            statusLabel.setText("Đã lưu chữ ký: " + file.getAbsolutePath());
            statusLabel.getStyleClass().removeAll("success-text", "error-text");
            statusLabel.getStyleClass().add("success-text");
        } catch (Exception ex) {
            AlertUtil.error(window(), "Lỗi lưu chữ ký", ex.getMessage());
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
