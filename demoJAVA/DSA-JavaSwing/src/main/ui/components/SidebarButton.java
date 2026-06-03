package main.ui.components;

import main.ui.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class SidebarButton extends JButton {
	private boolean active;

	public SidebarButton(String text) {
		super(text);
		setFont(Theme.FONT.deriveFont(14f));
		setForeground(Color.WHITE);
		setHorizontalAlignment(LEFT);
		setFocusPainted(false);
		setBorderPainted(false);
		setContentAreaFilled(false);
		setOpaque(false);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setBorder(Theme.padding(12, 16, 12, 16));
	}

	public void setActive(boolean active) {
		this.active = active;
		repaint();
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (active) {
			g2.setColor(Theme.PRIMARY);
			g2.fillRoundRect(8, 4, getWidth() - 16, getHeight() - 8, 12, 12);
		}
		g2.dispose();
		super.paintComponent(g);
	}
}
