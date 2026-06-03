package main.ui;

import main.ui.components.CardPanel;
import main.ui.components.RoundedButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class HomePanel extends JPanel {
	public HomePanel(MainFrame frame) {
		setLayout(new BorderLayout(0, 24));
		setBackground(Theme.APP_BG);
		setBorder(Theme.padding(32, 34, 32, 34));

		JPanel header = new JPanel(new BorderLayout(0, 8));
		header.setOpaque(false);
		header.add(UIFactory.title("DSA Digital Signature System"), BorderLayout.NORTH);
		JLabel desc = new JLabel("Hệ thống ký số và xác thực tính toàn vẹn dữ liệu bằng thuật toán DSA");
		desc.setFont(Theme.FONT.deriveFont(16f));
		desc.setForeground(Theme.MUTED);
		header.add(desc, BorderLayout.CENTER);
		add(header, BorderLayout.NORTH);

		JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
		grid.setOpaque(false);
		grid.add(
				card("⚙", "Sinh tham số", "Tạo p, q, g theo kích thước demo.", "Bắt đầu", () -> frame.showPage("parameters")));
		grid.add(card("🔐", "Sinh khóa", "Tạo private key x và public key y.", "Tạo khóa", () -> frame.showPage("keys")));
		grid.add(card("✍", "Ký file", "Băm SHA-256 và tạo chữ ký r, s.", "Ký dữ liệu", () -> frame.showPage("sign")));
		grid.add(
				card("✓", "Xác thực", "Kiểm tra file có bị chỉnh sửa hay không.", "Kiểm tra", () -> frame.showPage("verify")));
		add(grid, BorderLayout.CENTER);
	}

	private CardPanel card(String icon, String title, String text, String action, Runnable runnable) {
		CardPanel card = new CardPanel();
		card.setLayout(new BorderLayout(0, 14));
		JLabel label = new JLabel(
				"<html><div style='font-size:32px'>" + icon + "</div><h2>" + title + "</h2><p>" + text + "</p></html>");
		label.setFont(Theme.FONT);
		label.setForeground(Theme.TEXT);
		card.add(label, BorderLayout.CENTER);
		RoundedButton button = new RoundedButton(action);
		button.addActionListener(e -> runnable.run());
		card.add(button, BorderLayout.SOUTH);
		return card;
	}
}
