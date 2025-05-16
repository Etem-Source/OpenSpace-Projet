#ifndef MONITORING_H
#define MONITORING_H

#include <QWidget>
#include <QLabel>
#include <QPushButton>
#include <QVBoxLayout>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QTimer>

class Monitoring : public QWidget {
    Q_OBJECT

public:
    explicit Monitoring(QWidget *parent = nullptr);
    ~Monitoring();  // ✅ Ajout du destructeur

signals:
    void goToGestion();
    void goToLogs();
    void goToReservations();
    void goToLogin();

public slots:
    void fetchThresholds();  // ✅ Fonction publique pour mise à jour des seuils

private slots:
    void onThresholdsReceived(QNetworkReply *reply);  // ✅ Fonction de traitement des données

private:
    QNetworkAccessManager *networkManager;
    QTimer *updateTimer;

    QLabel *temperatureLabel;
    QLabel *temperatureValue;
    QLabel *temperatureThreshold;

    QLabel *lightLabel;
    QLabel *lightValue;
    QLabel *lightThreshold;

    QLabel *co2Label;
    QLabel *co2Value;
    QLabel *co2Threshold;

    QLabel *peopleCountLabel;
    QLabel *peopleValue;
    QLabel *peopleThreshold;
};

#endif // MONITORING_H
