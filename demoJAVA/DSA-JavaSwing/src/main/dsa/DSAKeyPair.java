package main.dsa;

import java.math.BigInteger;

public class DSAKeyPair {
	private final BigInteger x;
	private final BigInteger y;

	public DSAKeyPair(BigInteger x, BigInteger y) {
		this.x = x;
		this.y = y;
	}

	public BigInteger getX() {
		return x;
	}

	public BigInteger getY() {
		return y;
	}
}
