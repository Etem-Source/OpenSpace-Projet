#ifndef GESTION_H
#define GESTION_H

#include <QWidget>
#include <QLabel>
#include <QLineEdit>
#include <QPushButton>
#include <QVBoxLayout>
#include <QNetworkAccessManager>
#include <QNetworkReply>

class Gestion : public QWidget {
    Q_OBJECT

public:
    explicit Gestion(QWidget *parent = nullptr);
    ~Gestion();  // ✅ Ajout du destructeur

signals:
    void goToMonitoring();  // ✅ Permet de revenir à la page Monitoring
    void seuilUpdated();    // ✅ Signal déclenché après mise à jour d'un seuil

private slots:
    void updateTemperature();
    void updateCO2();
    void updateLight();
    void onThresholdUpdateResponse(QNetworkReply *reply);  // ✅ Ajout pour gérer la réponse serveur
    void sendThresholdUpdate(const QString &url, const QString &key, const QString &value);

private:
    QNetworkAccessManager *networkManager;  // ✅ Gestion des requêtes HTTP

    QLabel *temperatureLabel;
    QLabel *co2Label;
    QLabel *lightLabel;

    QLineEdit *temperatureInput;
    QLineEdit *co2Input;
    QLineEdit *lightInput;

    QPushButton *temperatureButton;
    QPushButton *co2Button;
    QPushButton *lightButton;
    QPushButton *backButton;
};

#endif // GESTION_H
