package util;

import model.DsaKeyPairText;

public final class KeyFileFormatter {
    private static final String PRIVATE_MARKER = "PRIVATE_KEY_BASE64";
    private static final String PUBLIC_MARKER = "PUBLIC_KEY_BASE64";

    private KeyFileFormatter() {
    }

    public static String format(DsaKeyPairText keyPair) {
        return PRIVATE_MARKER + "\n"
                + keyPair.privateKeyText().trim()
                + "\n\n" + PUBLIC_MARKER + "\n"
                + keyPair.publicKeyText().trim()
                + "\n";
    }

    public static DsaKeyPairText parse(String content) {
        int privateStart = content.indexOf(PRIVATE_MARKER);
        int publicStart = content.indexOf(PUBLIC_MARKER);

        if (privateStart < 0 || publicStart < 0 || publicStart <= privateStart) {
            throw new IllegalArgumentException("Tệp khóa không đúng định dạng.");
        }

        String privateKey = content.substring(privateStart + PRIVATE_MARKER.length(), publicStart).trim();
        String publicKey = content.substring(publicStart + PUBLIC_MARKER.length()).trim();
        return new DsaKeyPairText(privateKey, publicKey);
    }
}
