package service;

import model.DSAKeyPairModel;
import model.DSASignatureModel;
import util.BigIntegerUtil;
import util.DateTimeUtil;
import util.FileUtil;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

public class DSASignService {
    private final SecureRandom secureRandom = new SecureRandom();
    private final HashService hashService = new HashService();
    private final DSAParameterService parameterService = new DSAParameterService();

    public DSASignatureModel signText(String text, DSAKeyPairModel privateKey, String hashAlgorithm) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Văn bản cần ký không được để trống");
        }
        DSAParameterService.PrivateKeyValues keyValues = parameterService.validatePrivateKey(privateKey);
        String normalizedHash = HashService.normalizeAlgorithm(hashAlgorithm);
        byte[] hashBytes = hashService.digestText(text, normalizedHash);
        SignaturePair pair = signHash(hashBytes, keyValues);

        DSASignatureModel signature = new DSASignatureModel();
        signature.setAlgorithm("DSA");
        signature.setDataType("TEXT");
        signature.setHashAlgorithm(normalizedHash);
        signature.setHash(HashService.toHex(hashBytes));
        signature.setR(pair.r().toString());
        signature.setS(pair.s().toString());
        signature.setCreatedAt(DateTimeUtil.nowStorage());
        return signature;
    }

    public DSASignatureModel signFile(Path file, DSAKeyPairModel privateKey, String hashAlgorithm) throws Exception {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Tệp cần ký không hợp lệ");
        }
        DSAParameterService.PrivateKeyValues keyValues = parameterService.validatePrivateKey(privateKey);
        String normalizedHash = HashService.normalizeAlgorithm(hashAlgorithm);
        byte[] hashBytes = hashService.digestFile(file, normalizedHash);
        SignaturePair pair = signHash(hashBytes, keyValues);

        DSASignatureModel signature = new DSASignatureModel();
        signature.setAlgorithm("DSA");
        signature.setDataType("FILE");
        signature.setFileName(file.getFileName().toString());
        signature.setFileSize(FileUtil.humanReadableSize(Files.size(file)));
        signature.setFileHash(HashService.toHex(hashBytes));
        signature.setHashAlgorithm(normalizedHash);
        signature.setR(pair.r().toString());
        signature.setS(pair.s().toString());
        signature.setCreatedAt(DateTimeUtil.nowStorage());
        return signature;
    }

    private SignaturePair signHash(byte[] hashBytes, DSAParameterService.PrivateKeyValues key) {
        BigInteger p = key.p();
        BigInteger q = key.q();
        BigInteger g = key.g();
        BigInteger x = key.x();
        BigInteger hash = new BigInteger(1, hashBytes);

        BigInteger r;
        BigInteger s;
        do {
            BigInteger k = BigIntegerUtil.randomBetween(BigInteger.ONE, q.subtract(BigInteger.ONE), secureRandom);
            // DSA bắt buộc sinh k mới cho mỗi chữ ký; nếu r hoặc s bằng 0 thì bỏ k đó.
            r = g.modPow(k, p).mod(q);
            if (r.equals(BigInteger.ZERO)) {
                s = BigInteger.ZERO;
                continue;
            }
            s = k.modInverse(q).multiply(hash.add(x.multiply(r))).mod(q);
        } while (r.equals(BigInteger.ZERO) || s.equals(BigInteger.ZERO));

        return new SignaturePair(r, s);
    }

    private record SignaturePair(BigInteger r, BigInteger s) {
    }
}
