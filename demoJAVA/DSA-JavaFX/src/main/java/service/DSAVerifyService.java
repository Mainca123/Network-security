package service;

import model.DSAKeyPairModel;
import model.DSASignatureModel;
import util.BigIntegerUtil;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;

public class DSAVerifyService {
    private final HashService hashService = new HashService();
    private final DSAParameterService parameterService = new DSAParameterService();

    public VerificationResult verifyText(String originalText, DSASignatureModel signature, DSAKeyPairModel publicKey) throws Exception {
        if (originalText == null) {
            throw new IllegalArgumentException("Văn bản gốc không được để trống");
        }
        ensureSignature(signature, "TEXT");
        DSAParameterService.PublicKeyValues keyValues = parameterService.validatePublicKey(publicKey);
        String hashAlgorithm = HashService.normalizeAlgorithm(signature.getHashAlgorithm());
        byte[] currentHash = hashService.digestText(originalText, hashAlgorithm);
        String currentHashHex = HashService.toHex(currentHash);
        String initialHash = signature.getHash();

        if (initialHash != null && !initialHash.isBlank()
                && !currentHashHex.equalsIgnoreCase(initialHash)) {
            return VerificationResult.failed(ResultCode.CONTENT_CHANGED,
                    "Xác thực thất bại: Nội dung đã bị chỉnh sửa sau khi ký.")
                    .withHashes(initialHash, currentHashHex);
        }

        VerificationComputation computation = verifyHash(currentHash, signature, keyValues);
        return computation.valid()
                ? VerificationResult.success("Xác thực thành công")
                .withHashes(initialHash, currentHashHex)
                .withComputation(computation)
                : VerificationResult.failed(ResultCode.PUBLIC_KEY_OR_SIGNATURE_INVALID,
                "Xác thực thất bại: Chữ ký không hợp lệ hoặc không khớp với khóa công khai.")
                .withHashes(initialHash, currentHashHex)
                .withComputation(computation);
    }

    public VerificationResult verifyFile(Path originalFile, DSASignatureModel signature, DSAKeyPairModel publicKey) throws Exception {
        if (originalFile == null || !Files.isRegularFile(originalFile)) {
            throw new IllegalArgumentException("Tệp gốc không hợp lệ");
        }
        ensureSignature(signature, "FILE");
        DSAParameterService.PublicKeyValues keyValues = parameterService.validatePublicKey(publicKey);
        String hashAlgorithm = HashService.normalizeAlgorithm(signature.getHashAlgorithm());
        byte[] currentHash = hashService.digestFile(originalFile, hashAlgorithm);
        String currentHashHex = HashService.toHex(currentHash);
        String initialHash = signature.getFileHash();

        if (initialHash == null || initialHash.isBlank()) {
            throw new IllegalArgumentException("Tệp chữ ký không đúng định dạng hoặc bị hỏng.");
        }

        if (!currentHashHex.equalsIgnoreCase(initialHash)) {
            return VerificationResult.failed(ResultCode.CONTENT_CHANGED,
                    "Xác thực thất bại: Nội dung đã bị chỉnh sửa sau khi ký.")
                    .withHashes(initialHash, currentHashHex);
        }

        VerificationComputation computation = verifyHash(currentHash, signature, keyValues);
        return computation.valid()
                ? VerificationResult.success("Xác thực thành công")
                .withHashes(initialHash, currentHashHex)
                .withComputation(computation)
                : VerificationResult.failed(ResultCode.PUBLIC_KEY_OR_SIGNATURE_INVALID,
                "Xác thực thất bại: Chữ ký không hợp lệ hoặc không khớp với khóa công khai.")
                .withHashes(initialHash, currentHashHex)
                .withComputation(computation);
    }

    private void ensureSignature(DSASignatureModel signature, String expectedType) {
        if (signature == null || !"DSA".equalsIgnoreCase(signature.getAlgorithm())
                || signature.getR() == null || signature.getR().isBlank()
                || signature.getS() == null || signature.getS().isBlank()) {
            throw new IllegalArgumentException("Tệp chữ ký không đúng định dạng hoặc bị hỏng.");
        }
        if (!expectedType.equalsIgnoreCase(signature.getDataType())) {
            throw new IllegalArgumentException("Tệp chữ ký không đúng định dạng hoặc bị hỏng.");
        }
    }

