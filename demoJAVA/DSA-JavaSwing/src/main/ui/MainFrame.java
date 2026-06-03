package main.ui;

import main.dsa.DSAKeyPair;
import main.dsa.DSAParameter;
import main.ui.components.SidebarButton;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel content = new JPanel(cardLayout);
	private final Map<String, SidebarButton> buttons = new LinkedHashMap<>();
	private DSAParameter currentParameter;
	private DSAKeyPair currentKeyPair;

	public MainFrame() {
		super("DSA Digital Signature System");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(1120, 720));
		setSize(1180, 760);
		setLocationRelativeTo(null);

		content.setBackground(Theme.APP_BG);
		add(buildSidebar(), BorderLayout.WEST);
		add(content, BorderLayout.CENTER);

		addPage("home", "Trang chủ", new HomePanel(this));
		addPage("parameters", "Sinh tham số DSA", new ParameterPanel(this));
		addPage("keys", "Sinh khóa", new KeyPanel(this));
		addPage("sign", "Ký file", new SignPanel(this));
		addPage("verify", "Xác thực chữ ký", new VerifyPanel(this));
		addPage("guide", "Hướng dẫn sử dụng", new GuidePanel());
		showPage("home");
	}

	private JPanel buildSidebar() {
		JPanel sidebar = new JPanel();
		sidebar.setPreferredSize(new Dimension(240, 0));
		sidebar.setBackground(Theme.SIDEBAR_BG);
		sidebar.setLayout(new javax.swing.BoxLayout(sidebar, javax.swing.BoxLayout.Y_AXIS));
		sidebar.setBorder(Theme.padding(22, 12, 22, 12));

		JLabel brand = new JLabel("<html><b>DSA Digital</b><br>Signature System</html>");
		brand.setForeground(Color.WHITE);
		brand.setFont(Theme.SECTION_FONT);
		brand.setBorder(Theme.padding(0, 12, 20, 12));
		sidebar.add(brand);
		return sidebar;
	}

	private void addPage(String id, String title, JPanel panel) {
		content.add(panel, id);
		SidebarButton button = new SidebarButton(title);
		button.addActionListener(e -> showPage(id));
		buttons.put(id, button);
		((JPanel) getContentPane().getComponent(0)).add(button);
	}

	public void showPage(String id) {
		cardLayout.show(content, id);
		buttons.forEach((key, button) -> button.setActive(key.equals(id)));
	}

	public DSAParameter getCurrentParameter() {
		return currentParameter;
	}

	public void setCurrentParameter(DSAParameter currentParameter) {
		this.currentParameter = currentParameter;
	}

	public DSAKeyPair getCurrentKeyPair() {
		return currentKeyPair;
	}

	public void setCurrentKeyPair(DSAKeyPair currentKeyPair) {
		this.currentKeyPair = currentKeyPair;
	}
}
