#include "mainwindow.h"
#include "ui_mainwindow.h"

#include "crypto/dsa_manager.h"
#include "crypto/key_generator.h"
#include "key_storage/key_manager.h"

#include <QPushButton>
#include <QMessageBox>
#include <QFileDialog>
#include <QDateTime>
#include <QTextStream>
#include "file_io/file_signer.h"
#include "logging/logger.h"

#include <QScrollBar>
#include <QTimer>
#include <QFile>
#include <QResource>
#include <QGraphicsDropShadowEffect>

#include <QStyle>

MainWindow::MainWindow(QWidget *parent)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
{
    ui->setupUi(this);

    // ── Timer thời gian thực ──────────────────────────────────────────────
    QTimer *timer = new QTimer(this);
    connect(timer, &QTimer::timeout, this, [=]() {
        ui->lblTime->setText("🕒 " + QTime::currentTime().toString("hh:mm:ss AP"));
        ui->lblDate->setText("📅 " + QDate::currentDate().toString("dd/MM/yyyy"));
    });
    timer->start(1000);
    ui->lblTime->setText("🕒 " + QTime::currentTime().toString("hh:mm:ss AP"));
    ui->lblDate->setText("📅 " + QDate::currentDate().toString("dd/MM/yyyy"));

    // ── Nav buttons — autoExclusive ───────────────────────────────────────
    ui->btnDashboard->setAutoExclusive(true);
    ui->btnKeyManagement->setAutoExclusive(true);
    ui->btnSign->setAutoExclusive(true);
    ui->btnVerify->setAutoExclusive(true);
    ui->btnLogs->setAutoExclusive(true);
    ui->btnAdvanced->setAutoExclusive(true);
    ui->btnDashboard->setChecked(true);

    // ── Theme mặc định ───────────────────────────────────────────────────
    this->setMaximumSize(QWIDGETSIZE_MAX, QWIDGETSIZE_MAX);
    applyTheme(true); // light mặc định
    ui->stackedWidget->setCurrentWidget(ui->pageDashboard);
    ui->labelSystemStatus->setText("Trạng thái ệ thống:   SẴN SÀNG");

    // ── Navigation connects ───────────────────────────────────────────────
    connect(ui->btnDashboard, &QPushButton::clicked, this, [=](){
        ui->stackedWidget->setCurrentWidget(ui->pageDashboard);
    });
    connect(ui->btnKeyManagement, &QPushButton::clicked, this, [=](){
        ui->stackedWidget->setCurrentWidget(ui->pageKeyManagement);
    });
    connect(ui->btnSign, &QPushButton::clicked, this, [=](){
        ui->stackedWidget->setCurrentWidget(ui->pageSign);
    });
    connect(ui->btnVerify, &QPushButton::clicked, this, [=](){
        ui->stackedWidget->setCurrentWidget(ui->pageVerify);
    });

    // ── FIX BUG #3: btnLogs — chỉ dùng slot on_btnLogs_clicked (auto-connect)
    // KHÔNG connect lambda riêng ở đây — on_btnLogs_clicked() đã xử lý cả
    // setCurrentWidget lẫn đọc log file. Connect thêm lambda sẽ gây double-fire.

    connect(ui->btnAdvanced, &QPushButton::clicked, this, [=](){
        ui->stackedWidget->setCurrentWidget(ui->pageAdvanced);
    });

    // ── Generate DSA Keys ────────────────────────────────────────────────
    connect(ui->btnGenerateKeys, &QPushButton::clicked, this, &MainWindow::generateDSAKeys);

    // ── Choose Sign File ─────────────────────────────────────────────────
    connect(ui->btnChooseSignFile, &QPushButton::clicked, this, [=](){
        QString file = QFileDialog::getOpenFileName(this, "Chọn tập tin để ký");
        if (!file.isEmpty()) {
            ui->lineSignFile->setText(file);
            Logger::log("INFO", "Đã chọn file ký " + file);
        }
    });

    // ── Choose Private Key ───────────────────────────────────────────────
    connect(ui->btnChoosePrivateKey, &QPushButton::clicked, this, [=](){
        QString file = QFileDialog::getOpenFileName(this, "Chọn Private Key");
        if (!file.isEmpty()) {
            ui->linePrivateKey->setText(file);
            ui->labelLoadedKey->setText("Khóa đã tải: Private Key");
            Logger::log("INFO", "Đã chọn khóa " + file);
        }
    });

    // ── FIX BUG #1: btnSignFile — ĐÃ XÓA lambda connect giả cũ ở đây.
    // Chỉ dùng on_btnSignFile_clicked() (auto-connect) — xử lý thật.

    // ── Choose Verify File ───────────────────────────────────────────────
    connect(ui->btnChooseVerifyFile, &QPushButton::clicked, this, [=](){
        QString file = QFileDialog::getOpenFileName(this, "Chọn file gốc");
        if (!file.isEmpty()) {
            ui->lineVerifyFile->setText(file);
            Logger::log("INFO", "Đã chọn file kiểm tra gốc " + file);
        }
    });

    connect(ui->btnChooseSignatureFile, &QPushButton::clicked, this, [=](){
        QString file = QFileDialog::getOpenFileName(this, "Chọn file chữ ký");
        if (!file.isEmpty()) {
            ui->lineSignatureFile->setText(file);
            Logger::log("INFO", "Đã chọn file chữ ký " + file);
        }
    });

    connect(ui->btnChoosePublicKey, &QPushButton::clicked, this, [=](){
        QString file = QFileDialog::getOpenFileName(this, "Chọn Public Key");
        if (!file.isEmpty()) {
            ui->linePublicKey->setText(file);
            ui->labelLoadedKey->setText("Khóa đã tải: Public Key");
            Logger::log("INFO", "Đã chọn khóa " + file);
        }
    });

    connect(ui->btnGenerateParameters, &QPushButton::clicked, this, &MainWindow::on_btngenerateDSAParameters);

    addShadow(ui->cardKey);
    addShadow(ui->cardSign);
    addShadow(ui->cardVerify);
    addShadow(ui->cardLogs);
    addShadow(ui->welcomeBannerCard);

    addButtonShadow(ui->btnDashboard);
    addButtonShadow(ui->btnAdvanced);
    addButtonShadow(ui->btnChoosePrivateKey);
    addButtonShadow(ui->btnChoosePublicKey);
    addButtonShadow(ui->btnChooseSignFile);
    addButtonShadow(ui->btnChooseVerifyFile);
    addButtonShadow(ui->btnChooseSignatureFile);
    addButtonShadow(ui->btnVerifySignature);
    addButtonShadow(ui->btnVerify);
    addButtonShadow(ui->btnSign);
    addButtonShadow(ui->btnSignFile);
    addButtonShadow(ui->btnSaveKeyPair);
    addButtonShadow(ui->btnKeyManagement);
    addButtonShadow(ui->btnGenerateKeys);
    addButtonShadow(ui->btnGenerateParameters);
    addButtonShadow(ui->btnLogs);
}

