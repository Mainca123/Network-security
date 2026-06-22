import model.*;
import service.*;
import util.BigIntegerUtil;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;

public class RunTests {
    static int passed = 0, failed = 0;
    static List<String> failures = new ArrayList<>();
    static DSAKeyPairModel KP;
    static Path TMP, TXT_FILE, BIN_FILE;
    static DSAKeyService KS;
    static DSASignService SS;
    static DSAVerifyService VS;
    static DSAParameterService PS;
    static HashService HS;
    static FileService FS;

    public static void main(String[] args) throws Exception {
        KS = new DSAKeyService(); SS = new DSASignService();
        VS = new DSAVerifyService(); PS = new DSAParameterService();
        HS = new HashService(); FS = new FileService();
        TMP = Files.createTempDirectory("dsa-test-");
        TXT_FILE = TMP.resolve("test.txt");
        Files.writeString(TXT_FILE, "Noi dung kiem thu DSA.\nDong 2: @#$%!");
        BIN_FILE = TMP.resolve("test.bin");
        Files.write(BIN_FILE, new byte[]{0x00,0x01,(byte)0xFF,(byte)0xFE});

        banner("TEST HE THONG CHU KY SO DSA");

        group("NHOM 1: TIEN ICH CO BAN");
        test("UTIL-1: parseRequired so hop le", RunTests::t_util1);
        test("UTIL-2: parseRequired rong -> loi co 'P'", RunTests::t_util2);
        test("UTIL-3: parseRequired khong so -> loi co 'Q'", RunTests::t_util3);
        test("UTIL-4: isBetweenExclusive", RunTests::t_util4);
        test("UTIL-5: SHA-256 = 32 byte", RunTests::t_util5);
        test("UTIL-6: SHA-1=20 SHA-512=64 byte", RunTests::t_util6);
        test("UTIL-7: Hash tep txt = 32 byte", RunTests::t_util7);
        test("UTIL-8: Hash xac dinh", RunTests::t_util8);

        group("NHOM 2: TAO KHOA TU DONG");
        test("KEY-1: Tao tham so 1024/160 (P,Q nguyen to, Q|(P-1))", RunTests::t_key1);
        test("KEY-2: Tao cap khoa, Y=G^X mod P", RunTests::t_key2);
        test("KEY-3: Khoa bi mat khong co Y", RunTests::t_key3);
        test("KEY-4: Khoa cong khai khong co X", RunTests::t_key4);
        test("KEY-5: Luu va tai lai khoa JSON", RunTests::t_key5);

        group("NHOM 3: VALIDATE THAM SO THU CONG");
        test("PARAM-1: Tham so hop le -> isValid=true", RunTests::t_param1);
        test("PARAM-2: P rong -> loi 'P'", RunTests::t_param2);
        test("PARAM-3: Q rong -> loi 'Q'", RunTests::t_param3);
        test("PARAM-4: P=100 khong nguyen to -> loi 'P'", RunTests::t_param4);
        test("PARAM-5: Y gia mao -> loi 'Y'", RunTests::t_param5);
        test("PARAM-6: Chon khoa cong khai lam bi mat -> exception", RunTests::t_param6);
        test("PARAM-7: validatePublicKey hop le -> khong throw", RunTests::t_param7);

        group("NHOM 4: KY VAN BAN");
        test("SIGN-TEXT-1: Ky SHA-256 -> co r,s,hash", RunTests::t_sign1);
        test("SIGN-TEXT-2: Ky 2 lan -> r|s khac nhau (ngau nhien)", RunTests::t_sign2);
        test("SIGN-TEXT-3: SHA-1 va SHA-512 khong throw", RunTests::t_sign3);

        group("NHOM 5: KY TEP");
        test("SIGN-FILE-1: Ky tep txt -> FILE + fileHash", RunTests::t_sfile1);
        test("SIGN-FILE-2: Ky tep binary -> fileHash co", RunTests::t_sfile2);
        test("SIGN-FILE-3: Ky tep khong ton tai -> exception", RunTests::t_sfile3);
        test("SIGN-FILE-4: Luu/tai chu ky JSON -> du lieu khop", RunTests::t_sfile4);

        group("NHOM 6: XAC THUC VAN BAN");
        test("VERIFY-TEXT-1: Xac thuc dung -> SUCCESS", RunTests::t_vtext1);
        test("VERIFY-TEXT-2: Van ban thay doi -> CONTENT_CHANGED", RunTests::t_vtext2);
        test("VERIFY-TEXT-3: r gia mao -> fail", RunTests::t_vtext3);
        test("VERIFY-TEXT-4: Van ban rong -> exception", RunTests::t_vtext4);
        test("VERIFY-TEXT-5: Chu ky FILE dung cho TEXT -> exception", RunTests::t_vtext5);

        group("NHOM 7: XAC THUC TEP");
        test("VERIFY-FILE-1: Xac thuc tep dung -> SUCCESS", RunTests::t_vfile1);
        test("VERIFY-FILE-2: Tep chinh sua -> CONTENT_CHANGED", RunTests::t_vfile2);
        test("VERIFY-FILE-3: s gia mao -> fail", RunTests::t_vfile3);
        test("VERIFY-FILE-4: Tep khong ton tai -> exception", RunTests::t_vfile4);

        group("NHOM 8: END-TO-END");
        test("E2E-1: Tao khoa -> Ky van ban -> Xac thuc -> Gia mao bi phat hien", RunTests::t_e2e1);
        test("E2E-2: Ky tep -> Luu JSON -> Tai lai -> Xac thuc", RunTests::t_e2e2);
        test("E2E-3: Khoa thu cong P,Q -> sinh G,X,Y -> validate hop le", RunTests::t_e2e3);
        test("E2E-4: SHA-1/256/512 deu ky + xac thuc dung", RunTests::t_e2e4);

        summary();
    }

