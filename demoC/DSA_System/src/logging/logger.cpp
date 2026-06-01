#include "logger.h"

#include <QFile>
#include <QTextStream>
#include <QDateTime>
#include <QDir>

void Logger::log(
    const QString& action,
    const QString& message
    )
{
    // =========================
    // Tạo thư mục logs nếu chưa có
    // =========================

    QDir dir;

    if(!dir.exists("logs"))
    {
        dir.mkpath("logs");
    }

    // =========================
    // File log theo ngày
    // =========================

    QString fileName =
        "logs/" +
        QDate::currentDate().toString("yyyy-MM-dd")
        + ".log";

    QFile file(fileName);

    if(!file.open(QIODevice::Append | QIODevice::Text))
        return;

    QTextStream out(&file);

    QString time =
        QDateTime::currentDateTime()
            .toString("yyyy-MM-dd hh:mm:ss");

    out
        << "["
        << time
        << "] "
        << "["
        << action
        << "] "
        << message
        << "\n";

    file.close();
}