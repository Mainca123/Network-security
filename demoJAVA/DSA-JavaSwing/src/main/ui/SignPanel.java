package main.ui;

import main.dsa.DSAKeyPair;
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

public class SignPanel extends JPanel {
	private final MainFrame frame;
	private final DSAService service = new DSAService();
	private final JTextField fileField = UIFactory.field();
	private final ModernTextArea hashArea = new ModernTextArea(2);
	private final ModernTextArea rArea = new ModernTextArea(3);
	private final ModernTextArea sArea = new ModernTextArea(3);
	private final StatusLabel status = new StatusLabel();
	private File selectedFile;
	private DSASignature lastSignature;

	public SignPanel(MainFrame frame) {
		this.frame = frame;
		setLayout(new BorderLayout(0, 18));
		setBackground(Theme.APP_BG);
		setBorder(Theme.padding(28, 32, 28, 32));
		add(UIFactory.title("Ký file"), BorderLayout.NORTH);
		add(buildContent(), BorderLayout.CENTER);
	}

	private JPanel buildContent() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		GridBagConstraints c = constraints();

		fileField.setEditable(false);
		RoundedButton choose = new RoundedButton("Chọn file cần ký");
		RoundedButton sign = new RoundedButton("Ký file");
		RoundedButton save = new RoundedButton("Lưu chữ ký");
		RoundedButton clear = new RoundedButton("Làm mới", Theme.MUTED, Theme.TEXT);
		choose.addActionListener(e -> chooseFile());
		sign.addActionListener(e -> signFile());
		save.addActionListener(e -> saveSignature());
		clear.addActionListener(e -> clear());

		c.gridy = 0;
		panel.add(UIFactory.section("File dữ liệu"), c);
		c.gridy = 1;
		panel.add(fileField, c);
		JPanel buttons = new JPanel(new java.awt.GridLayout(1, 4, 10, 10));
		buttons.setOpaque(false);
		buttons.add(choose);
		buttons.add(sign);
		buttons.add(save);
		buttons.add(clear);
		c.gridy = 2;
		panel.add(buttons, c);

		addArea(panel, c, 3, "Hash SHA-256", hashArea);
		addArea(panel, c, 5, "Giá trị r", rArea);
		addArea(panel, c, 7, "Giá trị s", sArea);
		c.gridy = 9;
		panel.add(status, c);
		return panel;
	}

	private void chooseFile() {
		JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			selectedFile = chooser.getSelectedFile();
			fileField.setText(selectedFile.getAbsolutePath());
			status.setInfo("Đã chọn file.");
		}
	}

	private void signFile() {
		DSAParameter parameter = frame.getCurrentParameter();
		DSAKeyPair keyPair = frame.getCurrentKeyPair();
		if (selectedFile == null) {
			showError("Bạn chưa chọn file cần ký.");
			return;
		}
		if (parameter == null || keyPair == null) {
			showError("Bạn cần sinh tham số và khóa bí mật trước.");
			return;
		}
		status.setInfo("Đang xử lý...");
		new SwingWorker<DSASignature, Void>() {
			private byte[] data;

			protected DSASignature doInBackground() throws Exception {
				data = FileUtil.readBytes(selectedFile);
				return service.sign(data, parameter, keyPair.getX());
			}

			protected void done() {
				try {
					lastSignature = get();
					hashArea.setText(HashUtil.sha256Hex(data));
					rArea.setText(lastSignature.getR().toString());
					sArea.setText(lastSignature.getS().toString());
					status.setSuccess("Ký thành công. Bạn có thể lưu file .sig.");
				} catch (Exception ex) {
					showError(ex.getMessage());
				}
			}
		}.execute();
	}

	private void saveSignature() {
		if (lastSignature == null) {
			showError("Chưa có chữ ký để lưu.");
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(selectedFile.getName() + ".sig"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		try {
			KeyUtil.saveSignature(chooser.getSelectedFile(), lastSignature);
			status.setSuccess("Đã lưu chữ ký: " + chooser.getSelectedFile().getAbsolutePath());
		} catch (Exception ex) {
			showError(ex.getMessage());
		}
	}

	private void clear() {
		selectedFile = null;
		lastSignature = null;
		fileField.setText("");
		hashArea.setText("");
		rArea.setText("");
		sArea.setText("");
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
