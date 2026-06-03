package main.ui;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;

public class Theme {
	public static final Color APP_BG = new Color(244, 247, 251);
	public static final Color SIDEBAR_BG = new Color(28, 43, 70);
	public static final Color PRIMARY = new Color(37, 99, 235);
	public static final Color PRIMARY_DARK = new Color(29, 78, 216);
	public static final Color SUCCESS = new Color(22, 163, 74);
	public static final Color DANGER = new Color(220, 38, 38);
	public static final Color TEXT = new Color(30, 41, 59);
	public static final Color MUTED = new Color(100, 116, 139);
	public static final Color CARD_BG = Color.WHITE;
	public static final Color FIELD_BG = new Color(248, 250, 252);
	public static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14);
	public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 24);
	public static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 16);

	public static Border padding(int top, int left, int bottom, int right) {
		return BorderFactory.createEmptyBorder(top, left, bottom, right);
	}
}
