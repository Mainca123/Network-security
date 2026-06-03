package main.ui;

import main.ui.components.CardPanel;
import main.ui.components.ModernTextArea;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.Color;

public class UIFactory {
	public static JLabel title(String text) {
		JLabel label = new JLabel(text);
		label.setFont(Theme.TITLE_FONT);
		label.setForeground(Theme.TEXT);
		return label;
	}

	public static JLabel section(String text) {
		JLabel label = new JLabel(text);
		label.setFont(Theme.SECTION_FONT);
		label.setForeground(Theme.TEXT);
		return label;
	}

	public static JTextField field() {
		JTextField field = new JTextField();
		field.setFont(Theme.FONT);
		field.setForeground(Theme.TEXT);
		field.setBackground(Color.WHITE);
		field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)),
				Theme.padding(8, 10, 8, 10)));
		return field;
	}

	public static JScrollPane scroll(ModernTextArea area) {
		JScrollPane scrollPane = new JScrollPane(area);
		scrollPane.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));
		return scrollPane;
	}

	public static CardPanel cardWithTitle(String title) {
		CardPanel card = new CardPanel();
		card.setLayout(new java.awt.BorderLayout(0, 10));
		card.add(section(title), java.awt.BorderLayout.NORTH);
		return card;
	}
}
