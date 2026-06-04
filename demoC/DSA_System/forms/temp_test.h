/********************************************************************************
** Form generated from reading UI file 'temp_test.ui'
**
** Created by: Qt User Interface Compiler version 6.11.1
**
** WARNING! All changes made in this file will be lost when recompiling UI file!
********************************************************************************/

#ifndef TEMP_TEST_H
#define TEMP_TEST_H

#include <QtCore/QVariant>
#include <QtWidgets/QApplication>
#include <QtWidgets/QVBoxLayout>
#include <QtWidgets/QWidget>

QT_BEGIN_NAMESPACE

class Ui_Test
{
public:
    QVBoxLayout *verticalLayout;

    void setupUi(QWidget *Test)
    {
        if (Test->objectName().isEmpty())
            Test->setObjectName("Test");
        verticalLayout = new QVBoxLayout(Test);
        verticalLayout->setObjectName("verticalLayout");
        verticalLayout->setContentsMargins(QRect(10, 10, 10, 10));

        retranslateUi(Test);

        QMetaObject::connectSlotsByName(Test);
    } // setupUi

    void retranslateUi(QWidget *Test)
    {
        (void)Test;
    } // retranslateUi

};

namespace Ui {
    class Test: public Ui_Test {};
} // namespace Ui

QT_END_NAMESPACE

#endif // TEMP_TEST_H
