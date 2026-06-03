# Tài liệu giải thích logic source code DSA

Tài liệu này giải thích toàn bộ luồng logic chính trong thư mục `src`, tập trung vào phần lõi thuật toán DSA, cách giao diện Swing gọi vào service, cách đọc/ghi file khóa và chữ ký.

## 1. Tổng quan kiến trúc

Ứng dụng là desktop app Java Swing. Mã nguồn được chia thành 4 nhóm chính:

```text
src/main/
├── Main.java                  Điểm chạy chương trình
├── dsa/                       Lõi thuật toán DSA
├── util/                      Tiện ích hash, đọc/ghi file, đọc/ghi khóa
└── ui/                        Giao diện Swing và điều phối thao tác người dùng
```

Vai trò từng nhóm:

- `main.dsa`: chứa logic thuật toán DSA thuần túy, không phụ thuộc giao diện.
- `main.util`: chứa các hàm tiện ích cho file, SHA-256, private key, public key và chữ ký.
- `main.ui`: hiển thị màn hình, nhận thao tác người dùng và gọi các lớp xử lý.
- `MainFrame`: giữ trạng thái đang dùng của tham số DSA và cặp khóa.

Luồng tổng thể:

```text
Sinh tham số p, q, g
        ↓
Sinh cặp khóa x, y
        ↓
Chọn file và ký bằng private key x
        ↓
Lưu chữ ký r, s
        ↓
Xác thực file bằng public key y và chữ ký r, s
```

## 2. Các lớp dữ liệu trong `main.dsa`

### 2.1. `DSAParameter`

File: `src/main/dsa/DSAParameter.java`

Lớp này lưu 3 tham số công khai của DSA:

- `p`: số nguyên tố lớn.
- `q`: số nguyên tố nhỏ hơn `p`, đồng thời `q` chia hết `p - 1`.
- `g`: phần tử sinh dùng trong phép tính modulo `p`.

Các thuộc tính đều là `final`, nghĩa là sau khi tạo đối tượng thì giá trị không bị thay đổi.

### 2.2. `DSAKeyPair`

File: `src/main/dsa/DSAKeyPair.java`

Lớp này lưu cặp khóa:

- `x`: khóa bí mật, dùng để ký.
- `y`: khóa công khai, dùng để xác thực.

Công thức liên hệ:

```text
y = g^x mod p
```

### 2.3. `DSASignature`

File: `src/main/dsa/DSASignature.java`

Lớp này lưu chữ ký DSA gồm hai thành phần:

- `r`
- `s`

Một chữ ký hợp lệ phải thỏa:

```text
0 < r < q
0 < s < q
```

## 3. Lõi thuật toán trong `DSAService`

File: `src/main/dsa/DSAService.java`

`DSAService` là lớp quan trọng nhất của dự án. Lớp này tự cài đặt các chức năng:

- Sinh tham số `p`, `q`, `g`.
- Sinh cặp khóa `x`, `y`.
- Ký dữ liệu.
- Xác thực chữ ký.
- Tính giá trị xác thực `v` để giao diện hiển thị.

Lớp dùng:

- `SecureRandom` để sinh số ngẫu nhiên.
- `BigInteger` để tính toán số lớn.
- `HashUtil.sha256` để băm dữ liệu bằng SHA-256.

### 3.1. Sinh tham số `generateParameters`

```java
public DSAParameter generateParameters(int pBits, int qBits)
```

Hàm nhận:

- `pBits`: độ dài bit của `p`, ví dụ 512, 1024, 2048.
- `qBits`: độ dài bit của `q`, ví dụ 160, 256.

Các bước xử lý:

1. Kiểm tra `qBits < pBits`. Nếu `qBits >= pBits`, chương trình báo lỗi vì `q` phải nhỏ hơn `p`.
2. Sinh `q` bằng `BigInteger.probablePrime(qBits, random)`.
3. Gọi `generateP(pBits, q)` để sinh `p` sao cho `p` là số nguyên tố và `q | (p - 1)`.
4. Gọi `generateG(p, q)` để sinh `g`.
5. Trả về `new DSAParameter(p, q, g)`.

### 3.2. Sinh `p` trong `generateP`

```java
private BigInteger generateP(int pBits, BigInteger q)
```

Mục tiêu của hàm là tìm số nguyên tố `p` có đúng `pBits` bit và thỏa:

