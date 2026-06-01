#ifndef KEY_GENERATOR_H
#define KEY_GENERATOR_H

#include <openssl/bn.h>

    class KeyGenerator
{
public:

    KeyGenerator();

    ~KeyGenerator();

    // Sinh private key x
    BIGNUM* generatePrivateKey(const BIGNUM* q);

    // In BIGNUM
    void printBigNumber(const char* name,
                        const BIGNUM* number);

    BIGNUM* generatePublicKey(
        const BIGNUM* g,
        const BIGNUM* x,
        const BIGNUM* p
        );

private:

};

#endif