    // === UTIL ===
    static void t_util1() { eq(new BigInteger("12345"), BigIntegerUtil.parseRequired("12345","X"),"gia tri sai"); }
    static void t_util2() { try { BigIntegerUtil.parseRequired("","P"); fail("no throw"); } catch(IllegalArgumentException e) { has(e.getMessage(),"P"); } }
    static void t_util3() { try { BigIntegerUtil.parseRequired("abc","Q"); fail("no throw"); } catch(IllegalArgumentException e) { has(e.getMessage(),"Q"); } }
    static void t_util4() {
        ok(BigIntegerUtil.isBetweenExclusive(BigInteger.valueOf(5),BigInteger.ZERO,BigInteger.TEN),"5 in (0,10)");
        ok(!BigIntegerUtil.isBetweenExclusive(BigInteger.ZERO,BigInteger.ZERO,BigInteger.TEN),"0 NOT in (0,10)");
        ok(!BigIntegerUtil.isBetweenExclusive(BigInteger.TEN,BigInteger.ZERO,BigInteger.TEN),"10 NOT in (0,10)");
    }
    static void t_util5() throws Exception { byte[] h=HS.digestText("hello","SHA-256"); eq(32,h.length,"len"); eq(64,HashService.toHex(h).length(),"hexlen"); }
    static void t_util6() throws Exception { eq(20,HS.digestText("t","SHA-1").length,"sha1"); eq(64,HS.digestText("t","SHA-512").length,"sha512"); }
    static void t_util7() throws Exception { eq(32,HS.digestFile(TXT_FILE,"SHA-256").length,"len"); }
    static void t_util8() throws Exception { ok(Arrays.equals(HS.digestText("x","SHA-256"),HS.digestText("x","SHA-256")),"hash deterministic"); }

    // === KEY ===
    static DSAKeyPairModel PARAMS;
    static void t_key1() {
        PARAMS = KS.generateParameters(new DSAKeyService.SecurityLevel(1024,160));
        ok(PARAMS.getP()!=null,"P"); ok(PARAMS.getQ()!=null,"Q"); ok(PARAMS.getX()==null,"X null");
        BigInteger p=new BigInteger(PARAMS.getP()), q=new BigInteger(PARAMS.getQ());
        ok(p.isProbablePrime(80),"P prime"); ok(q.isProbablePrime(80),"Q prime");
        ok(p.subtract(BigInteger.ONE).mod(q).equals(BigInteger.ZERO),"Q|(P-1)");
        print("P="+p.bitLength()+"bit Q="+q.bitLength()+"bit");
    }
    static void t_key2() {
        KP = KS.generateKeyPair(PARAMS);
        ok(KP.getX()!=null,"X"); ok(KP.getY()!=null,"Y"); eq("KEY_PAIR",KP.getType(),"type");
        BigInteger p=new BigInteger(KP.getP()),g=new BigInteger(KP.getG()),x=new BigInteger(KP.getX()),y=new BigInteger(KP.getY());
        eq(g.modPow(x,p),y,"Y=G^X mod P");
    }
    static void t_key3() { DSAKeyPairModel pk=KS.toPrivateKey(KP); eq("PRIVATE_KEY",pk.getType(),"type"); ok(pk.getX()!=null,"X"); ok(pk.getY()==null,"Y null"); }
    static void t_key4() { DSAKeyPairModel pb=KS.toPublicKey(KP); eq("PUBLIC_KEY",pb.getType(),"type"); ok(pb.getY()!=null,"Y"); ok(pb.getX()==null,"X null"); }
    static void t_key5() throws Exception {
        Path pf=TMP.resolve("pk.json"),bf=TMP.resolve("pb.json");
        FS.writeJson(pf.toFile(),KS.toPrivateKey(KP)); FS.writeJson(bf.toFile(),KS.toPublicKey(KP));
        DSAKeyPairModel lp=FS.readJson(pf.toFile(),DSAKeyPairModel.class), lb=FS.readJson(bf.toFile(),DSAKeyPairModel.class);
        eq(KP.getX(),lp.getX(),"X"); eq(KP.getY(),lb.getY(),"Y"); eq(KP.getP(),lp.getP(),"P");
    }

