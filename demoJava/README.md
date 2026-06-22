# Hệ thống DSA

Ứng dụng desktop Java Core + JavaFX để tạo tham số DSA, sinh khóa, ký văn bản, ký tệp, xác thực chữ ký và lưu nhật ký hệ thống.

## Chạy dự án

Yêu cầu Java 17+ và Maven.

```bash
mvn clean javafx:run
```

Biên dịch kiểm tra:

```bash
mvn clean package
```

Nhật ký hệ thống được lưu tại thư mục người dùng: `~/.system-dsa/system-logs.json`.

## Demo nhanh

1. Mở `Tạo khóa tự động`, chọn `L = 1024, N = 160` và `SHA-256`.
2. Bấm `Tạo tham số`, sau đó bấm `Tạo khóa`.
3. Lưu khóa bí mật và khóa công khai ra hai tệp JSON riêng.
4. Mở `Tạo chữ ký`, tab `Ký văn bản`, nhập `Hello DSA`.
5. Chọn khóa bí mật, bấm `Ký văn bản`, sau đó `Lưu chữ ký`.
6. Mở `Xác thực chữ ký`, tab `Xác thực văn bản`.
7. Nhập lại `Hello DSA`, chọn tệp chữ ký và khóa công khai, bấm `Xác thực`.
8. Sửa nội dung thành `Hello DSA!` rồi xác thực lại để thấy kết quả thất bại.

Thuật toán ký và xác thực DSA được tự triển khai bằng `BigInteger`, `SecureRandom`, `MessageDigest`, `modPow()` và `modInverse()`, không dùng `java.security.Signature`.
