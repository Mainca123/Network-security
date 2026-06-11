package service;

import model.DSAKeyPairModel;
import model.DSAParameters;
import util.BigIntegerUtil;
import util.DateTimeUtil;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DSAParameterService {
    private static final int PRIME_CERTAINTY = 80;
    private final SecureRandom secureRandom = new SecureRandom();

    public ParameterValidationResult validate(DSAParameters parameters) {
        List<String> errors = new ArrayList<>();
        if (parameters == null) {
            errors.add("Tham số không được để trống");
            return new ParameterValidationResult(errors);
        }

        BigInteger p = parseField(parameters.getP(), "P", errors);
        BigInteger q = parseField(parameters.getQ(), "Q", errors);
        BigInteger g = parseField(parameters.getG(), "G", errors);
        BigInteger x = parseField(parameters.getX(), "X", errors);
        BigInteger y = parseField(parameters.getY(), "Y", errors);

        if (p != null && (p.signum() <= 0 || !p.isProbablePrime(PRIME_CERTAINTY))) {
            errors.add("P không phải số nguyên tố");
        }

        if (q != null && (q.signum() <= 0 || !q.isProbablePrime(PRIME_CERTAINTY))) {
            errors.add("Q không phải số nguyên tố");
        }

        if (p != null && q != null && q.signum() > 0 && !p.subtract(BigInteger.ONE).mod(q).equals(BigInteger.ZERO)) {
            errors.add("Q không chia hết P - 1");
        }

        if (!isValidGenerator(p, q, g)) {
            errors.add("G không hợp lệ");
        }

        if (q == null || x == null || !BigIntegerUtil.isBetweenExclusive(x, BigInteger.ZERO, q)) {
            errors.add("X không nằm trong khoảng hợp lệ");
        }

        if (!isValidY(p, g, x, y)) {
            errors.add("Y không khớp với X");
        }

        return new ParameterValidationResult(errors);
    }

    public DSAParameters generateFromManualInput(String pValue, String qValue, String xValue) {
        List<String> errors = new ArrayList<>();
        BigInteger p = parseField(pValue, "P", errors);
        BigInteger q = parseField(qValue, "Q", errors);
        BigInteger x = parseField(xValue, "X", errors);

        if (p != null && (p.signum() <= 0 || !p.isProbablePrime(PRIME_CERTAINTY))) {
            errors.add("P không phải số nguyên tố");
        }

        if (q != null && (q.signum() <= 0 || !q.isProbablePrime(PRIME_CERTAINTY))) {
            errors.add("Q không phải số nguyên tố");
        }

        if (p != null && q != null && q.signum() > 0 && !p.subtract(BigInteger.ONE).mod(q).equals(BigInteger.ZERO)) {
            errors.add("Q không chia hết P - 1");
        }

        if (q == null || x == null || !BigIntegerUtil.isBetweenExclusive(x, BigInteger.ZERO, q)) {
            errors.add("X không nằm trong khoảng hợp lệ");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }

        BigInteger g = generateGenerator(p, q);
        BigInteger y = g.modPow(x, p);
        return new DSAParameters(
                p.toString(),
                q.toString(),
                g.toString(),
                x.toString(),
                y.toString(),
                DateTimeUtil.nowStorage()
        );
    }

    public DomainParameters parseAndValidateDomain(String pValue, String qValue, String gValue) {
        BigInteger p = BigIntegerUtil.parseRequired(pValue, "P");
        BigInteger q = BigIntegerUtil.parseRequired(qValue, "Q");
        BigInteger g = BigIntegerUtil.parseRequired(gValue, "G");

        List<String> errors = new ArrayList<>();
        if (p.signum() <= 0 || !p.isProbablePrime(PRIME_CERTAINTY)) {
            errors.add("P không phải số nguyên tố");
        }
        if (q.signum() <= 0 || !q.isProbablePrime(PRIME_CERTAINTY)) {
            errors.add("Q không phải số nguyên tố");
        }
        if (q.signum() > 0 && !p.subtract(BigInteger.ONE).mod(q).equals(BigInteger.ZERO)) {
            errors.add("Q không chia hết P - 1");
        }
        if (!isValidGenerator(p, q, g)) {
            errors.add("G không hợp lệ");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
        return new DomainParameters(p, q, g);
    }

    public PrivateKeyValues validatePrivateKey(DSAKeyPairModel key) {
        if (key == null || !"DSA".equalsIgnoreCase(key.getAlgorithm())) {
            throw new IllegalArgumentException("Tệp khóa bí mật không đúng định dạng DSA");
        }
        DomainParameters domain = parseAndValidateDomain(key.getP(), key.getQ(), key.getG());
        BigInteger x = BigIntegerUtil.parseRequired(key.getX(), "X");
        if (!BigIntegerUtil.isBetweenExclusive(x, BigInteger.ZERO, domain.q())) {
            throw new IllegalArgumentException("X không nằm trong khoảng hợp lệ");
        }
        return new PrivateKeyValues(domain.p(), domain.q(), domain.g(), x);
    }

    public PublicKeyValues validatePublicKey(DSAKeyPairModel key) {
        if (key == null || !"DSA".equalsIgnoreCase(key.getAlgorithm())) {
            throw new IllegalArgumentException("Tệp khóa công khai không đúng định dạng DSA");
        }
        DomainParameters domain = parseAndValidateDomain(key.getP(), key.getQ(), key.getG());
        BigInteger y = BigIntegerUtil.parseRequired(key.getY(), "Y");
        if (!BigIntegerUtil.isBetweenExclusive(y, BigInteger.ZERO, domain.p())) {
            throw new IllegalArgumentException("Y không nằm trong khoảng hợp lệ");
        }
        if (!y.modPow(domain.q(), domain.p()).equals(BigInteger.ONE)) {
            throw new IllegalArgumentException("Y không khớp với tham số DSA");
        }
        return new PublicKeyValues(domain.p(), domain.q(), domain.g(), y);
    }

    private BigInteger parseField(String value, String fieldName, List<String> errors) {
        try {
            return BigIntegerUtil.parseRequired(value, fieldName);
        } catch (IllegalArgumentException ex) {
            errors.add(ex.getMessage());
            return null;
        }
    }

    private boolean isValidGenerator(BigInteger p, BigInteger q, BigInteger g) {
        if (p == null || q == null || g == null || p.signum() <= 0 || q.signum() <= 0) {
            return false;
        }
        return g.compareTo(BigInteger.ONE) > 0
                && g.compareTo(p) < 0
                && g.modPow(q, p).equals(BigInteger.ONE);
    }

    private boolean isValidY(BigInteger p, BigInteger g, BigInteger x, BigInteger y) {
        if (p == null || g == null || x == null || y == null || p.signum() <= 0 || x.signum() < 0) {
            return false;
        }
        return g.modPow(x, p).equals(y);
    }

    private BigInteger generateGenerator(BigInteger p, BigInteger q) {
        BigInteger exponent = p.subtract(BigInteger.ONE).divide(q);
        if (p.compareTo(BigInteger.valueOf(4)) < 0) {
            return BigInteger.TWO.modPow(exponent, p);
        }
        BigInteger g;
        do {
            BigInteger h = BigIntegerUtil.randomBetween(BigInteger.TWO, p.subtract(BigInteger.TWO), secureRandom);
            g = h.modPow(exponent, p);
        } while (g.compareTo(BigInteger.ONE) <= 0);
        return g;
    }

    public static class ParameterValidationResult {
        private final List<String> errors;

        private ParameterValidationResult(List<String> errors) {
            this.errors = List.copyOf(errors);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }
    }

    public static class DomainParameters {
        private final BigInteger p;
        private final BigInteger q;
        private final BigInteger g;

        public DomainParameters(BigInteger p, BigInteger q, BigInteger g) {
            this.p = p;
            this.q = q;
            this.g = g;
        }

        public BigInteger p() {
            return p;
        }

        public BigInteger q() {
            return q;
        }

        public BigInteger g() {
            return g;
        }
    }

    public static class PrivateKeyValues extends DomainParameters {
        private final BigInteger x;

        public PrivateKeyValues(BigInteger p, BigInteger q, BigInteger g, BigInteger x) {
            super(p, q, g);
            this.x = x;
        }

        public BigInteger x() {
            return x;
        }
    }

    public static class PublicKeyValues extends DomainParameters {
        private final BigInteger y;

        public PublicKeyValues(BigInteger p, BigInteger q, BigInteger g, BigInteger y) {
            super(p, q, g);
            this.y = y;
        }

        public BigInteger y() {
            return y;
        }
    }
}
