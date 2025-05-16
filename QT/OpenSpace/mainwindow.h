#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QLineEdit>
#include <QPushButton>
#include <QNetworkAccessManager>
#include <QTextEdit>
#include <QVBoxLayout>
#include <QNetworkReply>

class MainWindow : public QMainWindow {
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

private slots:
    void on_loginButton_clicked();
    void authenticateUser(const QString &username, const QString &password);
    void on_requestFinished(QNetworkReply *reply);
    void fetchThresholds();
    void onThresholdsReceived(QNetworkReply *reply);
    void fetchLogs();
    void onLogsReceived(QNetworkReply *reply);

private:
    QLineEdit *usernameLineEdit;
    QLineEdit *passwordLineEdit;
    QPushButton *loginButton;
    QTextEdit *logDisplay;
    QVBoxLayout *layout;
    QNetworkAccessManager *networkManager;

signals:
    void thresholdsUpdated(const QString &temperature, const QString &co2, const QString &light);

};

#endif // MAINWINDOW_H
