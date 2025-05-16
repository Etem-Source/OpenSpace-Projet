#include "mainwindow.h"
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QNetworkRequest>
#include <QMessageBox>
#include <QDebug>

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

    fetchLogs();
}

MainWindow::~MainWindow() {
    delete usernameLineEdit;
    delete passwordLineEdit;
    delete loginButton;
    delete logDisplay;
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
            fetchThresholds();  // ✅ Récupérer les seuils après connexion
            QMessageBox::information(this, "Connexion", "Bienvenue !");
        } else {
            QMessageBox::critical(this, "Erreur de connexion", message);
        }
    } else {
        QMessageBox::warning(this, "Erreur", "Erreur lors de la requête HTTP !");
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

        emit thresholdsUpdated(jsonObj["temperature"].toString(), jsonObj["co2"].toString(), jsonObj["light"].toString());
    }
    reply->deleteLater();
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
    if (reply->error() == QNetworkReply::NoError) {
        QByteArray responseData = reply->readAll();
        QJsonDocument doc = QJsonDocument::fromJson(responseData);
        QJsonArray logs = doc.array();

        logDisplay->clear();
        for (const QJsonValue &log : logs) {
            QJsonObject logObject = log.toObject();
            QString logEntry = QString("%1 | Moyenne: %2 | Réunion: %3 | Bureau: %4 | Détente: %5 | %6")
                                   .arg(logObject["ID"].toString())
                                   .arg(logObject["Moy"].toString())
                                   .arg(logObject["Zone_Reunion"].toString())
                                   .arg(logObject["Zone_Bureau"].toString())
                                   .arg(logObject["Zone_Detente"].toString())
                                   .arg(logObject["Date_Heure"].toString());

            logDisplay->append(logEntry);
        }
    } else {
        logDisplay->append("❌ Erreur récupération logs: " + reply->errorString());
    }
    reply->deleteLater();
}
