package util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeUtil {
    private static final DateTimeFormatter STORAGE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private DateTimeUtil() {
    }

    public static String nowStorage() {
        return LocalDateTime.now().format(STORAGE_FORMAT);
    }

    public static String nowDisplay() {
        return LocalDateTime.now().format(DISPLAY_FORMAT);
    }

    public static String display(String storageTimestamp) {
        if (storageTimestamp == null || storageTimestamp.isBlank()) {
            return "";
        }
        try {
            return LocalDateTime.parse(storageTimestamp, STORAGE_FORMAT).format(DISPLAY_FORMAT);
        } catch (DateTimeParseException ex) {
            return storageTimestamp;
        }
    }

    public static String displayMillis(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(DISPLAY_FORMAT);
    }
}
