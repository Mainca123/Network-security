package main.ui.components;

import main.ui.Theme;

import javax.swing.JTextArea;

public class ModernTextArea extends JTextArea {
	public ModernTextArea(int rows) {
		super(rows, 20);
		setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 13));
		setForeground(Theme.TEXT);
		setBackground(Theme.FIELD_BG);
		setLineWrap(true);
		setWrapStyleWord(true);
		setEditable(false);
		setBorder(Theme.padding(10, 10, 10, 10));
	}
}
