#include "login.h"
#include <QJsonDocument>
#include <QJsonObject>
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QDebug>

Login::Login(QWidget *parent) : QWidget(parent) {
    QVBoxLayout *mainLayout = new QVBoxLayout(this);
    mainLayout->setAlignment(Qt::AlignTop);
    this->setStyleSheet("background-color: #D3D3D3; padding: 30px;");

    QLabel *titleLabel = new QLabel("Page de Connexion");
    titleLabel->setAlignment(Qt::AlignCenter);
    titleLabel->setStyleSheet("color: #0056b3; font-size: 34px; font-weight: bold;");
    mainLayout->addWidget(titleLabel);

    QLabel *subtitleLabel = new QLabel("Veuillez entrer vos identifiants pour accéder au système");
    subtitleLabel->setAlignment(Qt::AlignCenter);
    subtitleLabel->setStyleSheet("font-size: 22px; color: #343A40;");
    mainLayout->addWidget(subtitleLabel);

    QGridLayout *gridLayout = new QGridLayout();
    gridLayout->setSpacing(25);

    QLabel *userLabel = new QLabel("Nom d'utilisateur :");
    userLabel->setStyleSheet("font-size: 20px; font-weight: bold;");
    usernameInput = new QLineEdit();
    usernameInput->setPlaceholderText("Entrez votre identifiant...");
    usernameInput->setStyleSheet("border: 2px solid #7D8B97; border-radius: 8px; padding: 12px; font-size: 16px;");

    QLabel *passwordLabel = new QLabel("Mot de passe :");
    passwordLabel->setStyleSheet("font-size: 20px; font-weight: bold;");
    passwordInput = new QLineEdit();
    passwordInput->setPlaceholderText("••••••••");
    passwordInput->setEchoMode(QLineEdit::Password);
    passwordInput->setStyleSheet("border: 2px solid #7D8B97; border-radius: 8px; padding: 12px; font-size: 16px;");

    loginButton = new QPushButton("Se connecter");
    loginButton->setStyleSheet("background-color: #007BFF; color: white; border-radius: 12px; padding: 14px; font-size: 16px;");

    loginErrorLabel = new QLabel("");
    loginErrorLabel->setAlignment(Qt::AlignCenter);
    loginErrorLabel->setStyleSheet("color: red; font-size: 16px;");

    gridLayout->addWidget(userLabel, 0, 0);
    gridLayout->addWidget(usernameInput, 0, 1);
    gridLayout->addWidget(passwordLabel, 1, 0);
    gridLayout->addWidget(passwordInput, 1, 1);

    mainLayout->addLayout(gridLayout);
    mainLayout->addWidget(loginErrorLabel);
    mainLayout->addWidget(loginButton);

    connect(loginButton, &QPushButton::clicked, this, &Login::onLoginClicked);

    setLayout(mainLayout);
    networkManager = new QNetworkAccessManager(this);
}

Login::~Login() {
    delete usernameInput;
    delete passwordInput;
    delete loginButton;
    delete loginErrorLabel;
    delete networkManager;
}

void Login::onLoginClicked() {
    QString username = usernameInput->text().trimmed();
    QString password = passwordInput->text().trimmed();

    if (username.isEmpty() || password.isEmpty()) {
        loginErrorLabel->setText("❌ Veuillez remplir tous les champs.");
        return;
    }

    QUrl url("http://localhost:3000/authenticate");
    QNetworkRequest request(url);
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    QJsonObject json;
    json["username"] = username;
    json["password"] = password;
    QJsonDocument doc(json);

    QNetworkReply *reply = networkManager->post(request, doc.toJson());
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        onReplyReceived(reply);
    });

    qDebug() << "🔄 Tentative de connexion pour:" << username;
}

void Login::onReplyReceived(QNetworkReply *reply) {
    if (!reply) return;

    if (reply->error() == QNetworkReply::NoError) {
        QByteArray responseData = reply->readAll();
        QJsonDocument jsonDoc = QJsonDocument::fromJson(responseData);
        QJsonObject jsonObj = jsonDoc.object();

        if (jsonObj["status"].toString() == "success") {
            qDebug() << "✅ Connexion réussie pour:" << usernameInput->text();
            emit loginSuccess();
            emit goToMonitoring();
        } else {
            loginErrorLabel->setText("❌ Identifiants incorrects. Veuillez réessayer.");
        }
    } else {
        loginErrorLabel->setText("❌ Erreur de connexion au serveur.");
        qWarning() << "Erreur HTTP:" << reply->errorString();
    }

    reply->deleteLater();
}
