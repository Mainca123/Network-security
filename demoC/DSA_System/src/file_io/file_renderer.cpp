#include "file_renderer.h"

#include <QImageReader>
#include <QPdfDocument>
#include <QPainter>
#include <QFont>
#include <QFontMetrics>
#include <QFile>
#include <QTextStream>
#include <QStringList>
#include <QProcess>
#include <QDir>
#include <QStandardPaths>
#include <QUuid>

// ─────────────────────────────────────────────
// Helpers kiểm tra loại file
// ─────────────────────────────────────────────
bool FileRenderer::isPdf(const QString& path)
{
    return path.endsWith(".pdf", Qt::CaseInsensitive);
}

bool FileRenderer::isImage(const QString& path)
{
    static const QStringList exts = {
        ".png", ".jpg", ".jpeg", ".bmp",
        ".gif", ".webp", ".tiff"
    };
    for (const auto& ext : exts)
        if (path.endsWith(ext, Qt::CaseInsensitive))
            return true;
    return false;
}

bool FileRenderer::isTxt(const QString& path)
{
    return path.endsWith(".txt",  Qt::CaseInsensitive)
    || path.endsWith(".log",  Qt::CaseInsensitive)
        || path.endsWith(".csv",  Qt::CaseInsensitive)
        || path.endsWith(".json", Qt::CaseInsensitive)
        || path.endsWith(".xml",  Qt::CaseInsensitive);
}

bool FileRenderer::isDocx(const QString& path)
{
    return path.endsWith(".docx", Qt::CaseInsensitive)
    || path.endsWith(".doc",  Qt::CaseInsensitive)
        || path.endsWith(".odt",  Qt::CaseInsensitive);  // bonus
}

// ─────────────────────────────────────────────
// Tìm đường dẫn LibreOffice tự động
// ─────────────────────────────────────────────
namespace
{
    QString findLibreOfficePath()
    {
        // Các đường dẫn phổ biến trên Windows
        const QStringList candidates = {
            "C:/Program Files/LibreOffice/program/soffice.exe",
            "C:/Program Files (x86)/LibreOffice/program/soffice.exe",
            "C:/msys64/ucrt64/bin/soffice.exe",
            // Linux/Mac (dùng thêm nếu cần)
            "/usr/bin/soffice",
            "/usr/lib/libreoffice/program/soffice",
            "/Applications/LibreOffice.app/Contents/MacOS/soffice"
        };

        for (const QString& p : candidates)
            if (QFile::exists(p))
                return p;

        return {}; // không tìm thấy
    }
}

// ─────────────────────────────────────────────
// Convert DOCX → PDF tạm → render
// ─────────────────────────────────────────────
static QList<QPixmap> renderDocx(const QString& path, int vpW, int vpH)
{
    QString soffice = findLibreOfficePath();
    if (soffice.isEmpty()) {
        qWarning() << "[FileRenderer] Không tìm thấy LibreOffice!";
        return {};
    }

    // Tạo thư mục tạm riêng để tránh xung đột
    QString tempDir = QDir::tempPath()
                      + "/dsa_preview_"
                      + QUuid::createUuid().toString(QUuid::Id128).left(8);
    QDir().mkpath(tempDir);

    // Chạy LibreOffice convert: docx → pdf
    QProcess proc;
    proc.start(soffice, {
                            "--headless",           // không mở GUI
                            "--convert-to", "pdf",  // định dạng đầu ra
                            "--outdir", tempDir,    // thư mục output
                            path                    // file đầu vào
                        });

    // Chờ tối đa 30 giây
    if (!proc.waitForFinished(30000)) {
        qWarning() << "[FileRenderer] LibreOffice timeout!";
        proc.kill();
        return {};
    }

    // Tìm file PDF vừa tạo
    QFileInfo fi(path);
    QString pdfPath = tempDir + "/" + fi.completeBaseName() + ".pdf";

    if (!QFile::exists(pdfPath)) {
        qWarning() << "[FileRenderer] PDF output không tồn tại:" << pdfPath;
        return {};
    }

    // Render PDF bằng QPdfDocument
    QPdfDocument doc;
    if (doc.load(pdfPath) != QPdfDocument::Error::None) {
        QFile::remove(pdfPath);
        QDir(tempDir).removeRecursively();
        return {};
    }

    QList<QPixmap> pages;
    for (int i = 0; i < doc.pageCount(); ++i) {
        QSizeF pageSize = doc.pagePointSize(i);
        double scale = qMin(vpW / pageSize.width(),
                            vpH / pageSize.height());
        QSize renderSize(qRound(pageSize.width()  * scale),
                         qRound(pageSize.height() * scale));

        QImage img = doc.render(i, renderSize);
        if (!img.isNull())
            pages.append(QPixmap::fromImage(img));
    }

    // Dọn file tạm
    QFile::remove(pdfPath);
    QDir(tempDir).removeRecursively();

    return pages;
}

