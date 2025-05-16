#ifndef LOGS_H
#define LOGS_H

#include <QWidget>
#include <QWebEngineView>

class Logs : public QWidget
{
    Q_OBJECT

public:
    explicit Logs(QWidget *parent = nullptr);
    ~Logs();

    void fetchLogs(const QString &url);  // (optionnel si utilisé ailleurs)

private slots:
    void loadTemperatureLogs();
    void loadCO2Logs();
    void loadLuminosityLogs();

    void goToMonitoring();

private:
    QWebEngineView *webView;
};

#endif // LOGS_H
