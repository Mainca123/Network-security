package crypto;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

import model.DsaKeyPairText;
import util.EncodingUtils;

public class DsaService {
    public static final String SIGNATURE_ALGORITHM = "DSA tự cài đặt + SHA-256";
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final int KEY_SIZE = 1024;
    private static final int SUBPRIME_SIZE = 160;
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final BigInteger TWO = BigInteger.TWO;

    private final SecureRandom random = new SecureRandom();

    public DsaKeyPairText generateKeyPair() throws GeneralSecurityException {
        DsaParameters parameters = generateParameters();
        BigInteger x = randomInRange(ONE, parameters.q().subtract(ONE));
        BigInteger y = parameters.g().modPow(x, parameters.p());

        DsaPrivateKey privateKey = new DsaPrivateKey(parameters.p(), parameters.q(), parameters.g(), x);
        DsaPublicKey publicKey = new DsaPublicKey(parameters.p(), parameters.q(), parameters.g(), y);
        return new DsaKeyPairText(formatPrivateKey(privateKey), formatPublicKey(publicKey));
    }

    public DsaKeyPairText generateManualKeyPair(String pText, String qText) throws GeneralSecurityException {
        BigInteger p = parseDecimalParameter(pText, "p");
        BigInteger q = parseDecimalParameter(qText, "q");
        validatePrimeParameters(p, q);

        BigInteger g = generateGeneratorFromParameters(p, q);
        BigInteger x = randomInRange(ONE, q.subtract(ONE));
        BigInteger y = g.modPow(x, p);

        DsaPrivateKey privateKey = new DsaPrivateKey(p, q, g, x);
        DsaPublicKey publicKey = new DsaPublicKey(p, q, g, y);
        return new DsaKeyPairText(formatPrivateKey(privateKey), formatPublicKey(publicKey));
    }

    public String sign(byte[] data, String privateKeyText) throws GeneralSecurityException {
        DsaPrivateKey key = parsePrivateKey(privateKeyText);
        BigInteger hash = hashToNumber(data, key.q());

        BigInteger r = ZERO;
        BigInteger s = ZERO;
        while (r.equals(ZERO) || s.equals(ZERO)) {
            BigInteger k = randomInRange(ONE, key.q().subtract(ONE));
            BigInteger kInverse = k.modInverse(key.q());
            r = key.g().modPow(k, key.p()).mod(key.q());
            s = kInverse.multiply(hash.add(key.x().multiply(r))).mod(key.q());
        }

        return "r=" + r.toString(16) + "\n"
                + "s=" + s.toString(16);
    }

    public boolean verify(byte[] data, String signatureText, String publicKeyText) throws GeneralSecurityException {
        DsaPublicKey key = parsePublicKey(publicKeyText);
        DsaSignature signature = parseSignature(signatureText);

        if (!isInRange(signature.r(), ONE, key.q().subtract(ONE))
                || !isInRange(signature.s(), ONE, key.q().subtract(ONE))) {
            return false;
        }

        BigInteger hash = hashToNumber(data, key.q());
        BigInteger w = signature.s().modInverse(key.q());
        BigInteger u1 = hash.multiply(w).mod(key.q());
        BigInteger u2 = signature.r().multiply(w).mod(key.q());
        BigInteger v = key.g().modPow(u1, key.p())
                .multiply(key.y().modPow(u2, key.p()))
                .mod(key.p())
                .mod(key.q());

        return v.equals(signature.r());
    }

    public void validateSignatureText(String signatureText) throws GeneralSecurityException {
        parseSignature(signatureText);
    }

