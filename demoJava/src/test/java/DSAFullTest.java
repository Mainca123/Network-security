import model.DSAKeyPairModel;
import model.DSAParameters;
import model.DSASignatureModel;
import org.junit.jupiter.api.*;
import service.*;
import util.BigIntegerUtil;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.SecureRandom;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DSAFullTest {
    static DSAKeyService keyService;
    static DSASignService signService;
    static DSAVerifyService verifyService;
    static DSAParameterService paramService;
    static HashService hashService;
    static FileService fileService;
    static DSAKeyPairModel parameters;
    static DSAKeyPairModel keyPair;
    static Path tempDir;
    static Path testTextFile;
    static Path testBinaryFile;

    @BeforeAll
    static void setup() throws Exception {
        keyService    = new DSAKeyService();
        signService   = new DSASignService();
        verifyService = new DSAVerifyService();
        paramService  = new DSAParameterService();
        hashService   = new HashService();
        fileService   = new FileService();
        tempDir = Files.createTempDirectory("dsa-test-");
        testTextFile = tempDir.resolve("test.txt");
        Files.writeString(testTextFile, "Day la noi dung kiem thu he thong DSA.\nDong 2: ky tu dac biet: @#$%!");
        testBinaryFile = tempDir.resolve("test.bin");
        Files.write(testBinaryFile, new byte[]{0x00, 0x01, 0x02, (byte)0xFF, (byte)0xFE});
        System.out.println("=== BAT DAU TEST HE THONG DSA ===");
        System.out.println("Thu muc test tam: " + tempDir);
    }

    @AfterAll
    static void cleanup() {
        System.out.println("\n=== KET THUC TEST ===");
    }

    // ---- NHOM 1: TIEN ICH ----
    @Test @Order(1)
    @DisplayName("[UTIL-1] BigIntegerUtil.parseRequired - Chuoi so hop le")
    void util_parseRequired_valid() {
        BigInteger result = BigIntegerUtil.parseRequired("12345", "X");
        assertEquals(new BigInteger("12345"), result);
        System.out.println("  PASS parseRequired hop le: " + result);
    }

    @Test @Order(2)
    @DisplayName("[UTIL-2] parseRequired - Chuoi rong -> loi ro rang")
    void util_parseRequired_empty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BigIntegerUtil.parseRequired("", "P"));
        assertTrue(ex.getMessage().contains("P"), "Thong bao phai de cap den truong P");
        System.out.println("  PASS Loi rong: " + ex.getMessage());
    }

    @Test @Order(3)
    @DisplayName("[UTIL-3] parseRequired - Khong phai so -> loi")
    void util_parseRequired_notNumber() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> BigIntegerUtil.parseRequired("abc123", "Q"));
        assertTrue(ex.getMessage().contains("Q"));
        System.out.println("  PASS Loi khong phai so: " + ex.getMessage());
    }

    @Test @Order(4)
    @DisplayName("[UTIL-4] isBetweenExclusive")
    void util_isBetweenExclusive() {
        BigInteger zero = BigInteger.ZERO;
        BigInteger ten  = BigInteger.TEN;
        assertTrue(BigIntegerUtil.isBetweenExclusive(BigInteger.valueOf(5), zero, ten));
        assertFalse(BigIntegerUtil.isBetweenExclusive(zero, zero, ten));
        assertFalse(BigIntegerUtil.isBetweenExclusive(ten, zero, ten));
        System.out.println("  PASS isBetweenExclusive dung");
    }

    @Test @Order(5)
    @DisplayName("[UTIL-5] HashService SHA-256")
    void util_hashText_sha256() throws Exception {
        byte[] hash = hashService.digestText("hello", "SHA-256");
        assertNotNull(hash);
        assertEquals(32, hash.length);
        String hex = HashService.toHex(hash);
        assertEquals(64, hex.length());
        System.out.println("  PASS SHA-256: " + hex.substring(0, 16) + "...");
    }

    @Test @Order(6)
    @DisplayName("[UTIL-6] HashService SHA-1 va SHA-512")
    void util_hashText_variants() throws Exception {
        byte[] sha1   = hashService.digestText("test", "SHA-1");
        byte[] sha512 = hashService.digestText("test", "SHA-512");
        assertEquals(20, sha1.length);
        assertEquals(64, sha512.length);
        System.out.println("  PASS SHA-1=" + sha1.length + "B SHA-512=" + sha512.length + "B");
    }

    @Test @Order(7)
    @DisplayName("[UTIL-7] HashService - Hash tep")
    void util_hashFile() throws Exception {
        byte[] hash = hashService.digestFile(testTextFile, "SHA-256");
        assertNotNull(hash);
        assertEquals(32, hash.length);
        System.out.println("  PASS Hash tep: " + HashService.toHex(hash).substring(0,16) + "...");
    }

    @Test @Order(8)
    @DisplayName("[UTIL-8] HashService - Cung noi dung -> cung hash")
    void util_hashDeterministic() throws Exception {
        byte[] h1 = hashService.digestText("noi dung kiem tra", "SHA-256");
        byte[] h2 = hashService.digestText("noi dung kiem tra", "SHA-256");
        assertArrayEquals(h1, h2);
        System.out.println("  PASS Hash xac dinh (deterministic)");
    }

    // ---- NHOM 2: TAO KHOA ----
    @Test @Order(10)
    @DisplayName("[KEY-1] Tao tham so DSA 1024/160")
    void key_generateParameters_1024() {
        parameters = keyService.generateParameters(new DSAKeyService.SecurityLevel(1024, 160));
        assertNotNull(parameters.getP());
        assertNotNull(parameters.getQ());
        assertNotNull(parameters.getG());
        assertNull(parameters.getX());
        assertNull(parameters.getY());
        assertEquals("DSA", parameters.getAlgorithm());
        BigInteger p = new BigInteger(parameters.getP());
        BigInteger q = new BigInteger(parameters.getQ());
        assertTrue(p.isProbablePrime(80));
        assertTrue(q.isProbablePrime(80));
        assertTrue(p.subtract(BigInteger.ONE).mod(q).equals(BigInteger.ZERO));
        System.out.println("  PASS Tham so P(" + p.bitLength() + "bit) Q(" + q.bitLength() + "bit)");
    }

    @Test @Order(11)
    @DisplayName("[KEY-2] Tao cap khoa tu tham so")
    void key_generateKeyPair() {
        keyPair = keyService.generateKeyPair(parameters);
        assertNotNull(keyPair.getX());
        assertNotNull(keyPair.getY());
        assertEquals("KEY_PAIR", keyPair.getType());
        BigInteger p = new BigInteger(keyPair.getP());
        BigInteger g = new BigInteger(keyPair.getG());
        BigInteger x = new BigInteger(keyPair.getX());
        BigInteger y = new BigInteger(keyPair.getY());
        assertEquals(g.modPow(x, p), y, "Y = G^X mod P");
        System.out.println("  PASS Cap khoa X(" + x.bitLength() + "bit) Y(" + y.bitLength() + "bit)");
    }

    @Test @Order(12)
    @DisplayName("[KEY-3] Tach khoa bi mat - khong co Y")
    void key_toPrivateKey() {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        assertEquals("PRIVATE_KEY", priv.getType());
        assertNotNull(priv.getX());
        assertNull(priv.getY());
        System.out.println("  PASS Khoa bi mat: co X, khong co Y");
    }

    @Test @Order(13)
    @DisplayName("[KEY-4] Tach khoa cong khai - khong co X")
    void key_toPublicKey() {
        DSAKeyPairModel pub = keyService.toPublicKey(keyPair);
        assertEquals("PUBLIC_KEY", pub.getType());
        assertNotNull(pub.getY());
        assertNull(pub.getX());
        System.out.println("  PASS Khoa cong khai: co Y, khong co X");
    }

    @Test @Order(14)
    @DisplayName("[KEY-5] Luu va doc lai khoa JSON")
    void key_saveAndLoad() throws Exception {
        Path privFile = tempDir.resolve("private.json");
        Path pubFile  = tempDir.resolve("public.json");
        fileService.writeJson(privFile.toFile(), keyService.toPrivateKey(keyPair));
        fileService.writeJson(pubFile.toFile(), keyService.toPublicKey(keyPair));
        DSAKeyPairModel lp = fileService.readJson(privFile.toFile(), DSAKeyPairModel.class);
        DSAKeyPairModel lb = fileService.readJson(pubFile.toFile(), DSAKeyPairModel.class);
        assertEquals(keyPair.getX(), lp.getX());
        assertEquals(keyPair.getY(), lb.getY());
        assertEquals(keyPair.getP(), lp.getP());
        System.out.println("  PASS Luu/tai JSON thanh cong");
    }

    // ---- NHOM 3: VALIDATE THAM SO ----
    @Test @Order(20)
    @DisplayName("[PARAM-1] Validate tham so hop le")
    void param_validateValid() {
        DSAParameters p = new DSAParameters(keyPair.getP(), keyPair.getQ(), keyPair.getG(),
                keyPair.getX(), keyPair.getY(), "2024-01-01");
        DSAParameterService.ParameterValidationResult r = paramService.validate(p);
        assertTrue(r.isValid(), "Tham so hop le: " + r.getErrors());
        System.out.println("  PASS Validate tham so hop le");
    }

    @Test @Order(21)
    @DisplayName("[PARAM-2] Validate - Thieu P")
    void param_validate_missingP() {
        DSAParameters p = new DSAParameters("", keyPair.getQ(), keyPair.getG(),
                keyPair.getX(), keyPair.getY(), "");
        DSAParameterService.ParameterValidationResult r = paramService.validate(p);
        assertFalse(r.isValid());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("P")), "Loi phai de cap P: " + r.getErrors());
        System.out.println("  PASS Thieu P: " + r.getErrors().get(0));
    }

    @Test @Order(22)
    @DisplayName("[PARAM-3] Validate - Thieu Q")
    void param_validate_missingQ() {
        DSAParameters p = new DSAParameters(keyPair.getP(), "", keyPair.getG(),
                keyPair.getX(), keyPair.getY(), "");
        DSAParameterService.ParameterValidationResult r = paramService.validate(p);
        assertFalse(r.isValid());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("Q")));
        System.out.println("  PASS Thieu Q: " + r.getErrors().get(0));
    }

    @Test @Order(23)
    @DisplayName("[PARAM-4] Validate - P khong nguyen to")
    void param_validate_pNotPrime() {
        DSAParameters p = new DSAParameters("100", keyPair.getQ(), keyPair.getG(),
                keyPair.getX(), keyPair.getY(), "");
        DSAParameterService.ParameterValidationResult r = paramService.validate(p);
        assertFalse(r.isValid());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("P")));
        System.out.println("  PASS P khong nguyen to: " + r.getErrors().get(0));
    }

    @Test @Order(24)
    @DisplayName("[PARAM-5] Validate - Y khong khop X")
    void param_validate_yMismatch() {
        DSAParameters p = new DSAParameters(keyPair.getP(), keyPair.getQ(), keyPair.getG(),
                keyPair.getX(), "123456789", "");
        DSAParameterService.ParameterValidationResult r = paramService.validate(p);
        assertFalse(r.isValid());
        assertTrue(r.getErrors().stream().anyMatch(e -> e.contains("Y")));
        System.out.println("  PASS Y khong khop X: " + r.getErrors().stream().filter(e->e.contains("Y")).findFirst().orElse("?"));
    }

    @Test @Order(25)
    @DisplayName("[PARAM-6] validatePrivateKey - Chon nham khoa cong khai")
    void param_validatePrivKey_wrongType() {
        DSAKeyPairModel pub = keyService.toPublicKey(keyPair);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> paramService.validatePrivateKey(pub));
        assertTrue(ex.getMessage().contains("khoa cong khai") || ex.getMessage().contains("bi mat") 
                || ex.getMessage().toLowerCase().contains("public") || ex.getMessage().toLowerCase().contains("private"),
                "Thong bao sai loai khoa: " + ex.getMessage());
        System.out.println("  PASS Chon sai khoa: " + ex.getMessage());
    }

    @Test @Order(26)
    @DisplayName("[PARAM-7] validatePublicKey - Hop le")
    void param_validatePublicKey_valid() {
        DSAKeyPairModel pub = keyService.toPublicKey(keyPair);
        assertDoesNotThrow(() -> paramService.validatePublicKey(pub));
        System.out.println("  PASS Validate khoa cong khai hop le");
    }

    // ---- NHOM 4: KY VAN BAN ----
    @Test @Order(30)
    @DisplayName("[SIGN-TEXT-1] Ky van ban thanh cong")
    void signText_success() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSASignatureModel sig = signService.signText("Hello DSA!", priv, "SHA-256");
        assertNotNull(sig.getR());
        assertNotNull(sig.getS());
        assertNotNull(sig.getHash());
        assertEquals("TEXT", sig.getDataType());
        assertEquals("DSA", sig.getAlgorithm());
        System.out.println("  PASS Ky thanh cong: r=" + sig.getR().substring(0,8) + "...");
    }

    @Test @Order(31)
    @DisplayName("[SIGN-TEXT-2] Ky hai lan -> r,s khac nhau (ngau nhien)")
    void signText_randomness() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSASignatureModel s1 = signService.signText("Cung noi dung", priv, "SHA-256");
        DSASignatureModel s2 = signService.signText("Cung noi dung", priv, "SHA-256");
        assertTrue(!s1.getR().equals(s2.getR()) || !s1.getS().equals(s2.getS()), "Chu ky phai ngau nhien");
        System.out.println("  PASS Chu ky ngau nhien");
    }

    @Test @Order(32)
    @DisplayName("[SIGN-TEXT-3] Ky voi SHA-1 va SHA-512")
    void signText_hashVariants() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        assertDoesNotThrow(() -> signService.signText("test", priv, "SHA-1"));
        assertDoesNotThrow(() -> signService.signText("test", priv, "SHA-512"));
        System.out.println("  PASS Ky SHA-1 va SHA-512 thanh cong");
    }

    // ---- NHOM 5: KY TEP ----
    @Test @Order(40)
    @DisplayName("[SIGN-FILE-1] Ky tep txt")
    void signFile_txt() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSASignatureModel sig = signService.signFile(testTextFile, priv, "SHA-256");
        assertNotNull(sig.getR());
        assertNotNull(sig.getFileHash());
        assertEquals("FILE", sig.getDataType());
        System.out.println("  PASS Ky tep txt: fileHash=" + sig.getFileHash().substring(0,16) + "...");
    }

    @Test @Order(41)
    @DisplayName("[SIGN-FILE-2] Ky tep binary")
    void signFile_binary() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSASignatureModel sig = signService.signFile(testBinaryFile, priv, "SHA-256");
        assertNotNull(sig.getFileHash());
        System.out.println("  PASS Ky tep binary: hash=" + sig.getFileHash().substring(0,16) + "...");
    }

    @Test @Order(42)
    @DisplayName("[SIGN-FILE-3] Ky tep khong ton tai -> exception")
    void signFile_notExist() {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        assertThrows(Exception.class, () -> signService.signFile(tempDir.resolve("ghost.txt"), priv, "SHA-256"));
        System.out.println("  PASS Ky tep khong ton tai -> exception dung");
    }

    @Test @Order(43)
    @DisplayName("[SIGN-FILE-4] Luu va tai lai chu ky tep")
    void signFile_saveLoad() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSASignatureModel sig = signService.signFile(testTextFile, priv, "SHA-256");
        Path sf = tempDir.resolve("sig-file.json");
        fileService.writeJson(sf.toFile(), sig);
        DSASignatureModel loaded = fileService.readJson(sf.toFile(), DSASignatureModel.class);
        assertEquals(sig.getR(), loaded.getR());
        assertEquals(sig.getS(), loaded.getS());
        assertEquals(sig.getFileHash(), loaded.getFileHash());
        System.out.println("  PASS Luu/tai chu ky tep thanh cong");
    }

    // ---- NHOM 6: XAC THUC VAN BAN ----
    @Test @Order(50)
    @DisplayName("[VERIFY-TEXT-1] Xac thuc van ban hop le")
    void verifyText_valid() throws Exception {
        String ct = "Noi dung can xac thuc chu ky DSA";
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        DSASignatureModel sig = signService.signText(ct, priv, "SHA-256");
        DSAVerifyService.VerificationResult r = verifyService.verifyText(ct, sig, pub);
        assertTrue(r.isValid());
        assertEquals(DSAVerifyService.ResultCode.SUCCESS, r.getCode());
        System.out.println("  PASS Xac thuc van ban hop le: " + r.getMessage());
    }

    @Test @Order(51)
    @DisplayName("[VERIFY-TEXT-2] Van ban bi thay doi -> CONTENT_CHANGED")
    void verifyText_contentChanged() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        DSASignatureModel sig = signService.signText("Noi dung goc", priv, "SHA-256");
        DSAVerifyService.VerificationResult r = verifyService.verifyText("Noi dung da bi sua", sig, pub);
        assertFalse(r.isValid());
        assertEquals(DSAVerifyService.ResultCode.CONTENT_CHANGED, r.getCode());
        System.out.println("  PASS Noi dung thay doi: " + r.getMessage());
    }

    @Test @Order(52)
    @DisplayName("[VERIFY-TEXT-3] Chu ky bi sua (r gia mao) -> FAIL")
    void verifyText_signatureTampered() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        DSASignatureModel sig = signService.signText("Noi dung", priv, "SHA-256");
        DSASignatureModel fake = new DSASignatureModel();
        fake.setAlgorithm(sig.getAlgorithm());
        fake.setDataType(sig.getDataType());
        fake.setHashAlgorithm(sig.getHashAlgorithm());
        fake.setHash(sig.getHash());
        fake.setR("123456789");
        fake.setS(sig.getS());
        DSAVerifyService.VerificationResult r = verifyService.verifyText("Noi dung", fake, pub);
        assertFalse(r.isValid());
        System.out.println("  PASS Chu ky bi gia mao: " + r.getMessage());
    }

    @Test @Order(53)
    @DisplayName("[VERIFY-TEXT-4] Van ban rong -> exception ro rang")
    void verifyText_emptyContent() throws Exception {
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSASignatureModel sig = signService.signText("goc", priv, "SHA-256");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verifyService.verifyText("", sig, pub));
        System.out.println("  PASS Van ban rong: " + ex.getMessage());
    }

    @Test @Order(54)
    @DisplayName("[VERIFY-TEXT-5] Chu ky FILE dung cho TEXT -> exception")
    void verifyText_wrongSignatureType() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        DSASignatureModel fs = signService.signFile(testTextFile, priv, "SHA-256");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> verifyService.verifyText("noi dung", fs, pub));
        System.out.println("  PASS Sai loai chu ky: " + ex.getMessage());
    }

    // ---- NHOM 7: XAC THUC TEP ----
    @Test @Order(60)
    @DisplayName("[VERIFY-FILE-1] Xac thuc tep hop le")
    void verifyFile_valid() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        DSASignatureModel sig = signService.signFile(testTextFile, priv, "SHA-256");
        DSAVerifyService.VerificationResult r = verifyService.verifyFile(testTextFile, sig, pub);
        assertTrue(r.isValid());
        assertEquals(DSAVerifyService.ResultCode.SUCCESS, r.getCode());
        System.out.println("  PASS Xac thuc tep hop le: " + r.getMessage());
    }

    @Test @Order(61)
    @DisplayName("[VERIFY-FILE-2] Tep bi chinh sua -> CONTENT_CHANGED")
    void verifyFile_fileModified() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        Path orig = tempDir.resolve("original.txt");
        Files.writeString(orig, "Noi dung goc ban dau");
        DSASignatureModel sig = signService.signFile(orig, priv, "SHA-256");
        Files.writeString(orig, "Noi dung da bi hacker sua!");
        DSAVerifyService.VerificationResult r = verifyService.verifyFile(orig, sig, pub);
        assertFalse(r.isValid());
        assertEquals(DSAVerifyService.ResultCode.CONTENT_CHANGED, r.getCode());
        System.out.println("  PASS Tep bi chinh sua: " + r.getMessage());
    }

    @Test @Order(62)
    @DisplayName("[VERIFY-FILE-3] Chu ky tep bi gia mao (s thay doi) -> FAIL")
    void verifyFile_signatureTampered() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        DSASignatureModel sig = signService.signFile(testTextFile, priv, "SHA-256");
        DSASignatureModel fake = new DSASignatureModel();
        fake.setAlgorithm(sig.getAlgorithm());
        fake.setDataType(sig.getDataType());
        fake.setHashAlgorithm(sig.getHashAlgorithm());
        fake.setFileHash(sig.getFileHash());
        fake.setR(sig.getR());
        fake.setS("987654321");
        DSAVerifyService.VerificationResult r = verifyService.verifyFile(testTextFile, fake, pub);
        assertFalse(r.isValid());
        System.out.println("  PASS Chu ky tep gia mao: " + r.getMessage());
    }

    @Test @Order(63)
    @DisplayName("[VERIFY-FILE-4] Tep goc khong ton tai -> exception")
    void verifyFile_fileNotExist() throws Exception {
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        DSASignatureModel sig = signService.signFile(testTextFile, priv, "SHA-256");
        assertThrows(Exception.class, () -> verifyService.verifyFile(tempDir.resolve("ghost.txt"), sig, pub));
        System.out.println("  PASS Tep khong ton tai -> exception dung");
    }

    // ---- NHOM 8: END-TO-END ----
    @Test @Order(70)
    @DisplayName("[E2E-1] Luong day du: Tao khoa -> Ky van ban -> Xac thuc")
    void e2e_signAndVerifyText() throws Exception {
        DSAKeyPairModel p   = keyService.generateParameters(new DSAKeyService.SecurityLevel(1024, 160));
        DSAKeyPairModel kp  = keyService.generateKeyPair(p);
        DSAKeyPairModel priv = keyService.toPrivateKey(kp);
        DSAKeyPairModel pub  = keyService.toPublicKey(kp);
        String content = "Hop dong dien tu so 2024/HC-001";
        DSASignatureModel sig = signService.signText(content, priv, "SHA-256");
        assertTrue(verifyService.verifyText(content, sig, pub).isValid());
        assertFalse(verifyService.verifyText(content + " (sua)", sig, pub).isValid());
        System.out.println("  PASS E2E van ban: ky, xac thuc dung, phat hien gia mao");
    }

    @Test @Order(71)
    @DisplayName("[E2E-2] Luong tep: Tao khoa -> Ky -> Luu JSON -> Tai lai -> Xac thuc")
    void e2e_signAndVerifyFile_withPersistence() throws Exception {
        DSAKeyPairModel p   = keyService.generateParameters(new DSAKeyService.SecurityLevel(1024, 160));
        DSAKeyPairModel kp  = keyService.generateKeyPair(p);
        Path privF = tempDir.resolve("e2e-priv.json");
        Path pubF  = tempDir.resolve("e2e-pub.json");
        fileService.writeJson(privF.toFile(), keyService.toPrivateKey(kp));
        fileService.writeJson(pubF.toFile(), keyService.toPublicKey(kp));
        DSAKeyPairModel lp  = fileService.readJson(privF.toFile(), DSAKeyPairModel.class);
        DSASignatureModel sig = signService.signFile(testTextFile, lp, "SHA-256");
        Path sf = tempDir.resolve("e2e-sig.json");
        fileService.writeJson(sf.toFile(), sig);
        DSAKeyPairModel lb  = fileService.readJson(pubF.toFile(), DSAKeyPairModel.class);
        DSASignatureModel ls = fileService.readJson(sf.toFile(), DSASignatureModel.class);
        assertTrue(verifyService.verifyFile(testTextFile, ls, lb).isValid());
        System.out.println("  PASS E2E tep voi persistence: toan ven hoan chinh");
    }

    @Test @Order(72)
    @DisplayName("[E2E-3] Tao khoa thu cong tu P,Q -> validate hop le")
    void e2e_manualKeyFromPQ() throws Exception {
        String pStr = keyPair.getP();
        String qStr = keyPair.getQ();
        SecureRandom rnd = new SecureRandom();
        BigInteger p = new BigInteger(pStr);
        BigInteger q = new BigInteger(qStr);
        BigInteger exp = p.subtract(BigInteger.ONE).divide(q);
        BigInteger g;
        do {
            BigInteger h = BigIntegerUtil.randomBetween(BigInteger.TWO, p.subtract(BigInteger.TWO), rnd);
            g = h.modPow(exp, p);
        } while (g.compareTo(BigInteger.ONE) <= 0);
        BigInteger x = BigIntegerUtil.randomBetween(BigInteger.ONE, q.subtract(BigInteger.ONE), rnd);
        BigInteger y = g.modPow(x, p);
        DSAParameters params = new DSAParameters(pStr, qStr, g.toString(), x.toString(), y.toString(), "test");
        DSAParameterService.ParameterValidationResult vr = paramService.validate(params);
        assertTrue(vr.isValid(), "Tham so thu cong tu P,Q phai hop le: " + vr.getErrors());
        System.out.println("  PASS Tao khoa thu cong tu P,Q: hop le");
    }

    @Test @Order(73)
    @DisplayName("[E2E-4] Ky va xac thuc voi 3 thuat toan bam")
    void e2e_allHashAlgorithms() throws Exception {
        String content = "Kiem tra tat ca thuat toan bam";
        DSAKeyPairModel priv = keyService.toPrivateKey(keyPair);
        DSAKeyPairModel pub  = keyService.toPublicKey(keyPair);
        for (String algo : new String[]{"SHA-1", "SHA-256", "SHA-512"}) {
            DSASignatureModel sig = signService.signText(content, priv, algo);
            DSAVerifyService.VerificationResult r = verifyService.verifyText(content, sig, pub);
            assertTrue(r.isValid(), algo + " phai xac thuc thanh cong");
            System.out.println("  PASS " + algo + ": ky + xac thuc thanh cong");
        }
    }
}