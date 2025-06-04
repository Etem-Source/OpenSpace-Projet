#include "mainwindow.h"
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QMessageBox>
#include <QDebug>
#include <QProcess>
#include <QVBoxLayout>

MainWindow::MainWindow(QWidget *parent) : QMainWindow(parent) {
    QWidget *centralWidget = new QWidget(this);
    setCentralWidget(centralWidget);

    usernameLineEdit = new QLineEdit(this);
    usernameLineEdit->setPlaceholderText("Nom d'utilisateur");

    passwordLineEdit = new QLineEdit(this);
    passwordLineEdit->setEchoMode(QLineEdit::Password);
    passwordLineEdit->setPlaceholderText("Mot de passe");

    loginButton = new QPushButton("Se connecter");
    connect(loginButton, &QPushButton::clicked, this, &MainWindow::on_loginButton_clicked);

    logDisplay = new QTextEdit(this);
    logDisplay->setReadOnly(true);

    layout = new QVBoxLayout();
    layout->addWidget(usernameLineEdit);
    layout->addWidget(passwordLineEdit);
    layout->addWidget(loginButton);
    layout->addWidget(logDisplay);

    centralWidget->setLayout(layout);

    networkManager = new QNetworkAccessManager(this);

    startNodeServer();    // 🚀 Lancement automatique du serveur Node.js
    fetchLogs();          // 📜 Récupération des logs au démarrage
}

MainWindow::~MainWindow() {
    // Les pointeurs enfants sont supprimés automatiquement par Qt.
}

void MainWindow::startNodeServer() {
    QString scriptPath = "/home/luka/OpenSpace/node.js"; // 🔁 Chemin vers ton script Node.js

    nodeProcess = new QProcess(this);
    nodeProcess->setProgram("node");
    nodeProcess->setArguments({scriptPath});
    nodeProcess->setWorkingDirectory("/home/luka/OpenSpace/");
    nodeProcess->start();

    connect(nodeProcess, &QProcess::readyReadStandardOutput, [this]() {
        qDebug() << "Node.js:" << nodeProcess->readAllStandardOutput();
    });

    connect(nodeProcess, &QProcess::readyReadStandardError, [this]() {
        qDebug() << "Node.js ERROR:" << nodeProcess->readAllStandardError();
    });

    connect(nodeProcess, QOverload<QProcess::ProcessError>::of(&QProcess::errorOccurred), [this](QProcess::ProcessError error) {
        qDebug() << "Erreur processus Node.js:" << error;
    });
}

void MainWindow::on_loginButton_clicked() {
    QString username = usernameLineEdit->text().trimmed();
    QString password = passwordLineEdit->text().trimmed();

    if (username.isEmpty() || password.isEmpty()) {
        QMessageBox::warning(this, "Erreur", "Veuillez remplir tous les champs !");
        return;
    }

    authenticateUser(username, password);
}

void MainWindow::authenticateUser(const QString &username, const QString &password) {
    QUrl url("http://localhost:3000/authenticate");
    QNetworkRequest request(url);
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    QJsonObject json;
    json["username"] = username;
    json["password"] = password;

    QJsonDocument doc(json);
    QNetworkReply *reply = networkManager->post(request, doc.toJson());

    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        on_requestFinished(reply);
    });
}

void MainWindow::on_requestFinished(QNetworkReply *reply) {
    if (reply->error() == QNetworkReply::NoError) {
        QByteArray responseData = reply->readAll();
        QJsonDocument doc = QJsonDocument::fromJson(responseData);
        QJsonObject response = doc.object();

        QString status = response["status"].toString();
        QString message = response["message"].toString();

        if (status == "success") {
            qDebug() << "🟢 Connexion réussie!";
            fetchThresholds();
            QMessageBox::information(this, "Connexion", "Bienvenue !");
        } else {
            QMessageBox::critical(this, "Erreur de connexion", message);
        }
    } else {
        QMessageBox::warning(this, "Erreur", "Erreur lors de la requête HTTP: " + reply->errorString());
    }

    reply->deleteLater();
}

void MainWindow::fetchThresholds() {
    QUrl url("http://localhost:3000/get-thresholds");
    QNetworkRequest request(url);
    QNetworkReply *reply = networkManager->get(request);

    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        onThresholdsReceived(reply);
    });
}

