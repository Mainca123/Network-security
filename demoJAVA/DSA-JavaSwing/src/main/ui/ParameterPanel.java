package main.ui;

import main.dsa.DSAParameter;
import main.dsa.DSAService;
import main.ui.components.ModernTextArea;
import main.ui.components.RoundedButton;
import main.ui.components.StatusLabel;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ParameterPanel extends JPanel {
	private final MainFrame frame;
	private final DSAService service = new DSAService();
	private final JComboBox<Integer> pBits = new JComboBox<>(new Integer[] { 512, 1024, 2048 });
	private final JComboBox<Integer> qBits = new JComboBox<>(new Integer[] { 160, 256 });
	private final ModernTextArea pArea = new ModernTextArea(5);
	private final ModernTextArea qArea = new ModernTextArea(3);
	private final ModernTextArea gArea = new ModernTextArea(5);
	private final StatusLabel status = new StatusLabel();

	public ParameterPanel(MainFrame frame) {
		this.frame = frame;
		setLayout(new BorderLayout(0, 18));
		setBackground(Theme.APP_BG);
		setBorder(Theme.padding(28, 32, 28, 32));
		add(UIFactory.title("Sinh tham số DSA"), BorderLayout.NORTH);
		add(buildForm(), BorderLayout.CENTER);
	}

	private JPanel buildForm() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setOpaque(false);
		GridBagConstraints c = baseConstraints();

		c.gridx = 0;
		c.gridy = 0;
		panel.add(UIFactory.section("Kích thước p"), c);
		c.gridx = 1;
		panel.add(pBits, c);
		c.gridx = 0;
		c.gridy = 1;
		panel.add(UIFactory.section("Kích thước q"), c);
		c.gridx = 1;
		panel.add(qBits, c);

		RoundedButton generate = new RoundedButton("Sinh tham số");
		RoundedButton clear = new RoundedButton("Làm mới", Theme.MUTED, Theme.TEXT);
		generate.addActionListener(e -> generate());
		clear.addActionListener(e -> clear());
		c.gridx = 0;
		c.gridy = 2;
		panel.add(generate, c);
		c.gridx = 1;
		panel.add(clear, c);

		addArea(panel, c, 3, "p", pArea);
		addArea(panel, c, 5, "q", qArea);
		addArea(panel, c, 7, "g", gArea);
		c.gridx = 0;
		c.gridy = 9;
		c.gridwidth = 2;
		panel.add(status, c);
		return panel;
	}

	private void generate() {
		int p = (Integer) pBits.getSelectedItem();
		int q = (Integer) qBits.getSelectedItem();
		status.setInfo("Đang sinh tham số, vui lòng chờ...");
		new SwingWorker<DSAParameter, Void>() {
			protected DSAParameter doInBackground() {
				return service.generateParameters(p, q);
			}

			protected void done() {
				try {
					DSAParameter parameter = get();
					frame.setCurrentParameter(parameter);
					pArea.setText(parameter.getP().toString());
					qArea.setText(parameter.getQ().toString());
					gArea.setText(parameter.getG().toString());
					status.setSuccess("Sinh tham số thành công.");
				} catch (Exception ex) {
					status.setDanger("Lỗi: " + ex.getMessage());
				}
			}
		}.execute();
	}

	private void clear() {
		pArea.setText("");
		qArea.setText("");
		gArea.setText("");
		status.setInfo("Đã làm mới.");
	}

	private void addArea(JPanel panel, GridBagConstraints c, int row, String label, ModernTextArea area) {
		c.gridx = 0;
		c.gridy = row;
		c.gridwidth = 2;
		panel.add(UIFactory.section(label), c);
		c.gridy = row + 1;
		panel.add(UIFactory.scroll(area), c);
	}

	private GridBagConstraints baseConstraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = new Insets(6, 6, 6, 6);
		return c;
	}
}
