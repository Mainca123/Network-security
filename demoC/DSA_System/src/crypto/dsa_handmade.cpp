#include "dsa_handmade.h"
#include <QRandomGenerator>
#include <QDebug>

bool DSA_Handmade::isNumber(const QString& text)
{
    if(text.trimmed().isEmpty())
        return false;

    for(QChar c : text)
    {
        if(!c.isDigit())
            return false;
    }

    return true;
}

bool DSA_Handmade::parseValue(
    const QString& text,
    long long& value,
    QString& error)
{
    if(text.trimmed().isEmpty())
    {
        error = "Không được để trống.";
        return false;
    }

    if(!isNumber(text))
    {
        error = "Chỉ được nhập số nguyên dương.";
        return false;
    }

    bool ok;

    value = text.toLongLong(&ok);

    if(!ok)
    {
        error = "Không thể chuyển đổi dữ liệu.";
        return false;
    }

    if(value <= 0)
    {
        error = "Giá trị phải lớn hơn 0.";
        return false;
    }

    if(value > MAX_VALUE)
    {
        error = QString(
                    "Giá trị vượt quá giới hạn (%1).")
                    .arg(MAX_VALUE);

        return false;
    }

    return true;
}

bool DSA_Handmade::isPrime(long long n)
{
    if(n < 2)
        return false;

    if(n == 2)
        return true;

    if(n % 2 == 0)
        return false;

    for(long long i = 3;
         i * i <= n;
         i += 2)
    {
        if(n % i == 0)
            return false;
    }

    return true;
}

long long DSA_Handmade::gcd(
    long long a,
    long long b)
{
    while(b != 0)
    {
        long long t = b;
        b = a % b;
        a = t;
    }

    return a;
}

long long DSA_Handmade::modPow(
    long long base,
    long long exp,
    long long mod)
{
    long long result = 1;

    base %= mod;

    while(exp > 0)
    {
        if(exp & 1)
        {
            result =
                (result * base) % mod;
        }

        base =
            (base * base) % mod;

        exp >>= 1;
    }

    return result;
}

long long DSA_Handmade::modInverse(
    long long a,
    long long mod)
{
    long long m0 = mod;
    long long y = 0;
    long long x = 1;

    if(mod == 1)
        return 0;

    while(a > 1)
    {
        long long q = a / mod;

        long long t = mod;

        mod = a % mod;
        a = t;

        t = y;

        y = x - q * y;
        x = t;
    }

    if(x < 0)
        x += m0;

    return x;
}

bool DSA_Handmade::validateP(
    long long p,
    QString& error)
{
    if(p < MIN_P)
    {
        error = QString(
                    "P phải >= %1")
                    .arg(MIN_P);
        return false;
    }

    if(p > MAX_VALUE)
    {
        error = QString(
                    "P phải <= %1")
                    .arg(MAX_VALUE);
        return false;
    }

    if(!isPrime(p))
    {
        error =
            "P phải là số nguyên tố.";
        return false;
    }

    return true;
}

bool DSA_Handmade::validateQ(
    long long p,
    long long q,
    QString& error)
{
    if(q < MIN_Q)
    {
        error = QString(
                    "Q phải >= %1")
                    .arg(MIN_Q);
        return false;
    }

    if(q > MAX_VALUE)
    {
        error = QString(
                    "Q phải <= %1")
                    .arg(MAX_VALUE);
        return false;
    }

    if(!isPrime(q))
    {
        error =
            "Q phải là số nguyên tố.";
        return false;
    }

    if((p - 1) % q != 0)
    {
        error =
            "Q phải là ước của (P - 1).";
        return false;
    }

    return true;
}

bool DSA_Handmade::validateG(
    long long p,
    long long q,
    long long g,
    QString& error)
{
    if(g < MIN_G)
    {
        error = QString(
                    "G phải >= %1")
                    .arg(MIN_G);
        return false;
    }

    if(g > MAX_VALUE)
    {
        error = QString(
                    "G phải <= %1")
                    .arg(MAX_VALUE);
        return false;
    }

    if(g >= p)
    {
        error =
            "G phải nhỏ hơn P.";
        return false;
    }

    if(modPow(g,q,p) != 1)
    {
        error =
            "G^Q mod P phải bằng 1.";
        return false;
    }

    return true;
}

bool DSA_Handmade::validateX(
    long long q,
    long long x,
    QString& error)
{
    if(x < MIN_X)
    {
        error =
            "X phải lớn hơn 0.";
        return false;
    }

    if(x > MAX_VALUE)
    {
        error = QString(
                    "X phải <= %1")
                    .arg(MAX_VALUE);
        return false;
    }

    if(x >= q)
    {
        error =
            "X phải nhỏ hơn Q.";
        return false;
    }

    return true;
}

bool DSA_Handmade::validateAll(
    long long p,
    long long q,
    long long g,
    long long x,
    QString& error)
{
    if(!validateP(p,error))
        return false;

    if(!validateQ(p,q,error))
        return false;

    if(!validateG(p,q,g,error))
        return false;

    if(!validateX(q,x,error))
        return false;

    return true;
}

long long DSA_Handmade::generatePublicKey(
    long long p,
    long long g,
    long long x)
{
    return modPow(g,x,p);
}

long long DSA_Handmade::simpleHash(
    const QString& message)
{
    long long hash = 0;

    for(QChar c : message)
    {
        hash =
            (hash * 31 + c.unicode())
            % MAX_VALUE;
    }

    return hash;
}


bool DSA_Handmade::generateSignature(
    const QString& message,
    long long p,
    long long q,
    long long g,
    long long x,
    long long& r,
    long long& s)
{
    long long hash = simpleHash(message) % q;

    for(int attempt = 0; attempt < 100; ++attempt)
    {
        long long k;

        do
        {
            k = QRandomGenerator::global()->bounded(1LL, q);
        }
        while(gcd(k, q) != 1);

        r = modPow(g, k, p) % q;

        if(r == 0)
            continue;

        long long kInv = modInverse(k, q);

        if(kInv <= 0)
            continue;

        s = (kInv * ((hash + x * r) % q)) % q;

        if(s == 0)
            continue;

        qDebug() << "hash =" << hash;
        qDebug() << "k =" << k;
        qDebug() << "r =" << r;
        qDebug() << "kInv =" << kInv;
        qDebug() << "s =" << s;
        return true;
    }

    qDebug() << "s =" << s;
    qDebug() << "r =" << r;
    return false;
}

bool DSA_Handmade::verifySignature(
    const QString& message,
    long long p,
    long long q,
    long long g,
    long long y,
    long long r,
    long long s)
{
    if(r <= 0 || r >= q)
        return false;

    if(s <= 0 || s >= q)
        return false;

    long long hash =
        simpleHash(message) % q;

    long long w =
        modInverse(s,q);

    if(w <= 0)
        return false;

    long long u1 =
        (hash * w) % q;

    long long u2 =
        (r * w) % q;

    long long gu1 =
        modPow(g,u1,p);

    long long yu2 =
        modPow(y,u2,p);

    long long v =
        ((gu1 * yu2) % p) % q;

    qDebug() << "MESSAGE =" << message;
    qDebug() << "HASH =" << hash;

    qDebug() << "R =" << r;
    qDebug() << "S =" << s;

    qDebug() << "W =" << w;
    qDebug() << "U1 =" << u1;
    qDebug() << "U2 =" << u2;

    qDebug() << "GU1 =" << gu1;
    qDebug() << "YU2 =" << yu2;

    qDebug() << "V =" << v;

    return v == r;
}