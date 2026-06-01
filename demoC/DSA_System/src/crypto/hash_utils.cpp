#include "hash_utils.h"
#include <QFile>
#include <QByteArray>
#include <openssl/evp.h>

QString HashUtils::hashFileSHA256(const QString &filePath) {
    QFile file(filePath);
    if (!file.open(QIODevice::ReadOnly)) {
        return QString(); // Trả về chuỗi rỗng nếu không mở được file
    }

    // 1. Khởi tạo cấu trúc ngữ cảnh băm (Digest Context) của OpenSSL
    EVP_MD_CTX* mdctx = EVP_MD_CTX_new();
    if (mdctx == nullptr) {
        file.close();
        return QString();
    }

    // 2. Chỉ định thuật toán băm là SHA-256
    if (EVP_DigestInit_ex(mdctx, EVP_sha256(), nullptr) != 1) {
        EVP_MD_CTX_free(mdctx);
        file.close();
        return QString();
    }

    // 3. Đọc file theo từng block 4KB và cập nhật vào OpenSSL băm liên tục
    char buffer[4096];
    while (!file.atEnd()) {
        qint64 bytesRead = file.read(buffer, sizeof(buffer));
        if (bytesRead > 0) {
            // Cập nhật khối dữ liệu vừa đọc vào hàm băm
            EVP_DigestUpdate(mdctx, buffer, bytesRead);
        }
    }
    file.close(); // Đóng file sau khi đọc xong

    // 4. Hoàn thành quá trình băm và lấy mảng byte kết quả
    unsigned char hashResult[EVP_MAX_MD_SIZE];
    unsigned int hashLen = 0;

    if (EVP_DigestFinal_ex(mdctx, hashResult, &hashLen) != 1) {
        EVP_MD_CTX_free(mdctx);
        return QString();
    }

    // 5. Giải phóng vùng nhớ ngữ cảnh OpenSSL
    EVP_MD_CTX_free(mdctx);

    // 6. Chuyển mảng byte thô sang chuỗi Hex (hệ 16) để hiển thị lên UI hoặc xử lý tiếp
    QByteArray hashByteArray(reinterpret_cast<char*>(hashResult), hashLen);
    return hashByteArray.toHex();
}