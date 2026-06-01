#include "file_signer.h"

#include "../crypto/dsa_manager.h"
#include "../key_storage/key_manager.h"

#include <QFile>
#include <QTextStream>

bool FileSigner::signFile(
    const QString& filePath,
    const QString& privateKeyPath,
    const QString& signaturePath
    )
{
    // check file
    if(!QFile::exists(filePath))
        return false;

    if(!QFile::exists(privateKeyPath))
        return false;

    // load key
    PrivateKeyData key;

    bool loaded =
        KeyManager::loadPrivateKey(
            privateKeyPath.toStdString(),
            key
            );

    if(!loaded)
        return false;

    // sign
    DSAManager dsa;

    BIGNUM* r = BN_new();
    BIGNUM* s = BN_new();

    bool signedOk =
        dsa.signFileCalculated(
            filePath,
            key.p,
            key.q,
            key.g,
            key.x,
            r,
            s
            );

    if(!signedOk)
    {
        BN_free(r);
        BN_free(s);

        BN_free(key.p);
        BN_free(key.q);
        BN_free(key.g);
        BN_free(key.x);

        return false;
    }

    // convert hex
    char* rHex = BN_bn2hex(r);
    char* sHex = BN_bn2hex(s);

    QString content =
        QString("r=%1\ns=%2")
            .arg(rHex)
            .arg(sHex);

    OPENSSL_free(rHex);
    OPENSSL_free(sHex);

    // save sig
    QFile sigFile(signaturePath);

    bool saved = false;

    if(sigFile.open(QIODevice::WriteOnly | QIODevice::Text))
    {
        QTextStream out(&sigFile);

        out << content;

        sigFile.close();

        saved = true;
    }

    // free
    BN_free(r);
    BN_free(s);

    BN_free(key.p);
    BN_free(key.q);
    BN_free(key.g);
    BN_free(key.x);

    return saved;
}

bool FileSigner::verifyFile(
    const QString& filePath,
    const QString& publicKeyPath,
    const QString& signaturePath
    )
{
    // =========================
    // 1. Check file tồn tại
    // =========================

    if(!QFile::exists(filePath))
        return false;

    if(!QFile::exists(publicKeyPath))
        return false;

    if(!QFile::exists(signaturePath))
        return false;

    // =========================
    // 2. Load public key
    // =========================

    PublicKeyData key;

    bool loaded =
        KeyManager::loadPublicKey(
            publicKeyPath.toStdString(),
            key
            );

    if(!loaded)
        return false;

    // =========================
    // 3. Đọc file chữ ký
    // =========================

    QFile sigFile(signaturePath);

    if(!sigFile.open(QIODevice::ReadOnly | QIODevice::Text))
        return false;

    QTextStream in(&sigFile);

    QString rLine = in.readLine();
    QString sLine = in.readLine();

    sigFile.close();

    QString rHex =
        rLine.section("=",1,1).trimmed();

    QString sHex =
        sLine.section("=",1,1).trimmed();

    // =========================
    // 4. Convert HEX -> BIGNUM
    // =========================

    BIGNUM* r = nullptr;
    BIGNUM* s = nullptr;

    BN_hex2bn(&r, rHex.toStdString().c_str());
    BN_hex2bn(&s, sHex.toStdString().c_str());

    // =========================
    // 5. Verify
    // =========================

    DSAManager dsa;

    bool verified =
        dsa.verifyFileCalculated(
            filePath,
            key.p,
            key.q,
            key.g,
            key.y,
            r,
            s
            );

    // =========================
    // 6. Free memory
    // =========================

    BN_free(key.p);
    BN_free(key.q);
    BN_free(key.g);
    BN_free(key.y);

    BN_free(r);
    BN_free(s);

    return verified;
}