```text
p - 1 chia hết cho q
```

Mỗi vòng lặp:

1. Tạo một số ứng viên ngẫu nhiên có `pBits` bit:

   ```java
   new BigInteger(pBits, random).setBit(pBits - 1)
   ```

   `setBit(pBits - 1)` bật bit cao nhất để đảm bảo số có đúng độ dài `pBits`.

2. Điều chỉnh ứng viên để `p - 1` là bội của `2q`:

   ```java
   p = candidate.subtract(candidate.subtract(ONE).mod(twoQ));
   ```

   Vì `p - 1` là bội của `2q`, chắc chắn `q` chia hết `p - 1`.

3. Kiểm tra `p`:

   ```java
   p.bitLength() == pBits
   p.isProbablePrime(certainty)
   ```

   `certainty = 80` là mức chắc chắn của phép kiểm tra nguyên tố xác suất.

Nếu `p` đạt điều kiện, hàm trả về `p`; nếu không, tiếp tục sinh ứng viên mới.

### 3.3. Sinh `g` trong `generateG`

```java
private BigInteger generateG(BigInteger p, BigInteger q)
```

DSA cần `g` là phần tử sinh phù hợp với nhóm con bậc `q`. Code tính:

```text
exponent = (p - 1) / q
g = h^exponent mod p
```

Mỗi vòng lặp:

1. Chọn `h` ngẫu nhiên trong khoảng `[2, p - 2]`.
2. Tính `g = h^((p - 1) / q) mod p`.
3. Nếu `g > 1`, chấp nhận `g`.

Điều kiện `g > 1` tránh trường hợp phần tử sinh không có ý nghĩa.

### 3.4. Sinh cặp khóa `generateKeyPair`

```java
public DSAKeyPair generateKeyPair(DSAParameter parameter)
```

Các bước xử lý:

1. Lấy `p`, `q`, `g` từ `parameter`.
2. Sinh private key `x` ngẫu nhiên trong `[1, q - 1]`.
3. Tính public key:

   ```text
   y = g^x mod p
   ```

4. Trả về `new DSAKeyPair(x, y)`.

`x` phải được giữ bí mật. `y`, `p`, `q`, `g` có thể lưu trong `public.key`.

### 3.5. Ký dữ liệu `sign`

```java
public DSASignature sign(byte[] data, DSAParameter parameter, BigInteger privateKey)
```

Hàm nhận byte của file, tham số DSA và private key `x`.

Các bước xử lý:

1. Lấy `q`, `p`, `g`.
2. Băm dữ liệu:

   ```java
   BigInteger h = hashToInteger(data).mod(q);
   ```

   `hashToInteger` dùng SHA-256 rồi chuyển mảng byte thành số nguyên dương.

3. Lặp cho đến khi sinh được chữ ký hợp lệ:

   - Chọn số ngẫu nhiên bí mật `k` trong `[1, q - 1]`.
   - Tính `r = (g^k mod p) mod q`.
   - Nếu `r = 0`, sinh lại `k`.
   - Tính `s = k^-1 * (h + x*r) mod q`.
   - Nếu `s != 0`, trả về `new DSASignature(r, s)`.

Lưu ý quan trọng: `k` phải là số ngẫu nhiên mới cho mỗi lần ký. Nếu tái sử dụng `k`, private key `x` có thể bị suy ra.

### 3.6. Xác thực chữ ký `verify`

```java
public boolean verify(byte[] data, DSASignature signature, DSAParameter parameter, BigInteger publicKey)
```

Hàm này tính lại giá trị xác thực `v` rồi so sánh với `r`:

```java
calculateVerificationValue(...).equals(signature.getR())
```

Nếu `v == r`, chữ ký hợp lệ.

### 3.7. Tính giá trị xác thực `calculateVerificationValue`

```java
public BigInteger calculateVerificationValue(
    byte[] data,
    DSASignature signature,
    DSAParameter parameter,
    BigInteger publicKey
)
```

Đây là hàm chính của bước xác thực. Giao diện dùng hàm này để hiển thị thêm giá trị `v`.

Các bước xử lý:

1. Lấy `q`, `r`, `s`.
2. Kiểm tra miền giá trị:

   ```java
   if (r <= 0 || r >= q || s <= 0 || s >= q) return -1;
   ```

3. Băm dữ liệu:

   ```text
   h = SHA-256(data) mod q
   ```

