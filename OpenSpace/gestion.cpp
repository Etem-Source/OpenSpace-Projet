#include "gestion.h"
#include <QJsonDocument>
#include <QJsonObject>
#include <QNetworkRequest>
#include <QDebug>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QGridLayout>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>

Gestion::Gestion(QWidget *parent) : QWidget(parent) {
    QVBoxLayout *mainLayout = new QVBoxLayout(this);
    mainLayout->setAlignment(Qt::AlignTop);
    this->setStyleSheet("background-color: #D3D3D3; padding: 30px;");

    QLabel *titleLabel = new QLabel("Gestion des seuils");
    titleLabel->setAlignment(Qt::AlignCenter);
    titleLabel->setStyleSheet("color: #0056b3; font-size: 34px; font-weight: bold;");
    mainLayout->addWidget(titleLabel);

    QLabel *subtitleLabel = new QLabel("Définissez les seuils des paramètres environnementaux");
    subtitleLabel->setAlignment(Qt::AlignCenter);
    subtitleLabel->setStyleSheet("font-size: 22px; color: #343A40;");
    mainLayout->addWidget(subtitleLabel);

    QGridLayout *gridLayout = new QGridLayout();
    gridLayout->setSpacing(25);

    auto createInputBlock = [](QString labelText, QLineEdit*& inputField, QPushButton*& updateButton, QString placeholder, QString buttonText) {
        QWidget *box = new QWidget();
        QVBoxLayout *layout = new QVBoxLayout(box);
        box->setStyleSheet("background-color: white; border-radius: 15px; padding: 20px; min-width: 260px;");

        QLabel *label = new QLabel(labelText);
        label->setStyleSheet("font-size: 20px; font-weight: bold;");
        inputField = new QLineEdit();
        inputField->setPlaceholderText(placeholder);
        inputField->setStyleSheet("border: 2px solid #7D8B97; border-radius: 8px; padding: 10px; font-size: 16px;");

        updateButton = new QPushButton(buttonText);
        updateButton->setStyleSheet("background-color: #007BFF; color: white; border-radius: 12px; padding: 14px; font-size: 16px;");

        layout->addWidget(label);
        layout->addWidget(inputField);
        layout->addWidget(updateButton);
        return box;
    };

    // ✅ Température à gauche, CO₂ à droite
    gridLayout->addWidget(createInputBlock("Température seuil (°C)", temperatureInput, temperatureButton, "Entrez une valeur...", "Actualiser Température"), 0, 0);
    gridLayout->addWidget(createInputBlock("CO2 seuil (ppm)", co2Input, co2Button, "Entrez une valeur...", "Actualiser CO2"), 0, 1);

    // ✅ Luminosité bien centrée en dessous
    gridLayout->addWidget(createInputBlock("Luminosité seuil (Lux)", lightInput, lightButton, "Entrez une valeur...", "Actualiser Luminosité"), 1, 0, 1, 2);

    mainLayout->addLayout(gridLayout);

    mainLayout->addSpacing(30);

    backButton = new QPushButton("Retour au Monitoring");
    backButton->setStyleSheet("background-color: #DC3545; color: white; border-radius: 12px; padding: 14px; font-size: 16px; min-width: 250px;");

    QHBoxLayout *buttonLayout = new QHBoxLayout();
    buttonLayout->setSpacing(20);
    buttonLayout->addWidget(backButton);

    mainLayout->addLayout(buttonLayout);

    connect(temperatureButton, &QPushButton::clicked, this, &Gestion::updateTemperature);
    connect(co2Button, &QPushButton::clicked, this, &Gestion::updateCO2);
    connect(lightButton, &QPushButton::clicked, this, &Gestion::updateLight);
    connect(backButton, &QPushButton::clicked, this, &Gestion::goToMonitoring);

    setLayout(mainLayout);
    networkManager = new QNetworkAccessManager(this);
}

Gestion::~Gestion() {
    delete temperatureInput;
    delete co2Input;
    delete lightInput;

    delete temperatureButton;
    delete co2Button;
    delete lightButton;
    delete backButton;

    delete networkManager;
}

// ✅ Fonction générique pour envoyer les seuils et gérer les réponses
void Gestion::sendThresholdUpdate(const QString &url, const QString &key, const QString &value) {
    if (value.trimmed().isEmpty()) {
        qWarning() << "❌ Valeur vide, mise à jour annulée pour " << key;
        return;
    }

    QNetworkRequest request(url);
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    QJsonObject json;
    json[key] = value;
    QJsonDocument doc(json);

    QNetworkReply *reply = networkManager->post(request, doc.toJson());
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        onThresholdUpdateResponse(reply);
    });

    qDebug() << "📤 Envoi du seuil " << key << " : " << value;
}

// ✅ Gestion des réponses du serveur
void Gestion::onThresholdUpdateResponse(QNetworkReply *reply) {
    if (!reply) return;

    if (reply->error() == QNetworkReply::NoError) {
        qDebug() << "✅ Seuil mis à jour avec succès.";
        emit seuilUpdated();
    } else {
        qWarning() << "❌ Erreur mise à jour du seuil :" << reply->errorString();
    }

    reply->deleteLater();
}

void Gestion::updateTemperature() {
    sendThresholdUpdate("http://localhost:3000/update-temperature", "temperature", temperatureInput->text());
}

void Gestion::updateCO2() {
    sendThresholdUpdate("http://localhost:3000/update-co2", "co2", co2Input->text());
}

void Gestion::updateLight() {
    sendThresholdUpdate("http://localhost:3000/update-light", "light", lightInput->text());
}
