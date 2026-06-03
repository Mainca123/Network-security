package main.util;

import main.dsa.DSAParameter;
import main.dsa.DSASignature;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class KeyUtil {
	public static void savePrivateKey(File file, DSAParameter parameter, BigInteger x) throws IOException {
		FileUtil.writeText(file,
				"p=" + parameter.getP() + "\nq=" + parameter.getQ() + "\ng=" + parameter.getG() + "\nx=" + x + "\n");
	}

	public static void savePublicKey(File file, DSAParameter parameter, BigInteger y) throws IOException {
		FileUtil.writeText(file,
				"p=" + parameter.getP() + "\nq=" + parameter.getQ() + "\ng=" + parameter.getG() + "\ny=" + y + "\n");
	}

	public static PrivateKeyData loadPrivateKey(File file) throws IOException {
		Map<String, BigInteger> values = parseKeyValue(FileUtil.readText(file));
		require(values, "p", "q", "g", "x");
		return new PrivateKeyData(new DSAParameter(values.get("p"), values.get("q"), values.get("g")), values.get("x"));
	}

	public static PublicKeyData loadPublicKey(File file) throws IOException {
		Map<String, BigInteger> values = parseKeyValue(FileUtil.readText(file));
		require(values, "p", "q", "g", "y");
		return new PublicKeyData(new DSAParameter(values.get("p"), values.get("q"), values.get("g")), values.get("y"));
	}

	public static void saveSignature(File file, DSASignature signature) throws IOException {
		FileUtil.writeText(file, "r=" + signature.getR() + "\ns=" + signature.getS() + "\n");
	}

	public static DSASignature loadSignature(File file) throws IOException {
		Map<String, BigInteger> values = parseKeyValue(FileUtil.readText(file));
		require(values, "r", "s");
		return new DSASignature(values.get("r"), values.get("s"));
	}

	private static Map<String, BigInteger> parseKeyValue(String text) {
		Map<String, BigInteger> values = new HashMap<>();
		String[] lines = text.split("\\R");
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			int index = trimmed.indexOf('=');
			if (index <= 0 || index == trimmed.length() - 1) {
				throw new IllegalArgumentException("Dòng không hợp lệ: " + line);
			}
			values.put(trimmed.substring(0, index).trim(), new BigInteger(trimmed.substring(index + 1).trim()));
		}
		return values;
	}

	private static void require(Map<String, BigInteger> values, String... keys) {
		for (String key : keys) {
			if (!values.containsKey(key)) {
				throw new IllegalArgumentException("Thiếu trường " + key + " trong file.");
			}
		}
	}

	public static class PrivateKeyData {
		private final DSAParameter parameter;
		private final BigInteger privateKey;

		public PrivateKeyData(DSAParameter parameter, BigInteger privateKey) {
			this.parameter = parameter;
			this.privateKey = privateKey;
		}

		public DSAParameter getParameter() {
			return parameter;
		}

		public BigInteger getPrivateKey() {
			return privateKey;
		}
	}

	public static class PublicKeyData {
		private final DSAParameter parameter;
		private final BigInteger publicKey;

		public PublicKeyData(DSAParameter parameter, BigInteger publicKey) {
			this.parameter = parameter;
			this.publicKey = publicKey;
		}

		public DSAParameter getParameter() {
			return parameter;
		}

		public BigInteger getPublicKey() {
			return publicKey;
		}
	}
}
