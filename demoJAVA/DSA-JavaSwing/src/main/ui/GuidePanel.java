package main.ui;

import main.ui.components.ModernTextArea;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class GuidePanel extends JPanel {
	public GuidePanel() {
		setLayout(new BorderLayout(0, 18));
		setBackground(Theme.APP_BG);
		setBorder(Theme.padding(28, 32, 28, 32));
		add(UIFactory.title("Hướng dẫn sử dụng"), BorderLayout.NORTH);

		ModernTextArea guide = new ModernTextArea(24);
		guide.setEditable(false);
		guide.setText("""
				1. Sinh tham số DSA
				   - Chọn kích thước p và q.
				   - Nhấn "Sinh tham số" để tạo p, q, g.

				2. Sinh khóa
				   - Sau khi có tham số, nhấn "Sinh khóa".
				   - Lưu private.key để ký file, lưu public.key để xác thực.

				3. Ký file
				   - Chọn file bất kỳ: txt, pdf, ảnh, zip, rar hoặc file nhị phân.
				   - Nhấn "Ký file". Ứng dụng sẽ băm SHA-256 và tự cài đặt DSA bằng BigInteger.
				   - Nhấn "Lưu chữ ký" để tạo file .sig chứa r và s.

				4. Xác thực chữ ký
				   - Chọn file gốc, file .sig và public.key.
				   - Nhấn "Xác thực chữ ký".
				   - Nếu v == r, chữ ký hợp lệ và file chưa bị thay đổi.

				Công thức ký:
				   r = (g^k mod p) mod q
				   s = k^-1 * (H(m) + x*r) mod q

				Công thức xác thực:
				   w = s^-1 mod q
				   u1 = H(m)*w mod q
				   u2 = r*w mod q
				   v = ((g^u1 * y^u2) mod p) mod q
				""");
		add(UIFactory.scroll(guide), BorderLayout.CENTER);
	}
}