void MainWindow::addButtonShadow(QPushButton *button)
{
    auto *shadow = new QGraphicsDropShadowEffect(button);
    shadow->setBlurRadius(18);
    shadow->setOffset(0, 3);
    shadow->setColor(QColor(37, 99, 235, 60));
    button->setGraphicsEffect(shadow);
}

void MainWindow::addShadow(QWidget *widget)
{
    auto *shadow = new QGraphicsDropShadowEffect(widget);
    shadow->setBlurRadius(25);
    shadow->setOffset(0, 4);
    shadow->setColor(QColor(0, 0, 0, 40));
    widget->setGraphicsEffect(shadow);
}

MainWindow::~MainWindow()
{
    clearCurrentKeys();
    delete ui;
}

void MainWindow::applyTheme(bool dark)
{
    QString path = dark
                       ? ":/forms/style_dark.qss"
                       : ":/forms/style_light.qss";

    QFile f(path);
    if (!f.open(QFile::ReadOnly)) {
        qDebug() << "Không tìm thấy file:" << path;
        return;
    }
    setStyleSheet(f.readAll());
    m_isDarkTheme = dark;
}

void MainWindow::clearCurrentKeys()
{
    if (current_p) { BN_free(current_p); current_p = nullptr; }
    if (current_q) { BN_free(current_q); current_q = nullptr; }
    if (current_g) { BN_free(current_g); current_g = nullptr; }
    if (current_x) { BN_free(current_x); current_x = nullptr; }
    if (current_y) { BN_free(current_y); current_y = nullptr; }
}

