package service;

import com.google.gson.reflect.TypeToken;
import model.SystemLog;
import util.DateTimeUtil;
import util.JsonUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LogService {
    private static final LogService INSTANCE = new LogService();
    private static final Type LOG_LIST_TYPE = new TypeToken<List<SystemLog>>() {
    }.getType();

    private final Path logPath = Paths.get(System.getProperty("user.home"), ".system-dsa", "system-logs.json");
    private final List<SystemLog> logs = new ArrayList<>();

    private LogService() {
        reload();
    }

    public static LogService getInstance() {
        return INSTANCE;
    }

    public synchronized void addLog(String type, String message, String status) {
        logs.add(new SystemLog(DateTimeUtil.nowStorage(), type, message, status));
        logs.sort(Comparator.comparing(SystemLog::getTimestamp).reversed());
        saveQuietly();
    }

    public synchronized List<SystemLog> getLogs() {
        return new ArrayList<>(logs);
    }

    public synchronized void clearLogs() {
        logs.clear();
        saveQuietly();
    }

    public synchronized void reload() {
        logs.clear();
        if (!Files.exists(logPath)) {
            return;
        }
        try {
            List<SystemLog> savedLogs = JsonUtil.read(logPath, LOG_LIST_TYPE);
            if (savedLogs != null) {
                logs.addAll(savedLogs);
                logs.sort(Comparator.comparing(SystemLog::getTimestamp).reversed());
            }
        } catch (Exception ignored) {
            logs.clear();
        }
    }

    public Path getLogPath() {
        return logPath;
    }

    public String format(SystemLog log) {
        return "[" + displayType(log.getType()) + "] [" + DateTimeUtil.display(log.getTimestamp()) + "] "
                + displayMessage(log.getMessage());
    }

    public String displayStatus(String status) {
        return switch (status == null ? "" : status.toUpperCase()) {
            case "SUCCESS" -> "Thành công";
            case "FAIL", "FAILED" -> "Thất bại";
            case "ERROR" -> "Lỗi";
            case "WARNING" -> "Cảnh báo";
            case "INFO", "INFORMATION" -> "Thông tin";
            default -> "Không xác định";
        };
    }

    private String displayType(String type) {
        return switch (type == null ? "" : type.toUpperCase()) {
            case "KEY" -> "Tạo khóa";
            case "SIGN" -> "Ký dữ liệu";
            case "VERIFY" -> "Xác thực";
            case "PARAMETER" -> "Tham số DSA";
            default -> "Hệ thống";
        };
    }

    private String displayMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        return message
                .replace("Generated Successfully", "Tạo thành công")
                .replace("Verification Success", "Xác thực thành công")
                .replace("Verification Failed", "Xác thực thất bại")
                .replace("Invalid Signature", "Chữ ký không hợp lệ")
                .replace("Signature Error", "Lỗi chữ ký")
                .replace("Private Key", "Khóa bí mật")
                .replace("Public Key", "Khóa công khai")
                .replace("Hash Algorithm", "Thuật toán băm")
                .replace("Security Level", "Mức bảo mật")
                .replace("Success", "Thành công")
                .replace("Error", "Lỗi")
                .replace("Warning", "Cảnh báo")
                .replace("Information", "Thông tin")
                .replace("private key", "khóa bí mật")
                .replace("public key", "khóa công khai")
                .replace("ký file", "ký tệp")
                .replace("Ký file", "Ký tệp")
                .replace("file chữ ký", "tệp chữ ký")
                .replace("File chữ ký", "Tệp chữ ký");
    }

    private void saveQuietly() {
        try {
            JsonUtil.write(logPath, logs);
        } catch (IOException ignored) {
            // Không làm hỏng luồng nghiệp vụ chính nếu không ghi được log.
        }
    }
}
