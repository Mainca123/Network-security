#ifndef KEY_MANAGER_H
#define KEY_MANAGER_H

#include <string>
#include <openssl/bn.h>

struct PrivateKeyData {
    BIGNUM* p;
    BIGNUM* q;
    BIGNUM* g;
    BIGNUM* x;
};

struct PublicKeyData {
    BIGNUM* p;
    BIGNUM* q;
    BIGNUM* g;
    BIGNUM* y;
};

class KeyManager {
public:
    static bool savePrivateKey(const BIGNUM* p, const BIGNUM* q, const BIGNUM* g, const BIGNUM* x, const std::string& path);
    static bool savePublicKey(const BIGNUM* p, const BIGNUM* q, const BIGNUM* g, const BIGNUM* y, const std::string& path);
    static bool loadKeyParam(const std::string& path, const std::string& keyName, BIGNUM** outBN);
    static bool loadPrivateKey(const std::string& path, PrivateKeyData& key);
    static bool loadPublicKey(const std::string& path, PublicKeyData& key);
    static void freePrivateKey(PrivateKeyData& key);
    static void freePublicKey(PublicKeyData& key);
};

#endif // KEY_MANAGER_H