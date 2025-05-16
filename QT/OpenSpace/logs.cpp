#include "logs.h"
#include <QWebEngineView>
#include <QUrl>
#include <QPushButton>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QStackedWidget>  // <-- Ajouter cette ligne

Logs::Logs(QWidget *parent)
    : QWidget(parent)
{
    // ✅ Création du QWebEngineView
    webView = new QWebEngineView(this);
    webView->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);

    // ✅ Création des boutons
    QPushButton *btnTemp = new QPushButton("Logs Température");
    QPushButton *btnCO2 = new QPushButton("Logs CO2");
    QPushButton *btnLight = new QPushButton("Logs Luminosité");
    QPushButton *btnExit = new QPushButton("Retour à Monitoring");

    // ✅ Connexions des boutons
    connect(btnTemp, &QPushButton::clicked, this, &Logs::loadTemperatureLogs);
    connect(btnCO2, &QPushButton::clicked, this, &Logs::loadCO2Logs);
    connect(btnLight, &QPushButton::clicked, this, &Logs::loadLuminosityLogs);
    connect(btnExit, &QPushButton::clicked, this, &Logs::goToMonitoring);  // Connexion pour revenir à Monitoring

    // ✅ Layout horizontal pour les boutons
    QHBoxLayout *buttonLayout = new QHBoxLayout();
    buttonLayout->addWidget(btnTemp);
    buttonLayout->addWidget(btnCO2);
    buttonLayout->addWidget(btnLight);
    buttonLayout->addWidget(btnExit);  // Ajouter le bouton "Retour à Monitoring"

    // ✅ Layout principal
    QVBoxLayout *mainLayout = new QVBoxLayout(this);
    mainLayout->addLayout(buttonLayout);
    mainLayout->addWidget(webView);

    setLayout(mainLayout);
}

Logs::~Logs() {}

void Logs::fetchLogs(const QString &url)
{
    webView->load(QUrl(url));
}

void Logs::loadTemperatureLogs()
{
    fetchLogs("http://192.168.29.55/Luka/logs_temp.html");
}

void Logs::loadCO2Logs()
{
    fetchLogs("http://192.168.29.55/Luka/logs_CO2.html");
}

void Logs::loadLuminosityLogs()
{
    fetchLogs("http://192.168.29.55/Luka/logs_light.html");
}

void Logs::goToMonitoring()
{
    // Réinitialiser la page des logs pour afficher les logs de température par défaut
    fetchLogs("http://192.168.29.55/Luka/logs_temp.html");

    // Mettre à jour l'index du QStackedWidget pour revenir à la page Monitoring
    QWidget *parentWidget = this->parentWidget();
    if (parentWidget) {
        QStackedWidget *stackedWidget = dynamic_cast<QStackedWidget *>(parentWidget);
        if (stackedWidget) {
            stackedWidget->setCurrentIndex(1);  // Revenir à la page Monitoring
        }
    }
    // Fermer la fenêtre actuelle (Logs)
    this->close();

}
