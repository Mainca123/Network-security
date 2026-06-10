#include "mainwindow.h"
#include "ui_mainwindow.h"
#include "logging/logger.h"
#include "crypto/dsa_manager.h"
#include "crypto/key_generator.h"
#include "file_io/file_signer.h"
#include <QFileDialog>
#include <QMessageBox>
#include <QTemporaryFile>
#include <QFile>
#include <QTextEdit>
#include <QLineEdit>
#include <QTextStream>
#include <QDateTime>
#include <QDir>
#include <QFileInfo>
#include <QDebug>
#include <openssl/bn.h>
#include <QGraphicsDropShadowEffect>
#include <QIntValidator>
#include "dsa_handmade.h"
#include <QImageReader>

MainWindow::MainWindow(QWidget *parent) : QMainWindow(parent), ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    QFile file(":/forms/style_light.qss");
    if(file.open(QFile::ReadOnly))
    {
        this->setStyleSheet(file.readAll());
        file.close();
    }
    // Shadow cho logo
    QGraphicsDropShadowEffect *logoShadow =
        new QGraphicsDropShadowEffect(this);

    logoShadow->setBlurRadius(20);
    logoShadow->setOffset(3, 4);
    logoShadow->setColor(QColor(0, 0, 0, 80));

    ui->logoIconLabel->setGraphicsEffect(logoShadow);

    // Shadow cho tất cả button
    QList<QPushButton*> buttons = findChildren<QPushButton*>();

    for (QPushButton *btn : buttons)
    {
        QGraphicsDropShadowEffect *shadow =
            new QGraphicsDropShadowEffect(btn);

        shadow->setBlurRadius(15);
        shadow->setOffset(3, 4);
        shadow->setColor(QColor(0, 0, 0, 80));

        btn->setGraphicsEffect(shadow);
    }

    // Shadow cho tất cả frame
    QList<QFrame*> frames = findChildren<QFrame*>();

    for (QFrame *frame : frames)
    {
        QString name = frame->objectName();

        if (name == "sidebarFrame")
        {
            continue;
        }
        QGraphicsDropShadowEffect *shadow =
            new QGraphicsDropShadowEffect(frame);

        shadow->setBlurRadius(25);
        shadow->setOffset(3, 5);
        shadow->setColor(QColor(0, 0, 0, 80));

        frame->setGraphicsEffect(shadow);
    }

    ui->inputP->setValidator(
        new QIntValidator(
            DSA_Handmade::MIN_P,
            DSA_Handmade::MAX_VALUE,
            this));

    ui->inputQ->setValidator(
        new QIntValidator(
            DSA_Handmade::MIN_Q,
            DSA_Handmade::MAX_VALUE,
            this));

    ui->inputG->setValidator(
        new QIntValidator(
            DSA_Handmade::MIN_G,
            DSA_Handmade::MAX_VALUE,
            this));

    ui->inputX->setValidator(
        new QIntValidator(
            DSA_Handmade::MIN_X,
            DSA_Handmade::MAX_VALUE,
            this));
    ui->inputY->setValidator(new QIntValidator(1, DSA_Handmade::MAX_VALUE, this));
    ui->inputR->setValidator(new QIntValidator(1, DSA_Handmade::MAX_VALUE, this));
    ui->inputS->setValidator(new QIntValidator(1, DSA_Handmade::MAX_VALUE, this));

    updateStatus("Sẵn sàng");
    setupConnections();
    onNavManualClicked();
}

MainWindow::~MainWindow()
{
    delete ui;
}

// ─────────────────────────────────────────────────────────────────────────────
// setupConnections — mỗi nút chỉ nối đúng một slot, không trùng lặp
// ─────────────────────────────────────────────────────────────────────────────

