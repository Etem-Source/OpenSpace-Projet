import os
import threading
import logging
import json
import mysql.connector
import paho.mqtt.client as mqtt
from dotenv import load_dotenv
import time

# Charger les variables d'environnement
load_dotenv()

# Configuration du broker MQTT
BROKER = os.getenv("MQTT_BROKER", "localhost")
PORT = int(os.getenv("MQTT_PORT", 1883))

# Configuration de la base de données
DB_CONFIG_MANAGER = {
    "host": os.getenv("DB_HOST_MANAGER"),
    "user": os.getenv("DB_USER_MANAGER"),
    "password": os.getenv("DB_PASSWORD_MANAGER"),
    "database": os.getenv("DB_NAME_MANAGER")
}

DB_CONFIG_THRESHOLDS = {
    "host": os.getenv("DB_HOST_THRESHOLDS"),
    "user": os.getenv("DB_USER_THRESHOLDS"),
    "password": os.getenv("DB_PASSWORD_THRESHOLDS"),
    "database": os.getenv("DB_NAME_THRESHOLDS")
}

# Topics MQTT
TOPICS = [
    ("Signal/PageMonitoring", 0),
    ("Reunion_Temp/topic", 0),
    ("Bureau_Temp/topic", 0),
    ("Detente_Temp/topic", 0),
    ("Reunion_AirQuality/topic", 0),
    ("Bureau_AirQuality/topic", 0),
    ("Detente_AirQuality/topic", 0),
    ("Reunion_Light/topic", 0),
    ("Bureau_Light/topic", 0),
    ("Detente_Light/topic", 0),
    ("user/authentication", 0),
    ("Seuil/Temperature", 0),
    ("Seuil/CO2", 0),
    ("Seuil/Light", 0)
]

# Mapping des topics de seuils aux tables de la base de données
THRESHOLD_TOPICS = {
    "Seuil/Temperature": "Logs_Temperature",
    "Seuil/CO2": "Logs_CO2",
    "Seuil/Light": "Logs_Luminosity"
}

# Liste des topics pour les moyennes
AVERAGE_TOPICS = {
    "temperature": "Average/Temperature",
    "co2": "Average/CO2",
    "light": "Average/Light"
}

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler()]
)
# Dictionnaires pour stocker les valeurs reçues des capteurs
temperature_data = {"Zone_Reunion": None, "Zone_Bureau": None, "Zone_Detente": None}
co2_data = {"Zone_Reunion": None, "Zone_Bureau": None, "Zone_Detente": None}
light_data = {"Zone_Reunion": None, "Zone_Bureau": None, "Zone_Detente": None}

# Verrou pour synchroniser l'accès aux données
data_lock = threading.Lock()
request_lock = threading.Lock()  # Verrou pour éviter les signaux multiples

# Indicateur pour vérifier si toutes les données ont été reçues
data_received = {"temperature": False, "co2": False, "light": False}

# Indicateur pour éviter les demandes multiples
request_in_progress = False

# Fonction pour vérifier les identifiants dans la base de données du gestionnaire
def verify_credentials(username, password):
    try:
        conn = mysql.connector.connect(**DB_CONFIG_MANAGER)  # Connexion à la base de données du gestionnaire
        cursor = conn.cursor()
        query = "SELECT * FROM Gestionnaire_Info WHERE User = %s AND Password = %s"
        cursor.execute(query, (username, password))
        result = cursor.fetchone()
        cursor.close()
        conn.close()
        return result is not None
    except Exception as e:
        logging.error(f"[ERREUR] {e}")
        return False


