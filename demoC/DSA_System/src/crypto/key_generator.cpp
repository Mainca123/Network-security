#include "key_generator.h"

#include <iostream>

KeyGenerator::KeyGenerator()
{

}

KeyGenerator::~KeyGenerator()
{

}

BIGNUM* KeyGenerator::generatePrivateKey(const BIGNUM* q)
{
    BIGNUM* x = BN_new();

    do
    {
        // Sinh random:
        // 0 <= x < q
        BN_rand_range(x, q);

    }
    while (BN_is_zero(x));

    return x;
}

void KeyGenerator::printBigNumber(const char* name,
                                  const BIGNUM* number)
{
    std::cout << name
              << " = "
              << BN_bn2hex(number)
              << std::endl;
}


BIGNUM* KeyGenerator::generatePublicKey(
        const BIGNUM* g,
        const BIGNUM* x,
        const BIGNUM* p
        )
{
    BIGNUM* y = BN_new();

    BN_CTX* ctx = BN_CTX_new();

    // y = g^x mod p
    BN_mod_exp(y, g, x, p, ctx );
    BN_CTX_free(ctx);

    return y;
}
