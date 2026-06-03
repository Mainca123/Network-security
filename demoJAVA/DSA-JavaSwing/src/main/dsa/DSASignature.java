package main.dsa;

import java.math.BigInteger;

public class DSASignature {
	private final BigInteger r;
	private final BigInteger s;

	public DSASignature(BigInteger r, BigInteger s) {
		this.r = r;
		this.s = s;
	}

	public BigInteger getR() {
		return r;
	}

	public BigInteger getS() {
		return s;
	}
}
