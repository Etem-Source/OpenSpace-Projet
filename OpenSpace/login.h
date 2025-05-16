#ifndef LOGIN_H
#define LOGIN_H

#include <QWidget>
#include <QLineEdit>
#include <QPushButton>
#include <QLabel>
#include <QVBoxLayout>
#include <QNetworkAccessManager>
#include <QNetworkReply>

class Login : public QWidget {
    Q_OBJECT

public:
    explicit Login(QWidget *parent = nullptr);
    ~Login();

signals:
    void loginSuccess();    // Signal émis après une connexion réussie
    void goToMonitoring();  // Signal pour accéder à la page Monitoring

private slots:
    void onLoginClicked();
    void onReplyReceived(QNetworkReply *reply);

private:
    QLineEdit *usernameInput;
    QLineEdit *passwordInput;
    QLabel *loginErrorLabel;
    QNetworkAccessManager *networkManager;
    QPushButton *loginButton;
};

#endif // LOGIN_H
