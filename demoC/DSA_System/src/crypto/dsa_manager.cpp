#include "dsa_manager.h"
#include <openssl/bn.h>
#include <iostream>
#include "dsa_manager.h"
#include <QFile>
#include <QByteArray>
#include <openssl/bn.h>
#include <openssl/sha.h>
#include <openssl/bn.h>
#include <openssl/sha.h>
#include <fstream>

#include <QDebug>
#include <QFile>



    DSAManager::DSAManager()
{
    dsa = DSA_new();
}

DSAManager::~DSAManager()
{
    if (dsa)
    {
        DSA_free(dsa);
    }
}

bool DSAManager::generateParameters(int bits)
{
    return DSA_generate_parameters_ex(
        dsa,
        bits,
        nullptr,
        0,
        nullptr,
        nullptr,
        nullptr
        );
}

void DSAManager::printParameters()
{
    const BIGNUM* p;
    const BIGNUM* q;
    const BIGNUM* g;

    DSA_get0_pqg(dsa, &p, &q, &g);

    std::cout << "===== DSA PARAMETERS =====" << std::endl;

    std::cout << "p = "
              << BN_bn2hex(p)
              << std::endl;

    std::cout << "q = "
              << BN_bn2hex(q)
              << std::endl;

    std::cout << "g = "
              << BN_bn2hex(g)
              << std::endl;
}

const BIGNUM* DSAManager::getQ() const
{
    const BIGNUM* p;
    const BIGNUM* q;
    const BIGNUM* g;

    DSA_get0_pqg(dsa, &p, &q, &g);

    return q;
}


const BIGNUM* DSAManager::getP() const
{
    const BIGNUM* p;
    const BIGNUM* q;
    const BIGNUM* g;

    DSA_get0_pqg(dsa, &p, &q, &g);

    return p;
}

const BIGNUM* DSAManager::getG() const
{
    const BIGNUM* p;
    const BIGNUM* q;
    const BIGNUM* g;

    DSA_get0_pqg(dsa, &p, &q, &g);

    return g;
}


// Hàm tự code thuật toán ký DSA từ các tham số thô
bool DSAManager::signFileCalculated(const QString &filePath,
                                    const BIGNUM *p, const BIGNUM *q, const BIGNUM *g, const BIGNUM *x,
                                    BIGNUM *res_r, BIGNUM *res_s)
{
    if (!p || !q || !g || !x || res_r == nullptr || res_s == nullptr) return false;

    // -----------------------------------------------------------------
    // Bước 1: Tính mã băm SHA-256 của File
    // -----------------------------------------------------------------
    QFile file(filePath);
    if (!file.open(QIODevice::ReadOnly)) return false;

    SHA256_CTX sha256Context;
    SHA256_Init(&sha256Context);

    char buffer[4096];
    while (!file.atEnd()) {
        qint64 bytesRead = file.read(buffer, sizeof(buffer));
        if (bytesRead > 0) {
            SHA256_Update(&sha256Context, buffer, bytesRead);
        }
    }
    file.close();

    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256_Final(hash, &sha256Context);

    // Chuyển mảng byte hash sang số nguyên lớn BIGNUM (gọi là H)
    BIGNUM *H = BN_new();
    BN_bin2bn(hash, SHA256_DIGEST_LENGTH, H);

    // -----------------------------------------------------------------
    // Bước 2: Thực hiện thuật toán ký toán học DSA
    // -----------------------------------------------------------------
    BN_CTX *ctx = BN_CTX_new();
    BIGNUM *k = BN_new();
    BIGNUM *k_inv = BN_new();
    BIGNUM *tmp = BN_new();
    BIGNUM *xr = BN_new();

    bool success = false;

    while (!success) {
        // 1. Sinh số ngẫu nhiên k sao cho 0 < k < q
        do {
            BN_rand_range(k, q);
        } while (BN_is_zero(k)); // Đảm bảo k != 0

        // 2. Tính r = (g^k mod p) mod q
        BN_mod_exp(tmp, g, k, p, ctx); // tmp = g^k mod p
        BN_mod(res_r, tmp, q, ctx);     // res_r = tmp mod q

        if (BN_is_zero(res_r)) continue; // Nếu r = 0 thì chọn lại k

        // 3. Tính k_inv = k^-1 mod q (Nghịch đảo modulo)
        if (!BN_mod_inverse(k_inv, k, q, ctx)) {
            continue; // Nếu không tìm được nghịch đảo, chọn lại k
        }

        // 4. Tính s = k^-1 * (H + x * r) mod q
        BN_mod_mul(xr, x, res_r, q, ctx); // xr = (x * r) mod q
        BN_mod_add(tmp, H, xr, q, ctx);  // tmp = (H + xr) mod q
        BN_mod_mul(res_s, k_inv, tmp, q, ctx); // res_s = (k_inv * tmp) mod q

        if (BN_is_zero(res_s)) continue; // Nếu s = 0 thì chọn lại k

        success = true; // Ký thành công!
    }

    // Giải phóng bộ nhớ tạm
    BN_free(H);
    BN_free(k);
    BN_free(k_inv);
    BN_free(tmp);
    BN_free(xr);
    BN_CTX_free(ctx);

    return true;
}

