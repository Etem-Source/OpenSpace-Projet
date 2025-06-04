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
    ~Monitoring();

signals:
    void goToGestion();
    void goToLogs();
    void goToReservations();
    void goToLogin();

public slots:
    void fetchThresholds();

private slots:
    void onThresholdsReceived(QNetworkReply *reply);
    void fetchData();
    void onDataReceived(QNetworkReply* reply);

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

    QLabel *temperatureAverage;
    QLabel *lightAverage;
    QLabel *co2Average;
    QLabel *peopleAverage;
};

#endif // MONITORING_H
