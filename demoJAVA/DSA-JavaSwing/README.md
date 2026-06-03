# DSA Digital Signature System

Ứng dụng desktop Java Swing dùng để ký số và xác thực tính toàn vẹn dữ liệu bằng thuật toán DSA. Phần lõi DSA được tự cài đặt bằng `BigInteger`, chỉ dùng thư viện chuẩn Java cho sinh số ngẫu nhiên, kiểm tra nguyên tố xác suất và SHA-256.

## Cấu trúc source

```text
src/
└── main/
    ├── Main.java
    ├── dsa/
    │   ├── DSAParameter.java
    │   ├── DSAKeyPair.java
    │   ├── DSASignature.java
    │   └── DSAService.java
    ├── ui/
    │   ├── MainFrame.java
    │   ├── HomePanel.java
    │   ├── ParameterPanel.java
    │   ├── KeyPanel.java
    │   ├── SignPanel.java
    │   ├── VerifyPanel.java
    │   ├── GuidePanel.java
    │   ├── Theme.java
    │   ├── UIFactory.java
    │   └── components/
    └── util/
        ├── FileUtil.java
        ├── HashUtil.java
        └── KeyUtil.java
```

## Chạy bằng terminal

Yêu cầu Java 17 trở lên.

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse -Filter *.java -Path src | ForEach-Object { $_.FullName })
java -cp bin main.Main
```

## Tạo project trong IntelliJ IDEA

1. Chọn `File > New > Project from Existing Sources`.
2. Trỏ đến thư mục project này.
3. Chọn JDK 17 hoặc mới hơn.
4. Đánh dấu thư mục `src` là `Sources Root` nếu IDE chưa tự nhận.
5. Chạy class `main.Main`.

## Tạo project trong NetBeans

1. Tạo project Java Application mới, không cần Maven.
2. Copy thư mục `src/main` vào thư mục source của project.
3. Chọn JDK 17 hoặc mới hơn.
4. Chạy class `main.Main`.

## Luồng sử dụng

1. Vào `Sinh tham số DSA`, chọn kích thước `p` và `q`, nhấn `Sinh tham số`.
2. Vào `Sinh khóa`, nhấn `Sinh khóa`, sau đó lưu `private.key` và `public.key`.
3. Vào `Ký file`, chọn file bất kỳ, nhấn `Ký file`, rồi lưu chữ ký `.sig`.
4. Vào `Xác thực chữ ký`, chọn file gốc, file `.sig`, file `public.key`, nhấn `Xác thực chữ ký`.

## Tóm tắt thuật toán DSA

Ứng dụng sinh `q` là số nguyên tố, sinh `p` sao cho `q` chia hết `p - 1`, sau đó tính phần tử sinh `g = h^((p-1)/q) mod p`. Khóa bí mật là `x` với `0 < x < q`; khóa công khai là `y = g^x mod p`.

Khi ký file, chương trình đọc toàn bộ byte, băm bằng SHA-256, chọn số ngẫu nhiên bí mật `k`, rồi tính:

```text
r = (g^k mod p) mod q
s = k^-1 * (H(m) + x*r) mod q
```

Khi xác thực, chương trình đọc lại file và chữ ký, kiểm tra `0 < r,s < q`, rồi tính:

```text
w = s^-1 mod q
u1 = H(m) * w mod q
u2 = r * w mod q
v = ((g^u1 * y^u2) mod p) mod q
```

Nếu `v == r`, kết luận chữ ký hợp lệ và file chưa bị thay đổi.

## Kiểm thử gợi ý

- Ký và xác thực file `.txt`: kết quả phải hợp lệ.
- Ký và xác thực file ảnh `.jpg` hoặc `.png`: kết quả phải hợp lệ.
- Ký và xác thực file `.pdf`: kết quả phải hợp lệ.
- Ký và xác thực file nén `.zip` hoặc `.rar`: kết quả phải hợp lệ.
- Sửa nội dung file sau khi ký rồi xác thực lại: kết quả phải không hợp lệ.
- Sửa file `.sig`, ví dụ thay một chữ số trong `r` hoặc `s`: kết quả phải là chữ ký giả mạo hoặc không hợp lệ.
- Dùng sai `public.key` để xác thực: kết quả phải thất bại.

## Gợi ý ảnh chụp màn hình cho báo cáo

- Dashboard `DSA Digital Signature System`.
- Màn sinh tham số hiển thị `p`, `q`, `g`.
- Màn sinh khóa hiển thị `x`, `y`.
- Màn ký file hiển thị hash SHA-256, `r`, `s` và trạng thái ký thành công.
- Màn xác thực thành công với thông báo màu xanh.
- Màn xác thực thất bại sau khi sửa file hoặc sửa chữ ký.

## Định dạng file

`private.key`

```text
p=<giá trị p>
q=<giá trị q>
g=<giá trị g>
x=<giá trị x>
```

`public.key`

```text
p=<giá trị p>
q=<giá trị q>
g=<giá trị g>
y=<giá trị y>
```

`*.sig`

```text
r=<giá trị r>
s=<giá trị s>
```