// ─────────────────────────────────────────────
// Render ảnh
// ─────────────────────────────────────────────
static QList<QPixmap> renderImage(const QString& path, int vpW, int vpH)
{
    QImageReader reader(path);
    reader.setAutoTransform(true);
    QImage img = reader.read();
    if (img.isNull()) return {};

    return { QPixmap::fromImage(img).scaled(
        vpW, vpH,
        Qt::KeepAspectRatio,
        Qt::SmoothTransformation) };
}

// ─────────────────────────────────────────────
// Render PDF
// ─────────────────────────────────────────────
static QList<QPixmap> renderPdf(const QString& path, int vpW, int vpH)
{
    QList<QPixmap> pages;
    QPdfDocument doc;
    if (doc.load(path) != QPdfDocument::Error::None)
        return pages;

    for (int i = 0; i < doc.pageCount(); ++i) {
        QSizeF pageSize = doc.pagePointSize(i);
        double scale = qMin(vpW / pageSize.width(),
                            vpH / pageSize.height());
        QSize renderSize(qRound(pageSize.width()  * scale),
                         qRound(pageSize.height() * scale));

        QImage img = doc.render(i, renderSize);
        if (!img.isNull())
            pages.append(QPixmap::fromImage(img));
    }
    return pages;
}

// ─────────────────────────────────────────────
// Render TXT
// ─────────────────────────────────────────────
static QString decodeWindows1258(const QByteArray& raw)
{
    // Bảng map CP1258 → Unicode cho vùng 0x80–0xFF
    // Các byte 0x00–0x7F giống ASCII, chỉ cần map phần mở rộng
    static const char16_t cp1258map[128] = {
        // 0x80
        0x20AC, 0xFFFD, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021,
        0x02C6, 0x2030, 0xFFFD, 0x2039, 0x0152, 0xFFFD, 0xFFFD, 0xFFFD,
        // 0x90
        0xFFFD, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
        0x02DC, 0x2122, 0xFFFD, 0x203A, 0x0153, 0xFFFD, 0xFFFD, 0x0178,
        // 0xA0
        0x00A0, 0x00A1, 0x00A2, 0x00A3, 0x00A4, 0x00A5, 0x00A6, 0x00A7,
        0x00A8, 0x00A9, 0x00AA, 0x00AB, 0x00AC, 0x00AD, 0x00AE, 0x00AF,
        // 0xB0
        0x00B0, 0x00B1, 0x00B2, 0x00B3, 0x00B4, 0x00B5, 0x00B6, 0x00B7,
        0x00B8, 0x00B9, 0x00BA, 0x00BB, 0x00BC, 0x00BD, 0x00BE, 0x00BF,
        // 0xC0
        0x00C0, 0x00C1, 0x00C2, 0x0102, 0x00C4, 0x00C5, 0x00C6, 0x00C7,
        0x00C8, 0x00C9, 0x00CA, 0x00CB, 0x0300, 0x00CD, 0x00CE, 0x00CF,
        // 0xD0
        0x0110, 0x00D1, 0x0309, 0x00D3, 0x00D4, 0x01A0, 0x00D6, 0x00D7,
        0x00D8, 0x00D9, 0x00DA, 0x00DB, 0x00DC, 0x01AF, 0x0303, 0x00DF,
        // 0xE0
        0x00E0, 0x00E1, 0x00E2, 0x0103, 0x00E4, 0x00E5, 0x00E6, 0x00E7,
        0x00E8, 0x00E9, 0x00EA, 0x00EB, 0x0301, 0x00ED, 0x00EE, 0x00EF,
        // 0xF0
        0x0111, 0x00F1, 0x0323, 0x00F3, 0x00F4, 0x01A1, 0x00F6, 0x00F7,
        0x00F8, 0x00F9, 0x00FA, 0x00FB, 0x00FC, 0x01B0, 0x20AB, 0x00FF,
    };

    QString result;
    result.reserve(raw.size());
    for (unsigned char c : raw) {
        if (c < 0x80)
            result += QChar(c);
        else
            result += QChar(cp1258map[c - 0x80]);
    }
    // Normalize: gộp combining marks (dấu tách) thành ký tự dựng sẵn
    return result.normalized(QString::NormalizationForm_C);
}
static QList<QPixmap> renderTxt(const QString& path, int vpW, int vpH)
{

    // DEBUG — xóa sau khi fix xong
    QFile dbg(path);
    dbg.open(QIODevice::ReadOnly);
    QByteArray rawDbg = dbg.readAll();
    dbg.close();

    qDebug() << "=== RAW HEX (bytes 0-40) ===" << rawDbg.left(40).toHex(' ');

    // Tìm byte đầu tiên > 0x7F (ký tự non-ASCII)
    for (int i = 0; i < rawDbg.size(); ++i) {
        unsigned char c = (unsigned char)rawDbg[i];
        if (c > 0x7F) {
            qDebug() << "First non-ASCII at byte" << i
                     << "= 0x" + QString::number(c, 16).toUpper()
                     << "context:" << rawDbg.mid(i-2, 8).toHex(' ');
            break;
        }
    }

    // So sánh 3 cách decode
    QString asUtf8    = QString::fromUtf8(rawDbg);
    QString asLocal   = QString::fromLocal8Bit(rawDbg);
    QStringDecoder cp1258dec("Windows-1258");
    QString as1258;
    if (cp1258dec.isValid()) {
        as1258 = QString(cp1258dec(rawDbg));
    } else {
        as1258 = "N/A";
    }

    qDebug() << "fromUtf8   (100):" << asUtf8.left(100);
    qDebug() << "fromLocal8Bit(100):" << asLocal.left(100);
    qDebug() << "Windows-1258(100):" << as1258.left(100);
    qDebug() << "cp1258 isValid:" << cp1258dec.isValid();

    // ── Đọc file và tự detect encoding ──────────────────────────────────
    QFile file(path);
    if (!file.open(QIODevice::ReadOnly))
        return {};
    QByteArray raw = file.readAll();
    file.close();

    QString fullText;

    if (raw.startsWith("\xFF\xFE")) {
        fullText = QString::fromUtf16(
            reinterpret_cast<const char16_t*>(raw.constData() + 2),
            (raw.size() - 2) / 2);
    }
    else if (raw.startsWith("\xFE\xFF")) {
        QStringDecoder dec(QStringConverter::Utf16BE);
        fullText = dec(raw.mid(2));
    }
    else if (raw.startsWith("\xEF\xBB\xBF")) {
        fullText = QString::fromUtf8(raw.constData() + 3, raw.size() - 3)
        .normalized(QString::NormalizationForm_C);
    }
    else {
        QString attempt = QString::fromUtf8(raw);
        if (!attempt.contains(QChar(0xFFFD))) {
            // UTF-8 hợp lệ
            fullText = attempt.normalized(QString::NormalizationForm_C);
        } else {
            // ANSI Vietnamese (CP1258) — dùng bảng tự decode
            fullText = decodeWindows1258(raw);
        }
    }

    // ── QUAN TRỌNG: Normalize NFD → NFC ─────────────────────────────────
    // QPainter không render được ký tự tổ hợp dạng NFD (dấu tách rời).
    // Bước này gộp lại thành ký tự dựng sẵn NFC → hiển thị đúng tiếng Việt.
    fullText = fullText.normalized(QString::NormalizationForm_C);

    // ── Font hỗ trợ Unicode / tiếng Việt ────────────────────────────────
    QFont font;
    font.setFamilies({"Segoe UI", "Arial Unicode MS", "Noto Sans",
                      "DejaVu Sans", "Tahoma"});
    font.setPointSize(11);

    QFontMetrics fm(font);
    const int PADDING      = 32;
    const int LINE_H       = fm.lineSpacing();
    int       usableW      = vpW - PADDING * 2;
    int       usableH      = vpH - PADDING * 2;
    int       linesPerPage = qMax(1, usableH / LINE_H);

    // ── Word-wrap ────────────────────────────────────────────────────────
    QStringList wrappedLines;
    for (const QString& rawLine : fullText.split('\n')) {
        if (rawLine.isEmpty()) { wrappedLines.append(""); continue; }
        QString remaining = rawLine;
        while (!remaining.isEmpty()) {
            if (fm.horizontalAdvance(remaining) <= usableW) {
                wrappedLines.append(remaining);
                break;
            }
            int cutAt = remaining.size() * usableW
                        / fm.horizontalAdvance(remaining);
            while (cutAt > 0 &&
                   fm.horizontalAdvance(remaining.left(cutAt)) > usableW)
                --cutAt;
            if (cutAt == 0) { wrappedLines.append(remaining); break; }
            wrappedLines.append(remaining.left(cutAt));
            remaining = remaining.mid(cutAt);
        }
    }

    // ── Render từng trang ────────────────────────────────────────────────
    QList<QPixmap> pages;
    int pageCount = qMax(1, (wrappedLines.size() + linesPerPage - 1)
                                / linesPerPage);
    for (int p = 0; p < pageCount; ++p) {
        QPixmap pixmap(vpW, vpH);
        pixmap.fill(Qt::white);
        QPainter painter(&pixmap);
        painter.setFont(font);
        painter.setPen(Qt::black);

        int startLine = p * linesPerPage;
        int endLine   = qMin(startLine + linesPerPage,
                           (int)wrappedLines.size());
        for (int l = startLine; l < endLine; ++l) {
            int y = PADDING + (l - startLine) * LINE_H + fm.ascent();
            painter.drawText(PADDING, y, wrappedLines[l]);
        }
        painter.end();
        pages.append(pixmap);
    }
    return pages;
}

// ─────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────
QList<QPixmap> FileRenderer::render(const QString& filePath,
                                    int vpW, int vpH)
{
    if (isImage(filePath)) return renderImage(filePath, vpW, vpH);
    if (isPdf  (filePath)) return renderPdf  (filePath, vpW, vpH);
    if (isTxt  (filePath)) return renderTxt  (filePath, vpW, vpH);
    if (isDocx (filePath)) return renderDocx (filePath, vpW, vpH);
    return {};
}