void MainWindow::setupConnections()
{
    // ── Sidebar: điều hướng (mỗi nav button → đúng 1 slot nav) ──────────────
    connect(ui->btnThuCong,         &QPushButton::clicked, this, &MainWindow::onNavManualClicked);
    connect(ui->btnTaoKhoaTuDong,   &QPushButton::clicked, this, &MainWindow::onNavAutoKeyClicked);
    connect(ui->btnTaoChuKy,        &QPushButton::clicked, this, &MainWindow::onNavCreateSignatureClicked);
    connect(ui->btnXacThucChuKy,    &QPushButton::clicked, this, &MainWindow::onNavVerifySignatureClicked);
    connect(ui->btnNhatKyHeThong,   &QPushButton::clicked, this, &MainWindow::onNavSystemLogsClicked);

    // ── Trang Thủ công ────────────────────────────────────────────────────────
    // btnManualGenerate = nút "Tạo khóa" trong card Tạo khóa (tách khỏi btnManual nav)
    connect(ui->btnTaoKhoaThuCong,    &QPushButton::clicked, this, &MainWindow::onManualGenerateClicked);
    connect(ui->btnXoaKoaThuCong,     &QPushButton::clicked, this, &MainWindow::onManualClearClicked);
    connect(ui->btnKyThuCong,         &QPushButton::clicked, this, &MainWindow::onManualSignClicked);
    connect(ui->btnXoaChuKyThuCong,   &QPushButton::clicked, this, &MainWindow::onManualSignClearClicked);
    connect(ui->btnXacThucThuCong,    &QPushButton::clicked, this, &MainWindow::onManualVerifyClicked);
    connect(ui->btnXoaXacThucThuCong, &QPushButton::clicked, this, &MainWindow::onManualVerifyClearClicked);

    // ── Trang Tạo khóa tự động ───────────────────────────────────────────────
    connect(ui->btnSinhKhoaTuDong,  &QPushButton::clicked, this, &MainWindow::onAutoGenerateClicked);
    connect(ui->btnXoaKhoaTuDong,   &QPushButton::clicked, this, &MainWindow::onAutoClearClicked);
    connect(ui->btnTaiKhoa,         &QPushButton::clicked, this, &MainWindow::onAutoDownloadClicked);

    // ── Trang Tạo chữ ký (file) ──────────────────────────────────────────────
    // btnCreateSignFilePick = nút "Chọn file" trong card (tách khỏi btnCreateSignFile nav)
    connect(ui->btnChonFileKy,         &QPushButton::clicked, this, &MainWindow::onCreateChooseFileClicked);
    connect(ui->btnChonFileKhoaBiMat,  &QPushButton::clicked, this, &MainWindow::onCreateChoosePrivateKeyClicked);
    connect(ui->btnChonKhoaCongKhai,   &QPushButton::clicked, this, &MainWindow::onCreateChoosePublicKeyClicked);
    connect(ui->btnKyFlie,             &QPushButton::clicked, this, &MainWindow::onCreateSignatureClicked);
    connect(ui->btnTaiChuKy,           &QPushButton::clicked, this, &MainWindow::onCreateDownloadClicked);

    // ── Trang Xác thực chữ ký (file) ────────────────────────────────────────
    connect(ui->btnChonFileGoc,                   &QPushButton::clicked, this, &MainWindow::onVerifyChooseOriginalClicked);
    connect(ui->btnChonFileChuKy,                 &QPushButton::clicked, this, &MainWindow::onVerifyChooseSignatureClicked);
    connect(ui->btnChonFileKhoaCongKhaiXacThuc,   &QPushButton::clicked, this, &MainWindow::onVerifyChoosePublicKeyClicked);
    connect(ui->btnXacThuc,                       &QPushButton::clicked, this, &MainWindow::onVerifySignClicked);

    // ── Trang Nhật ký hệ thống ───────────────────────────────────────────────
    // lineSystemLogsSearch: gõ Enter cũng kích hoạt lọc
    connect(ui->btnLocNhatKyHeThong,    &QPushButton::clicked,  this, &MainWindow::onFilterLogsClicked);
    connect(ui->inputTimKiemNhatKy,     &QLineEdit::returnPressed, this, &MainWindow::onFilterLogsClicked);
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

void MainWindow::updateStatus(const QString &statusText)
{
    ui->btnStatus->setText(statusText);
}

void MainWindow::appendLog(QTextEdit *logWidget, const QString &message)
{
    const QString now  = QDateTime::currentDateTime().toString("dd-MM-yyyy hh:mm");
    const QString line = QString("[%1] %2").arg(now, message);
    logWidget->append(line);
    Logger::log("UI", message);
}

bool MainWindow::writeTextToTempFile(const QString &text, QString &outPath)
{
    QTemporaryFile tempFile;
    tempFile.setAutoRemove(false);   // caller cần đường dẫn tồn tại sau khi close

    if (!tempFile.open())
        return false;

    tempFile.write(text.toUtf8());
    tempFile.flush();
    outPath = tempFile.fileName();
    tempFile.close();
    return true;
}

QString MainWindow::bnToHex(const BIGNUM *number) const
{
    if (!number)
        return QString();

    char *hex = BN_bn2hex(number);
    QString result = QString::fromUtf8(hex);
    OPENSSL_free(hex);
    return result;
}

bool MainWindow::parseDecimalBigNumber(QLineEdit *lineEdit,
                                       BIGNUM   **outBn,
                                       const QString &paramName,
                                       QString  &errorMessage)
{
    const QString text = lineEdit->text().trimmed();

    if (text.isEmpty()) {
        errorMessage = QString("%1 không được để trống.").arg(paramName);
        return false;
    }

    BIGNUM *number = BN_new();
    if (!BN_dec2bn(&number, text.toStdString().c_str())) {
        BN_free(number);
        errorMessage = QString("Giá trị %1 không hợp lệ.").arg(paramName);
        return false;
    }

    if (BN_is_zero(number) || BN_is_negative(number)) {
        BN_free(number);
        errorMessage = QString("%1 phải là số nguyên dương.").arg(paramName);
        return false;
    }

    *outBn = number;
    return true;
}

bool MainWindow::saveKeyPairToDirectory(const QString &directoryPath)
{
    if (directoryPath.isEmpty())
        return false;

    const QDir dir(directoryPath);
    if (!dir.exists())
        return false;

    const QString privatePath = dir.filePath("dsa_private_key.txt");
    const QString publicPath  = dir.filePath("dsa_public_key.txt");

    if (!saveTextToFile(privatePath,
                        QString("p=%1\nq=%2\ng=%3\nx=%4\n")
                            .arg(autoPhex, autoQhex, autoGhex, autoXhex)))
        return false;

    if (!saveTextToFile(publicPath,
                        QString("p=%1\nq=%2\ng=%3\ny=%4\n")
                            .arg(autoPhex, autoQhex, autoGhex, autoYhex)))
        return false;

    return true;
}

bool MainWindow::saveTextToFile(const QString &filePath, const QString &text)
{
    QFile file(filePath);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Text))
        return false;

    QTextStream out(&file);
    out << text;
    file.close();
    return true;
}