    // === PARAM ===
    static void t_param1() { var r=PS.validate(new DSAParameters(KP.getP(),KP.getQ(),KP.getG(),KP.getX(),KP.getY(),"")); ok(r.isValid(),"valid: "+r.getErrors()); }
    static void t_param2() { var r=PS.validate(new DSAParameters("",KP.getQ(),KP.getG(),KP.getX(),KP.getY(),"")); ok(!r.isValid(),"!valid"); ok(r.getErrors().stream().anyMatch(e->e.contains("P")),"err has P: "+r.getErrors()); print(r.getErrors().get(0)); }
    static void t_param3() { var r=PS.validate(new DSAParameters(KP.getP(),"",KP.getG(),KP.getX(),KP.getY(),"")); ok(!r.isValid(),"!valid"); ok(r.getErrors().stream().anyMatch(e->e.contains("Q")),"err has Q: "+r.getErrors()); print(r.getErrors().get(0)); }
    static void t_param4() { var r=PS.validate(new DSAParameters("100",KP.getQ(),KP.getG(),KP.getX(),KP.getY(),"")); ok(!r.isValid(),"!valid"); ok(r.getErrors().stream().anyMatch(e->e.contains("P")),"err has P: "+r.getErrors()); print(r.getErrors().get(0)); }
    static void t_param5() { var r=PS.validate(new DSAParameters(KP.getP(),KP.getQ(),KP.getG(),KP.getX(),"999","")); ok(!r.isValid(),"!valid"); ok(r.getErrors().stream().anyMatch(e->e.contains("Y")),"err has Y: "+r.getErrors()); print(r.getErrors().stream().filter(e->e.contains("Y")).findFirst().orElse("?")); }
    static void t_param6() { try { PS.validatePrivateKey(KS.toPublicKey(KP)); fail("no throw"); } catch(IllegalArgumentException e) { ok(e.getMessage().length()>5,"msg not empty"); print(e.getMessage()); } }
    static void t_param7() throws Exception { PS.validatePublicKey(KS.toPublicKey(KP)); }

    // === SIGN TEXT ===
    static void t_sign1() throws Exception {
        DSASignatureModel s=SS.signText("Hello DSA!",KS.toPrivateKey(KP),"SHA-256");
        ok(s.getR()!=null&&!s.getR().isEmpty(),"r"); ok(s.getS()!=null&&!s.getS().isEmpty(),"s");
        ok(s.getHash()!=null,"hash"); eq("TEXT",s.getDataType(),"type"); eq("DSA",s.getAlgorithm(),"alg");
        print("r="+s.getR().substring(0,10)+"... s="+s.getS().substring(0,10)+"...");
    }
    static void t_sign2() throws Exception {
        DSASignatureModel s1=SS.signText("same",KS.toPrivateKey(KP),"SHA-256"), s2=SS.signText("same",KS.toPrivateKey(KP),"SHA-256");
        ok(!s1.getR().equals(s2.getR())||!s1.getS().equals(s2.getS()),"signatures must differ");
    }
    static void t_sign3() throws Exception { SS.signText("x",KS.toPrivateKey(KP),"SHA-1"); SS.signText("x",KS.toPrivateKey(KP),"SHA-512"); }

