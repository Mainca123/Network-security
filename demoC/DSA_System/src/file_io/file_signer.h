#ifndef FILE_SIGNER_H
#define FILE_SIGNER_H

#include <QString>

class FileSigner
{
public:

    static bool signFile(
        const QString& filePath,
        const QString& privateKeyPath,
        const QString& signaturePath
        );
    static bool verifyFile(
        const QString& filePath,
        const QString& publicKeyPath,
        const QString& signaturePath
        );

};

#endif