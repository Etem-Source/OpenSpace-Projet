QT       += core gui

QT += websockets
QT += webenginewidgets

INCLUDEPATH += /usr/local/include
LIBS += -L/usr/local/lib -lpaho-mqtt3c

greaterThan(QT_MAJOR_VERSION, 4): QT += widgets

CONFIG += c++17

# You can make your code fail to compile if it uses deprecated APIs.
# In order to do so, uncomment the following line.
#DEFINES += QT_DISABLE_DEPRECATED_BEFORE=0x060000    # disables all the APIs deprecated before Qt 6.0.0

SOURCES += \
    gestion.cpp \
    login.cpp \
    logs.cpp \
    main.cpp \
    mainwindow.cpp \
    monitoring.cpp \
    reservation.cpp

HEADERS += \
    gestion.h \
    login.h \
    logs.h \
    mainwindow.h \
    monitoring.h \
    reservation.h

FORMS += \
    mainwindow.ui

# Default rules for deployment.
qnx: target.path = /tmp/$${TARGET}/bin
else: unix:!android: target.path = /opt/$${TARGET}/bin
!isEmpty(target.path): INSTALLS += target