    // === SIGN FILE ===
    static void t_sfile1() throws Exception { DSASignatureModel s=SS.signFile(TXT_FILE,KS.toPrivateKey(KP),"SHA-256"); ok(s.getR()!=null,"r"); ok(s.getFileHash()!=null,"fh"); eq("FILE",s.getDataType(),"type"); }
    static void t_sfile2() throws Exception { ok(SS.signFile(BIN_FILE,KS.toPrivateKey(KP),"SHA-256").getFileHash()!=null,"fh"); }
    static void t_sfile3() { try { SS.signFile(TMP.resolve("ghost.txt"),KS.toPrivateKey(KP),"SHA-256"); fail("no throw"); } catch(Exception e) { print(e.getClass().getSimpleName()); } }
    static void t_sfile4() throws Exception {
        DSASignatureModel s=SS.signFile(TXT_FILE,KS.toPrivateKey(KP),"SHA-256"); Path sf=TMP.resolve("sig.json");
        FS.writeJson(sf.toFile(),s); DSASignatureModel l=FS.readJson(sf.toFile(),DSASignatureModel.class);
        eq(s.getR(),l.getR(),"r"); eq(s.getS(),l.getS(),"s"); eq(s.getFileHash(),l.getFileHash(),"fh");
    }

    // === VERIFY TEXT ===
    static void t_vtext1() throws Exception {
        String ct="Noi dung xac thuc";
        DSASignatureModel sig=SS.signText(ct,KS.toPrivateKey(KP),"SHA-256");
        var r=VS.verifyText(ct,sig,KS.toPublicKey(KP));
        ok(r.isValid(),"valid"); eq(DSAVerifyService.ResultCode.SUCCESS,r.getCode(),"code"); print(r.getMessage());
    }
    static void t_vtext2() throws Exception {
        DSASignatureModel sig=SS.signText("Goc",KS.toPrivateKey(KP),"SHA-256");
        var r=VS.verifyText("Da sua",sig,KS.toPublicKey(KP));
        ok(!r.isValid(),"!valid"); eq(DSAVerifyService.ResultCode.CONTENT_CHANGED,r.getCode(),"code"); print(r.getMessage());
    }
    static void t_vtext3() throws Exception {
        DSASignatureModel sig=SS.signText("nd",KS.toPrivateKey(KP),"SHA-256");
        DSASignatureModel fake=new DSASignatureModel(); fake.setAlgorithm(sig.getAlgorithm()); fake.setDataType(sig.getDataType());
        fake.setHashAlgorithm(sig.getHashAlgorithm()); fake.setHash(sig.getHash()); fake.setR("123"); fake.setS(sig.getS());
        var r=VS.verifyText("nd",fake,KS.toPublicKey(KP)); ok(!r.isValid(),"!valid"); print(r.getMessage());
    }
    static void t_vtext4() throws Exception {
        DSASignatureModel sig=SS.signText("g",KS.toPrivateKey(KP),"SHA-256");
        try { VS.verifyText("",sig,KS.toPublicKey(KP)); fail("no throw"); } catch(IllegalArgumentException e) { print(e.getMessage()); }
    }
    static void t_vtext5() throws Exception {
        DSASignatureModel fs2=SS.signFile(TXT_FILE,KS.toPrivateKey(KP),"SHA-256");
        try { VS.verifyText("x",fs2,KS.toPublicKey(KP)); fail("no throw"); } catch(IllegalArgumentException e) { print(e.getMessage()); }
    }

    // === VERIFY FILE ===
    static void t_vfile1() throws Exception {
        DSASignatureModel sig=SS.signFile(TXT_FILE,KS.toPrivateKey(KP),"SHA-256");
        var r=VS.verifyFile(TXT_FILE,sig,KS.toPublicKey(KP));
        ok(r.isValid(),"valid"); eq(DSAVerifyService.ResultCode.SUCCESS,r.getCode(),"code"); print(r.getMessage());
    }
    static void t_vfile2() throws Exception {
        Path orig=TMP.resolve("orig.txt"); Files.writeString(orig,"Goc");
        DSASignatureModel sig=SS.signFile(orig,KS.toPrivateKey(KP),"SHA-256");
        Files.writeString(orig,"Da sua!");
        var r=VS.verifyFile(orig,sig,KS.toPublicKey(KP));
        ok(!r.isValid(),"!valid"); eq(DSAVerifyService.ResultCode.CONTENT_CHANGED,r.getCode(),"code"); print(r.getMessage());
    }
    static void t_vfile3() throws Exception {
        DSASignatureModel sig=SS.signFile(TXT_FILE,KS.toPrivateKey(KP),"SHA-256");
        DSASignatureModel fake=new DSASignatureModel(); fake.setAlgorithm(sig.getAlgorithm()); fake.setDataType(sig.getDataType());
        fake.setHashAlgorithm(sig.getHashAlgorithm()); fake.setFileHash(sig.getFileHash()); fake.setR(sig.getR()); fake.setS("999");
        ok(!VS.verifyFile(TXT_FILE,fake,KS.toPublicKey(KP)).isValid(),"!valid");
    }
    static void t_vfile4() throws Exception {
        DSASignatureModel sig=SS.signFile(TXT_FILE,KS.toPrivateKey(KP),"SHA-256");
        try { VS.verifyFile(TMP.resolve("ghost.txt"),sig,KS.toPublicKey(KP)); fail("no throw"); } catch(Exception e) { print(e.getMessage()); }
    }

