#ifndef HASH_UTILS_H
#define HASH_UTILS_H

#include <QString>

class HashUtils {
public:
    // Hàm băm file bằng SHA-256, trả về chuỗi Hex (64 ký tự)
    static QString hashFileSHA256(const QString &filePath);
};

#endif // HASH_UTILS_H