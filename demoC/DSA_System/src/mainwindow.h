#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QString>
#include <openssl/bn.h>

// Forward declarations — tránh include nặng trong header
namespace Ui { class MainWindow; }
class QLineEdit;
class QTextEdit;

struct SignatureRecord
{
    int id;

    QString fileName;

    QString fileHash;

    QString publicKeyHash;

    QString signature;
};
/**
 * @brief MainWindow — cửa sổ chính của hệ thống DSA v2.0
 *
 * Quản lý 5 trang:
 *   pageManual       — Thủ công (tạo khóa / ký / xác thực theo tham số nhập tay)
 *   pageAutoKey      — Tạo khóa tự động bằng DSAManager
 *   pageCreateSign   — Tạo chữ ký từ file
 *   pageVerifySign   — Xác thực chữ ký từ file
 *   pageSystemLogs   — Nhật ký hệ thống
 */
class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);
    ~MainWindow() override;

private:
    // ── UI ──────────────────────────────────────────────────────────────
    Ui::MainWindow *ui;

    // ── Trạng thái trang Tạo chữ ký (file) ─────────────────────────────
    QString currentCreateFilePath;          ///< Đường dẫn file cần ký
    QString currentCreatePrivateKeyPath;    ///< Đường dẫn khóa bí mật
    QString currentCreatePublicKeyPath;     ///< Đường dẫn khóa công khai
    QString lastCreateSignaturePath;        ///< Đường dẫn file .sig vừa tạo

    // ── Trạng thái trang Xác thực chữ ký (file) ─────────────────────────
    QString currentVerifyOriginalFilePath;  ///< File gốc cần xác thực
    QString currentVerifySignaturePath;     ///< File chữ ký .sig
    QString currentVerifyPublicKeyPath;     ///< Khóa công khai để xác thực
    QString lastVerifyResultMessage;        ///< Kết quả xác thực gần nhất

    // ── Tham số khóa tự động (dạng hex) ─────────────────────────────────
    QString autoPhex;
    QString autoQhex;
    QString autoGhex;
    QString autoXhex;
    QString autoYhex;

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * @brief Kết nối tất cả signal/slot giữa các nút và slot xử lý.
     *        Được gọi một lần trong constructor.
     */
    void setupConnections();

    /**
     * @brief Cập nhật text trên badge trạng thái hệ thống (statusLabel).
     * @param statusText Chuỗi trạng thái hiển thị.
     */
    void updateStatus(const QString &statusText);

    /**
     * @brief Thêm một dòng log có timestamp vào widget log và ghi vào Logger.
     * @param logWidget  Widget QTextEdit nhận dòng log.
     * @param message    Nội dung thông báo.
     */
    void appendLog(QTextEdit *logWidget, const QString &message);

    /**
     * @brief Đọc tất cả file *.log trong thư mục logs/ và lọc theo filterText.
     * @param filterText Chuỗi lọc (rỗng = hiển thị tất cả).
     */
    void readLogFiles(const QString &filterText);

    /**
     * @brief Ghi text vào file tạm thời; trả về đường dẫn qua outPath.
     *        Dùng để tạo file tạm cho thao tác ký/xác thực thủ công.
     * @return true nếu thành công, false nếu không mở được file.
     */
    bool writeTextToTempFile(const QString &text, QString &outPath);

    /**
     * @brief Lưu cặp khóa bí mật + công khai vào thư mục directoryPath.
     *        Sử dụng autoPhex … autoYhex đã được tạo trước đó.
     * @return true nếu cả hai file được ghi thành công.
     */
    bool saveKeyPairToDirectory(const QString &directoryPath);

    /**
     * @brief Ghi chuỗi text vào file tại filePath (UTF-8, ghi đè).
     * @return true nếu thành công.
     */
    bool saveTextToFile(const QString &filePath, const QString &text);

    /**
     * @brief Chuyển BIGNUM sang chuỗi hex (uppercase).
     *        Caller không cần giải phóng bộ nhớ.
     * @return Chuỗi hex hoặc QString() nếu number == nullptr.
     */
    QString bnToHex(const BIGNUM *number) const;

    /**
     * @brief Đọc và parse số thập phân từ QLineEdit thành BIGNUM.
     *        Kiểm tra: không rỗng, hợp lệ, dương, khác 0.
     * @param lineEdit    Widget nguồn.
     * @param outBn       Con trỏ nhận BIGNUM đã cấp phát (caller giải phóng).
     * @param paramName   Tên tham số dùng trong thông báo lỗi (vd. "P").
     * @param errorMessage Chuỗi lỗi được gán nếu trả về false.
     * @return true nếu parse thành công.
     */
    bool parseDecimalBigNumber(QLineEdit *lineEdit,
                               BIGNUM   **outBn,
                               const QString &paramName,
                               QString  &errorMessage);
    bool saveSignatureRegistry(
        const QString& filePath,
        const QString& signaturePath,
        const QString& publicKeyPath);

    int findSignatureRecord(
        const QString& fileHash,
        const QString& signatureContent,
        SignatureRecord& record);

private slots:
    // ── Điều hướng sidebar ────────────────────────────────────────────
    void onNavManualClicked();           ///< Chuyển sang trang Thủ công
    void onNavAutoKeyClicked();          ///< Chuyển sang trang Tạo khóa tự động
    void onNavCreateSignatureClicked();  ///< Chuyển sang trang Tạo chữ ký
    void onNavVerifySignatureClicked();  ///< Chuyển sang trang Xác thực chữ ký
    void onNavSystemLogsClicked();       ///< Chuyển sang trang Nhật ký hệ thống

    // ── Trang Thủ công — Tạo khóa ────────────────────────────────────
    void onManualGenerateClicked();      ///< Tính Y = g^x mod p từ P,Q,G,X nhập tay
    void onManualClearClicked();         ///< Xóa toàn bộ input thủ công

    // ── Trang Tạo khóa tự động ───────────────────────────────────────
    void onAutoGenerateClicked();        ///< Sinh P,Q,G,X,Y tự động (DSAManager 1024-bit)
    void onAutoClearClicked();           ///< Reset hiển thị về "số in"
    void onAutoDownloadClicked();        ///< Lưu cặp khóa ra thư mục người dùng chọn

    // ── Trang Tạo chữ ký (file) ──────────────────────────────────────
    void onCreateChooseFileClicked();        ///< Mở dialog chọn file cần ký
    void onCreateChoosePrivateKeyClicked();  ///< Mở dialog chọn khóa bí mật
    void onCreateChoosePublicKeyClicked();   ///< Mở dialog chọn khóa công khai
    void onCreateSignatureClicked();         ///< Thực hiện ký file → tạo .sig
    void onCreateDownloadClicked();          ///< Lưu file .sig ra vị trí người dùng chọn

    // ── Trang Xác thực chữ ký (file) ────────────────────────────────
    void onVerifyChooseOriginalClicked();    ///< Mở dialog chọn file gốc
    void onVerifyChooseSignatureClicked();   ///< Mở dialog chọn file chữ ký .sig
    void onVerifyChoosePublicKeyClicked();   ///< Mở dialog chọn khóa công khai
    void onVerifySignClicked();              ///< Xác thực chữ ký file
    void onVerifyDownloadClicked();          ///< Lưu kết quả xác thực ra file .txt

    // ── Trang Nhật ký hệ thống ───────────────────────────────────────
    void onFilterLogsClicked();              ///< Lọc log theo từ khóa inputLogsSearch
};

#endif // MAINWINDOW_H
