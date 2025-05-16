#include <ESP8266WiFi.h>  // Inclusion de la bibliothèque ESP8266WiFi pour la connexion WiFi
#include <PubSubClient.h>  // Inclusion de la bibliothèque PubSubClient pour la communication MQTT
#include <Wire.h>  // Inclusion de la bibliothèque Wire pour la communication I2C
#include "Seeed_BMP280.h"  // Inclusion de la bibliothèque Seeed_BMP280 pour le capteur BMP280
#include "Air_Quality_Sensor.h"  // Inclusion de la bibliothèque Air_Quality_Sensor pour le capteur de qualité de l'air
#include "Digital_Light_TSL2561.h"  // Inclusion de la bibliothèque Digital_Light_TSL2561 pour le capteur de luminosité
#include <multi_channel_relay.h>  // Inclusion de la bibliothèque pour le module relais

Multi_Channel_Relay relay;  // Création d'un objet pour le module relais
// Remplacez par le SSID et le mot de passe de votre réseau WiFi
const char* ssid = "WiFiOpenSpaceGestion";  // SSID du réseau WiFi
const char* password = "29092004";  // Mot de passe du réseau WiFi
const char* mqtt_server = "192.168.4.1";  // Adresse IP du broker MQTT

const char* hostname = "ESP8266_Bureau";  // Nom d'hôte personnalisé

WiFiClient espClient;  // Création d'un objet WiFiClient pour gérer la connexion WiFi
PubSubClient client(espClient);  // Création d'un objet PubSubClient en utilisant l'objet WiFiClient pour la communication MQTT
BMP280 bmp280;  // Création d'un objet BMP280 pour lAPRe capteur BMP280
AirQualitySensor airQualitySensor(A0);  // Création d'un objet AirQualitySensor pour le capteur de qualité de l'air

const int pirPin = D5;  // Broche à laquelle le module PIR Parallax Rev B est connecté
unsigned long lastMotionTime = 0;  // Variable pour stocker le temps du dernier message envoyé
unsigned long lastNoMotionTime = 0;  // Variable pour stocker le temps de la dernière détection de mouvement

// Variables globales pour les seuils
float seuilTemperature = 25.0;  // Seuil par défaut pour la température
int seuilCO2 = 400;             // Seuil par défaut pour le CO2
int seuilLight = 300;           // Seuil par défaut pour la luminosité

// Déclarations des fonctions
void callback(char* topic, byte* payload, unsigned int length);
void processMessage(const char* topic, const String& message);
void handleTemperatureRequest();
void handleAirQualityRequest();
void handleLightRequest();
void reconnectMQTT();
void initWiFi();
void checkWiFi();
void initBMP280();
void initAirQualitySensor();
void initLightSensor();
void initPIRSensor();
void checkMotion();
void setup();
void loop();

// Fonction de rappel pour la réception des messages MQTT
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message reçu [");  // Affichage du début du message reçu
  Serial.print(topic);  // Affichage du topic du message reçu
  Serial.print("] : ");  // Affichage de la fin du début du message reçu
  for (unsigned int i = 0; i < length; i++) {  // Boucle pour afficher le contenu du message reçu
    Serial.print((char)payload[i]);  // Affichage du contenu du message reçu
  }
  Serial.println();  // Saut de ligne après l'affichage du message reçu

  // Analyser le message reçu et appeler les fonctions correspondantes
  String message = String((char*)payload).substring(0, length);  // Conversion du payload en String
  processMessage(topic, message);  // Appel de la fonction processMessage pour traiter le message
}

void processMessage(const char* topic, const String& message) {
  if (String(topic) == "Signal/RequestTemperature") {
    handleTemperatureRequest();
  } else if (String(topic) == "Signal/RequestCO2") {
    handleAirQualityRequest();
  } else if (String(topic) == "Signal/RequestLight") {
    handleLightRequest();
  } else if (String(topic) == "Seuil/Temperature") {
    seuilTemperature = message.toFloat();  // Mettre à jour le seuil de température
    Serial.print("Nouveau seuil de température : ");
    Serial.println(seuilTemperature);
  } else if (String(topic) == "Seuil/CO2") {
    seuilCO2 = message.toInt();  // Mettre à jour le seuil de CO2
    Serial.print("Nouveau seuil de CO2 : ");
    Serial.println(seuilCO2);
  } else if (String(topic) == "Seuil/Light") {
    seuilLight = message.toInt();  // Mettre à jour le seuil de luminosité
    Serial.print("Nouveau seuil de luminosité : ");
    Serial.println(seuilLight);
  }
}

