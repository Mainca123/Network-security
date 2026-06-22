package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

public final class UiFactory {
	private static final int BORDER_RADIUS = 8;
	private static final int BORDER_WIDTH = 2;

	private UiFactory() {
	}

	public static JPanel card() {
		JPanel panel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(Theme.BORDER);
				g2.setStroke(new BasicStroke(1.5f));
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, BORDER_RADIUS, BORDER_RADIUS);
			}
		};
		panel.setBackground(Theme.SURFACE);
		panel.setBorder(new EmptyBorder(16, 16, 16, 16));
		panel.setOpaque(true);
		return panel;
	}

	public static JLabel sectionTitle(String text) {
		JLabel label = new JLabel(text);
		label.setForeground(Theme.PRIMARY_LIGHT);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
		return label;
	}

	public static JLabel caption(String text) {
		JLabel label = new JLabel(text);
		label.setForeground(Theme.MUTED_TEXT);
		label.setFont(label.getFont().deriveFont(12f));
		return label;
	}

	public static JButton primaryButton(String text) {
		return button(text, Theme.PRIMARY, Theme.SURFACE, Theme.PRIMARY_DARK);
	}

	public static JButton secondaryButton(String text) {
		return button(text, Theme.SURFACE_MUTED, Theme.TEXT, Theme.BORDER);
	}

	public static JButton successButton(String text) {
		return button(text, Theme.SUCCESS, Theme.SURFACE, Theme.SUCCESS_DARK);
	}

	public static JButton dangerButton(String text) {
		return button(text, Theme.DANGER, Theme.SURFACE, new Color(220, 60, 60));
	}

	public static JScrollPane scroll(Component component) {
		JScrollPane scrollPane = new JScrollPane(component);
		scrollPane.setBorder(new CustomBorder());
		scrollPane.getViewport().setBackground(Theme.SURFACE_MUTED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(12);
		return scrollPane;
	}

	public static JTextArea textArea(int rows) {
		JTextArea area = new JTextArea(rows, 20);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setBorder(new EmptyBorder(12, 12, 12, 12));
		area.setBackground(Theme.SURFACE_MUTED);
		area.setForeground(Theme.TEXT);
		area.setCaretColor(Theme.PRIMARY);
		return area;
	}

	public static JTextField textField() {
		JTextField field = new JTextField();
		field.setBorder(new CustomInputBorder());
		field.setBackground(Theme.SURFACE_MUTED);
		field.setForeground(Theme.TEXT);
		field.setCaretColor(Theme.PRIMARY);
		return field;
	}

	private static JButton button(String text, Color background, Color foreground, Color hoverColor) {
		JButton button = new JButton(text) {
			private boolean hover = false;

			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(hover ? hoverColor : background);
				g2.fillRoundRect(0, 0, getWidth(), getHeight(), BORDER_RADIUS, BORDER_RADIUS);
				g2.setColor(foreground);
				java.awt.FontMetrics fm = g2.getFontMetrics();
				int x = (getWidth() - fm.stringWidth(getText())) / 2;
				int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
				g2.drawString(getText(), x, y);
			}
		};
		button.setBackground(background);
		button.setForeground(foreground);
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setPreferredSize(new Dimension(120, 38));
		button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				((javax.swing.JButton) e.getSource()).repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				((javax.swing.JButton) e.getSource()).repaint();
			}
		});
		return button;
	}

	static class CustomBorder extends javax.swing.border.AbstractBorder {
		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(Theme.BORDER);
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawRoundRect(x, y, width - 1, height - 1, BORDER_RADIUS, BORDER_RADIUS);
		}

		@Override
		public java.awt.Insets getBorderInsets(Component c) {
			return new java.awt.Insets(2, 2, 2, 2);
		}
	}

	static class CustomInputBorder extends javax.swing.border.AbstractBorder {
		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(Theme.BORDER);
			g2.setStroke(new BasicStroke(1.5f));
			g2.drawRoundRect(x, y, width - 1, height - 1, 6, 6);
		}

		@Override
		public java.awt.Insets getBorderInsets(Component c) {
			return new java.awt.Insets(8, 10, 8, 10);
		}
	}
}
