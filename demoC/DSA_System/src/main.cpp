#include "mainwindow.h"

#include <QApplication>
#include <QDir>
#include <QFile>
#include <QResource>
#include <QDebug>

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);

    qDebug() << QDir::currentPath();

    qDebug() << "Resource exists:"
             << QResource(":/forms/style_light.qss").isValid();

    MainWindow w;

    w.showMaximized();

    return QApplication::exec();
}