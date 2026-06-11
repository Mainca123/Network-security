package util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class AlertUtil {
    private static final ButtonType ACCEPT_BUTTON = new ButtonType("Đồng ý", ButtonBar.ButtonData.OK_DONE);

    private AlertUtil() {
    }

    public static void info(Window owner, String title, String message) {
        show(owner, Alert.AlertType.INFORMATION, title, message);
    }

    public static void error(Window owner, String title, String message) {
        show(owner, Alert.AlertType.ERROR, title, message);
    }

    public static void warning(Window owner, String title, String message) {
        show(owner, Alert.AlertType.WARNING, title, message);
    }

    private static void show(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ACCEPT_BUTTON);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setResizable(true);
        alert.getDialogPane().setPrefWidth(580);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }
}