// Fonction pour gérer les demandes de température
void handleTemperatureRequest() {
  Serial.println("Signal de température reçu");  // Affichage d'un message indiquant que le signal de température a été reçu
  // Lecture de la température du BMP280
  float temperature = bmp280.getTemperature();  // Lecture de la température du capteur BMP280
  char tempStr[8];  // Création d'un tableau de caractères pour stocker la température sous forme de chaîne
  dtostrf(temperature, 1, 2, tempStr);  // Conversion de la température en chaîne de caractères
  client.publish("Bureau_Temp/topic", tempStr);  // Publication de la température sur le topic MQTT
  Serial.print("Température envoyée : ");  // Affichage d'un message indiquant que la température a été envoyée
  Serial.println(tempStr);  // Affichage de la température envoyée
}

// Fonction pour gérer les demandes de qualité de l'air
void handleAirQualityRequest() {
  Serial.println("Signal de qualité de l'air reçu");  // Affichage d'un message indiquant que le signal de qualité de l'air a été reçu
  // Lecture de la qualité de l'air
  int quality = airQualitySensor.slope();  // Lecture de la qualité de l'air
  int airValue = airQualitySensor.getValue();  // Lecture de la valeur de la qualité de l'air
  char airStr[8];  // Création d'un tableau de caractères pour stocker la valeur de la qualité de l'air sous forme de chaîne
  itoa(airValue, airStr, 10);  // Conversion de la valeur de la qualité de l'air en chaîne de caractères
  client.publish("Bureau_AirQuality/topic", airStr);  // Publication de la qualité de l'air sur le topic MQTT
  Serial.print("Qualité de l'air envoyée : ");  // Affichage d'un message indiquant que la qualité de l'air a été envoyée
  Serial.println(airStr);  // Affichage de la qualité de l'air envoyée
}

// Fonction pour gérer les demandes de luminosité
void handleLightRequest() {
  Serial.println("Signal de luminosité reçu");  // Affichage d'un message indiquant que le signal de luminosité a été reçu
  // Lecture de la luminosité
  uint32_t lux = TSL2561.readVisibleLux();  // Lecture de la luminosité du capteur TSL2561
  char luxStr[8];  // Création d'un tableau de caractères pour stocker la luminosité sous forme de chaîne
  itoa(lux, luxStr, 10);  // Conversion de la luminosité en chaîne de caractères
  client.publish("Bureau_Light/topic", luxStr);  // Publication de la luminosité sur le topic MQTT
  Serial.print("Luminosité envoyée : ");  // Affichage d'un message indiquant que la luminosité a été envoyée
  Serial.println(luxStr);  // Affichage de la luminosité envoyée

  // Affichage de la luminosité
  Serial.print("The Light value is: ");  // Affichage d'un message indiquant la valeur de la luminosité
  Serial.println(lux);  // Affichage de la valeur de la luminosité
}

void reconnectMQTT() {
  while (!client.connected()) {
    Serial.print("Connexion à MQTT...");
    if (client.connect("ESP_Client_Bureau")) {
      Serial.println("Connecté !");
      client.subscribe("Signal/RequestTemperature");
      client.subscribe("Signal/RequestCO2");
      client.subscribe("Signal/RequestLight");
      client.subscribe("Seuil/Temperature");  // S'abonner au topic pour le seuil de température
      client.subscribe("Seuil/CO2");          // S'abonner au topic pour le seuil de CO2
      client.subscribe("Seuil/Light");        // S'abonner au topic pour le seuil de luminosité
    } else {
      Serial.print("Échec, code erreur : ");
      Serial.println(client.state());
      delay(2000);
      checkWiFi();
    }
  }
}