    public String sha256(byte[] data) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        return EncodingUtils.toHex(digest.digest(data));
    }

    public DsaPrivateKey parsePrivateKey(String privateKeyText) throws GeneralSecurityException {
        Map<String, BigInteger> values = parseKeyValueText(privateKeyText);
        DsaPrivateKey key = new DsaPrivateKey(
                required(values, "p"),
                required(values, "q"),
                required(values, "g"),
                required(values, "x"));
        validatePrivateKey(key);
        return key;
    }

    public DsaPublicKey parsePublicKey(String publicKeyText) throws GeneralSecurityException {
        Map<String, BigInteger> values = parseKeyValueText(publicKeyText);
        DsaPublicKey key = new DsaPublicKey(
                required(values, "p"),
                required(values, "q"),
                required(values, "g"),
                required(values, "y"));
        validatePublicKey(key);
        return key;
    }

    private DsaParameters generateParameters() throws GeneralSecurityException {
        BigInteger q = BigInteger.probablePrime(SUBPRIME_SIZE, random);
        BigInteger p = generatePrimeP(q);
        BigInteger g = generateGenerator(p, q);
        return new DsaParameters(p, q, g);
    }

    private BigInteger generatePrimeP(BigInteger q) throws GeneralSecurityException {
        int counter = 0;
        while (counter < 200_000) {
            BigInteger k = new BigInteger(KEY_SIZE - SUBPRIME_SIZE, random)
                    .setBit(KEY_SIZE - SUBPRIME_SIZE - 1);
            BigInteger p = k.multiply(q).add(ONE);
            if (p.bitLength() == KEY_SIZE && p.isProbablePrime(80)) {
                return p;
            }
            counter++;
        }
        throw new GeneralSecurityException("Không sinh được số nguyên tố p phù hợp. Vui lòng thử lại.");
    }

    private BigInteger generateGenerator(BigInteger p, BigInteger q) throws GeneralSecurityException {
        BigInteger exponent = p.subtract(ONE).divide(q);
        for (int counter = 0; counter < 20_000; counter++) {
            BigInteger h = randomInRange(TWO, p.subtract(TWO));
            BigInteger g = h.modPow(exponent, p);
            if (g.compareTo(ONE) > 0) {
                return g;
            }
        }
        throw new GeneralSecurityException("Không sinh được phần tử sinh g.");
    }

    private BigInteger generateGeneratorFromParameters(BigInteger p, BigInteger q) throws GeneralSecurityException {
        BigInteger exponent = p.subtract(ONE).divide(q);
        BigInteger h = TWO;
        while (h.compareTo(p.subtract(ONE)) < 0) {
            BigInteger g = h.modPow(exponent, p);
            if (g.compareTo(ONE) > 0) {
                return g;
            }
            h = h.add(ONE);
        }
        throw new GeneralSecurityException("Không sinh được phần tử sinh g từ p và q.");
    }

    private BigInteger hashToNumber(byte[] data, BigInteger q) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        BigInteger hash = new BigInteger(1, digest.digest(data));
        int extraBits = hash.bitLength() - q.bitLength();
        if (extraBits > 0) {
            hash = hash.shiftRight(extraBits);
        }
        return hash;
    }

    private void validatePrivateKey(DsaPrivateKey key) throws GeneralSecurityException {
        validateParameters(key.p(), key.q(), key.g());
        if (!isInRange(key.x(), ONE, key.q().subtract(ONE))) {
            throw new GeneralSecurityException("Khóa bí mật x phải nằm trong khoảng [1, q-1].");
        }
    }

    private void validatePublicKey(DsaPublicKey key) throws GeneralSecurityException {
        validateParameters(key.p(), key.q(), key.g());
        if (!isInRange(key.y(), TWO, key.p().subtract(TWO))) {
            throw new GeneralSecurityException("Khóa công khai y không hợp lệ.");
        }
    }

    private void validateParameters(BigInteger p, BigInteger q, BigInteger g) throws GeneralSecurityException {
        if (!p.subtract(ONE).mod(q).equals(ZERO)) {
            throw new GeneralSecurityException("Tham số q không chia hết p-1.");
        }
        if (g.compareTo(ONE) <= 0 || g.compareTo(p) >= 0 || !g.modPow(q, p).equals(ONE)) {
            throw new GeneralSecurityException("Tham số g không hợp lệ.");
        }
    }

    private void validatePrimeParameters(BigInteger p, BigInteger q) throws GeneralSecurityException {
        if (!p.isProbablePrime(100) || !q.isProbablePrime(100)) {
            throw new GeneralSecurityException("p và q phải là số nguyên tố.");
        }
        if (!p.subtract(ONE).mod(q).equals(ZERO)) {
            throw new GeneralSecurityException("(p - 1) phải chia hết cho q.");
        }
    }

    private BigInteger parseDecimalParameter(String text, String name) throws GeneralSecurityException {
        if (text == null || text.trim().isEmpty()) {
            throw new GeneralSecurityException("Thiếu tham số " + name + ".");
        }
        try {
            return new BigInteger(text.trim());
        } catch (NumberFormatException ex) {
            throw new GeneralSecurityException(name + " phải là số nguyên hợp lệ.", ex);
        }
    }

    private String formatPrivateKey(DsaPrivateKey key) {
        return "p=" + key.p().toString(16) + "\n"
                + "q=" + key.q().toString(16) + "\n"
                + "g=" + key.g().toString(16) + "\n"
                + "x=" + key.x().toString(16);
    }

    private String formatPublicKey(DsaPublicKey key) {
        return "p=" + key.p().toString(16) + "\n"
                + "q=" + key.q().toString(16) + "\n"
                + "g=" + key.g().toString(16) + "\n"
                + "y=" + key.y().toString(16);
    }

    private DsaSignature parseSignature(String signatureText) throws GeneralSecurityException {
        Map<String, BigInteger> values = parseKeyValueText(signatureText);
        return new DsaSignature(required(values, "r"), required(values, "s"));
    }

    private Map<String, BigInteger> parseKeyValueText(String text) throws GeneralSecurityException {
        Map<String, BigInteger> values = new LinkedHashMap<>();
        if (text == null || text.trim().isEmpty()) {
            throw new GeneralSecurityException("Dữ liệu đang trống.");
        }

        String[] lines = text.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0 || separator == trimmed.length() - 1) {
                throw new GeneralSecurityException("Dòng không đúng định dạng key=value: " + trimmed);
            }
            String key = trimmed.substring(0, separator).trim().toLowerCase();
            String value = trimmed.substring(separator + 1).trim();
            values.put(key, new BigInteger(value, 16));
        }
        return values;
    }

    private BigInteger required(Map<String, BigInteger> values, String name) throws GeneralSecurityException {
        BigInteger value = values.get(name);
        if (value == null) {
            throw new GeneralSecurityException("Thiếu tham số " + name + ".");
        }
        return value;
    }

    private BigInteger randomInRange(BigInteger minInclusive, BigInteger maxInclusive) {
        BigInteger range = maxInclusive.subtract(minInclusive).add(ONE);
        BigInteger value;
        do {
            value = new BigInteger(range.bitLength(), random);
        } while (value.compareTo(range) >= 0);
        return value.add(minInclusive);
    }

    private boolean isInRange(BigInteger value, BigInteger minInclusive, BigInteger maxInclusive) {
        return value.compareTo(minInclusive) >= 0 && value.compareTo(maxInclusive) <= 0;
    }

    public record DsaPrivateKey(BigInteger p, BigInteger q, BigInteger g, BigInteger x) {
    }

    public record DsaPublicKey(BigInteger p, BigInteger q, BigInteger g, BigInteger y) {
    }

    private record DsaParameters(BigInteger p, BigInteger q, BigInteger g) {
    }

    private record DsaSignature(BigInteger r, BigInteger s) {
    }
}
