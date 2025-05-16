#ifndef RESERVATIONS_H
#define RESERVATIONS_H

#include <QWidget>
#include <QWebEngineView>

class Reservations : public QWidget
{
    Q_OBJECT

public:
    explicit Reservations(QWidget *parent = nullptr);
    ~Reservations();

    void fetchReservationPage(const QString &url);
    void loadReunionReservation();
    void loadBureauReservation();
    void goToMonitoring();  // Fonction pour revenir à Monitoring

private:
    QWebEngineView *webView;
};

#endif // RESERVATIONS_H
