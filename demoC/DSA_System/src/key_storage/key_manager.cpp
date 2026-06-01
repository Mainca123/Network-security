#include "key_manager.h"

#include <QFile>
#include <QTextStream>
#include <QString>
#include <QByteArray>
#include <QDebug>
#include <QFileInfo>
#include <QDir>

#include <iostream>
#include <cstdio>

// =========================
// SAVE PRIVATE KEY
// =========================
bool KeyManager::savePrivateKey(const BIGNUM* p, const BIGNUM* q, const BIGNUM* g, const BIGNUM* x, const std::string& path)
{
    if (!p || !q || !g || !x) {
        qDebug() << "savePrivateKey: NULL BIGNUM";
        return false;
    }

    QString qpath = QString::fromStdString(path);
    QFileInfo fileInfo(qpath);
    QDir parentDir = fileInfo.absoluteDir();
    if (!parentDir.exists() && !parentDir.mkpath(".")) {
        qDebug() << "savePrivateKey: Không tạo được thư mục cha:" << parentDir.absolutePath();
        return false;
    }

    QFile file(qpath);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Text)) {
        qDebug() << "savePrivateKey: Không mở được file:" << qpath;
        return false;
    }

    auto writeBN = [&](const char* key, const BIGNUM* bn) -> bool {
        char* hex = BN_bn2hex(bn);
        if (!hex) return false;
        QTextStream(&file) << key << "=" << hex << "\n";
        OPENSSL_free(hex);
        return true;
    };

    if (!writeBN("p", p) || !writeBN("q", q) || !writeBN("g", g) || !writeBN("x", x)) {
        qDebug() << "savePrivateKey: Ghi thất bại";
        file.close();
        return false;
    }

    file.close();
    qDebug() << "savePrivateKey: Lưu thành công" << qpath;
    return true;
}

// =========================
// SAVE PUBLIC KEY
// =========================
bool KeyManager::savePublicKey(const BIGNUM* p, const BIGNUM* q, const BIGNUM* g, const BIGNUM* y, const std::string& path)
{
    if (!p || !q || !g || !y) {
        qDebug() << "savePublicKey: NULL BIGNUM";
        return false;
    }

    QString qpath = QString::fromStdString(path);
    QFileInfo fileInfo(qpath);
    QDir parentDir = fileInfo.absoluteDir();
    if (!parentDir.exists() && !parentDir.mkpath(".")) {
        qDebug() << "savePublicKey: Không tạo được thư mục cha:" << parentDir.absolutePath();
        return false;
    }

    QFile file(qpath);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Text)) {
        qDebug() << "savePublicKey: Không mở được file:" << qpath;
        return false;
    }

    QTextStream out(&file);
    char* pHex = BN_bn2hex(p);
    char* qHex = BN_bn2hex(q);
    char* gHex = BN_bn2hex(g);
    char* yHex = BN_bn2hex(y);

    bool ok = (pHex && qHex && gHex && yHex);
    if (ok) {
        out << "p=" << pHex << "\n";
        out << "q=" << qHex << "\n";
        out << "g=" << gHex << "\n";
        out << "y=" << yHex << "\n";
    } else {
        qDebug() << "savePublicKey: BN_bn2hex thất bại";
    }

    if (pHex) OPENSSL_free(pHex);
    if (qHex) OPENSSL_free(qHex);
    if (gHex) OPENSSL_free(gHex);
    if (yHex) OPENSSL_free(yHex);

    file.close();
    if (ok) {
        qDebug() << "savePublicKey: Lưu thành công" << qpath;
        return true;
    }
    return false;
}

// =========================
// LOAD SINGLE PARAM
// =========================
bool KeyManager::loadKeyParam(const std::string& path, const std::string& keyName, BIGNUM** outBN)
{
    QFile file(QString::fromStdString(path));
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        qDebug() << "loadKeyParam: Không mở được file" << path;
        return false;
    }

    QTextStream in(&file);
    QString prefix = QString::fromStdString(keyName) + "=";
    while (!in.atEnd()) {
        QString line = in.readLine().trimmed();
        if (line.startsWith(prefix)) {
            QString hexValue = line.mid(prefix.length());
            QByteArray hexBytes = hexValue.toUtf8();
            if (BN_hex2bn(outBN, hexBytes.constData()) == 0) {
                qDebug() << "loadKeyParam: BN_hex2bn thất bại cho" << keyName;
                file.close();
                return false;
            }
            file.close();
            return true;
        }
    }
    file.close();
    qDebug() << "loadKeyParam: Không tìm thấy key" << keyName << "trong" << path;
    return false;
}

// =========================
// LOAD PRIVATE KEY
// =========================
bool KeyManager::loadPrivateKey(const std::string& path, PrivateKeyData& key)
{
    key.p = nullptr;
    key.q = nullptr;
    key.g = nullptr;
    key.x = nullptr;

    bool pOk = loadKeyParam(path, "p", &key.p);
    bool qOk = loadKeyParam(path, "q", &key.q);
    bool gOk = loadKeyParam(path, "g", &key.g);
    bool xOk = loadKeyParam(path, "x", &key.x);

    if (!(pOk && qOk && gOk && xOk)) {
        freePrivateKey(key);
        return false;
    }
    return true;
}

// =========================
// LOAD PUBLIC KEY
// =========================
bool KeyManager::loadPublicKey(const std::string& path, PublicKeyData& key)
{
    key.p = nullptr;
    key.q = nullptr;
    key.g = nullptr;
    key.y = nullptr;

    bool pOk = loadKeyParam(path, "p", &key.p);
    bool qOk = loadKeyParam(path, "q", &key.q);
    bool gOk = loadKeyParam(path, "g", &key.g);
    bool yOk = loadKeyParam(path, "y", &key.y);

    if (!(pOk && qOk && gOk && yOk)) {
        freePublicKey(key);
        return false;
    }
    return true;
}

// =========================
// FREE FUNCTIONS
// =========================
void KeyManager::freePrivateKey(PrivateKeyData& key)
{
    if (key.p) { BN_free(key.p); key.p = nullptr; }
    if (key.q) { BN_free(key.q); key.q = nullptr; }
    if (key.g) { BN_free(key.g); key.g = nullptr; }
    if (key.x) { BN_free(key.x); key.x = nullptr; }
}

void KeyManager::freePublicKey(PublicKeyData& key)
{
    if (key.p) { BN_free(key.p); key.p = nullptr; }
    if (key.q) { BN_free(key.q); key.q = nullptr; }
    if (key.g) { BN_free(key.g); key.g = nullptr; }
    if (key.y) { BN_free(key.y); key.y = nullptr; }
}