// Fonction pour initialiser la connexion WiFi
void initWiFi() {
  // Connexion WiFi
  Serial.println("Connexion au WiFi...");  // Affichage d'un message indiquant la tentative de connexion au WiFi
  WiFi.begin(ssid, password);  // Connexion au réseau WiFi
  while (WiFi.status() != WL_CONNECTED) {  // Tant que la connexion n'est pas établie
    delay(500);  // Attendre 500 millisecondes
    Serial.print(".");  // Affichage d'un point pour indiquer la tentative de connexion
  }
  Serial.println("\nWiFi connecté");  // Affichage d'un message indiquant que la connexion WiFi a réussi
  Serial.print("Adresse IP : ");
  Serial.println(WiFi.localIP());  // Affichage de l'adresse IP de l'ESP8266
}

// Fonction pour vérifier et reconnecter le WiFi si nécessaire
void checkWiFi() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi déconnecté, tentative de reconnexion...");
    WiFi.disconnect();
    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) {
      delay(500);
      Serial.print(".");
    }
    Serial.println("\nWiFi reconnecté");
    Serial.print("Adresse IP : ");
    Serial.println(WiFi.localIP());
  }
}

// Fonction pour initialiser le capteur BMP280
void initBMP280() {
  // Initialisation du capteur BMP280
  if (!bmp280.init()) {  // Si l'initialisation du capteur BMP280 échoue
    Serial.println("Erreur de communication avec le capteur BMP280 !");  // Affichage d'un message d'erreur
    while (1);  // Boucle infinie pour arrêter le programme
  }
  Serial.println("BMP280 ready.");  // Affichage d'un message indiquant que le capteur BMP280 est prêt
}

// Fonction pour initialiser le capteur de qualité de l'air
void initAirQualitySensor() {
  // Initialisation du capteur de qualité de l'air
  Serial.println("Waiting sensor to init...");  // Affichage d'un message indiquant l'attente de l'initialisation du capteur
  delay(20000);  // Attendre 20 secondes pour l'initialisation du capteur
  if (airQualitySensor.init()) {  // Si l'initialisation du capteur de qualité de l'air réussit
    Serial.println("Air Quality Sensor ready.");  // Affichage d'un message indiquant que le capteur de qualité de l'air est prêt
  } else {  // Si l'initialisation du capteur de qualité de l'air échoue
    Serial.println("Air Quality Sensor ERROR!");  // Affichage d'un message d'erreur
  }
}

// Fonction pour initialiser le capteur de luminosité
void initLightSensor() {
  // Initialisation du capteur de luminosité
  Wire.begin(D2, D1);  // Initialiser I2C avec les broches SDA et SCL
  TSL2561.init();  // Initialisation du capteur TSL2561
  Serial.println("Light Sensor ready.");  // Affichage d'un message indiquant que le capteur de luminosité est prêt
}

// Fonction pour initialiser le capteur PIR
void initPIRSensor() {
  // Initialisation du module PIR Parallax Rev B
  pinMode(pirPin, INPUT);  // Définir la broche du capteur PIR comme entrée
  Serial.println("PIR Sensor ready.");  // Affichage d'un message indiquant que le capteur PIR est prêt
}

// Variables pour suivre l'état des relais
bool isFanOn = false;       // État du ventilateur (Channel 1)
bool isHeaterOn = false;    // État du chauffage (Channel 3)
bool isLightOn = false;     // État de la LED (Channel 2)

