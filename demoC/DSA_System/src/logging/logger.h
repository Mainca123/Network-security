#ifndef LOGGER_H
#define LOGGER_H

#include <QString>

class Logger
{
public:

    static void log(
        const QString& action,
        const QString& message
        );

};

#endif