package service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class HashService {
    public byte[] digestText(String text, String algorithm) throws NoSuchAlgorithmException {
        return digestBytes(text.getBytes(StandardCharsets.UTF_8), algorithm);
    }

    public String hashText(String text, String algorithm) throws NoSuchAlgorithmException {
        return toHex(digestText(text, algorithm));
    }

    public byte[] digestBytes(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(normalizeAlgorithm(algorithm));
        return digest.digest(data);
    }

    public byte[] digestFile(Path path, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(normalizeAlgorithm(algorithm));
        try (InputStream inputStream = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return digest.digest();
    }

    public String hashFile(Path path, String algorithm) throws IOException, NoSuchAlgorithmException {
        return toHex(digestFile(path, algorithm));
    }

    public static String normalizeAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            return "SHA-256";
        }
        String normalized = algorithm.trim().toUpperCase(Locale.ROOT).replace("_", "-");
        return switch (normalized) {
            case "SHA1", "SHA-1" -> "SHA-1";
            case "SHA256", "SHA-256" -> "SHA-256";
            case "SHA512", "SHA-512" -> "SHA-512";
            default -> normalized;
        };
    }

    public static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            hex[i * 2] = alphabet[value >>> 4];
            hex[i * 2 + 1] = alphabet[value & 0x0f];
        }
        return new String(hex);
    }
}
