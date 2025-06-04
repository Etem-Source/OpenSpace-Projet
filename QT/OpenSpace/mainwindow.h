#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QLineEdit>
#include <QPushButton>
#include <QTextEdit>
#include <QVBoxLayout>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QProcess>

class MainWindow : public QMainWindow {
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

signals:
    void thresholdsUpdated(const QString &temp, const QString &co2, const QString &light);

private slots:
    void on_loginButton_clicked();
    void on_requestFinished(QNetworkReply *reply);
    void onThresholdsReceived(QNetworkReply *reply);
    void onLogsReceived(QNetworkReply *reply);

private:
    QLineEdit *usernameLineEdit;
    QLineEdit *passwordLineEdit;
    QPushButton *loginButton;
    QTextEdit *logDisplay;
    QVBoxLayout *layout;

    QNetworkAccessManager *networkManager;
    QProcess *nodeProcess = nullptr;

    void startNodeServer();
    void authenticateUser(const QString &username, const QString &password);
    void fetchThresholds();
    void fetchLogs();

    void checkForAlerts(const QString &thresholdTemp, const QString &thresholdCo2, const QString &thresholdLight,
                        const QString &avgTemp, const QString &avgCo2, const QString &avgLight);

};

#endif // MAINWINDOW_H
