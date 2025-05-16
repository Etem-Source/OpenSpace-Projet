#include "reservation.h"
#include <QWebEngineView>
#include <QPushButton>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QStackedWidget>  // Ajouter cette ligne

Reservations::Reservations(QWidget *parent)
    : QWidget(parent)
{
    // ✅ Création du QWebEngineView
    webView = new QWebEngineView(this);
    webView->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);

    // ✅ Création des boutons pour les réservations
    QPushButton *btnReunion = new QPushButton("Réservation Réunion");
    QPushButton *btnBureau = new QPushButton("Réservation Bureau");
    QPushButton *btnExit = new QPushButton("Retour à Monitoring");

    // ✅ Connexions des boutons
    connect(btnReunion, &QPushButton::clicked, this, &Reservations::loadReunionReservation);
    connect(btnBureau, &QPushButton::clicked, this, &Reservations::loadBureauReservation);
    connect(btnExit, &QPushButton::clicked, this, &Reservations::goToMonitoring);  // Connexion pour revenir à Monitoring

    // ✅ Layout horizontal pour les boutons
    QHBoxLayout *buttonLayout = new QHBoxLayout();
    buttonLayout->addWidget(btnReunion);
    buttonLayout->addWidget(btnBureau);
    buttonLayout->addWidget(btnExit);  // Ajouter le bouton "Retour à Monitoring"

    // ✅ Layout principal
    QVBoxLayout *mainLayout = new QVBoxLayout(this);
    mainLayout->addLayout(buttonLayout);
    mainLayout->addWidget(webView);

    setLayout(mainLayout);
}

Reservations::~Reservations() {}

void Reservations::fetchReservationPage(const QString &url)
{
    webView->load(QUrl(url));
}

void Reservations::loadReunionReservation()
{
    fetchReservationPage("http://192.168.29.55/Luka/reservation_reunion.html");
}

void Reservations::loadBureauReservation()
{
    fetchReservationPage("http://192.168.29.55/Luka/reservation_bureau.html");
}

void Reservations::goToMonitoring()
{
    fetchReservationPage("http://192.168.29.55/Luka/reservation_reunion.html");

    // Mettre à jour l'index du QStackedWidget pour revenir à la page Monitoring
    QWidget *parentWidget = this->parentWidget();
    if (parentWidget) {
        QStackedWidget *stackedWidget = dynamic_cast<QStackedWidget *>(parentWidget);
        if (stackedWidget) {
            stackedWidget->setCurrentIndex(1);  // Revenir à la page Monitoring
        }
    }
        this->close();
}