def save_threshold_to_db(data, table_name):
    """
    Enregistre les seuils dans la base de données.
    """
    try:
        conn = mysql.connector.connect(**DB_CONFIG_THRESHOLDS)
        cursor = conn.cursor()

        # Préparer la requête SQL en fonction de la table
        if table_name == "Logs_Temperature":
            sql = f"""
                INSERT INTO {table_name} (Logs_Temperature_Seuil, Date_Heure)
                VALUES (%s, NOW())
            """
            values = (data["Logs_Temperature_Seuil"],)
        elif table_name == "Logs_CO2":
            sql = f"""
                INSERT INTO {table_name} (Logs_C02_Seuil, Date_Heure)
                VALUES (%s, NOW())
            """
            values = (data["Logs_C02_Seuil"],)
        elif table_name == "Logs_Luminosity":
            sql = f"""
                INSERT INTO {table_name} (Logs_Temperature_Seuil, Date_Heure)
                VALUES (%s, NOW())
            """
            values = (data["Logs_Temperature_Seuil"],)

        # Exécuter la requête SQL
        cursor.execute(sql, values)
        conn.commit()
        logging.info(f"[SUCCÈS] Seuils enregistrés dans {table_name}.")
    except Exception as e:
        logging.error(f"[ERREUR] Impossible d'enregistrer les seuils dans la base de données : {e}")
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()


def on_connect(client, userdata, flags, rc):
    """
    Callback appelé lorsque le client se connecte au broker MQTT.
    """
    if rc == 0:
        logging.info(f"[INFO] Connecté au broker MQTT à {BROKER}:{PORT}")
        client.subscribe(TOPICS)
        logging.info("[INFO] Abonné aux topics MQTT.")
    else:
        logging.error(f"[ERREUR] Échec de la connexion au broker MQTT, code de retour : {rc}")


def request_sensor_data(client):
    """
    Envoie des signaux explicites aux capteurs pour demander les données.
    """
    global request_in_progress

    with request_lock:
        if request_in_progress:
            logging.info("[INFO] Une demande est déjà en cours. Aucune nouvelle demande envoyée.")
            return

        try:
            # Marquer qu'une demande est en cours
            request_in_progress = True

            # Publier les signaux pour demander les données
            client.publish("Signal/RequestTemperature", "Request")
            logging.info("[INFO] Signal envoyé pour RequestTemperature")
            client.publish("Signal/RequestCO2", "Request")
            logging.info("[INFO] Signal envoyé pour RequestCO2")
            client.publish("Signal/RequestLight", "Request")
            logging.info("[INFO] Signal envoyé pour RequestLight")
        except Exception as e:
            logging.error(f"[ERREUR] Échec de l'envoi des signaux aux capteurs : {e}")
            request_in_progress = False  # Réinitialiser en cas d'erreur



