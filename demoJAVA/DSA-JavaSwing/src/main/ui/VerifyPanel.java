package main.ui;

import main.dsa.DSAParameter;
import main.dsa.DSAService;
import main.dsa.DSASignature;
import main.ui.components.ModernTextArea;
import main.ui.components.RoundedButton;
import main.ui.components.StatusLabel;
import main.util.FileUtil;
import main.util.HashUtil;
import main.util.KeyUtil;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.math.BigInteger;

public class VerifyPanel extends JPanel {
	private final DSAService service = new DSAService();
	private final JTextField fileField = UIFactory.field();
	private final JTextField sigField = UIFactory.field();
	private final JTextField pubField = UIFactory.field();
	private final ModernTextArea hashArea = new ModernTextArea(2);
	private final ModernTextArea rArea = new ModernTextArea(3);
	private final ModernTextArea sArea = new ModernTextArea(3);
	private final ModernTextArea vArea = new ModernTextArea(3);
	private final StatusLabel status = new StatusLabel();
	private File dataFile;
	private File signatureFile;
	private File publicKeyFile;

	public VerifyPanel(MainFrame frame) {
		setLayout(new BorderLayout(0, 18));
		setBackground(Theme.APP_BG);
		setBorder(Theme.padding(28, 32, 28, 32));
		add(UIFactory.title("Xác thực chữ ký"), BorderLayout.NORTH);
		add(buildContent(), BorderLayout.CENTER);
	}

	private JPanel buildContent() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		GridBagConstraints c = constraints();
		fileField.setEditable(false);
		sigField.setEditable(false);
		pubField.setEditable(false);

		c.gridy = 0;
		panel.add(UIFactory.section("File gốc cần xác thực"), c);
		c.gridy = 1;
		panel.add(fileField, c);
		c.gridy = 2;
		panel.add(UIFactory.section("File chữ ký .sig"), c);
		c.gridy = 3;
		panel.add(sigField, c);
		c.gridy = 4;
		panel.add(UIFactory.section("File khóa công khai public.key"), c);
		c.gridy = 5;
		panel.add(pubField, c);

		JPanel buttons = new JPanel(new java.awt.GridLayout(1, 5, 10, 10));
		buttons.setOpaque(false);
		RoundedButton chooseFile = new RoundedButton("Chọn file xác thực");
		RoundedButton chooseSig = new RoundedButton("Chọn chữ ký");
		RoundedButton choosePub = new RoundedButton("Chọn public key");
		RoundedButton verify = new RoundedButton("Xác thực chữ ký");
		RoundedButton clear = new RoundedButton("Làm mới", Theme.MUTED, Theme.TEXT);
		chooseFile.addActionListener(e -> chooseDataFile());
		chooseSig.addActionListener(e -> chooseSignatureFile());
		choosePub.addActionListener(e -> choosePublicKeyFile());
		verify.addActionListener(e -> verify());
		clear.addActionListener(e -> clear());
		buttons.add(chooseFile);
		buttons.add(chooseSig);
		buttons.add(choosePub);
		buttons.add(verify);
		buttons.add(clear);
		c.gridy = 6;
		panel.add(buttons, c);

		addArea(panel, c, 7, "Hash SHA-256 của file", hashArea);
		addArea(panel, c, 9, "r đọc từ chữ ký", rArea);
		addArea(panel, c, 11, "s đọc từ chữ ký", sArea);
		addArea(panel, c, 13, "v tính được", vArea);
		c.gridy = 15;
		panel.add(status, c);
		return panel;
	}

	private void chooseDataFile() {
		dataFile = chooseOpen();
		if (dataFile != null)
			fileField.setText(dataFile.getAbsolutePath());
	}

	private void chooseSignatureFile() {
		signatureFile = chooseOpen();
		if (signatureFile != null)
			sigField.setText(signatureFile.getAbsolutePath());
	}

	private void choosePublicKeyFile() {
		publicKeyFile = chooseOpen();
		if (publicKeyFile != null)
			pubField.setText(publicKeyFile.getAbsolutePath());
	}

	private File chooseOpen() {
		JFileChooser chooser = new JFileChooser();
		return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
	}

	private void verify() {
		if (dataFile == null || signatureFile == null || publicKeyFile == null) {
			showError("Bạn cần chọn đủ file gốc, file .sig và public.key.");
			return;
		}
		status.setInfo("Đang xử lý...");
		new SwingWorker<Boolean, Void>() {
			private byte[] data;
			private DSASignature signature;
			private BigInteger v;

			protected Boolean doInBackground() throws Exception {
				data = FileUtil.readBytes(dataFile);
				signature = KeyUtil.loadSignature(signatureFile);
				KeyUtil.PublicKeyData publicData = KeyUtil.loadPublicKey(publicKeyFile);
				DSAParameter parameter = publicData.getParameter();
				v = service.calculateVerificationValue(data, signature, parameter, publicData.getPublicKey());
				return v.equals(signature.getR());
			}

			protected void done() {
				try {
					boolean valid = get();
					hashArea.setText(HashUtil.sha256Hex(data));
					rArea.setText(signature.getR().toString());
					sArea.setText(signature.getS().toString());
					vArea.setText(v.toString());
					if (valid) {
						status.setSuccess("Chữ ký hợp lệ - File chưa bị thay đổi");
					} else {
						status.setDanger("Chữ ký không hợp lệ - File có thể đã bị chỉnh sửa");
					}
				} catch (Exception ex) {
					status.setDanger("Chữ ký giả mạo hoặc file không hợp lệ: " + ex.getMessage());
					JOptionPane.showMessageDialog(VerifyPanel.this, ex.getMessage(), "Xác thực thất bại",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private void clear() {
		dataFile = null;
		signatureFile = null;
		publicKeyFile = null;
		fileField.setText("");
		sigField.setText("");
		pubField.setText("");
		hashArea.setText("");
		rArea.setText("");
		sArea.setText("");
		vArea.setText("");
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
		c.insets = new Insets(6, 6, 6, 6);
		return c;
	}
}