bool DSAManager::verifyFileCalculated(
    const QString& filePath,
    const BIGNUM* p,
    const BIGNUM* q,
    const BIGNUM* g,
    const BIGNUM* y,
    const BIGNUM* r,
    const BIGNUM* s
    )
{
    // =========================
    // 1. Check r,s hợp lệ
    // =========================

    if(BN_is_zero(r) || BN_is_negative(r))
        return false;

    if(BN_is_zero(s) || BN_is_negative(s))
        return false;

    if(BN_cmp(r, q) >= 0)
        return false;

    if(BN_cmp(s, q) >= 0)
        return false;

    // =========================
    // 2. Đọc file
    // =========================

    QFile file(filePath);

    if(!file.open(QIODevice::ReadOnly))
        return false;

    QByteArray data = file.readAll();

    file.close();

    // =========================
    // 3. Hash SHA1
    // =========================

    unsigned char hash[SHA256_DIGEST_LENGTH];

    SHA256(
        reinterpret_cast<const unsigned char*>(data.constData()),
        data.size(),
        hash
        );

    // =========================
    // 4. Tạo context
    // =========================

    BN_CTX* ctx = BN_CTX_new();

    if(!ctx)
        return false;

    // =========================
    // 5. HASH -> BIGNUM
    // =========================

    BIGNUM* hm =
        BN_bin2bn(
            hash,
            SHA256_DIGEST_LENGTH,
            nullptr
            );

    if(!hm)
    {
        BN_CTX_free(ctx);
        return false;
    }

    // =========================
    // 6. hm = hm mod q
    // =========================

    BIGNUM* hm_mod = BN_new();

    BN_mod(
        hm_mod,
        hm,
        q,
        ctx
        );

    // =========================
    // 7. w = s^-1 mod q
    // =========================

    BIGNUM* w =
        BN_mod_inverse(
            nullptr,
            s,
            q,
            ctx
            );

    if(!w)
    {
        BN_free(hm);
        BN_free(hm_mod);

        BN_CTX_free(ctx);

        return false;
    }

    // =========================
    // 8. u1 = (hm * w) mod q
    // =========================

    BIGNUM* u1 = BN_new();

    BN_mod_mul(
        u1,
        hm_mod,
        w,
        q,
        ctx
        );

    // =========================
    // 9. u2 = (r * w) mod q
    // =========================

    BIGNUM* u2 = BN_new();

    BN_mod_mul(
        u2,
        r,
        w,
        q,
        ctx
        );

    // =========================
    // 10. gu1 = g^u1 mod p
    // =========================

    BIGNUM* gu1 = BN_new();

    BN_mod_exp(
        gu1,
        g,
        u1,
        p,
        ctx
        );

    // =========================
    // 11. yu2 = y^u2 mod p
    // =========================

    BIGNUM* yu2 = BN_new();

    BN_mod_exp(
        yu2,
        y,
        u2,
        p,
        ctx
        );

    // =========================
    // 12. temp = (gu1 * yu2) mod p
    // =========================

    BIGNUM* temp = BN_new();

    BN_mod_mul(
        temp,
        gu1,
        yu2,
        p,
        ctx
        );

    // =========================
    // 13. v = temp mod q
    // =========================

    BIGNUM* v = BN_new();

    BN_mod(
        v,
        temp,
        q,
        ctx
        );

    // =========================
    // 14. Compare v == r
    // =========================

    bool verified =
        (BN_cmp(v, r) == 0);

    // =========================
    // DEBUG
    // =========================

    char* vHex = BN_bn2hex(v);
    char* rHex = BN_bn2hex(r);

    qDebug() << "VERIFY RESULT =" << verified;
    qDebug() << "v =" << vHex;
    qDebug() << "r =" << rHex;

    OPENSSL_free(vHex);
    OPENSSL_free(rHex);

    // =========================
    // 15. Free memory
    // =========================

    BN_free(hm);
    BN_free(hm_mod);

    BN_free(w);

    BN_free(u1);
    BN_free(u2);

    BN_free(gu1);
    BN_free(yu2);

    BN_free(temp);
    BN_free(v);

    BN_CTX_free(ctx);

    return verified;
}