void MainWindow::readLogFiles(const QString &filterText)
{
    ui->textNhatKyHeThong->clear();

    QDir logsDir("logs");
    if (!logsDir.exists()) {
        ui->textNhatKyHeThong->setPlainText("Không tìm thấy thư mục logs.");
        return;
    }

    const QStringList files = logsDir.entryList(
        QStringList() << "*.log", QDir::Files, QDir::Name);

    if (files.isEmpty()) {
        ui->textNhatKyHeThong->setPlainText("Không có nhật ký nào.");
        return;
    }

    const QString needle = filterText.trimmed();
    for (const QString &fileName : files) {
        QFile file(logsDir.filePath(fileName));
        if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
            continue;

        QTextStream in(&file);
        while (!in.atEnd()) {
            const QString line = in.readLine();
            if (needle.isEmpty() || line.contains(needle, Qt::CaseInsensitive))
                ui->textNhatKyHeThong->append(line);
        }
        file.close();
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Điều hướng Sidebar
// ─────────────────────────────────────────────────────────────────────────────

void MainWindow::onNavManualClicked()
{
    ui->btnThuCong->setChecked(true);
    ui->btnTaoKhoaTuDong->setChecked(false);
    ui->btnTaoChuKy->setChecked(false);
    ui->btnXacThucChuKy->setChecked(false);
    ui->btnNhatKyHeThong->setChecked(false);

    ui->stackedWidget->setCurrentWidget(ui->pageThuCong);
    ui->pageTitle->setText("Chế độ thủ công");
    ;
}

void MainWindow::onNavAutoKeyClicked()
{
    ui->btnThuCong->setChecked(false);
    ui->btnTaoKhoaTuDong->setChecked(true);
    ui->btnTaoChuKy->setChecked(false);
    ui->btnXacThucChuKy->setChecked(false);
    ui->btnNhatKyHeThong->setChecked(false);

    ui->stackedWidget->setCurrentWidget(ui->pageTaoKhoaTuDong);
    ui->pageTitle->setText("Tạo khóa tự động");
    ;
}

void MainWindow::onNavCreateSignatureClicked()
{
    ui->btnThuCong->setChecked(false);
    ui->btnTaoKhoaTuDong->setChecked(false);
    ui->btnTaoChuKy->setChecked(true);
    ui->btnXacThucChuKy->setChecked(false);
    ui->btnNhatKyHeThong->setChecked(false);

    ui->stackedWidget->setCurrentWidget(ui->pageTaoChuKy);
    ui->pageTitle->setText("Tạo chữ ký");
    ;
}

void MainWindow::onNavVerifySignatureClicked()
{
    ui->btnThuCong->setChecked(false);
    ui->btnTaoKhoaTuDong->setChecked(false);
    ui->btnTaoChuKy->setChecked(false);
    ui->btnXacThucChuKy->setChecked(true);
    ui->btnNhatKyHeThong->setChecked(false);

    ui->stackedWidget->setCurrentWidget(ui->pageXacThucChuKy);
    ui->pageTitle->setText("Xác thực chữ ký");
    ;
}

void MainWindow::onNavSystemLogsClicked()
{
    ui->btnThuCong->setChecked(false);
    ui->btnTaoKhoaTuDong->setChecked(false);
    ui->btnTaoChuKy->setChecked(false);
    ui->btnXacThucChuKy->setChecked(false);
    ui->btnNhatKyHeThong->setChecked(true);

    ui->stackedWidget->setCurrentWidget(ui->pageNhatKy);
    ui->pageTitle->setText("Nhật ký hệ thống");

    readLogFiles(ui->inputTimKiemNhatKy->text());
}

// ─────────────────────────────────────────────────────────────────────────────
// Trang Thủ công — Tạo khóa
// ─────────────────────────────────────────────────────────────────────────────

void MainWindow::onManualGenerateClicked()
{
    QString error;

    long long p;
    long long q;
    long long g;
    long long x;

    // ===== P =====
    if(!DSA_Handmade::parseValue(
            ui->inputP->text(),
            p,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi tham số P",
            error);

        ui->inputP->setFocus();
        return;
    }

    // ===== Q =====
    if(!DSA_Handmade::parseValue(
            ui->inputQ->text(),
            q,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi tham số Q",
            error);

        ui->inputQ->setFocus();
        return;
    }

    // ===== G =====
    if(!DSA_Handmade::parseValue(
            ui->inputG->text(),
            g,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi tham số G",
            error);

        ui->inputG->setFocus();
        return;
    }

    // ===== X =====
    if(!DSA_Handmade::parseValue(
            ui->inputX->text(),
            x,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi tham số X",
            error);

        ui->inputX->setFocus();
        return;
    }

    // ===== Kiểm tra P =====
    if(!DSA_Handmade::validateP(
            p,
            error))
    {
        QMessageBox::warning(
            this,
            "P không hợp lệ",
            error);

        ui->inputP->setFocus();
        return;
    }

    // ===== Kiểm tra Q =====
    if(!DSA_Handmade::validateQ(
            p,
            q,
            error))
    {
        QMessageBox::warning(
            this,
            "Q không hợp lệ",
            error);

        ui->inputQ->setFocus();
        return;
    }

    // ===== Kiểm tra G =====
    if(!DSA_Handmade::validateG(
            p,
            q,
            g,
            error))
    {
        QMessageBox::warning(
            this,
            "G không hợp lệ",
            error);

        ui->inputG->setFocus();
        return;
    }

    // ===== Kiểm tra X =====
    if(!DSA_Handmade::validateX(
            q,
            x,
            error))
    {
        QMessageBox::warning(
            this,
            "X không hợp lệ",
            error);

        ui->inputX->setFocus();
        return;
    }

    // ===== Sinh khóa công khai =====
    long long y =
        DSA_Handmade::generatePublicKey(
            p,
            g,
            x);

    ui->inputY->setText(
        QString::number(y));

    appendLog(
        ui->textTrangThaiThaoTacThuCong,
        "Đã tạo khóa thủ công thành công.");

    Logger::log(
        "Manual",
        QString("P=%1 Q=%2 G=%3 X=%4 Y=%5")
            .arg(p)
            .arg(q)
            .arg(g)
            .arg(x)
            .arg(y));
}

void MainWindow::onManualClearClicked()
{
    ui->inputP->clear();
    ui->inputQ->clear();
    ui->inputG->clear();
    ui->inputX->clear();
    appendLog(ui->textTrangThaiThaoTacThuCong, "Đã xóa toàn bộ dữ liệu thủ công.");
}

// ─────────────────────────────────────────────────────────────────────────────
// Trang Thủ công — Ký
// ─────────────────────────────────────────────────────────────────────────────
void MainWindow::onManualSignClicked()
{
    QString error;

    long long p;
    long long q;
    long long g;
    long long x;

    if(!DSA_Handmade::parseValue(
            ui->inputP->text(),
            p,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi P",
            error);
        return;
    }

    if(!DSA_Handmade::parseValue(
            ui->inputQ->text(),
            q,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi Q",
            error);
        return;
    }

    if(!DSA_Handmade::parseValue(
            ui->inputG->text(),
            g,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi G",
            error);
        return;
    }

    if(!DSA_Handmade::parseValue(
            ui->inputX->text(),
            x,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi X",
            error);
        return;
    }

    if(!DSA_Handmade::validateAll(
            p,q,g,x,error))
    {
        QMessageBox::warning(
            this,
            "DSA không hợp lệ",
            error);
        return;
    }

    QString message =
        ui->inputVanBanKy
            ->toPlainText()
            .trimmed();

    if(message.isEmpty())
    {
        QMessageBox::warning(
            this,
            "Lỗi",
            "Vui lòng nhập nội dung cần ký.");
        return;
    }

    long long r;
    long long s;

    bool ok =
        DSA_Handmade::generateSignature(
            message,
            p,
            q,
            g,
            x,
            r,
            s);

    if(!ok)
    {
        QMessageBox::warning(
            this,
            "Lỗi",
            "Không thể tạo chữ ký.");
        return;
    }

    ui->inputR->setText(QString::number(r));
    ui->inputS->setText(QString::number(s));
    appendLog(
        ui->textTrangThaiThaoTacThuCong,
        QString(
            "\n----- CHỮ KÝ DSA -----\n"
            "r = %1\n"
            "s = %2")
            .arg(r)
            .arg(s));

    Logger::log(
        "Manual.Sign",
        QString(
            "Message=%1 | r=%2 | s=%3")
            .arg(message)
            .arg(r)
            .arg(s));
}

void MainWindow::onManualSignClearClicked()
{
    ui->inputY->clear();
    ui->inputVanBanKy->clear();
    appendLog(ui->textTrangThaiThaoTacThuCong, "Đã xóa nội dung ký.");
}

// ─────────────────────────────────────────────────────────────────────────────
// Trang Thủ công — Xác thực
// ─────────────────────────────────────────────────────────────────────────────

void MainWindow::onManualVerifyClicked()
{
    QString error;

    long long p;
    long long q;
    long long g;
    long long y;
    long long r;
    long long s;

    if(!DSA_Handmade::parseValue(
            ui->inputP->text(),
            p,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi P",
            error);
        return;
    }

    if(!DSA_Handmade::parseValue(
            ui->inputQ->text(),
            q,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi Q",
            error);
        return;
    }

    if(!DSA_Handmade::parseValue(
            ui->inputG->text(),
            g,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi G",
            error);
        return;
    }

    if(!DSA_Handmade::parseValue(
            ui->inputY->text(),
            y,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi Y",
            error);
        return;
    }

    if(!DSA_Handmade::parseValue(
            ui->inputR->text(),
            r,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi R",
            error);
        return;
    }

    if(!DSA_Handmade::parseValue(
            ui->inputS->text(),
            s,
            error))
    {
        QMessageBox::warning(
            this,
            "Lỗi S",
            error);
        return;
    }

    QString message =
        ui->inputVanBanXacThuc
            ->toPlainText()
            .trimmed();

    if(message.isEmpty())
    {
        QMessageBox::warning(
            this,
            "Lỗi",
            "Vui lòng nhập văn bản cần xác thực.");
        return;
    }

    bool verified =
        DSA_Handmade::verifySignature(
            message,
            p,
            q,
            g,
            y,
            r,
            s);

    QString resultText;

    if(verified)
    {
        resultText = "Chữ ký hợp lệ";

        QMessageBox::information(
            this,
            "Xác thực thành công",
            resultText);
    }
    else
    {
        resultText = "Chữ ký không hợp lệ";

        QMessageBox::warning(
            this,
            "Xác thực thất bại",
            resultText);
    }

    appendLog(
        ui->textTrangThaiThaoTacThuCong,
        resultText);

    Logger::log(
        "Manual.Verify",
        resultText);
}

void MainWindow::onManualVerifyClearClicked()
{
    ui->inputR->clear();
    ui->inputS->clear();
    ui->inputVanBanXacThuc->clear();
    appendLog(ui->textTrangThaiThaoTacThuCong, "Đã xóa dữ liệu xác thực.");
}

// ─────────────────────────────────────────────────────────────────────────────
// Trang Tạo khóa tự động
// ─────────────────────────────────────────────────────────────────────────────

void MainWindow::onAutoGenerateClicked()
{
    DSAManager dsa;
    if (!dsa.generateParameters(1024)) {
        QMessageBox::warning(this, "Lỗi", "Không thể tạo tham số DSA.");
        return;
    }

    autoPhex = bnToHex(dsa.getP());
    autoQhex = bnToHex(dsa.getQ());
    autoGhex = bnToHex(dsa.getG());

    KeyGenerator generator;
    BIGNUM *x = generator.generatePrivateKey(dsa.getQ());
    BIGNUM *y = generator.generatePublicKey(dsa.getG(), x, dsa.getP());
    autoXhex = bnToHex(x);
    autoYhex = bnToHex(y);

    // Hiển thị tham số — cắt ngắn nếu quá dài để UI không vỡ layout
    auto truncate = [](const QString &s, int maxLen = 60) -> QString {
        return s.length() > maxLen ? s.left(maxLen) + "…" : s;
    };

    QString keyInfo;
    keyInfo += "=== THAM SỐ DSA ===\n\n";
    keyInfo += "P:\n" + autoPhex + "\n\n";
    keyInfo += "Q:\n" + autoQhex + "\n\n";
    keyInfo += "G:\n" + autoGhex + "\n\n";
    keyInfo += "=== KHÓA ===\n\n";
    keyInfo += "Private Key (X):\n" + autoXhex + "\n\n";
    keyInfo += "Public Key (Y):\n" + autoYhex + "\n";

    ui->textHienThiKhoaTuDong->setPlainText(keyInfo);

    BN_free(x);
    BN_free(y);
}

void MainWindow::onAutoClearClicked()
{

    ui->textHienThiKhoaTuDong->clear();
    autoPhex.clear(); autoQhex.clear(); autoGhex.clear();
    autoXhex.clear(); autoYhex.clear();
}

void MainWindow::onAutoDownloadClicked()
{
    if (autoPhex.isEmpty() || autoQhex.isEmpty() ||
        autoGhex.isEmpty() || autoXhex.isEmpty() || autoYhex.isEmpty())
    {
        QMessageBox::information(this, "Thông báo",
                                 "Vui lòng tạo khóa tự động trước khi tải xuống.");
        return;
    }

    const QString directory =
        QFileDialog::getExistingDirectory(this, "Chọn thư mục lưu khóa");
    if (directory.isEmpty())
        return;

    if (!saveKeyPairToDirectory(directory)) {
        QMessageBox::warning(this, "Lỗi",
                             "Không thể lưu khóa vào thư mục đã chọn.");
        return;
    }
    QMessageBox::information(this, "Thông báo",
                             "Lưu khóa thành công");
}

// ─────────────────────────────────────────────────────────────────────────────
// Trang Tạo chữ ký (file)
// ─────────────────────────────────────────────────────────────────────────────

#include "file_io/file_renderer.h"

void MainWindow::onCreateChooseFileClicked()
{
    const QString path = QFileDialog::getOpenFileName(
        this, "Chọn file cần ký", "",
        "Tất cả file được hỗ trợ "
        "(*.png *.jpg *.jpeg *.bmp *.pdf *.txt *.log *.csv *.json *.xml "
        "*.docx *.doc *.odt);;"
        "Images (*.png *.jpg *.jpeg *.bmp);;"
        "PDF (*.pdf);;"
        "Word (*.docx *.doc *.odt);;"
        "Text (*.txt *.log *.csv *.json *.xml)");
    if (path.isEmpty()) return;

    currentCreateFilePath = path;
    ui->inputDuongDanFileCanKy->setText(path);
    QApplication::processEvents();

    int vpW = ui->hienThiFileCanKy->viewport()->width();
    int vpH = ui->hienThiFileCanKy->viewport()->height();

    // *** Render với kích thước THỰC của frame, không phải viewport ***
    const int PAGE_SPACING = 3;
    const int PAGE_PADDING = 3;
    const int FRAME_X      = 15;
    const int FRAME_W      = vpW - 40;   // phải khớp với setGeometry bên dưới
    const int FRAME_H      = vpH - 10;

    // Truyền kích thước frame thực vào renderer
    QList<QPixmap> pages = FileRenderer::render(path, FRAME_W, FRAME_H);
    if (pages.isEmpty()) {
        QMessageBox::warning(this, "Lỗi", "Không thể đọc file này.");
        return;
    }

    QWidget* container = new QWidget;
    int totalH = (FRAME_H + PAGE_SPACING) * pages.size() + PAGE_SPACING;
    container->setFixedSize(vpW, totalH);

    for (int i = 0; i < pages.size(); ++i) {
        QFrame* pageFrame = new QFrame(container);
        int yPos = PAGE_SPACING + i * (FRAME_H + PAGE_SPACING);

        // Kích thước frame
        pageFrame->setGeometry(FRAME_X, yPos, FRAME_W, FRAME_H);
        pageFrame->setStyleSheet(
            "background: white;"
            "border: 1px solid #DADADA;"
            "border-radius: 6px;");

        // imageLabel vừa khít frame, trừ padding
        int labelW = FRAME_W - PAGE_PADDING * 2;
        int labelH = FRAME_H - PAGE_PADDING * 2;

        QLabel* imageLabel = new QLabel(pageFrame);
        imageLabel->setGeometry(PAGE_PADDING, PAGE_PADDING, labelW, labelH);
        imageLabel->setAlignment(Qt::AlignCenter);
        imageLabel->setStyleSheet("border: none; background: transparent;");

        // Scale pixmap vừa label — giờ labelW/labelH đã đúng
        imageLabel->setPixmap(
            pages[i].scaled(
                labelW, labelH,
                Qt::KeepAspectRatio,
                Qt::SmoothTransformation));
    }

    ui->hienThiFileCanKy->setWidget(container);
}

void MainWindow::onCreateChoosePrivateKeyClicked()
{
    const QString path = QFileDialog::getOpenFileName(
        this, "Chọn khóa bí mật", QString(), "Text Files (*.txt);;All Files (*)");
    if (path.isEmpty())
        return;

    currentCreatePrivateKeyPath = path;
    ui->inputDuongDanKhoaBiMat->setText(path);
}

void MainWindow::onCreateChoosePublicKeyClicked()
{
    const QString path = QFileDialog::getOpenFileName(
        this, "Chọn khóa công khai", QString(), "Text Files (*.txt);;All Files (*)");
    if (path.isEmpty())
        return;

    currentCreatePublicKeyPath = path;
    ui->inputDuongDanKhoaCongKhai->setText(path);
}

void MainWindow::onCreateSignatureClicked()
{
    if (currentCreateFilePath.isEmpty()) {
        QMessageBox::warning(this, "Lỗi",
                             "Vui lòng chọn file cần ký trước khi tạo chữ ký.");
        return;
    }
    if (currentCreatePrivateKeyPath.isEmpty()) {
        QMessageBox::warning(this, "Lỗi",
                             "Vui lòng chọn khóa bí mật trước khi tạo chữ ký.");
        return;
    }

    if (currentCreatePublicKeyPath.isEmpty()) {
        QMessageBox::warning(this, "Lỗi",
                             "Vui lòng chọn khóa công khai trước khi tạo chữ ký.");
        return;
    }

    const QFileInfo fileInfo(currentCreateFilePath);
    const QString signaturePath = fileInfo.absoluteFilePath() + ".sig";

    if (!FileSigner::signFile(currentCreateFilePath,
                              currentCreatePrivateKeyPath,
                              signaturePath))
    {
        appendLog(ui->textTrangThaiThaoTacKyFile, "Tạo chữ ký thất bại.");
        QMessageBox::warning(this, "Lỗi", "Tạo chữ ký thất bại.");
        return;
    }

    lastCreateSignaturePath = signaturePath;
    appendLog(ui->textTrangThaiThaoTacKyFile,
        QString("Đã tạo chữ ký thành công: %1").arg(signaturePath));
}

void MainWindow::onCreateDownloadClicked()
{
    if (lastCreateSignaturePath.isEmpty() ||
        !QFile::exists(lastCreateSignaturePath))
    {
        QMessageBox::information(this, "Thông báo",
            "Chưa có chữ ký nào để tải xuống. Vui lòng tạo chữ ký trước.");
        return;
    }

    const QString savePath = QFileDialog::getSaveFileName(
        this, "Lưu file chữ ký", lastCreateSignaturePath,
        "Signature Files (*.sig);;All Files (*)");
    if (savePath.isEmpty())
        return;

    if (savePath != lastCreateSignaturePath && !QFile::copy(lastCreateSignaturePath, savePath)) {
        QMessageBox::warning(this, "Lỗi",
            "Không thể lưu chữ ký đến đường dẫn đã chọn.");
        return;
    }

    appendLog(ui->textTrangThaiThaoTacKyFile,
        QString("Đã lưu chữ ký tới: %1").arg(savePath));
    ;
}

// ─────────────────────────────────────────────────────────────────────────────
// Trang Xác thực chữ ký (file)
// ─────────────────────────────────────────────────────────────────────────────

void MainWindow::onVerifyChooseOriginalClicked()
{
    const QString path = QFileDialog::getOpenFileName(
        this, "Chọn file cần ký", "",
        "Tất cả file được hỗ trợ "
        "(*.png *.jpg *.jpeg *.bmp *.pdf *.txt *.log *.csv *.json *.xml "
        "*.docx *.doc *.odt);;"
        "Images (*.png *.jpg *.jpeg *.bmp);;"
        "PDF (*.pdf);;"
        "Word (*.docx *.doc *.odt);;"
        "Text (*.txt *.log *.csv *.json *.xml)");
    if (path.isEmpty()) return;

    currentVerifyOriginalFilePath = path;
    ui->inputDuongDanFileGoc->setText(path);

    QApplication::processEvents();
    int vpW = ui->hienThiVanBanGoc->viewport()->width();
    int vpH = ui->hienThiVanBanGoc->viewport()->height();


    QList<QPixmap> pages = FileRenderer::render(path, vpW, vpH);
    if (pages.isEmpty()) {
        QMessageBox::warning(this, "Lỗi", "Không thể đọc file này.");
        return;
    }


    const int PAGE_SPACING = 3;
    const int PAGE_PADDING = 3;

    QWidget* container = new QWidget;
    int totalH = (vpH + PAGE_SPACING) * pages.size() + PAGE_SPACING;
    container->setFixedSize(vpW, totalH);

    for (int i = 0; i < pages.size(); ++i) {
        QFrame* pageFrame = new QFrame(container);
        int yPos = PAGE_SPACING + i * (vpH + PAGE_SPACING);
        pageFrame->setGeometry(15, yPos, vpW-40, vpH-10);
        pageFrame->setStyleSheet(
            "background: white;"
            "border: 1px solid #DADADA;"
            "border-radius: 6px;");

        QLabel* imageLabel = new QLabel(pageFrame);
        imageLabel->setGeometry(
            PAGE_PADDING, PAGE_PADDING,
            vpW - PAGE_PADDING * 2,
            vpH - PAGE_PADDING * 2);
        imageLabel->setAlignment(Qt::AlignCenter);
        imageLabel->setStyleSheet("border: none; background: transparent;");
        imageLabel->setPixmap(
            pages[i].scaled(
                imageLabel->width(),
                imageLabel->height(),
                Qt::KeepAspectRatio,
                Qt::SmoothTransformation));
    }

    ui->hienThiVanBanGoc->setWidget(container);
}

void MainWindow::onVerifyChooseSignatureClicked()
{
    const QString path = QFileDialog::getOpenFileName(
        this, "Chọn file chữ ký", QString(),
        "Signature Files (*.sig);;All Files (*)");
    if (path.isEmpty())
        return;

    currentVerifySignaturePath = path;
    ui->inputDuongDanFileChuKy->setText(path);
}

void MainWindow::onVerifyChoosePublicKeyClicked()
{
    const QString path = QFileDialog::getOpenFileName(
        this, "Chọn khóa công khai", QString(),
        "Text Files (*.txt);;All Files (*)");
    if (path.isEmpty())
        return;

    currentVerifyPublicKeyPath = path;
    ui->inputDuongDanFileKhoaCongKhaiXacThuc->setText(path);
}

void MainWindow::onVerifySignClicked()
{
    if (currentVerifyOriginalFilePath.isEmpty())
    {
        QMessageBox::warning(
            this,
            "Thiếu file",
            "Vui lòng chọn file gốc cần xác thực.");
        return;
    }

    if (currentVerifySignaturePath.isEmpty())
    {
        QMessageBox::warning(
            this,
            "Thiếu chữ ký",
            "Vui lòng chọn file chữ ký (.sig).");
        return;
    }

    if (currentVerifyPublicKeyPath.isEmpty())
    {
        QMessageBox::warning(
            this,
            "Thiếu khóa công khai",
            "Vui lòng chọn khóa công khai.");
        return;
    }

    const bool verified = FileSigner::verifyFile(
        currentVerifyOriginalFilePath,
        currentVerifyPublicKeyPath,
        currentVerifySignaturePath);

    const QString message = verified
                                ? "Xác thực chữ ký thành công."
                                : "Xác thực chữ ký thất bại.";

    appendLog(ui->textTrangThaiThaoTacXacThuc, message);
    lastVerifyResultMessage = message;
    Logger::log("Verify.File", message);
}

void MainWindow::onVerifyDownloadClicked()
{
    if (lastVerifyResultMessage.isEmpty()) {
        QMessageBox::information(this, "Thông báo",
                                 "Chưa có kết quả xác thực để lưu. Vui lòng xác thực trước.");
        return;
    }

    const QString savePath = QFileDialog::getSaveFileName(
        this, "Lưu kết quả xác thực", "verify_result.txt",
        "Text Files (*.txt);;All Files (*)");
    if (savePath.isEmpty())
        return;

    if (!saveTextToFile(savePath, lastVerifyResultMessage)) {
        QMessageBox::warning(this, "Lỗi",
                             "Không thể lưu kết quả xác thực.");
        return;
    }

    appendLog(ui->textTrangThaiThaoTacXacThuc,
              QString("Đã lưu kết quả xác thực tới: %1").arg(savePath));
}

// ─────────────────────────────────────────────────────────────────────────────
// Trang Nhật ký hệ thống
// ─────────────────────────────────────────────────────────────────────────────

void MainWindow::onFilterLogsClicked()
{
    const QString keyword = ui->inputTimKiemNhatKy->text().trimmed();
    ui->textNhatKyHeThong->setText(keyword);
    readLogFiles(keyword);
}