def on_message(client, userdata, msg):
    """
    Callback appelé lorsque le client reçoit un message sur un topic abonné.
    """
    global temperature_data, co2_data, light_data, data_received, request_in_progress

    topic = msg.topic
    payload = msg.payload.decode("utf-8")

    logging.info(f"[MQTT] Reçu : {topic} -> {payload}")

    with data_lock:
        # Traitement des seuils
        if topic in THRESHOLD_TOPICS:
            table_name = THRESHOLD_TOPICS[topic]
            threshold_data = {}
            if table_name == "Logs_Temperature":
                threshold_data = {"Logs_Temperature_Seuil": float(payload)}
            elif table_name == "Logs_CO2":
                threshold_data = {"Logs_C02_Seuil": float(payload)}
            elif table_name == "Logs_Luminosity":
                threshold_data = {"Logs_Temperature_Seuil": float(payload)}
            save_threshold_to_db(threshold_data, table_name)

        # Traitement des valeurs des capteurs
        elif topic in ["Reunion_Temp/topic", "Bureau_Temp/topic", "Detente_Temp/topic"]:
            zone = topic.split("_")[0].split("/")[-1]
            temperature_data[f"Zone_{zone}"] = float(payload)
            data_received["temperature"] = all(temperature_data.values())
        elif topic in ["Reunion_AirQuality/topic", "Bureau_AirQuality/topic", "Detente_AirQuality/topic"]:
            zone = topic.split("_")[0].split("/")[-1]
            co2_data[f"Zone_{zone}"] = float(payload)
            data_received["co2"] = all(co2_data.values())
        elif topic in ["Reunion_Light/topic", "Bureau_Light/topic", "Detente_Light/topic"]:
            zone = topic.split("_")[0].split("/")[-1]
            light_data[f"Zone_{zone}"] = float(payload)
            data_received["light"] = all(light_data.values())

        # Vérifier si toutes les données ont été reçues
        if all(data_received.values()):
            logging.info("[INFO] Toutes les données ont été reçues. Calcul des moyennes...")
            calculate_and_publish_average(client, temperature_data, "temperature")
            calculate_and_publish_average(client, co2_data, "co2")
            calculate_and_publish_average(client, light_data, "light")
             # Réinitialiser les indicateurs et les données après le calcul
            data_received = {"temperature": False, "co2": False, "light": False}
            temperature_data = {"Zone_Reunion": None, "Zone_Bureau": None, "Zone_Detente": None}
            co2_data = {"Zone_Reunion": None, "Zone_Bureau": None, "Zone_Detente": None}
            light_data = {"Zone_Reunion": None, "Zone_Bureau": None, "Zone_Detente": None}
            request_in_progress = False  # Permettre de nouvelles demandes

        # Traitement de l'authentification utilisateur
        elif topic == "user/authentication":
            try:
                auth_data = json.loads(payload)
                username = auth_data.get("username")
                password = auth_data.get("password")

                if username and password:
                    # Vérification des identifiants dans la base de données
                    if verify_credentials(username, password):
                        response = {"status": "success", "message": "Authentication successful"}
                        client.publish("user/authentication/response", json.dumps(response))
                        logging.info(f"[INFO] Authentification réussie pour l'utilisateur {username}.")
                    else:
                        response = {"status": "failure", "message": "Invalid credentials"}
                        client.publish("user/authentication/response", json.dumps(response))
                        logging.warning(f"[WARNING] Échec de l'authentification pour l'utilisateur {username}.")
                else:
                    response = {"status": "failure", "message": "Missing username or password"}
                    client.publish("user/authentication/response", json.dumps(response))
                    logging.warning("[WARNING] Identifiants manquants dans le payload.")
            except json.JSONDecodeError as e:
                logging.error(f"[ERREUR] Erreur de décodage JSON pour l'authentification : {e}")

        # Traitement de la demande explicite de calcul des moyennes
        elif topic == "Signal/PageMonitoring":
            try:
                data = json.loads(payload)
                logging.info(f"[INFO] Payload reçu : {data}")
                if data.get("request") == "data":
                    logging.info("[INFO] Demande de données reçue. Réinitialisation des indicateurs et envoi des signaux...")
                    data_received = {"temperature": False, "co2": False, "light": False}  # Réinitialisation
                    request_sensor_data(client)
                elif data.get("request") == "calculate_averages":
                    logging.info("[INFO] Demande de calcul des moyennes reçue.")
                    if all(data_received.values()):
                        logging.info("[INFO] Toutes les données ont été reçues. Calcul des moyennes...")
                        calculate_and_publish_average(client, temperature_data, "temperature")
                        calculate_and_publish_average(client, co2_data, "co2")
                        calculate_and_publish_average(client, light_data, "light")
                        # Réinitialiser les indicateurs après le calcul
                        data_received = {"temperature": False, "co2": False, "light": False}
                        request_in_progress = False
                    else:
                        logging.warning("[WARNING] Données manquantes pour le calcul des moyennes.")
                else:
                    logging.warning("[WARNING] Requête non reconnue dans le payload.")
            except json.JSONDecodeError as e:
                logging.error(f"[ERREUR] Erreur de décodage JSON : {e}")
def calculate_and_publish_average(client, data, data_type):
    """
    Calcule la moyenne des données et la publie sur le topic MQTT correspondant.
    """
    logging.info(f"[INFO] Traitement des données pour {data_type} : {data}")
    valid_values = [v for v in data.values() if isinstance(v, (int, float)) and v is not None]
    if valid_values:
        average = round(sum(valid_values) / len(valid_values), 2)
        logging.info(f"[INFO] Moyenne de {data_type} : {average}")
        topic = AVERAGE_TOPICS[data_type]
        client.publish(topic, average)
        logging.info(f"[INFO] Moyenne publiée sur le topic : {topic}")
    else:
        logging.warning(f"[WARNING] Aucune donnée valide pour calculer la moyenne de {data_type}.")


def main():
    """
    Point d'entrée principal pour démarrer le client MQTT et gérer les cycles de traitement.
    """
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_message = on_message

    # Connexion au broker MQTT
    client.connect(BROKER, PORT, 60)

    # Lancer la boucle MQTT
    client.loop_forever()


if __name__ == "__main__":
    main()