    private VerificationComputation verifyHash(byte[] hashBytes, DSASignatureModel signature, DSAParameterService.PublicKeyValues key) {
        BigInteger p = key.p();
        BigInteger q = key.q();
        BigInteger g = key.g();
        BigInteger y = key.y();
        BigInteger r = BigIntegerUtil.parseRequired(signature.getR(), "R");
        BigInteger s = BigIntegerUtil.parseRequired(signature.getS(), "S");

        if (!BigIntegerUtil.isBetweenExclusive(r, BigInteger.ZERO, q)
                || !BigIntegerUtil.isBetweenExclusive(s, BigInteger.ZERO, q)) {
            return new VerificationComputation(false, r, s, null, y);
        }

        BigInteger hash = new BigInteger(1, hashBytes);
        // Công thức xác thực DSA: w = s^-1, u1 = H(m)w, u2 = rw, v = (g^u1 * y^u2 mod p) mod q.
        BigInteger w = s.modInverse(q);
        BigInteger u1 = hash.multiply(w).mod(q);
        BigInteger u2 = r.multiply(w).mod(q);
        BigInteger v = g.modPow(u1, p).multiply(y.modPow(u2, p)).mod(p).mod(q);
        return new VerificationComputation(v.equals(r), r, s, v, y);
    }

    public enum ResultCode {
        SUCCESS,
        CONTENT_CHANGED,
        PUBLIC_KEY_OR_SIGNATURE_INVALID,
        MALFORMED_SIGNATURE
    }

    private record VerificationComputation(boolean valid, BigInteger r, BigInteger s, BigInteger v, BigInteger publicKeyY) {
    }

    public static class VerificationResult {
        private final boolean valid;
        private final ResultCode code;
        private final String message;
        private String initialHash;
        private String currentHash;
        private String r;
        private String s;
        private String v;
        private String publicKeyY;

        private VerificationResult(boolean valid, ResultCode code, String message) {
            this.valid = valid;
            this.code = code;
            this.message = message;
        }

        public static VerificationResult success(String message) {
            return new VerificationResult(true, ResultCode.SUCCESS, message);
        }

        public static VerificationResult failed(ResultCode code, String message) {
            return new VerificationResult(false, code, message);
        }

        public boolean isValid() {
            return valid;
        }

        public ResultCode getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getInitialHash() {
            return initialHash;
        }

        public String getCurrentHash() {
            return currentHash;
        }

        public String getR() {
            return r;
        }

        public String getS() {
            return s;
        }

        public String getV() {
            return v;
        }

        public String getPublicKeyY() {
            return publicKeyY;
        }

        public String toDetailedMessage() {
            StringBuilder builder = new StringBuilder(message);
            if (initialHash != null && !initialHash.isBlank()) {
                builder.append("\nMã băm ban đầu: ").append(initialHash);
            }
            if (currentHash != null && !currentHash.isBlank()) {
                builder.append("\nMã băm hiện tại: ").append(currentHash);
            }
            if (r != null) {
                builder.append("\nr: ").append(r);
            }
            if (s != null) {
                builder.append("\ns: ").append(s);
            }
            if (v != null) {
                builder.append("\nv: ").append(v);
                builder.append(v.equals(r) ? "\nKết quả: v == r" : "\nKết quả: v != r");
            }
            if (publicKeyY != null && !valid) {
                builder.append("\nKhóa công khai y: ").append(publicKeyY);
            }
            return builder.toString();
        }

        private VerificationResult withHashes(String initialHash, String currentHash) {
            this.initialHash = initialHash;
            this.currentHash = currentHash;
            return this;
        }

        private VerificationResult withComputation(VerificationComputation computation) {
            this.r = computation.r() == null ? null : computation.r().toString();
            this.s = computation.s() == null ? null : computation.s().toString();
            this.v = computation.v() == null ? null : computation.v().toString();
            this.publicKeyY = computation.publicKeyY() == null ? null : computation.publicKeyY().toString();
            return this;
        }
    }
}
