#ifndef DSA_HANDMADE_H
#define DSA_HANDMADE_H
#include <climits>
#include <QString>

class DSA_Handmade
{
public:
    static constexpr long long MIN_P = 23;
    static constexpr long long MIN_Q = 11;
    static constexpr long long MIN_G = 2;
    static constexpr long long MIN_X = 1;

    static constexpr long long MAX_VALUE = 1000000000LL;

    //========================
    // Kiểm tra dữ liệu đầu vào
    //========================
    static bool isNumber(const QString& text);

    static bool parseValue(
        const QString& text,
        long long& value,
        QString& error);

    //========================
    // Toán học cơ bản
    //========================
    static bool isPrime(long long n);

    static long long gcd(
        long long a,
        long long b);

    static long long modPow(
        long long base,
        long long exp,
        long long mod);

    static long long modInverse(
        long long a,
        long long mod);

    //========================
    // Kiểm tra tham số DSA
    //========================
    static bool validateP(
        long long p,
        QString& error);

    static bool validateQ(
        long long p,
        long long q,
        QString& error);

    static bool validateG(
        long long p,
        long long q,
        long long g,
        QString& error);

    static bool validateX(
        long long q,
        long long x,
        QString& error);

    static bool validateAll(
        long long p,
        long long q,
        long long g,
        long long x,
        QString& error);

    //========================
    // Sinh khóa
    //========================
    static long long generatePublicKey(
        long long p,
        long long g,
        long long x);
    static long long simpleHash(
        const QString& message);

    static bool generateSignature(
        const QString& message,
        long long p,
        long long q,
        long long g,
        long long x,
        long long& r,
        long long& s);

    static bool verifySignature(
        const QString& message,
        long long p,
        long long q,
        long long g,
        long long y,
        long long r,
        long long s);

};

#endif