void MainWindow::onThresholdsReceived(QNetworkReply *reply) {
    if (reply->error() == QNetworkReply::NoError) {
        QByteArray responseData = reply->readAll();
        QJsonDocument jsonDoc = QJsonDocument::fromJson(responseData);
        QJsonObject jsonObj = jsonDoc.object();

        QString thresholdTemp = jsonObj["temperature"].toString();
        QString thresholdCo2 = jsonObj["co2"].toString();
        QString thresholdLight = jsonObj["light"].toString();

        // Exemple valeurs moyennes (à adapter)
        QString avgTemp = "25";
        QString avgCo2 = "600";
        QString avgLight = "300";

        emit thresholdsUpdated(thresholdTemp, thresholdCo2, thresholdLight);

        // Appel à la fonction d'alerte
        checkForAlerts(thresholdTemp, thresholdCo2, thresholdLight, avgTemp, avgCo2, avgLight);

    } else {
        qDebug() << "Erreur récupération seuils:" << reply->errorString();
    }

    reply->deleteLater();
}

void MainWindow::checkForAlerts(const QString &thresholdTemp, const QString &thresholdCo2, const QString &thresholdLight,
                                const QString &avgTemp, const QString &avgCo2, const QString &avgLight)
{
    qDebug() << "Seuil Temp:" << thresholdTemp << ", Moyenne Temp:" << avgTemp;
    qDebug() << "Seuil CO2:" << thresholdCo2 << ", Moyenne CO2:" << avgCo2;
    qDebug() << "Seuil Light:" << thresholdLight << ", Moyenne Light:" << avgLight;

    if (avgTemp.toDouble() > thresholdTemp.toDouble()) {
        QMessageBox::warning(this, "Alerte Température", "La température moyenne dépasse le seuil !");
    }
    if (avgCo2.toDouble() > thresholdCo2.toDouble()) {
        QMessageBox::warning(this, "Alerte CO2", "Le taux de CO2 moyen dépasse le seuil !");
    }
    if (avgLight.toDouble() > thresholdLight.toDouble()) {
        QMessageBox::warning(this, "Alerte Luminosité", "La luminosité moyenne dépasse le seuil !");
    }
}


void MainWindow::fetchLogs() {
    QUrl url("http://localhost:3000/get-logs");
    QNetworkRequest request(url);
    QNetworkReply *reply = networkManager->get(request);

    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        onLogsReceived(reply);
    });
}

void MainWindow::onLogsReceived(QNetworkReply *reply) {
    qDebug() << "📩 Réponse reçue de /get-logs";

    if (reply->error() != QNetworkReply::NoError) {
        qDebug() << "❌ Erreur récupération logs:" << reply->errorString();
        logDisplay->append("❌ Erreur récupération logs: " + reply->errorString());
        reply->deleteLater();
        return;
    }

    QByteArray responseData = reply->readAll();
    qDebug() << "🧾 Données reçues :" << responseData;

    QJsonParseError jsonError;
    QJsonDocument doc = QJsonDocument::fromJson(responseData, &jsonError);
    if (doc.isNull()) {
        qDebug() << "❌ Erreur de parsing JSON:" << jsonError.errorString();
        logDisplay->append("❌ Erreur de parsing JSON: " + jsonError.errorString());
        reply->deleteLater();
        return;
    }

    if (!doc.isArray()) {
        qDebug() << "❌ Format JSON inattendu (pas un tableau)";
        logDisplay->append("❌ Format JSON inattendu (pas un tableau)");
        reply->deleteLater();
        return;
    }

    QJsonArray logs = doc.array();
    logDisplay->clear();

    for (const QJsonValue &log : logs) {
        QJsonObject logObject = log.toObject();

        QString logEntry = QString("%1 | Moyenne: %2 | Réunion: %3 | Bureau: %4 | Détente: %5 | %6")
                               .arg(logObject["ID"].toString())
                               .arg(logObject["Moyenne"].toString())
                               .arg(logObject["Réunion"].toString())
                               .arg(logObject["Bureau"].toString())
                               .arg(logObject["Détente"].toString())
                               .arg(logObject["Date"].toString());

        logDisplay->append(logEntry);
    }

    reply->deleteLater();
}