// ═══════════════════════════════════════════════════════════════
// GENERATE DSA KEYS
// ═══════════════════════════════════════════════════════════════
void MainWindow::generateDSAKeys()
{
    if (isGeneratingKeys) return;
    isGeneratingKeys = true;

    ui->textKeyInfo->clear();
    ui->textKeyInfo->append("Đang tạo tham số DSA...\n");
    ui->btnGenerateKeys->setEnabled(false);
    ui->btnGenerateKeys->setText("Đang tạo khóa...");

    DSAManager dsa;
    bool success = dsa.generateParameters(2048);

    if (!success) {
        QMessageBox::critical(this, "Error", "Tạo khóa thất bại!");
        ui->btnGenerateKeys->setEnabled(true);
        ui->btnGenerateKeys->setText("Tạo cặp khóa");
        isGeneratingKeys = false;
        Logger::log("KEYGEN", "Tạo khóa thất bại");
        return;
    }

    KeyGenerator keyGen;
    BIGNUM* x = keyGen.generatePrivateKey(dsa.getQ());
    BIGNUM* y = keyGen.generatePublicKey(dsa.getG(), x, dsa.getP());

    bool privateSaved = KeyManager::savePrivateKey(
        dsa.getP(), dsa.getQ(), dsa.getG(), x,
        "storage/keys/private/private_key.txt"
        );
    qDebug() << "privateSaved =" << privateSaved << "path =" << "storage/keys/private/private_key.txt";
    bool publicSaved = KeyManager::savePublicKey(
        dsa.getP(), dsa.getQ(), dsa.getG(), y,
        "storage/keys/public/public_key.txt"
        );

    showKeyInformation(dsa.getP(), dsa.getQ(), dsa.getG(), x, y);

    if (privateSaved && publicSaved) {
        clearCurrentKeys();
        current_p = BN_dup(dsa.getP());
        current_q = BN_dup(dsa.getQ());
        current_g = BN_dup(dsa.getG());
        current_x = BN_dup(x);
        current_y = BN_dup(y);

        QMessageBox::information(this, "Success", "Sinh khóa thành công");
        ui->labelLastAction->setText("Hành động cuối: Tạo khóa DSA");
        Logger::log("KEYGEN", "Đã tạo cặp khóa DSA thành công");
    } else {
        QMessageBox::critical(this, "Error", "Không thể tạo đường dẫn file lưu khóa");
        Logger::log("KEYGEN", "Tạo cặp khóa DSA thất bại");
    }

    ui->btnGenerateKeys->setEnabled(true);
    ui->btnGenerateKeys->setText("Tạo cặp khóa");
    isGeneratingKeys = false;

    BN_free(x);
    BN_free(y);
}

// ═══════════════════════════════════════════════════════════════
// SAVE KEY PAIR
// FIX BUG #2: enable lại nút khi user cancel dialog
// ═══════════════════════════════════════════════════════════════
void MainWindow::on_btnSaveKeyPair_clicked()
{
    if (!current_x || !current_y) {
        QMessageBox::warning(this, "Cảnh báo", "Không tìm thấy dữ liệu khóa!");
        return;
    }

    ui->btnSaveKeyPair->setEnabled(false);

    QString dirPath = QFileDialog::getExistingDirectory(
        this, "Chọn thư mục lưu khóa",
        QDir::homePath(),
        QFileDialog::ShowDirsOnly
        );

    // ── FIX: enable lại nếu user cancel ──────────────────────
    if (dirPath.isEmpty()) {
        ui->btnSaveKeyPair->setEnabled(true);
        return;
    }

    QString privatePath = QDir(dirPath).filePath("private_key.txt");
    QString publicPath  = QDir(dirPath).filePath("public_key.txt");

    bool privOk = KeyManager::savePrivateKey(
        current_p, current_q, current_g, current_x,
        privatePath.toStdString()
        );
    bool pubOk = KeyManager::savePublicKey(
        current_p, current_q, current_g, current_y,
        publicPath.toStdString()
        );

    if (privOk && pubOk) {
        QMessageBox::information(this, "Thành công", "Đã xuất khóa!");
        Logger::log("KEYGEN", "Lưu khóa thành công");
    } else {
        QMessageBox::critical(this, "Thất bại",
                              "Không thể ghi file. Kiểm tra quyền ghi tại: " + dirPath);
        Logger::log("KEYGEN", "Lưu khóa thất bại");
    }

    ui->btnSaveKeyPair->setEnabled(true);
}

// ═══════════════════════════════════════════════════════════════
// SHOW KEY INFORMATION
// ═══════════════════════════════════════════════════════════════
void MainWindow::showKeyInformation(
    const BIGNUM* p, const BIGNUM* q,
    const BIGNUM* g, const BIGNUM* x, const BIGNUM* y)
{
    QString info;
    info += "=========== DSA PARAMETERS ===========\n\n";
    info += "p:\n" + QString(BN_bn2hex(p)) + "\n\n";
    info += "q:\n" + QString(BN_bn2hex(q)) + "\n\n";
    info += "g:\n" + QString(BN_bn2hex(g)) + "\n\n";
    info += "=========== PRIVATE KEY ===========\n\n";
    info += "x:\n" + QString(BN_bn2hex(x)) + "\n\n";
    info += "=========== PUBLIC KEY ===========\n\n";
    info += "y:\n" + QString(BN_bn2hex(y)) + "\n\n";
    ui->textKeyInfo->setPlainText(info);
}

