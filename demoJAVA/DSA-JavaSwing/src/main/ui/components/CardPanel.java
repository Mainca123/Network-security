package main.ui.components;

import main.ui.Theme;

import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class CardPanel extends JPanel {
	public CardPanel() {
		setOpaque(false);
		setBorder(new CompoundBorder(javax.swing.BorderFactory.createLineBorder(new Color(226, 232, 240)),
				Theme.padding(16, 16, 16, 16)));
	}

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Theme.CARD_BG);
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
		g2.dispose();
		super.paintComponent(g);
	}
}
