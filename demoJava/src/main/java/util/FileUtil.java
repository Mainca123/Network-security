package util;

import java.nio.file.Path;
import java.util.Locale;

public final class FileUtil {
    private FileUtil() {
    }

    public static String extension(Path path) {
        String name = path.getFileName().toString();
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unitIndex = -1;
        do {
            value /= 1024.0;
            unitIndex++;
        } while (value >= 1024.0 && unitIndex < units.length - 1);
        return String.format(Locale.US, "%.2f %s", value, units[unitIndex]);
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\n\n...[đã rút gọn phần xem trước]";
    }
}
