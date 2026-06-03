package main.dsa;

import main.util.HashUtil;

import java.math.BigInteger;
import java.security.SecureRandom;

public class DSAService {
	private static final BigInteger ZERO = BigInteger.ZERO;
	private static final BigInteger ONE = BigInteger.ONE;
	private static final BigInteger TWO = BigInteger.TWO;
	private final SecureRandom random = new SecureRandom();

	public DSAParameter generateParameters(int pBits, int qBits) {
		if (qBits >= pBits) {
			throw new IllegalArgumentException("qBits phải nhỏ hơn pBits.");
		}

		BigInteger q = BigInteger.probablePrime(qBits, random);
		BigInteger p = generateP(pBits, q);
		BigInteger g = generateG(p, q);
		return new DSAParameter(p, q, g);
	}

	private BigInteger generateP(int pBits, BigInteger q) {
		int certainty = 80;
		BigInteger twoQ = q.shiftLeft(1);

		while (true) {
			BigInteger candidate = new BigInteger(pBits, random).setBit(pBits - 1);
			BigInteger p = candidate.subtract(candidate.subtract(ONE).mod(twoQ));
			if (p.bitLength() == pBits && p.isProbablePrime(certainty)) {
				return p;
			}
		}
	}

	private BigInteger generateG(BigInteger p, BigInteger q) {
		BigInteger exponent = p.subtract(ONE).divide(q);
		while (true) {
			BigInteger h = randomInRange(TWO, p.subtract(TWO));
			BigInteger g = h.modPow(exponent, p);
			if (g.compareTo(ONE) > 0) {
				return g;
			}
		}
	}

	public DSAKeyPair generateKeyPair(DSAParameter parameter) {
		BigInteger x = randomInRange(ONE, parameter.getQ().subtract(ONE));
		BigInteger y = parameter.getG().modPow(x, parameter.getP());
		return new DSAKeyPair(x, y);
	}

	public DSASignature sign(byte[] data, DSAParameter parameter, BigInteger privateKey) {
		BigInteger q = parameter.getQ();
		BigInteger p = parameter.getP();
		BigInteger g = parameter.getG();
		BigInteger h = hashToInteger(data).mod(q);

		while (true) {
			BigInteger k = randomInRange(ONE, q.subtract(ONE));
			BigInteger r = g.modPow(k, p).mod(q);
			if (r.equals(ZERO)) {
				continue;
			}
			BigInteger s = k.modInverse(q)
					.multiply(h.add(privateKey.multiply(r)))
					.mod(q);
			if (!s.equals(ZERO)) {
				return new DSASignature(r, s);
			}
		}
	}

	public boolean verify(byte[] data, DSASignature signature, DSAParameter parameter, BigInteger publicKey) {
		return calculateVerificationValue(data, signature, parameter, publicKey).equals(signature.getR());
	}

	public BigInteger calculateVerificationValue(byte[] data, DSASignature signature, DSAParameter parameter,
			BigInteger publicKey) {
		BigInteger q = parameter.getQ();
		BigInteger r = signature.getR();
		BigInteger s = signature.getS();
		if (r.compareTo(ZERO) <= 0 || r.compareTo(q) >= 0 || s.compareTo(ZERO) <= 0 || s.compareTo(q) >= 0) {
			return BigInteger.valueOf(-1);
		}

		BigInteger h = hashToInteger(data).mod(q);
		BigInteger w = s.modInverse(q);
		BigInteger u1 = h.multiply(w).mod(q);
		BigInteger u2 = r.multiply(w).mod(q);
		return parameter.getG().modPow(u1, parameter.getP())
				.multiply(publicKey.modPow(u2, parameter.getP()))
				.mod(parameter.getP())
				.mod(q);
	}

	private BigInteger hashToInteger(byte[] data) {
		return new BigInteger(1, HashUtil.sha256(data));
	}

	private BigInteger randomInRange(BigInteger minInclusive, BigInteger maxInclusive) {
		BigInteger range = maxInclusive.subtract(minInclusive).add(ONE);
		BigInteger value;
		do {
			value = new BigInteger(range.bitLength(), random);
		} while (value.compareTo(range) >= 0);
		return value.add(minInclusive);
	}
}
