package main.ui.components;

import main.ui.Theme;

import javax.swing.JLabel;
import java.awt.Color;

public class StatusLabel extends JLabel {
	public StatusLabel() {
		super("Sẵn sàng");
		setOpaque(true);
		setFont(Theme.FONT.deriveFont(java.awt.Font.BOLD));
		setBorder(Theme.padding(10, 12, 10, 12));
		setInfo("Sẵn sàng");
	}

	public void setInfo(String text) {
		setText(text);
		setForeground(Theme.PRIMARY);
		setBackground(new Color(219, 234, 254));
	}

	public void setSuccess(String text) {
		setText(text);
		setForeground(Theme.SUCCESS);
		setBackground(new Color(220, 252, 231));
	}

	public void setDanger(String text) {
		setText(text);
		setForeground(Theme.DANGER);
		setBackground(new Color(254, 226, 226));
	}
}
