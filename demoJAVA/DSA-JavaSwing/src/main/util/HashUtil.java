package main.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
	public static byte[] sha256(byte[] data) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(data);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Không tìm thấy thuật toán SHA-256.", e);
		}
	}

	public static String sha256Hex(byte[] data) {
		StringBuilder builder = new StringBuilder();
		for (byte b : sha256(data)) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}
}