// ═══════════════════════════════════════════════════════════════
// SIGN FILE
// FIX BUG #1: Đây là handler duy nhất — không còn lambda giả song song
// ═══════════════════════════════════════════════════════════════
void MainWindow::on_btnSignFile_clicked()
{
    QString filePath = ui->lineSignFile->text().trimmed();
    QString keyPath  = ui->linePrivateKey->text().trimmed();

    if (filePath.isEmpty()) {
        QMessageBox::warning(this, "Lỗi", "Chưa chọn file cần ký");
        return;
    }
    if (keyPath.isEmpty()) {
        QMessageBox::warning(this, "Lỗi", "Chưa chọn private key");
        return;
    }
    if (!QFile::exists(filePath)) {
        QMessageBox::warning(this, "Lỗi", "File không tồn tại");
        return;
    }
    if (!QFile::exists(keyPath)) {
        QMessageBox::warning(this, "Lỗi", "Private key không tồn tại");
        return;
    }

    QMessageBox::information(this, "Lưu chữ ký", "Hãy chọn nơi lưu chữ ký");
    QString savePath = QFileDialog::getSaveFileName(
        this, "Lưu chữ ký",
        QDir::homePath() + "/signature.sig",
        "Signature (*.sig)"
        );
    if (savePath.isEmpty()) return;

    ui->btnSignFile->setEnabled(false);

    bool ok = FileSigner::signFile(filePath, keyPath, savePath);

    if (ok) {
        ui->textSign->append("===== DSA SIGNATURE =====");
        ui->textSign->append("File: " + filePath);
        ui->textSign->append("Chữ ký lưu tại: " + savePath);
        ui->textSign->append("✔ Ký thành công.\n");
        ui->labelLastAction->setText("Hành động cuối: Đã ký file");
        Logger::log("SIGN", "Ký thành công: " + savePath);
    } else {
        QMessageBox::critical(this, "Lỗi", "Ký thất bại! Kiểm tra lại khóa và file.");
        Logger::log("SIGN", "Ký thất bại");
    }

    ui->btnSignFile->setEnabled(true);
}

// ═══════════════════════════════════════════════════════════════
// VERIFY SIGNATURE
// FIX BUG #1: Handler duy nhất, không còn lambda song song
// FIX BUG #4: Kết quả dùng objectName property để QSS xử lý màu
// ═══════════════════════════════════════════════════════════════
void MainWindow::on_btnVerifySignature_clicked()
{
    ui->btnVerifySignature->setEnabled(false);

    QString filePath      = ui->lineVerifyFile->text().trimmed();
    QString publicKeyPath = ui->linePublicKey->text().trimmed();
    QString signaturePath = ui->lineSignatureFile->text().trimmed();

    auto reEnable = [=](){ ui->btnVerifySignature->setEnabled(true); };

    if (filePath.isEmpty()) {
        QMessageBox::warning(this, "Lỗi", "Chưa có file kiểm tra");
        return reEnable();
    }
    if (signaturePath.isEmpty()) {
        QMessageBox::warning(this, "Lỗi", "Chưa có chữ ký");
        return reEnable();
    }
    if (publicKeyPath.isEmpty()) {
        QMessageBox::warning(this, "Lỗi", "Chưa có khóa công khai");
        return reEnable();
    }
    if (!QFile::exists(filePath)) {
        QMessageBox::warning(this, "Lỗi", "File kiểm tra không tồn tại");
        return reEnable();
    }
    if (!QFile::exists(signaturePath)) {
        QMessageBox::warning(this, "Lỗi", "Chữ ký không tồn tại");
        return reEnable();
    }
    if (!QFile::exists(publicKeyPath)) {
        QMessageBox::warning(this, "Lỗi", "Khóa không tồn tại");
        return reEnable();
    }

    bool ok = FileSigner::verifyFile(filePath, publicKeyPath, signaturePath);

    if (ok) {
        ui->labelVerifyResult->setText("✔ Chữ ký hợp lệ");
        ui->labelVerifyResult->setProperty("verifyState", "valid");
        ui->labelVerifyResult->style()->unpolish(ui->labelVerifyResult);
        ui->labelVerifyResult->style()->polish(ui->labelVerifyResult);
        ui->labelLastAction->setText("Hành động cuối: Xác minh thành công");
        Logger::log("VERIFY", "Xác minh chữ ký thành công");

        // ===== THÊM THÔNG BÁO =====
        QMessageBox::information(this, "Xác minh chữ ký",
                                 "✅ Chữ ký hợp lệ!\nTệp tin chưa bị thay đổi và đúng chủ sở hữu.");
    } else {
        ui->labelVerifyResult->setText("✖ Chữ ký không hợp lệ");
        ui->labelVerifyResult->setProperty("verifyState", "invalid");
        ui->labelVerifyResult->style()->unpolish(ui->labelVerifyResult);
        ui->labelVerifyResult->style()->polish(ui->labelVerifyResult);
        ui->labelLastAction->setText("Hành động cuối: Xác minh thất bại");
        Logger::log("VERIFY", "Xác minh chữ ký thất bại");

        // ===== THÊM THÔNG BÁO =====
        QMessageBox::warning(this, "Xác minh chữ ký",
                             "❌ Chữ ký không hợp lệ!\nTệp tin có thể đã bị sửa đổi hoặc khóa công khai không đúng.");
    }

    reEnable();
}

