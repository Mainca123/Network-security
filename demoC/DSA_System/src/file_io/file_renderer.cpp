#include "file_renderer.h"

#include <QImageReader>
#include <QPdfDocument>
#include <QPainter>
#include <QFont>
#include <QFontMetrics>
#include <QFile>
#include <QTextStream>
#include <QStringList>
#include <QProcess>          // thêm
#include <QDir>              // thêm
#include <QStandardPaths>    // thêm
#include <QUuid>             // thêm

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
// Render ảnh (giữ nguyên)
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
// Render PDF (giữ nguyên)
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
// Render TXT (giữ nguyên)
// ─────────────────────────────────────────────
static QList<QPixmap> renderTxt(const QString& path, int vpW, int vpH)
{
    QFile file(path);
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
        return {};

    QTextStream in(&file);
    in.setEncoding(QStringConverter::Utf8);
    QString fullText = in.readAll();
    file.close();

    QFont font("Consolas", 11);
    QFontMetrics fm(font);
    const int PADDING      = 32;
    const int LINE_H       = fm.lineSpacing();
    int       usableW      = vpW - PADDING * 2;
    int       usableH      = vpH - PADDING * 2;
    int       linesPerPage = usableH / LINE_H;

    QStringList wrappedLines;
    for (const QString& rawLine : fullText.split('\n')) {
        if (rawLine.isEmpty()) { wrappedLines.append(""); continue; }
        QString remaining = rawLine;
        while (!remaining.isEmpty()) {
            if (fm.horizontalAdvance(remaining) <= usableW) {
                wrappedLines.append(remaining); break;
            }
            int cutAt = remaining.size() * usableW
                        / fm.horizontalAdvance(remaining);
            while (cutAt > 0 &&
                   fm.horizontalAdvance(remaining.left(cutAt)) > usableW)
                --cutAt;
            wrappedLines.append(remaining.left(cutAt));
            remaining = remaining.mid(cutAt);
        }
    }

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
// Entry point — thêm nhánh isDocx
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