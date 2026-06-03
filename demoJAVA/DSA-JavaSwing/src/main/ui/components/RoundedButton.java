package main.ui.components;

import main.ui.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class RoundedButton extends JButton {
	private final Color normal;
	private final Color hover;
	private boolean hovered;

	public RoundedButton(String text) {
		this(text, Theme.PRIMARY, Theme.PRIMARY_DARK);
	}

	public RoundedButton(String text, Color normal, Color hover) {
		super(text);
		this.normal = normal;
		this.hover = hover;
		setFont(Theme.FONT.deriveFont(java.awt.Font.BOLD));
		setForeground(Color.WHITE);
		setFocusPainted(false);
		setBorderPainted(false);
		setContentAreaFilled(false);
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setBorder(Theme.padding(10, 16, 10, 16));
		addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseEntered(java.awt.event.MouseEvent e) {
				hovered = true;
				repaint();
			}

			public void mouseExited(java.awt.event.MouseEvent e) {
				hovered = false;
				repaint();
			}
		});
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(hovered ? hover : normal);
		g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
		g2.dispose();
		super.paintComponent(g);
	}
}