4. Tính nghịch đảo modulo:

   ```text
   w = s^-1 mod q
   ```

5. Tính:

   ```text
   u1 = h*w mod q
   u2 = r*w mod q
   ```

6. Tính giá trị xác thực:

   ```text
   v = ((g^u1 * y^u2) mod p) mod q
   ```

7. Trả về `v`.

Nếu file, chữ ký và public key đúng cặp, `v` sẽ bằng `r`.

### 3.8. Chuyển hash thành số nguyên

```java
private BigInteger hashToInteger(byte[] data) {
    return new BigInteger(1, HashUtil.sha256(data));
}
```

Tham số `1` trong `new BigInteger(1, bytes)` ép mảng byte thành số dương. Nếu không truyền `1`, `BigInteger` có thể hiểu bit đầu là dấu âm/dương theo biểu diễn bù 2.

### 3.9. Sinh số ngẫu nhiên trong khoảng

```java
private BigInteger randomInRange(BigInteger minInclusive, BigInteger maxInclusive)
```

Hàm sinh số ngẫu nhiên trong khoảng đóng `[minInclusive, maxInclusive]`.

Logic:

1. Tính `range = max - min + 1`.
2. Sinh `value` có số bit bằng `range.bitLength()`.
3. Nếu `value >= range`, sinh lại.
4. Trả về `value + min`.

Cách này đảm bảo giá trị sinh ra luôn nằm trong khoảng yêu cầu.

## 4. Tiện ích trong `main.util`

### 4.1. `HashUtil`

File: `src/main/util/HashUtil.java`

Có hai hàm:

```java
public static byte[] sha256(byte[] data)
public static String sha256Hex(byte[] data)
```

`sha256` dùng `MessageDigest.getInstance("SHA-256")` để băm byte của file.

`sha256Hex` chuyển hash byte thành chuỗi hex để hiển thị trên màn hình ký và xác thực.

### 4.2. `FileUtil`

File: `src/main/util/FileUtil.java`

Có ba hàm:

- `readBytes(File file)`: đọc toàn bộ file thành `byte[]`, dùng khi ký và xác thực.
- `writeText(File file, String content)`: ghi text UTF-8.
- `readText(File file)`: đọc text UTF-8.

Ứng dụng ký được cả file nhị phân vì khi ký/xác thực, file dữ liệu được đọc bằng `readBytes`, không phải đọc text.

### 4.3. `KeyUtil`

File: `src/main/util/KeyUtil.java`

Lớp này chịu trách nhiệm lưu và đọc:

- private key
- public key
- chữ ký

Định dạng `private.key`:

```text
p=<giá trị p>
q=<giá trị q>
g=<giá trị g>
x=<giá trị private key>
```

Định dạng `public.key`:

```text
p=<giá trị p>
q=<giá trị q>
g=<giá trị g>
y=<giá trị public key>
```

Định dạng file chữ ký `.sig`:

```text
r=<giá trị r>
s=<giá trị s>
```

Hàm `parseKeyValue` đọc từng dòng dạng `key=value`, bỏ qua dòng rỗng, sau đó chuyển value thành `BigInteger`.

Hàm `require` kiểm tra các trường bắt buộc:

- Private key phải có `p`, `q`, `g`, `x`.
- Public key phải có `p`, `q`, `g`, `y`.
- Signature phải có `r`, `s`.

Nếu thiếu trường hoặc value không phải số nguyên hợp lệ, chương trình sẽ báo lỗi.

## 5. Luồng UI gọi vào lõi DSA

### 5.1. `Main.java`

File: `src/main/Main.java`

Điểm vào chương trình:

1. Gọi `SwingUtilities.invokeLater` để tạo UI trên Event Dispatch Thread của Swing.
2. Đặt Look and Feel theo hệ thống.
3. Tạo `new MainFrame().setVisible(true)`.

### 5.2. `MainFrame`

File: `src/main/ui/MainFrame.java`

`MainFrame` là cửa sổ chính:

- Dùng `CardLayout` để chuyển giữa các màn hình.
- Sidebar chứa các nút điều hướng.
- Giữ `currentParameter` và `currentKeyPair`.

Các panel khác lấy dữ liệu từ `MainFrame` để dùng chung tham số và khóa.

### 5.3. `ParameterPanel`

File: `src/main/ui/ParameterPanel.java`

