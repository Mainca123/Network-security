package ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public final class Theme {
	// Modern Dark-Inspired Color Palette
	public static final Color BACKGROUND = new Color(15, 15, 20);
	public static final Color SURFACE = new Color(25, 25, 35);
	public static final Color SURFACE_MUTED = new Color(35, 35, 50);
	public static final Color BORDER = new Color(50, 50, 70);
	public static final Color TEXT = new Color(240, 240, 250);
	public static final Color MUTED_TEXT = new Color(150, 155, 170);
	public static final Color PRIMARY = new Color(100, 180, 255);
	public static final Color PRIMARY_DARK = new Color(70, 140, 220);
	public static final Color PRIMARY_LIGHT = new Color(130, 200, 255);
	public static final Color SUCCESS = new Color(80, 200, 120);
	public static final Color SUCCESS_DARK = new Color(50, 160, 90);
	public static final Color WARNING = new Color(255, 180, 60);
	public static final Color DANGER = new Color(255, 100, 100);
	public static final Color ACCENT = new Color(100, 200, 255);

	private Theme() {
	}

	public static void install() {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException ignored) {
		}

		Font baseFont = new Font("Segoe UI", Font.PLAIN, 13);
		Font monoFont = new Font("JetBrains Mono", Font.PLAIN, 12);
		if (!monoFont.getFamily().contains("JetBrains")) {
			monoFont = new Font("Consolas", Font.PLAIN, 12);
		}

		UIManager.put("defaultFont", baseFont);
		UIManager.put("Label.font", baseFont);
		UIManager.put("Button.font", baseFont.deriveFont(Font.BOLD, 13f));
		UIManager.put("RadioButton.font", baseFont);
		UIManager.put("TextArea.font", monoFont);
		UIManager.put("TextField.font", monoFont);
		UIManager.put("Panel.background", BACKGROUND);
		UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
		UIManager.put("Button.background", PRIMARY);
		UIManager.put("Button.foreground", TEXT);
	}
}
