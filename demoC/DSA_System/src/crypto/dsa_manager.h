#ifndef DSA_MANAGER_H
#define DSA_MANAGER_H

#include <QString>
#include <openssl/dsa.h>
#include <openssl/bn.h>

class DSAManager
{
public:
    DSAManager();
    ~DSAManager();

    // Các hàm cấu hình tham số mặc định của bạn
    bool generateParameters(int bits = 2048);
    void printParameters();

    // Các hàm Getter lấy các số nguyên lớn
    const BIGNUM* getQ() const;
    const BIGNUM* getP() const;
    const BIGNUM* getG() const;

    /**
     * @brief Tự ký số thủ công bằng công thức toán học DSA
     * @param filePath Đường dẫn file cần ký
     * @param p Tham số số nguyên tố lớn p
     * @param q Tham số số nguyên tố q (ước số của p-1)
     * @param g Tham số phần tử sinh g
     * @param x Khóa bí mật (Private Key)
     * @param res_r Con trỏ nhận giá trị chữ ký r (Output)
     * @param res_s Con trỏ nhận giá trị chữ ký s (Output)
     * @return true nếu ký thành công, false nếu thất bại
     */
    bool signFileCalculated(const QString &filePath,
                            const BIGNUM *p, const BIGNUM *q, const BIGNUM *g, const BIGNUM *x,
                            BIGNUM *res_r, BIGNUM *res_s);
    bool verifyFileCalculated(
        const QString& filePath,
        const BIGNUM* p,
        const BIGNUM* q,
        const BIGNUM* g,
        const BIGNUM* y,
        const BIGNUM* r,
        const BIGNUM* s
        );

private:
    DSA* dsa; // Đối tượng DSA của OpenSSL dùng cho các hàm quản lý tham số hệ thống
};

#endif // DSA_MANAGER_H