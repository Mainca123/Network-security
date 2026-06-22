package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import crypto.DsaService;
import model.DataMode;
import model.DsaKeyPairText;
import util.KeyFileFormatter;

public class MainFrame extends JFrame {
	private final DsaService dsaService = new DsaService();

	private final JTextArea inputArea = UiFactory.textArea(12);
	private final JTextArea privateKeyArea = UiFactory.textArea(6);
	private final JTextArea publicKeyArea = UiFactory.textArea(6);
	private final JTextArea signatureArea = UiFactory.textArea(6);
	private final JTextArea logArea = UiFactory.textArea(5);
	private final JTextField manualPField = UiFactory.textField();
	private final JTextField manualQField = UiFactory.textField();
	private final JTextField selectedFileField = UiFactory.textField();
	private final JTextField hashField = UiFactory.textField();
	private final JLabel statusLabel = new JLabel("Sẵn sàng");
	private final JRadioButton textModeButton = new JRadioButton("Văn bản", true);
	private final JRadioButton fileModeButton = new JRadioButton("Tệp tin");

	public MainFrame() {
		super("Hệ thống chữ ký số DSA");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(1180, 760));
		setContentPane(createContent());
		setSize(1220, 800);
		setLocationRelativeTo(null);
		generateKeyPair();
	}

	private JPanel createContent() {
		JPanel root = new JPanel(new BorderLayout(12, 12));
		root.setBackground(Theme.BACKGROUND);
		root.setBorder(new EmptyBorder(12, 12, 12, 12));
		root.add(createHeader(), BorderLayout.NORTH);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createDataPanel(), createSignaturePanel());
		splitPane.setResizeWeight(0.45);
		splitPane.setBorder(BorderFactory.createEmptyBorder());
		splitPane.setDividerSize(8);
		splitPane.setBackground(Theme.BACKGROUND);
		root.add(splitPane, BorderLayout.CENTER);
		root.add(createLogPanel(), BorderLayout.SOUTH);
		return root;
	}

	private JPanel createHeader() {
		JPanel panel = new JPanel(new BorderLayout(12, 8));
		panel.setBackground(Theme.SURFACE);
		panel.setBorder(new CompoundBorder(
				new LineBorder(Theme.BORDER, 1, true),
				new EmptyBorder(16, 16, 16, 16)));

		JLabel title = new JLabel("🔐 DSA Digital Signature System");
		title.setForeground(Theme.PRIMARY_LIGHT);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));

		JLabel subtitle = new JLabel("Custom DSA Implementation with SHA-256 · Secure Data Signing & Verification");
		subtitle.setForeground(Theme.MUTED_TEXT);
		subtitle.setFont(subtitle.getFont().deriveFont(12f));

		JPanel titlePanel = new JPanel(new BorderLayout(4, 4));
		titlePanel.setOpaque(false);
		titlePanel.add(title, BorderLayout.NORTH);
		titlePanel.add(subtitle, BorderLayout.CENTER);

		statusLabel.setOpaque(true);
		statusLabel.setBackground(Theme.SUCCESS_DARK);
		statusLabel.setForeground(Theme.TEXT);
		statusLabel.setBorder(new EmptyBorder(10, 14, 10, 14));
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 12f));

		panel.add(titlePanel, BorderLayout.CENTER);
		panel.add(statusLabel, BorderLayout.EAST);
		return panel;
	}

	private JPanel createDataPanel() {
		JPanel panel = UiFactory.card();
		panel.setLayout(new BorderLayout(10, 10));

		JPanel top = new JPanel(new BorderLayout(8, 8));
		top.setOpaque(false);
		top.add(UiFactory.sectionTitle("Dữ liệu cần xử lý"), BorderLayout.WEST);
		top.add(createModeSelector(), BorderLayout.EAST);

		inputArea.setText("Nhập nội dung cần ký hoặc cần xác minh tại đây.");
		selectedFileField.setEditable(false);
		hashField.setEditable(false);

		panel.add(top, BorderLayout.NORTH);
		panel.add(UiFactory.scroll(inputArea), BorderLayout.CENTER);
		panel.add(createDataFooter(), BorderLayout.SOUTH);
		return panel;
	}

	private JPanel createModeSelector() {
		JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		modePanel.setOpaque(false);
		ButtonGroup group = new ButtonGroup();
		group.add(textModeButton);
		group.add(fileModeButton);
		textModeButton.setOpaque(false);
		fileModeButton.setOpaque(false);
		modePanel.add(textModeButton);
		modePanel.add(fileModeButton);
		return modePanel;
	}

	private JPanel createDataFooter() {
		JPanel footer = new JPanel(new GridBagLayout());
		footer.setOpaque(false);
		GridBagConstraints gbc = baseConstraints();

		JButton chooseFileButton = UiFactory.secondaryButton("Chọn tệp");
		chooseFileButton.addActionListener(event -> chooseDataFile());

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		footer.add(selectedFileField, gbc);

		gbc.gridx = 1;
		gbc.weightx = 0;
		footer.add(chooseFileButton, gbc);

		JButton hashButton = UiFactory.primaryButton("Tính hash");
		JButton clearButton = UiFactory.secondaryButton("Xóa");
		hashButton.addActionListener(event -> calculateHash());
		clearButton.addActionListener(event -> clearInput());

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		footer.add(hashField, gbc);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		actions.setOpaque(false);
		actions.add(clearButton);
		actions.add(hashButton);

		gbc.gridx = 1;
		gbc.weightx = 0;
		footer.add(actions, gbc);
		return footer;
	}

	private JPanel createSignaturePanel() {
		JPanel panel = UiFactory.card();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = baseConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 0;
		panel.add(createManualKeyPanel(), gbc);

		addSection(panel, gbc, 1, "Khóa bí mật DSA (p, q, g, x)", privateKeyArea, 0.24);
		addSection(panel, gbc, 2, "Khóa công khai DSA (p, q, g, y)", publicKeyArea, 0.22);
		addSection(panel, gbc, 3, "Chữ ký DSA (r, s)", signatureArea, 0.24);

		gbc.gridy = 4;
		gbc.weighty = 0;
		panel.add(createActionPanel(), gbc);
		return panel;
	}

	private JPanel createManualKeyPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		GridBagConstraints gbc = baseConstraints();
		gbc.insets = new Insets(0, 0, 6, 8);

		JLabel title = UiFactory.sectionTitle("Tham số sinh khóa thủ công");
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 4;
		gbc.weightx = 1;
		panel.add(title, gbc);

		gbc.gridwidth = 1;
		gbc.gridy = 1;
		gbc.weightx = 0;
		panel.add(UiFactory.caption("p"), gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.5;
		panel.add(manualPField, gbc);

		gbc.gridx = 2;
		gbc.weightx = 0;
		panel.add(UiFactory.caption("q"), gbc);

		gbc.gridx = 3;
		gbc.weightx = 0.5;
		gbc.insets = new Insets(0, 0, 6, 0);
		panel.add(manualQField, gbc);
		return panel;
	}

	private void addSection(JPanel parent, GridBagConstraints gbc, int row, String title, JTextArea area,
			double weightY) {
		JPanel section = new JPanel(new BorderLayout(0, 6));
		section.setOpaque(false);
		section.add(UiFactory.sectionTitle(title), BorderLayout.NORTH);
		section.add(UiFactory.scroll(area), BorderLayout.CENTER);

		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.weighty = weightY;
		parent.add(section, gbc);
	}

	private JPanel createActionPanel() {
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		actions.setOpaque(false);

		JButton generateButton = UiFactory.secondaryButton("Tạo khóa");
		JButton generateManualButton = UiFactory.secondaryButton("Tạo từ p,q");
		JButton signButton = UiFactory.primaryButton("Ký dữ liệu");
		JButton verifyButton = UiFactory.primaryButton("Xác minh");
		JButton saveKeysButton = UiFactory.secondaryButton("Lưu khóa");
		JButton loadKeysButton = UiFactory.secondaryButton("Mở khóa");
		JButton saveSignatureButton = UiFactory.secondaryButton("Lưu chữ ký");
		JButton loadSignatureButton = UiFactory.secondaryButton("Mở chữ ký");

		generateButton.addActionListener(event -> generateKeyPair());
		generateManualButton.addActionListener(event -> generateManualKeyPair());
		signButton.addActionListener(event -> signData());
		verifyButton.addActionListener(event -> verifySignature());
		saveKeysButton.addActionListener(event -> saveKeys());
		loadKeysButton.addActionListener(event -> loadKeys());
		saveSignatureButton.addActionListener(event -> saveSignature());
		loadSignatureButton.addActionListener(event -> loadSignature());

		actions.add(generateButton);
		actions.add(generateManualButton);
		actions.add(signButton);
		actions.add(verifyButton);
		actions.add(saveKeysButton);
		actions.add(loadKeysButton);
		actions.add(saveSignatureButton);
		actions.add(loadSignatureButton);
		return actions;
	}

	private JPanel createLogPanel() {
		JPanel panel = UiFactory.card();
		panel.setLayout(new BorderLayout(0, 8));
		logArea.setEditable(false);
		panel.add(UiFactory.sectionTitle("Nhật ký xử lý"), BorderLayout.NORTH);
		panel.add(UiFactory.scroll(logArea), BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(100, 150));
		return panel;
	}

	private void chooseDataFile() {
		JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			selectedFileField.setText(file.getAbsolutePath());
			fileModeButton.setSelected(true);
			setStatus("Đã chọn tệp", Theme.SUCCESS);
			log("► Chọn tệp: " + file.getName() + " (" + file.length() + " bytes)");
		}
	}

	private void generateKeyPair() {
		try {
			DsaKeyPairText keyPair = dsaService.generateKeyPair();
			privateKeyArea.setText(keyPair.privateKeyText());
			publicKeyArea.setText(keyPair.publicKeyText());
			signatureArea.setText("");
			setStatus("Tạo khóa thành công", Theme.SUCCESS);
			log("✓ Tạo cặp khóa DSA thành công (1024-bit)");
		} catch (GeneralSecurityException ex) {
			showError("Lỗi khi tạo khóa.", ex);
		}
	}

	private void generateManualKeyPair() {
		try {
			DsaKeyPairText keyPair = dsaService.generateManualKeyPair(
					manualPField.getText(),
					manualQField.getText());
			privateKeyArea.setText(keyPair.privateKeyText());
			publicKeyArea.setText(keyPair.publicKeyText());
			signatureArea.setText("");
			setStatus("Tạo khóa từ tham số", Theme.SUCCESS);
			log("✓ Tạo cặp khóa DSA từ tham số p và q nhập thủ công.");
		} catch (GeneralSecurityException ex) {
			showError("Không thể tạo cặp khóa từ p và q.", ex);
		}
	}

	private void signData() {
		try {
			String signature = dsaService.sign(readCurrentData(), privateKeyArea.getText());
			signatureArea.setText(signature);
			calculateHash();
			setStatus("Ký thành công", Theme.SUCCESS);
			log("✓ Ký dữ liệu thành công bằng DSA + SHA-256");
		} catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
			showError("Lỗi khi ký dữ liệu", ex);
		}
	}

	private void verifySignature() {
		try {
			boolean valid = dsaService.verify(readCurrentData(), signatureArea.getText(), publicKeyArea.getText());
			calculateHash();
			if (valid) {
				setStatus("Chữ ký hợp lệ", Theme.SUCCESS);
				log("✓ Xác minh chữ ký: THÀNH CÔNG - Dữ liệu còn nguyên vẹn");
				JOptionPane.showMessageDialog(this,
						"✓ Chữ ký hợp lệ\n\nDữ liệu đã được xác minh thành công và chưa bị sửa đổi.",
						"Xác minh chữ ký", JOptionPane.INFORMATION_MESSAGE);
			} else {
				setStatus("Chữ ký không hợp lệ", Theme.DANGER);
				log("✗ Xác minh chữ ký: THẤT BẠI - Dữ liệu có thể đã bị sửa đổi");
				JOptionPane.showMessageDialog(this,
						"✗ Chữ ký không hợp lệ\n\nDữ liệu, chữ ký hoặc khóa công khai không khớp.",
						"Xác minh chữ ký", JOptionPane.WARNING_MESSAGE);
			}
		} catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
			showError("Lỗi khi xác minh chữ ký", ex);
		}
	}

	private void calculateHash() {
		try {
			hashField.setText(dsaService.sha256(readCurrentData()));
			log("◇ Tính hash SHA-256 cho " + currentMode().displayName());
		} catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
			showError("Lỗi khi tính hash", ex);
		}
	}

	private byte[] readCurrentData() throws IOException {
		if (currentMode() == DataMode.FILE) {
			String path = selectedFileField.getText().trim();
			if (path.isEmpty()) {
				throw new IllegalArgumentException("Vui lòng chọn tệp tin cần xử lý.");
			}
			return Files.readAllBytes(new File(path).toPath());
		}
		return inputArea.getText().getBytes(StandardCharsets.UTF_8);
	}

	private DataMode currentMode() {
		return fileModeButton.isSelected() ? DataMode.FILE : DataMode.TEXT;
	}

	private void clearInput() {
		inputArea.setText("");
		selectedFileField.setText("");
		hashField.setText("");
		textModeButton.setSelected(true);
		setStatus("Sẵn sàng", Theme.SUCCESS);
		log("↻ Đã xóa dữ liệu đầu vào");
	}

	private void saveKeys() {
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File("dsa_keys.txt"));
		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			DsaKeyPairText keyPair = new DsaKeyPairText(privateKeyArea.getText(), publicKeyArea.getText());
			writeTextFile(chooser.getSelectedFile(), KeyFileFormatter.format(keyPair), "✓ Đã lưu cặp khóa");
		}
	}

	private void loadKeys() {
		JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				String content = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
				DsaKeyPairText keyPair = KeyFileFormatter.parse(content);
				dsaService.parsePrivateKey(keyPair.privateKeyText());
				dsaService.parsePublicKey(keyPair.publicKeyText());
				privateKeyArea.setText(keyPair.privateKeyText());
				publicKeyArea.setText(keyPair.publicKeyText());
				setStatus("Đã mở khóa", Theme.SUCCESS);
				log("► Mở cặp khóa từ tệp: " + chooser.getSelectedFile().getName());
			} catch (IOException | GeneralSecurityException | RuntimeException ex) {
				showError("Tệp khóa không hợp lệ", ex);
			}
		}
	}

	private void saveSignature() {
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File("dsa_signature.txt"));
		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			writeTextFile(chooser.getSelectedFile(), signatureArea.getText().trim() + "\n", "✓ Đã lưu chữ ký");
		}
	}

	private void loadSignature() {
		JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				String signature = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8).trim();
				dsaService.validateSignatureText(signature);
				signatureArea.setText(signature);
				setStatus("Đã mở chữ ký", Theme.SUCCESS);
				log("► Mở chữ ký từ tệp: " + chooser.getSelectedFile().getName());
			} catch (IOException | GeneralSecurityException | IllegalArgumentException ex) {
				showError("Tệp chữ ký không hợp lệ.", ex);
			}
		}
	}

	private void writeTextFile(File file, String content, String successMessage) {
		try {
			Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
			setStatus("Đã lưu tệp", Theme.SUCCESS);
			log(successMessage + " » " + file.getAbsolutePath());
		} catch (IOException ex) {
			showError("Không thể ghi tệp", ex);
		}
	}

	private void setStatus(String text, java.awt.Color color) {
		statusLabel.setText(" ● " + text.toUpperCase() + " ");
		statusLabel.setForeground(color);
		statusLabel.setBackground(adjustColorBrightness(color, 0.2));
	}

	private void log(String message) {
		String timestamp = String.format("[%02d:%02d:%02d] ",
				java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
				java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE),
				java.util.Calendar.getInstance().get(java.util.Calendar.SECOND));
		logArea.append(timestamp + message + System.lineSeparator());
		logArea.setCaretPosition(logArea.getDocument().getLength());
	}

	private void showError(String message, Exception ex) {
		setStatus("Lỗi", Theme.DANGER);
		log("✗ " + message + ": " + ex.getMessage());
		JOptionPane.showMessageDialog(this,
				message + "\n\n" + ex.getMessage(),
				"Lỗi hệ thống",
				JOptionPane.ERROR_MESSAGE);
	}

	private static GridBagConstraints baseConstraints() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 10, 0);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		return gbc;
	}

	private static java.awt.Color adjustColorBrightness(java.awt.Color color, double factor) {
		int r = Math.max(0, Math.min(255, (int) (color.getRed() * (1 + factor))));
		int g = Math.max(0, Math.min(255, (int) (color.getGreen() * (1 + factor))));
		int b = Math.max(0, Math.min(255, (int) (color.getBlue() * (1 + factor))));
		return new java.awt.Color(r, g, b);
	}
}
