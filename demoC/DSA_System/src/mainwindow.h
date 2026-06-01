#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <openssl/bn.h>
#include "crypto/dsa_manager.h"
#include <QPushButton>

QT_BEGIN_NAMESPACE
namespace Ui { class MainWindow; }
QT_END_NAMESPACE

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

private slots:
    void on_btnSaveKeyPair_clicked();
    void on_btnSignFile_clicked();
    void on_btnVerifySignature_clicked();
    void on_btnLogs_clicked();
    void on_btngenerateDSAParameters();
private:
    Ui::MainWindow *ui;

    // Con trỏ quản lý đối tượng DSA, khởi tạo một lần duy nhất
    DSAManager *dsaManager = nullptr;

    bool isGeneratingKeys = false;
    void generateDSAKeys();
    void showKeyInformation(const BIGNUM* p, const BIGNUM* q, const BIGNUM* g, const BIGNUM* x, const BIGNUM* y);
    void clearCurrentKeys();
    void addShadow(QWidget *widget);
    void addButtonShadow(QPushButton *button);

    // Lưu trữ các tham số DSA hiện tại để truyền vào hàm signFileCalculated
    BIGNUM* current_p = nullptr;
    BIGNUM* current_q = nullptr;
    BIGNUM* current_g = nullptr;
    BIGNUM* current_x = nullptr;
    BIGNUM* current_y = nullptr;

private:
    bool m_isDarkTheme = false;
    void applyTheme(bool dark);
};
#endif // MAINWINDOW_H