#include "monitoring.h"
#include <QJsonDocument>
#include <QJsonObject>
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QGridLayout>
#include <QLabel>
#include <QPushButton>
#include <QDebug>

Monitoring::Monitoring(QWidget *parent) : QWidget(parent) {
    QVBoxLayout *mainLayout = new QVBoxLayout(this);
    mainLayout->setAlignment(Qt::AlignTop); // ✅ Titres remontés
    this->setStyleSheet("background-color: #D3D3D3; padding: 30px;");

    networkManager = new QNetworkAccessManager(this);
    updateTimer = new QTimer(this);
    connect(updateTimer, &QTimer::timeout, this, &Monitoring::fetchThresholds);
    updateTimer->start(20000);

    QLabel *titleLabel = new QLabel("Monitoring de l'Open-Space");
    titleLabel->setAlignment(Qt::AlignCenter);
    titleLabel->setStyleSheet("color: #0056b3; font-size: 34px; font-weight: bold;");
    mainLayout->addWidget(titleLabel);

    QLabel *subtitleLabel = new QLabel("Paramètres environnementaux");
    subtitleLabel->setAlignment(Qt::AlignCenter);
    subtitleLabel->setStyleSheet("font-size: 22px; color: #343A40;");
    mainLayout->addWidget(subtitleLabel);

    QGridLayout *gridLayout = new QGridLayout();
    gridLayout->setSpacing(25); // ✅ Espacement optimisé

    temperatureLabel = new QLabel("Température");
    temperatureValue = new QLabel("0 °C");
    temperatureThreshold = new QLabel("Seuil : 22 °C");

    lightLabel = new QLabel("Éclairement");
    lightValue = new QLabel("0 Lux");
    lightThreshold = new QLabel("Seuil : 300 Lux");

    co2Label = new QLabel("CO2");
    co2Value = new QLabel("0 ppm");
    co2Threshold = new QLabel("Seuil : 800 ppm");

    peopleCountLabel = new QLabel("Nombre de personnes");
    peopleValue = new QLabel("0");
    peopleThreshold = new QLabel("Seuil : 50");

    auto createBlock = [](QLabel* title, QLabel* value, QLabel* threshold) {
        QWidget *box = new QWidget();
        QVBoxLayout *layout = new QVBoxLayout(box);
        box->setStyleSheet("background-color: white; border-radius: 15px; padding: 20px; min-width: 260px;");
        title->setStyleSheet("font-size: 20px; font-weight: bold;");
        value->setStyleSheet("font-size: 28px; font-weight: bold; color: #007BFF;");
        threshold->setStyleSheet("font-size: 16px; color: #5A6268;");
        layout->addWidget(title);
        layout->addWidget(value);
        layout->addWidget(threshold);
        return box;
    };

    gridLayout->addWidget(createBlock(temperatureLabel, temperatureValue, temperatureThreshold), 0, 0);
    gridLayout->addWidget(createBlock(lightLabel, lightValue, lightThreshold), 0, 1);
    gridLayout->addWidget(createBlock(co2Label, co2Value, co2Threshold), 1, 0);
    gridLayout->addWidget(createBlock(peopleCountLabel, peopleValue, peopleThreshold), 1, 1);

    mainLayout->addLayout(gridLayout);

    // ✅ Ajout d'un espace avant les boutons
    mainLayout->addSpacing(30);

    // ✅ Boutons bien centrés en bas et plus compacts
    QPushButton *gestionButton = new QPushButton("Gestion");
    QPushButton *reservationsButton = new QPushButton("Réservations");
    QPushButton *logoutButton = new QPushButton("Déconnexion");
    QPushButton *logsButton = new QPushButton("Logs");

    gestionButton->setStyleSheet("background-color: #007BFF; color: white; border-radius: 12px; padding: 14px; font-size: 16px; min-width: 230px;");
    logsButton->setStyleSheet("background-color: #007BFF; color: white; border-radius: 12px; padding: 14px; font-size: 16px; min-width: 230px;");
    reservationsButton->setStyleSheet("background-color: #007BFF; color: white; border-radius: 12px; padding: 14px; font-size: 16px; min-width: 230px;");
    logoutButton->setStyleSheet("background-color: #DC3545; color: white; border-radius: 12px; padding: 14px; font-size: 16px; min-width: 230px;");

    QHBoxLayout *buttonLayout = new QHBoxLayout();
    buttonLayout->setSpacing(20); // ✅ Espacement ajusté
    buttonLayout->addWidget(gestionButton);
    buttonLayout->addWidget(logsButton);
    buttonLayout->addWidget(reservationsButton);
    buttonLayout->addWidget(logoutButton);

    mainLayout->addLayout(buttonLayout);

    // ✅ Connexion des boutons aux signaux de navigation
    connect(gestionButton, &QPushButton::clicked, this, &Monitoring::goToGestion);
    connect(logsButton, &QPushButton::clicked, this, &Monitoring::goToLogs);
    connect(reservationsButton, &QPushButton::clicked, this, &Monitoring::goToReservations);
    connect(logoutButton, &QPushButton::clicked, this, &Monitoring::goToLogin);

    setLayout(mainLayout);
}

Monitoring::~Monitoring() {
    delete networkManager;
    delete updateTimer;
}

void Monitoring::fetchThresholds() {
    QUrl url("http://localhost:3000/get-thresholds");
    QNetworkRequest request(url);
    QNetworkReply *reply = networkManager->get(request);
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        onThresholdsReceived(reply);
    });
}

void Monitoring::onThresholdsReceived(QNetworkReply *reply) {
    if (!reply) return;

    if (reply->error() == QNetworkReply::NoError) {
        QByteArray responseData = reply->readAll();
        QJsonDocument jsonDoc = QJsonDocument::fromJson(responseData);
        QJsonObject jsonObj = jsonDoc.object();

        temperatureValue->setText(jsonObj["temperature"].toString() + " °C");
        temperatureThreshold->setText("Seuil : " + jsonObj["temperature"].toString() + " °C");

        lightValue->setText(jsonObj["light"].toString() + " Lux");
        lightThreshold->setText("Seuil : " + jsonObj["light"].toString() + " Lux");

        co2Value->setText(jsonObj["co2"].toString() + " ppm");
        co2Threshold->setText("Seuil : " + jsonObj["co2"].toString() + " ppm");

        peopleValue->setText(jsonObj["people_count"].toString());
        peopleThreshold->setText("Seuil : " + jsonObj["people_count"].toString());

        qDebug() << "✅ Seuils mis à jour automatiquement : " << jsonObj;
    } else {
        qWarning() << "❌ Erreur lors de la requête des seuils : " << reply->errorString();
    }

    reply->deleteLater();
}
