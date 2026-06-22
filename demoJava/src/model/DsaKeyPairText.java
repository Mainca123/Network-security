package model;

public class DsaKeyPairText {
    private final String privateKeyText;
    private final String publicKeyText;

    public DsaKeyPairText(String privateKeyText, String publicKeyText) {
        this.privateKeyText = privateKeyText;
        this.publicKeyText = publicKeyText;
    }

    public String privateKeyText() {
        return privateKeyText;
    }

    public String publicKeyText() {
        return publicKeyText;
    }
}
