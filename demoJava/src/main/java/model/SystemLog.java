package model;

public class SystemLog {
    private String timestamp;
    private String type;
    private String message;
    private String status;

    public SystemLog() {
    }

    public SystemLog(String timestamp, String type, String message, String status) {
        this.timestamp = timestamp;
        this.type = type;
        this.message = message;
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