Màn hình sinh tham số DSA.

Khi bấm `Sinh tham số`:

1. Lấy `pBits`, `qBits` từ combobox.
2. Hiển thị trạng thái đang xử lý.
3. Chạy `SwingWorker` để tránh làm treo giao diện.
4. Trong `doInBackground`, gọi:

   ```java
   service.generateParameters(p, q)
   ```

5. Trong `done`, lưu `DSAParameter` vào `MainFrame`, hiển thị `p`, `q`, `g` và báo thành công.

### 5.4. `KeyPanel`

File: `src/main/ui/KeyPanel.java`

Màn hình sinh, lưu và đọc khóa.

Khi bấm `Sinh khóa`:

1. Lấy `parameter = frame.getCurrentParameter()`.
2. Nếu chưa có tham số, báo lỗi.
3. Gọi `service.generateKeyPair(parameter)`.
4. Lưu cặp khóa vào `frame.setCurrentKeyPair(keyPair)`.
5. Hiển thị `x`, `y`.

Khi bấm `Lưu khóa bí mật`, chương trình gọi `KeyUtil.savePrivateKey`.

Khi bấm `Lưu khóa công khai`, chương trình gọi `KeyUtil.savePublicKey`.

Khi bấm `Đọc private.key`:

1. Gọi `KeyUtil.loadPrivateKey`.
2. Cập nhật `currentParameter`.
3. Tính lại public key:

   ```text
   y = g^x mod p
   ```

4. Tạo lại `DSAKeyPair(x, y)` và hiển thị lên giao diện.

Khi bấm `Đọc public.key`:

1. Gọi `KeyUtil.loadPublicKey`.
2. Cập nhật `currentParameter`.
3. Đặt `currentKeyPair = null` vì public key không có `x`, nên không đủ để ký.
4. Hiển thị `y`.

### 5.5. `SignPanel`

File: `src/main/ui/SignPanel.java`

Màn hình ký file.

Trạng thái nội bộ:

- `selectedFile`: file người dùng chọn để ký.
- `lastSignature`: chữ ký vừa sinh, dùng để lưu file `.sig`.

Khi bấm `Ký file`:

1. Kiểm tra đã chọn file.
2. Kiểm tra đã có tham số và private key.
3. Chạy `SwingWorker`.
4. Trong `doInBackground`:

   ```java
   data = FileUtil.readBytes(selectedFile);
   return service.sign(data, parameter, keyPair.getX());
   ```

5. Trong `done`, lưu chữ ký vào `lastSignature`, hiển thị hash SHA-256, `r`, `s` và báo ký thành công.

Khi bấm `Lưu chữ ký`, chương trình gọi `KeyUtil.saveSignature`.

### 5.6. `VerifyPanel`

File: `src/main/ui/VerifyPanel.java`

Màn hình xác thực chữ ký. Người dùng cần chọn:

- File gốc cần xác thực.
- File chữ ký `.sig`.
- File khóa công khai `public.key`.

Khi bấm `Xác thực chữ ký`:

1. Kiểm tra đã chọn đủ 3 file.
2. Chạy `SwingWorker`.
3. Trong `doInBackground`:

   ```java
   data = FileUtil.readBytes(dataFile);
   signature = KeyUtil.loadSignature(signatureFile);
   publicData = KeyUtil.loadPublicKey(publicKeyFile);
   parameter = publicData.getParameter();
   v = service.calculateVerificationValue(data, signature, parameter, publicData.getPublicKey());
   return v.equals(signature.getR());
   ```

4. Trong `done`, hiển thị hash SHA-256, `r`, `s`, `v`.
5. Nếu `v == r`, báo chữ ký hợp lệ.
6. Nếu `v != r`, báo chữ ký không hợp lệ.

Nếu file `.sig` hoặc `public.key` sai định dạng, chương trình bắt exception và hiển thị thông báo xác thực thất bại.

## 6. Công thức DSA trong dự án

### 6.1. Sinh tham số

```text
q: số nguyên tố
p: số nguyên tố, q | (p - 1)
g = h^((p - 1) / q) mod p, với g > 1
```

### 6.2. Sinh khóa

```text
x ngẫu nhiên trong [1, q - 1]
y = g^x mod p
```

### 6.3. Ký

```text
h = SHA-256(data) mod q
k ngẫu nhiên trong [1, q - 1]
r = (g^k mod p) mod q
s = k^-1 * (h + x*r) mod q
signature = (r, s)
```