    // === E2E ===
    static void t_e2e1() throws Exception {
        DSAKeyPairModel p=KS.generateParameters(new DSAKeyService.SecurityLevel(1024,160)), kp=KS.generateKeyPair(p);
        String ct="Hop dong 2024/HC-001";
        DSASignatureModel sig=SS.signText(ct,KS.toPrivateKey(kp),"SHA-256");
        ok(VS.verifyText(ct,sig,KS.toPublicKey(kp)).isValid(),"ok"); ok(!VS.verifyText(ct+"x",sig,KS.toPublicKey(kp)).isValid(),"tamper detected");
    }
    static void t_e2e2() throws Exception {
        DSAKeyPairModel p=KS.generateParameters(new DSAKeyService.SecurityLevel(1024,160)), kp=KS.generateKeyPair(p);
        Path pf=TMP.resolve("e-p.json"),bf=TMP.resolve("e-b.json"),sf=TMP.resolve("e-s.json");
        FS.writeJson(pf.toFile(),KS.toPrivateKey(kp)); FS.writeJson(bf.toFile(),KS.toPublicKey(kp));
        DSASignatureModel sig=SS.signFile(TXT_FILE,FS.readJson(pf.toFile(),DSAKeyPairModel.class),"SHA-256");
        FS.writeJson(sf.toFile(),sig);
        ok(VS.verifyFile(TXT_FILE,FS.readJson(sf.toFile(),DSASignatureModel.class),FS.readJson(bf.toFile(),DSAKeyPairModel.class)).isValid(),"valid");
    }
    static void t_e2e3() throws Exception {
        SecureRandom rnd=new SecureRandom();
        BigInteger p=new BigInteger(KP.getP()), q=new BigInteger(KP.getQ());
        BigInteger exp=p.subtract(BigInteger.ONE).divide(q), g;
        do { g=BigIntegerUtil.randomBetween(BigInteger.TWO,p.subtract(BigInteger.TWO),rnd).modPow(exp,p); } while(g.compareTo(BigInteger.ONE)<=0);
        BigInteger x=BigIntegerUtil.randomBetween(BigInteger.ONE,q.subtract(BigInteger.ONE),rnd), y=g.modPow(x,p);
        var vr=PS.validate(new DSAParameters(p.toString(),q.toString(),g.toString(),x.toString(),y.toString(),""));
        ok(vr.isValid(),"valid: "+vr.getErrors());
    }
    static void t_e2e4() throws Exception {
        for(String a:new String[]{"SHA-1","SHA-256","SHA-512"}) {
            DSASignatureModel sig=SS.signText("test",KS.toPrivateKey(KP),a);
            ok(VS.verifyText("test",sig,KS.toPublicKey(KP)).isValid(),a); print(a+": PASS");
        }
    }

    // === HELPERS ===
    static void banner(String t) { System.out.println("\nâ•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—"); System.out.println("â•‘  "+t+"  â•‘"); System.out.println("â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•"); }
    static void group(String n) { System.out.println("\nâ”€â”€â”€ "+n+" â”€â”€â”€"); }
    static void print(String m) { System.out.println("        >>> "+m); }
    static void test(String name, TestBlock b) { try { b.run(); System.out.println("  [PASS] "+name); passed++; } catch(Throwable e) { System.out.println("  [FAIL] "+name+" => "+e.getMessage()); failed++; failures.add(name); } }
    static void eq(Object e, Object a, String m) { if(!e.equals(a)) throw new AssertionError(m+": exp="+e+" got="+a); }
    static void ok(boolean c, String m) { if(!c) throw new AssertionError(m); }
    static void has(String t, String s) { if(!t.contains(s)) throw new AssertionError("'"+t+"' must contain '"+s+"'"); }
    static void fail(String m) { throw new AssertionError(m); }
    static void summary() {
        System.out.println("\nâ•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—");
        System.out.printf ("â•‘  KET QUA: %2d PASS | %2d FAIL | Tong: %2d test     â•‘%n",passed,failed,passed+failed);
        System.out.println("â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
        if(!failures.isEmpty()) { System.out.println("\nCAC TEST THAT BAI:"); failures.forEach(f->System.out.println("  [FAIL] "+f)); }
        System.exit(failed > 0 ? 1 : 0);
    }
    @FunctionalInterface interface TestBlock { void run() throws Exception; }
}