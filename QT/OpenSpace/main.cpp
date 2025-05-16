#include <QApplication>
#include <QStackedWidget>
#include <QProcess>
#include <QDebug>
#include "login.h"
#include "monitoring.h"
#include "gestion.h"
#include "logs.h"
#include "reservation.h"

int main(int argc, char *argv[]) {
    QApplication app(argc, argv);

    // ✅ Application du style CSS amélioré
    QString styleSheet = R"(
        QWidget {
            background-color: #EAEDED;
            font-family: 'Segoe UI', sans-serif;
            font-size: 14px;
        }

        QStackedWidget {
            border: 2px solid #7D8B97;
            border-radius: 12px;
            padding: 10px;
        }

        QPushButton {
            background-color: #6C757D;
            color: white;
            border-radius: 12px;
            padding: 10px;
            font-size: 16px;
            border: none;
        }

        QPushButton:hover {
            background-color: #5A6268;
        }

        QLabel {
            color: #343A40;
            font-weight: bold;
            font-size: 18px;
        }

        QLineEdit {
            border: 2px solid #5A6268;
            border-radius: 8px;
            padding: 8px;
            color: #007BFF;
            font-size: 15px;
        }

        QLineEdit:focus {
            border-color: #007BFF;
            background-color: #D6EAF8;
        }
    )";
    app.setStyleSheet(styleSheet);

    // ✅ Création du `QStackedWidget`
    QStackedWidget stackedWidget;

    Login *loginPage = new Login(&stackedWidget);
    Monitoring *monitoringPage = new Monitoring(&stackedWidget);
    Gestion *gestionPage = new Gestion(&stackedWidget);
    Logs *logsPage = new Logs(&stackedWidget);  // 👈 Correction: Ajout du parent
    Reservations *reservationsPage = new Reservations(&stackedWidget);

    stackedWidget.addWidget(loginPage);
    stackedWidget.addWidget(monitoringPage);
    stackedWidget.addWidget(gestionPage);
    stackedWidget.addWidget(logsPage);
    stackedWidget.addWidget(reservationsPage);

    // ✅ Connexions entre les pages
    QObject::connect(loginPage, &Login::goToMonitoring, [&]() {
        monitoringPage->fetchThresholds();
        stackedWidget.setCurrentIndex(1);
    });

    QObject::connect(monitoringPage, &Monitoring::goToGestion, [&]() {
        stackedWidget.setCurrentIndex(2);
    });

    QObject::connect(monitoringPage, &Monitoring::goToLogs, [&]() {

        Logs *logsPage = new Logs();
        logsPage->resize(1920, 1080);
        logsPage->show();
        logsPage->fetchLogs("http://192.168.29.55/Luka/logs_temp.html");
    });

    QObject::connect(monitoringPage, &Monitoring::goToReservations, [&]() {
        stackedWidget.setCurrentIndex(4);
    });

    QObject::connect(monitoringPage, &Monitoring::goToLogin, [&]() {
        stackedWidget.setCurrentIndex(0);
    });

    QObject::connect(gestionPage, &Gestion::goToMonitoring, [&]() {
        stackedWidget.setCurrentIndex(1);
    });

    // ✅ Lancer le processus Node.js dès le démarrage de l'application Qt
    QProcess *nodeProcess = new QProcess();

    // Chemin vers l'exécutable Node.js
    QString nodeExecutable = "node";  // Assurez-vous que Node.js est dans votre PATH
    QStringList arguments;
    arguments << "/home/luka/OpenSpace/node.js";  // Remplacez par le chemin de votre script Node.js

    // Démarrez le processus Node.js
    nodeProcess->start(nodeExecutable, arguments);

    // Vérifiez si le processus a démarré correctement
    if (!nodeProcess->waitForStarted()) {
        qDebug() << "Erreur au démarrage du processus Node.js";
    } else {
        qDebug() << "Node.js démarré avec succès";
    }

    // ✅ Paramètres de la fenêtre
    stackedWidget.setWindowTitle("Open-Space Management");
    stackedWidget.setFixedSize(1920, 1080);
    stackedWidget.setCurrentIndex(0);
    stackedWidget.show();

    return app.exec();
}