### 6.4. Xác thực

```text
Kiểm tra 0 < r < q và 0 < s < q
h = SHA-256(data) mod q
w = s^-1 mod q
u1 = h*w mod q
u2 = r*w mod q
v = ((g^u1 * y^u2) mod p) mod q
Hợp lệ nếu v == r
```

## 7. Các điểm kiểm tra lỗi và ràng buộc

Trong `DSAService`:

- `qBits` phải nhỏ hơn `pBits`.
- `p` phải là số nguyên tố và đúng độ dài bit.
- `p - 1` phải chia hết cho `q`.
- `g` phải lớn hơn `1`.
- `x` và `k` nằm trong `[1, q - 1]`.
- Khi ký, nếu `r = 0` hoặc `s = 0` thì sinh lại `k`.
- Khi xác thực, nếu `r` hoặc `s` ngoài khoảng hợp lệ thì trả về `-1`.

Trong `KeyUtil`:

- File key/signature phải có dạng `key=value`.
- Value phải là số nguyên hợp lệ với `BigInteger`.
- Private key bắt buộc có `p`, `q`, `g`, `x`.
- Public key bắt buộc có `p`, `q`, `g`, `y`.
- Signature bắt buộc có `r`, `s`.

Trong UI:

- Không cho sinh khóa nếu chưa có tham số.
- Không cho lưu khóa nếu chưa có cặp khóa.
- Không cho ký nếu chưa chọn file hoặc chưa có private key.
- Không cho xác thực nếu chưa chọn đủ file gốc, file `.sig`, `public.key`.

## 8. Vì sao dùng `SwingWorker`

`ParameterPanel`, `SignPanel`, `VerifyPanel` dùng `SwingWorker` cho các việc có thể tốn thời gian:

- Sinh tham số DSA.
- Đọc file và ký.
- Đọc file, đọc key, tính xác thực.

Nếu chạy trực tiếp trên luồng UI, cửa sổ Swing có thể bị đứng. `SwingWorker` đưa việc nặng sang background thread, sau đó cập nhật UI trong `done()`.

## 9. Ghi chú về bảo mật

Dự án tự cài đặt DSA để minh họa thuật toán và phục vụ học tập. Nếu dùng trong môi trường thực tế cần lưu ý:

- Phải bảo vệ file `private.key` vì nó chứa `x`.
- Không được tái sử dụng `k` khi ký.
- Nên dùng kích thước tham số mạnh, ưu tiên `p = 2048 bit`, `q = 256 bit`.
- Trong hệ thống sản xuất, nên ưu tiên thư viện mật mã đã được kiểm định thay vì tự cài đặt.

## 10. Tóm tắt vai trò từng file

```text
src/main/Main.java
  Khởi động ứng dụng Swing.

src/main/dsa/DSAParameter.java
  Lưu p, q, g.

src/main/dsa/DSAKeyPair.java
  Lưu x, y.

src/main/dsa/DSASignature.java
  Lưu r, s.

src/main/dsa/DSAService.java
  Cài đặt sinh tham số, sinh khóa, ký, xác thực.

src/main/util/HashUtil.java
  Băm SHA-256 và chuyển hash sang hex.

src/main/util/FileUtil.java
  Đọc byte file, đọc/ghi text UTF-8.

src/main/util/KeyUtil.java
  Lưu/đọc private.key, public.key, file .sig.

src/main/ui/MainFrame.java
  Cửa sổ chính, sidebar, CardLayout, trạng thái dùng chung.

src/main/ui/ParameterPanel.java
  Màn hình sinh p, q, g.

src/main/ui/KeyPanel.java
  Màn hình sinh, lưu, đọc khóa.

src/main/ui/SignPanel.java
  Màn hình chọn file, ký file, lưu chữ ký.

src/main/ui/VerifyPanel.java
  Màn hình xác thực file bằng signature và public key.

src/main/ui/GuidePanel.java
  Hiển thị hướng dẫn sử dụng và công thức.

src/main/ui/Theme.java
  Màu sắc, font, padding dùng chung.

src/main/ui/UIFactory.java
  Tạo các thành phần UI dùng lại như title, label, text field, scroll pane.

src/main/ui/components/*
  Các component giao diện tùy biến: button, card, text area, status label.
```
