#pragma once
#include <QString>
#include <QList>
#include <QPixmap>

class FileRenderer
{
public:
    static QList<QPixmap> render(const QString& filePath, int vpW, int vpH);

    static bool isPdf  (const QString& path);
    static bool isImage(const QString& path);
    static bool isTxt  (const QString& path);
    static bool isDocx (const QString& path);
};