// ═══════════════════════════════════════════════════════════════
// LOGS
// FIX BUG #3: Gộp setCurrentWidget vào đây, xóa lambda riêng trong constructor
// ═══════════════════════════════════════════════════════════════
void MainWindow::on_btnLogs_clicked()
{
    // Chuyển trang (trước đây có cả lambda riêng — đã xóa)
    ui->stackedWidget->setCurrentWidget(ui->pageLogs);

    QString logFile = "logs/" +
                      QDate::currentDate().toString("yyyy-MM-dd") + ".log";

    QFile file(logFile);
    if (!file.exists()) {
        ui->plainTextEditLogs->clear();
        ui->plainTextEditLogs->appendPlainText("[LOG] Chưa có dữ liệu log.");
        return;
    }

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        ui->plainTextEditLogs->clear();
        ui->plainTextEditLogs->appendPlainText("[ERROR] Không thể mở file log!");
        return;
    }

    QTextStream in(&file);
    QString content = in.readAll();
    file.close();

    ui->plainTextEditLogs->clear();
    ui->plainTextEditLogs->setPlainText(content);
    ui->plainTextEditLogs->verticalScrollBar()
        ->setValue(ui->plainTextEditLogs->verticalScrollBar()->maximum());

    ui->labelLastAction->setText("Hành động cuối: Xem nhật ký hệ thống");
}

void MainWindow::on_btngenerateDSAParameters()
{
    // Chống click liên tục
    static bool isGenerating = false;
    if (isGenerating) return;
    isGenerating = true;

    ui->btnGenerateParameters->setEnabled(false);
    ui->btnGenerateParameters->setText("Đang sinh tham số...");
    ui->textParameters->clear();

    // Tạo đối tượng DSAManager (tạm thời, sẽ giải phóng sau khi dùng)
    DSAManager dsa;
    bool success = dsa.generateParameters(2048);  // 2048-bit an toàn

    if (!success) {
        ui->textParameters->append("❌ Lỗi: Không thể sinh tham số DSA.");
        Logger::log("PARAM", "Sinh tham số thất bại");
    } else {
        // Lấy các tham số p, q, g (dưới dạng BIGNUM*)
        const BIGNUM* p = dsa.getP();
        const BIGNUM* q = dsa.getQ();
        const BIGNUM* g = dsa.getG();

        // Chuyển sang chuỗi hex để hiển thị (dễ đọc, đầy đủ)
        char* pHex = BN_bn2hex(p);
        char* qHex = BN_bn2hex(q);
        char* gHex = BN_bn2hex(g);

        ui->textParameters->append("===== DSA PARAMETERS (2048-bit) =====");
        ui->textParameters->append(QString("p = %1").arg(pHex));
        ui->textParameters->append(QString("q = %1").arg(qHex));
        ui->textParameters->append(QString("g = %1").arg(gHex));
        ui->textParameters->append("=====================================");

        // Giải phóng bộ nhớ hex
        OPENSSL_free(pHex);
        OPENSSL_free(qHex);
        OPENSSL_free(gHex);

        Logger::log("PARAM", "Đã sinh tham số DSA thành công (2048-bit)");
    }

    ui->btnGenerateParameters->setEnabled(true);
    ui->btnGenerateParameters->setText("Tạo tham số DSA");
    isGenerating = false;
}