package service;

import model.DSAKeyPairModel;
import model.DSAParameters;
import util.BigIntegerUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DSAParameterService {
    private static final int PRIME_CERTAINTY = 80;

    public ParameterValidationResult validate(DSAParameters parameters) {
        List<String> errors = new ArrayList<>();
        if (parameters == null) {
            errors.add("Dữ liệu tham số rỗng, vui lòng nhập đầy đủ");
            return new ParameterValidationResult(errors);
        }

        String pStr = parameters.getP();
        String qStr = parameters.getQ();
        String gStr = parameters.getG();
        String xStr = parameters.getX();
        String yStr = parameters.getY();

        // Kiểm tra thiếu từng trường trước
        if (pStr == null || pStr.isBlank()) errors.add("Thiếu P – vui lòng nhập số nguyên tố P");
        if (qStr == null || qStr.isBlank()) errors.add("Thiếu Q – vui lòng nhập số nguyên tố Q");
        if (gStr == null || gStr.isBlank()) errors.add("Thiếu G – vui lòng nhập phần tử sinh G");
        if (xStr == null || xStr.isBlank()) errors.add("Thiếu X – vui lòng nhập khóa bí mật X");
        if (yStr == null || yStr.isBlank()) errors.add("Thiếu Y – vui lòng nhập khóa công khai Y");

        if (!errors.isEmpty()) {
            return new ParameterValidationResult(errors);
        }

        BigInteger p = parseField(pStr, "P", errors);
        BigInteger q = parseField(qStr, "Q", errors);
        BigInteger g = parseField(gStr, "G", errors);
        BigInteger x = parseField(xStr, "X", errors);
        BigInteger y = parseField(yStr, "Y", errors);

        if (p != null && p.signum() <= 0) {
            errors.add("P phải là số dương");
        } else if (p != null && !p.isProbablePrime(PRIME_CERTAINTY)) {
            errors.add("P không phải số nguyên tố – hãy kiểm tra lại giá trị P");
        }

        if (q != null && q.signum() <= 0) {
            errors.add("Q phải là số dương");
        } else if (q != null && !q.isProbablePrime(PRIME_CERTAINTY)) {
            errors.add("Q không phải số nguyên tố – hãy kiểm tra lại giá trị Q");
        }

        if (p != null && q != null && q.signum() > 0 && p.isProbablePrime(PRIME_CERTAINTY) && q.isProbablePrime(PRIME_CERTAINTY)
                && !p.subtract(BigInteger.ONE).mod(q).equals(BigInteger.ZERO)) {
            errors.add("Q không chia hết (P - 1) – P và Q không tương thích");
        }

        if (g != null && p != null && q != null && !isValidGenerator(p, q, g)) {
            errors.add("G không hợp lệ – G phải thỏa G^Q ≡ 1 (mod P) và 1 < G < P");
        }

        if (q != null && x != null && !BigIntegerUtil.isBetweenExclusive(x, BigInteger.ZERO, q)) {
            errors.add("X không hợp lệ – X phải nằm trong khoảng (0, Q)");
        }

        if (p != null && g != null && x != null && y != null && !isValidY(p, g, x, y)) {
            errors.add("Y không khớp với X – Y phải thỏa Y = G^X mod P");
        }

        return new ParameterValidationResult(errors);
    }

    public DomainParameters parseAndValidateDomain(String pValue, String qValue, String gValue) {
        BigInteger p = BigIntegerUtil.parseRequired(pValue, "P");
        BigInteger q = BigIntegerUtil.parseRequired(qValue, "Q");
        BigInteger g = BigIntegerUtil.parseRequired(gValue, "G");

        List<String> errors = new ArrayList<>();
        if (p.signum() <= 0) {
            errors.add("P phải là số dương");
        } else if (!p.isProbablePrime(PRIME_CERTAINTY)) {
            errors.add("P không phải số nguyên tố – giá trị P trong tệp khóa bị sai");
        }
        if (q.signum() <= 0) {
            errors.add("Q phải là số dương");
        } else if (!q.isProbablePrime(PRIME_CERTAINTY)) {
            errors.add("Q không phải số nguyên tố – giá trị Q trong tệp khóa bị sai");
        }
        if (q.signum() > 0 && !p.subtract(BigInteger.ONE).mod(q).equals(BigInteger.ZERO)) {
            errors.add("Q không chia hết (P - 1) – tệp khóa bị hỏng hoặc không đúng cặp");
        }
        if (!isValidGenerator(p, q, g)) {
            errors.add("G không hợp lệ – tệp khóa bị hỏng hoặc không đúng cặp");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", errors));
        }
        return new DomainParameters(p, q, g);
    }

    public PrivateKeyValues validatePrivateKey(DSAKeyPairModel key) {
        if (key == null) {
            throw new IllegalArgumentException("Tệp không đúng định dạng JSON khóa DSA");
        }
        if (!"DSA".equalsIgnoreCase(key.getAlgorithm())) {
            throw new IllegalArgumentException("Tệp này không phải khóa DSA (algorithm='" + key.getAlgorithm() + "')");
        }
        if (key.getX() == null || key.getX().isBlank()) {
            throw new IllegalArgumentException("Tệp này là khóa công khai, không phải khóa bí mật – hãy chọn đúng tệp khóa bí mật");
        }
        DomainParameters domain = parseAndValidateDomain(key.getP(), key.getQ(), key.getG());
        BigInteger x = BigIntegerUtil.parseRequired(key.getX(), "X");
        if (!BigIntegerUtil.isBetweenExclusive(x, BigInteger.ZERO, domain.q())) {
            throw new IllegalArgumentException("Khóa bí mật X không hợp lệ – X phải nằm trong khoảng (0, Q)");
        }
        return new PrivateKeyValues(domain.p(), domain.q(), domain.g(), x);
    }

    public PublicKeyValues validatePublicKey(DSAKeyPairModel key) {
        if (key == null) {
            throw new IllegalArgumentException("Tệp không đúng định dạng JSON khóa DSA");
        }
        if (!"DSA".equalsIgnoreCase(key.getAlgorithm())) {
            throw new IllegalArgumentException("Tệp này không phải khóa DSA (algorithm='" + key.getAlgorithm() + "')");
        }
        if (key.getY() == null || key.getY().isBlank()) {
            throw new IllegalArgumentException("Tệp khóa không chứa khóa công khai Y – tệp bị hỏng hoặc sai loại");
        }
        DomainParameters domain = parseAndValidateDomain(key.getP(), key.getQ(), key.getG());
        BigInteger y = BigIntegerUtil.parseRequired(key.getY(), "Y");
        if (!BigIntegerUtil.isBetweenExclusive(y, BigInteger.ZERO, domain.p())) {
            throw new IllegalArgumentException("Khóa công khai Y không hợp lệ – Y phải nằm trong khoảng (0, P)");
        }
        if (!y.modPow(domain.q(), domain.p()).equals(BigInteger.ONE)) {
            throw new IllegalArgumentException("Khóa công khai Y không khớp với tham số P, Q – tệp bị hỏng hoặc không đúng cặp khóa");
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
