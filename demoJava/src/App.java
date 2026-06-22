import javax.swing.SwingUtilities;

import ui.MainFrame;
import ui.Theme;

public class App {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Theme.install();
			new MainFrame().setVisible(true);
		});
	}
}