void checkThresholds() {
  // Vérification du seuil de température avec une marge de 5 %
  float temperature = bmp280.getTemperature();
  float margin = seuilTemperature * 0.05;  // Calcul de la marge de 5 %
  float lowerThreshold = seuilTemperature - margin;  // Seuil inférieur (seuil - 5 %)
  float upperThreshold = seuilTemperature + margin;  // Seuil supérieur (seuil + 5 %)

  if (temperature < lowerThreshold) {
    if (!isHeaterOn) {  // Si le chauffage est actuellement éteint
      Serial.println("Température en dessous du seuil avec marge, activation du chauffage (Channel 3)");
      relay.turn_on_channel(3);  // Activer le chauffage
      isHeaterOn = true;  // Mettre à jour l'état
    }
  } else if (temperature > upperThreshold) {
    if (isHeaterOn) {  // Si le chauffage est actuellement allumé
      Serial.println("Température au-dessus du seuil avec marge, désactivation du chauffage (Channel 3)");
      relay.turn_off_channel(3);  // Désactiver le chauffage
      isHeaterOn = false;  // Mettre à jour l'état
    }
  }

  // Vérification du seuil de luminosité
  uint32_t lux = TSL2561.readVisibleLux();
  if (lux < seuilLight) {
    if (!isLightOn) {  // Si la LED est actuellement éteinte
      Serial.println("Luminosité en dessous du seuil, activation de la LED (Channel 2)");
      relay.turn_on_channel(2);  // Activer la LED
      isLightOn = true;  // Mettre à jour l'état
    }
  } else {
    if (isLightOn) {  // Si la LED est actuellement allumée
      Serial.println("Luminosité au-dessus du seuil, désactivation de la LED (Channel 2)");
      relay.turn_off_channel(2);  // Désactiver la LED
      isLightOn = false;  // Mettre à jour l'état
    }
  }

  // Vérification du seuil de CO2
  int airValue = airQualitySensor.getValue();
  if (airValue > seuilCO2) {
    if (!isFanOn) {  // Si le ventilateur est actuellement éteint
      Serial.println("CO2 au-dessus du seuil, activation du ventilateur (Channel 1)");
      relay.turn_on_channel(1);  // Activer le ventilateur
      isFanOn = true;  // Mettre à jour l'état
    }
  } else {
    if (isFanOn) {  // Si le ventilateur est actuellement allumé
      Serial.println("CO2 en dessous du seuil, désactivation du ventilateur (Channel 1)");
      relay.turn_off_channel(1);  // Désactiver le ventilateur
      isFanOn = false;  // Mettre à jour l'état
    }
  }
}

// Fonction pour vérifier le mouvement détecté par le capteur PIR
void checkMotion() {
  // Lecture de l'état du capteur PIR
  int motionDetected = digitalRead(pirPin);  // Lecture de l'état de la broche du capteur PIR
  unsigned long currentTime = millis();  // Obtenir le temps actuel en millisecondes

  // Envoyer un message au broker MQTT si un mouvement est détecté
  if (motionDetected == HIGH) {  // Si un mouvement est détecté
    if (currentTime - lastMotionTime > 5000) {  // Vérifier si 5 secondes se sont écoulées depuis le dernier message
      client.publish("Bureau_Motion/topic", "Mouvement détecté !");  // Publication d'un message sur le topic MQTT
      Serial.println("Message envoyé au broker MQTT : Mouvement détecté !");  // Affichage d'un message indiquant que le message a été envoyé
      lastMotionTime = currentTime;  // Mettre à jour le temps du dernier message envoyé
    }
    lastNoMotionTime = currentTime;  // Mettre à jour le temps de la dernière détection de mouvement
  } else {
    if (currentTime - lastNoMotionTime > 120000) {  // Vérifier si 2 minutes (120000 ms) se sont écoulées depuis la dernière détection de mouvement
      // Effectuer l'action souhaitée après 2 minutes sans détection de mouvement
      client.publish("Bureau_Motion/topic", "Aucune détection de mouvement depuis 2 minutes.");  // Publication d'un message sur le topic MQTT
      Serial.println("Aucune détection de mouvement depuis 2 minutes.");
      // Ajoutez ici le code pour l'action à effectuer après 2 minutes sans détection de mouvement
      lastNoMotionTime = currentTime;  // Réinitialiser le temps de la dernière détection de mouvement pour éviter des actions répétées
    }
  }
}

// Fonction setup pour initialiser le programme
void setup() {
  Serial.begin(9600);
  Serial.println("Démarrage de l'ESP8266");
  relay.begin(0x20); 
  Serial.print("Version du firmware du relais : 0x");
  Serial.println(relay.getFirmwareVersion(), HEX);

  // Initialisation des autres composants
  WiFi.hostname(hostname);
  initWiFi();
  initBMP280();
  initAirQualitySensor();
  initLightSensor();
  initPIRSensor();

  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
  reconnectMQTT();
}

// Fonction loop pour exécuter le programme en boucle
void loop() {
  checkWiFi();  // Vérifier et reconnecter le WiFi si nécessaire

  if (!client.connected()) {
    reconnectMQTT();  // Reconnecter au broker MQTT si nécessaire
  }
  client.loop();  // Gérer les messages MQTT

  checkMotion();  // Vérifier le mouvement détecté par le capteur PIR
  checkThresholds();  // Vérifier les seuils et contrôler les relais

  delay(1000);  // Attendre 1 seconde avant la prochaine vérification
}