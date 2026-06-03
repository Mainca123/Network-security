package main.ui;

import main.dsa.DSAKeyPair;
import main.dsa.DSAParameter;
import main.dsa.DSAService;
import main.ui.components.ModernTextArea;
import main.ui.components.RoundedButton;
import main.ui.components.StatusLabel;
import main.util.KeyUtil;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

public class KeyPanel extends JPanel {
	private final MainFrame frame;
	private final DSAService service = new DSAService();
	private final ModernTextArea xArea = new ModernTextArea(5);
	private final ModernTextArea yArea = new ModernTextArea(5);
	private final StatusLabel status = new StatusLabel();

	public KeyPanel(MainFrame frame) {
		this.frame = frame;
		setLayout(new BorderLayout(0, 18));
		setBackground(Theme.APP_BG);
		setBorder(Theme.padding(28, 32, 28, 32));
		add(UIFactory.title("Sinh khóa DSA"), BorderLayout.NORTH);
		add(buildContent(), BorderLayout.CENTER);
	}

	private JPanel buildContent() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		GridBagConstraints c = constraints();

		RoundedButton generate = new RoundedButton("Sinh khóa");
		RoundedButton savePrivate = new RoundedButton("Lưu khóa bí mật");
		RoundedButton savePublic = new RoundedButton("Lưu khóa công khai");
		RoundedButton loadPrivate = new RoundedButton("Đọc private.key", Theme.MUTED, Theme.TEXT);
		RoundedButton loadPublic = new RoundedButton("Đọc public.key", Theme.MUTED, Theme.TEXT);
		RoundedButton clear = new RoundedButton("Làm mới", Theme.MUTED, Theme.TEXT);

		generate.addActionListener(e -> generateKey());
		savePrivate.addActionListener(e -> savePrivateKey());
		savePublic.addActionListener(e -> savePublicKey());
		loadPrivate.addActionListener(e -> loadPrivateKey());
		loadPublic.addActionListener(e -> loadPublicKey());
		clear.addActionListener(e -> clear());

		JPanel buttons = new JPanel(new java.awt.GridLayout(2, 3, 10, 10));
		buttons.setOpaque(false);
		buttons.add(generate);
		buttons.add(savePrivate);
		buttons.add(savePublic);
		buttons.add(loadPrivate);
		buttons.add(loadPublic);
		buttons.add(clear);

		c.gridx = 0;
		c.gridy = 0;
		panel.add(buttons, c);
		addArea(panel, c, 1, "Private key x", xArea);
		addArea(panel, c, 3, "Public key y", yArea);
		c.gridy = 5;
		panel.add(status, c);
		return panel;
	}

	private void generateKey() {
		DSAParameter parameter = frame.getCurrentParameter();
		if (parameter == null) {
			showError("Bạn cần sinh tham số DSA trước.");
			return;
		}
		DSAKeyPair keyPair = service.generateKeyPair(parameter);
		frame.setCurrentKeyPair(keyPair);
		xArea.setText(keyPair.getX().toString());
		yArea.setText(keyPair.getY().toString());
		status.setSuccess("Sinh khóa thành công.");
	}

	private void savePrivateKey() {
		if (frame.getCurrentParameter() == null || frame.getCurrentKeyPair() == null) {
			showError("Chưa có khóa để lưu.");
			return;
		}
		File file = chooseSave("private.key");
		if (file == null)
			return;
		try {
			KeyUtil.savePrivateKey(file, frame.getCurrentParameter(), frame.getCurrentKeyPair().getX());
			status.setSuccess("Đã lưu khóa bí mật: " + file.getAbsolutePath());
		} catch (Exception ex) {
			showError(ex.getMessage());
		}
	}

	private void savePublicKey() {
		if (frame.getCurrentParameter() == null || frame.getCurrentKeyPair() == null) {
			showError("Chưa có khóa để lưu.");
			return;
		}
		File file = chooseSave("public.key");
		if (file == null)
			return;
		try {
			KeyUtil.savePublicKey(file, frame.getCurrentParameter(), frame.getCurrentKeyPair().getY());
			status.setSuccess("Đã lưu khóa công khai: " + file.getAbsolutePath());
		} catch (Exception ex) {
			showError(ex.getMessage());
		}
	}

	private void loadPrivateKey() {
		File file = chooseOpen();
		if (file == null)
			return;
		try {
			KeyUtil.PrivateKeyData data = KeyUtil.loadPrivateKey(file);
			frame.setCurrentParameter(data.getParameter());
			frame.setCurrentKeyPair(new DSAKeyPair(data.getPrivateKey(),
					data.getParameter().getG().modPow(data.getPrivateKey(), data.getParameter().getP())));
			xArea.setText(data.getPrivateKey().toString());
			yArea.setText(frame.getCurrentKeyPair().getY().toString());
			status.setSuccess("Đã đọc khóa bí mật.");
		} catch (Exception ex) {
			showError(ex.getMessage());
		}
	}

	private void loadPublicKey() {
		File file = chooseOpen();
		if (file == null)
			return;
		try {
			KeyUtil.PublicKeyData data = KeyUtil.loadPublicKey(file);
			frame.setCurrentParameter(data.getParameter());
			frame.setCurrentKeyPair(null);
			yArea.setText(data.getPublicKey().toString());
			status.setSuccess("Đã đọc khóa công khai.");
		} catch (Exception ex) {
			showError(ex.getMessage());
		}
	}

	private File chooseOpen() {
		JFileChooser chooser = new JFileChooser();
		return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
	}

	private File chooseSave(String suggestedName) {
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(suggestedName));
		return chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
	}

	private void clear() {
		xArea.setText("");
		yArea.setText("");
		status.setInfo("Đã làm mới.");
	}

	private void showError(String message) {
		status.setDanger("Lỗi: " + message);
		JOptionPane.showMessageDialog(this, message, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
	}

	private void addArea(JPanel panel, GridBagConstraints c, int row, String label, ModernTextArea area) {
		c.gridy = row;
		panel.add(UIFactory.section(label), c);
		c.gridy = row + 1;
		panel.add(UIFactory.scroll(area), c);
	}

	private GridBagConstraints constraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = new Insets(7, 6, 7, 6);
		return c;
	}
}
