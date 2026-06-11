package util;

import java.math.BigInteger;
import java.security.SecureRandom;

public final class BigIntegerUtil {
    public static final BigInteger ZERO = BigInteger.ZERO;
    public static final BigInteger ONE = BigInteger.ONE;
    public static final BigInteger TWO = BigInteger.TWO;

    private BigIntegerUtil() {
    }

    public static BigInteger parseRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " không được để trống");
        }

        try {
            return new BigInteger(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " không phải số nguyên hợp lệ", ex);
        }
    }

    public static BigInteger randomBetween(BigInteger minInclusive, BigInteger maxInclusive, SecureRandom random) {
        if (minInclusive.compareTo(maxInclusive) > 0) {
            throw new IllegalArgumentException("Khoảng sinh số ngẫu nhiên không hợp lệ");
        }

        BigInteger range = maxInclusive.subtract(minInclusive).add(ONE);
        BigInteger candidate;
        do {
            candidate = new BigInteger(range.bitLength(), random);
        } while (candidate.compareTo(range) >= 0);
        return candidate.add(minInclusive);
    }

    public static boolean isBetweenExclusive(BigInteger value, BigInteger minExclusive, BigInteger maxExclusive) {
        return value.compareTo(minExclusive) > 0 && value.compareTo(maxExclusive) < 0;
    }
}
