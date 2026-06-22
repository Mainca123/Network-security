package service;

import model.DSAKeyPairModel;
import util.BigIntegerUtil;
import util.DateTimeUtil;

import java.math.BigInteger;
import java.security.SecureRandom;

public class DSAKeyService {
    private static final int PRIME_CERTAINTY = 80;
    private final SecureRandom secureRandom = new SecureRandom();
    private final DSAParameterService parameterService = new DSAParameterService();

    public DSAKeyPairModel generateParameters(SecurityLevel securityLevel) {
        BigInteger[] pq = generatePAndQ(securityLevel.l(), securityLevel.n());
        BigInteger p = pq[0];
        BigInteger q = pq[1];
        BigInteger g = generateGenerator(p, q);

        DSAKeyPairModel model = new DSAKeyPairModel();
        model.setAlgorithm("DSA");
        model.setType("PARAMETERS");
        model.setP(p.toString());
        model.setQ(q.toString());
        model.setG(g.toString());
        model.setCreatedAt(DateTimeUtil.nowStorage());
        return model;
    }

    public DSAKeyPairModel generateKeyPair(DSAKeyPairModel parameters) {
        DSAParameterService.DomainParameters domain =
                parameterService.parseAndValidateDomain(parameters.getP(), parameters.getQ(), parameters.getG());

        BigInteger x = BigIntegerUtil.randomBetween(BigInteger.ONE, domain.q().subtract(BigInteger.ONE), secureRandom);
        BigInteger y = domain.g().modPow(x, domain.p());

        DSAKeyPairModel keyPair = new DSAKeyPairModel();
        keyPair.setAlgorithm("DSA");
        keyPair.setType("KEY_PAIR");
        keyPair.setP(domain.p().toString());
        keyPair.setQ(domain.q().toString());
        keyPair.setG(domain.g().toString());
        keyPair.setX(x.toString());
        keyPair.setY(y.toString());
        keyPair.setCreatedAt(DateTimeUtil.nowStorage());
        return keyPair;
    }

    public DSAKeyPairModel toPrivateKey(DSAKeyPairModel keyPair) {
        DSAKeyPairModel privateKey = new DSAKeyPairModel();
        privateKey.setAlgorithm("DSA");
        privateKey.setType("PRIVATE_KEY");
        privateKey.setP(keyPair.getP());
        privateKey.setQ(keyPair.getQ());
        privateKey.setG(keyPair.getG());
        privateKey.setX(keyPair.getX());
        privateKey.setCreatedAt(DateTimeUtil.nowStorage());
        return privateKey;
    }

    public DSAKeyPairModel toPublicKey(DSAKeyPairModel keyPair) {
        DSAKeyPairModel publicKey = new DSAKeyPairModel();
        publicKey.setAlgorithm("DSA");
        publicKey.setType("PUBLIC_KEY");
        publicKey.setP(keyPair.getP());
        publicKey.setQ(keyPair.getQ());
        publicKey.setG(keyPair.getG());
        publicKey.setY(keyPair.getY());
        publicKey.setCreatedAt(DateTimeUtil.nowStorage());
        return publicKey;
    }

    public DSAKeyPairModel toFullKeyPair(DSAKeyPairModel keyPair) {
        DSAKeyPairModel full = new DSAKeyPairModel();
        full.setAlgorithm("DSA");
        full.setType("KEY_PAIR");
        full.setP(keyPair.getP());
        full.setQ(keyPair.getQ());
        full.setG(keyPair.getG());
        full.setX(keyPair.getX());
        full.setY(keyPair.getY());
        full.setCreatedAt(DateTimeUtil.nowStorage());
        return full;
    }

    private BigInteger[] generatePAndQ(int l, int n) {
        while (true) {
            BigInteger q = BigInteger.probablePrime(n, secureRandom);
            BigInteger minK = BigInteger.ONE.shiftLeft(l - 1).subtract(BigInteger.ONE).divide(q).add(BigInteger.ONE);
            BigInteger maxK = BigInteger.ONE.shiftLeft(l).subtract(BigInteger.TWO).divide(q);

            for (int i = 0; i < 20000; i++) {
                BigInteger k = BigIntegerUtil.randomBetween(minK, maxK, secureRandom);
                if (k.testBit(0)) {
                    k = k.compareTo(maxK) < 0 ? k.add(BigInteger.ONE) : k.subtract(BigInteger.ONE);
                }
                BigInteger p = q.multiply(k).add(BigInteger.ONE);
                if (p.bitLength() == l && p.isProbablePrime(PRIME_CERTAINTY)) {
                    return new BigInteger[]{p, q};
                }
            }
        }
    }

    private BigInteger generateGenerator(BigInteger p, BigInteger q) {
        BigInteger exponent = p.subtract(BigInteger.ONE).divide(q);
        BigInteger g;
        do {
            BigInteger h = BigIntegerUtil.randomBetween(BigInteger.TWO, p.subtract(BigInteger.TWO), secureRandom);
            // Công thức DSA: g = h^((p - 1) / q) mod p, chọn lại nếu g <= 1.
            g = h.modPow(exponent, p);
        } while (g.compareTo(BigInteger.ONE) <= 0);
        return g;
    }

    public static SecurityLevel parseSecurityLevel(String value) {
        if (value == null) {
            return new SecurityLevel(1024, 160);
        }
        if (value.contains("3072")) {
            return new SecurityLevel(3072, 256);
        }
        if (value.contains("224")) {
            return new SecurityLevel(2048, 224);
        }
        if (value.contains("2048") && value.contains("256")) {
            return new SecurityLevel(2048, 256);
        }
        return new SecurityLevel(1024, 160);
    }

    public static class SecurityLevel {
        private final int l;
        private final int n;

        public SecurityLevel(int l, int n) {
            this.l = l;
            this.n = n;
        }

        public int l() {
            return l;
        }

        public int n() {
            return n;
        }
    }
}
