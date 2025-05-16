import os
import time
import threading
import logging
import paho.mqtt.client as mqtt
import mysql.connector
from dotenv import load_dotenv

# Charger les variables d'environnement
load_dotenv()

# Configuration du gestionnaire
DB_CONFIG_MANAGER = {
    "host": os.getenv("DB_HOST_MANAGER"),
    "user": os.getenv("DB_USER_MANAGER"),
    "password": os.getenv("DB_PASSWORD_MANAGER"),
    "database": os.getenv("DB_NAME_MANAGER")
}

# Configuration de la base de données des capteurs
DB_CONFIG_SENSORS = {
    "host": os.getenv("DB_HOST_SENSORS"),
    "user": os.getenv("DB_USER_SENSORS"),
    "password": os.getenv("DB_PASSWORD_SENSORS"),
    "database": os.getenv("DB_NAME_SENSORS")
}

# Configuration du broker MQTT
BROKER = os.getenv("MQTT_BROKER")
PORT = int(os.getenv("MQTT_PORT"))

# Topics MQTT
TOPICS = [
    ("Reunion_Temp/topic", 0),
    ("Bureau_Temp/topic", 0),
    ("Detente_Temp/topic", 0),
    ("Reunion_AirQuality/topic", 0),
    ("Bureau_AirQuality/topic", 0),
    ("Detente_AirQuality/topic", 0),
    ("Reunion_Light/topic", 0),
    ("Bureau_Light/topic", 0),
    ("Detente_Light/topic", 0)
]

# Liste des topics pour les moyennes
AVERAGE_TOPICS = {
    "temperature": "Average/Temperature",
    "co2": "Average/CO2",
    "light": "Average/Light"
}

# Dictionnaires pour stocker les valeurs reçues des capteurs
temperature_data = {"Zone_Reunion": None, "Zone_Bureau": None, "Zone_Detente": None}
co2_data = {"Zone_Reunion": None, "Zone_Bureau": None, "Zone_Detente": None}
light_data = {"Zone_Reunion": None, "Zone_Bureau": None, "Zone_Detente": None}

# Variables pour suivre l'état des données reçues
temperature_received = False
co2_received = False
light_received = False

# Verrou pour synchroniser l'accès aux données
data_lock = threading.Lock()

# Configuration des logs
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")


def on_message(client, userdata, msg):
    """
    Callback pour le traitement des messages MQTT reçus.
    """
    global temperature_received, co2_received, light_received

    topic = msg.topic
    payload = msg.payload.decode("utf-8", errors="replace")  # Remplace les caractères invalides

    try:
        with data_lock:
            # Afficher les logs uniquement pour les nouvelles données
            logging.info(f"[MQTT] Reçu : {topic} -> {payload}")

            # Traitement des valeurs de température
            if topic == "Reunion_Temp/topic":
                temperature_data["Zone_Reunion"] = float(payload)
            elif topic == "Bureau_Temp/topic":
                temperature_data["Zone_Bureau"] = float(payload)
            elif topic == "Detente_Temp/topic":
                temperature_data["Zone_Detente"] = float(payload)

            # Vérifier si toutes les données de température sont reçues
            temperature_received = all(v is not None for v in temperature_data.values())

            # Traitement des valeurs de CO2
            if topic == "Reunion_AirQuality/topic":
                co2_data["Zone_Reunion"] = float(payload)
            elif topic == "Bureau_AirQuality/topic":
                co2_data["Zone_Bureau"] = float(payload)
            elif topic == "Detente_AirQuality/topic":
                co2_data["Zone_Detente"] = float(payload)

            # Vérifier si toutes les données de CO2 sont reçues
            co2_received = all(v is not None for v in co2_data.values())

            # Traitement des valeurs de luminosité
            if topic == "Reunion_Light/topic":
                light_data["Zone_Reunion"] = float(payload)
            elif topic == "Bureau_Light/topic":
                light_data["Zone_Bureau"] = float(payload)
            elif topic == "Detente_Light/topic":
                light_data["Zone_Detente"] = float(payload)

            # Vérifier si toutes les données de lumière sont reçues
            light_received = all(v is not None for v in light_data.values())
    except ValueError:
        logging.error(f"[ERREUR] Impossible de convertir la valeur reçue : {payload}")

def request_sensor_data(client, data_type):
    """
    Fonction pour demander les données des capteurs via MQTT.
    """
    logging.info(f"[INFO] Demande des valeurs pour {data_type}...")
    if data_type == "temperature":
        client.publish("Signal/RequestTemperature", "Request")
    elif data_type == "co2":
        client.publish("Signal/RequestCO2", "Request")
    elif data_type == "light":
        client.publish("Signal/RequestLight", "Request")
    logging.info(f"[INFO] Signal envoyé pour {data_type}.")


def wait_for_data_with_retry(client, data_type, timeout=10):
    """
    Attend que toutes les données pour un type donné soient reçues ou qu'un délai soit dépassé.
    Si les données ne sont pas reçues, renvoie une fois le signal de demande.
    """
    global temperature_received, co2_received, light_received

    start_time = time.time()
    retry_sent = False

    while True:
        # Vérifier si toutes les données ont été reçues
        if data_type == "temperature" and temperature_received:
            logging.info(f"[INFO] Toutes les données de {data_type} ont été reçues.")
            return True
        elif data_type == "co2" and co2_received:
            logging.info(f"[INFO] Toutes les données de {data_type} ont été reçues.")
            return True
        elif data_type == "light" and light_received:
            logging.info(f"[INFO] Toutes les données de {data_type} ont été reçues.")
            return True

        # Vérifier si le délai est dépassé
        if time.time() - start_time > timeout:
            if not retry_sent:
                logging.warning(f"[WARNING] Données manquantes pour {data_type}. Renvoi du signal...")
                request_sensor_data(client, data_type)  # Renvoi du signal
                retry_sent = True
                start_time = time.time()  # Réinitialiser le délai après le renvoi
            else:
                logging.error(f"[ERROR] Données toujours manquantes pour {data_type} après une deuxième tentative.")
                return False

        time.sleep(1)
def calculate_and_save_data(client, data, data_type):
    """
    Calcule la moyenne des données, la publie sur le topic MQTT correspondant
    et enregistre les valeurs et la moyenne dans la base de données.
    """
    global temperature_received, co2_received, light_received

    logging.info(f"[INFO] Traitement des données pour {data_type} : {data}")
    valid_values = [v for v in data.values() if isinstance(v, (int, float)) and v is not None]
    if valid_values:
        average = round(sum(valid_values) / len(valid_values), 2)
        logging.info(f"[INFO] Moyenne de {data_type} : {average}")
        client.publish(AVERAGE_TOPICS[data_type], average)
        save_data_to_db(data, data_type, average)
    else:
        logging.warning(f"[WARNING] Aucune donnée valide pour calculer la moyenne de {data_type}.")

    # Réinitialiser l'indicateur pour ce type de données
    if data_type == "temperature":
        temperature_received = False
    elif data_type == "co2":
        co2_received = False
    elif data_type == "light":
        light_received = False

def save_data_to_db(data, data_type, average):
    """
    Enregistre les valeurs des capteurs et la moyenne dans la base de données.
    """
    try:
        # Connexion à la base de données
        conn = mysql.connector.connect(**DB_CONFIG_SENSORS)
        cursor = conn.cursor()

        # Déterminer la table cible en fonction du type de données
        table_mapping = {
            "temperature": "Logs_Temperature",
            "co2": "Logs_CO2",
            "light": "Logs_Luminosite"
        }
        table_name = table_mapping.get(data_type)
        if not table_name:
            logging.error(f"[ERREUR] Type de données inconnu : {data_type}")
            return

        # Préparer la requête SQL
        query = f"""
            INSERT INTO {table_name} (Zone_Reunion, Zone_Bureau, Zone_Detente, Moy, Date_Heure)
            VALUES (%s, %s, %s, %s, NOW())
        """
        values = (
            data.get("Zone_Reunion", -1),  # Valeur par défaut si aucune donnée
            data.get("Zone_Bureau", -1),
            data.get("Zone_Detente", -1),
            average
        )

        # Exécuter la requête et valider les changements
        cursor.execute(query, values)
        conn.commit()
        logging.info(f"[INFO] Données de {data_type} enregistrées dans la table {table_name}.")
    except mysql.connector.Error as e:
        logging.error(f"[ERREUR] Impossible d'enregistrer les données dans la base de données : {e}")
    finally:
        # Fermer la connexion à la base de données
        if conn.is_connected():
            cursor.close()
            conn.close()


def main():
    """
    Point d'entrée principal pour démarrer le client MQTT et gérer les cycles de traitement.
    """
    global temperature_received, co2_received, light_received

    client = mqtt.Client()
    client.on_message = on_message

    # Connexion au broker MQTT
    client.connect(BROKER, PORT, 60)

    # S'abonner aux topics
    client.subscribe(TOPICS)

    # Lancer la boucle MQTT dans un thread
    mqtt_thread = threading.Thread(target=client.loop_forever, daemon=True)
    mqtt_thread.start()

    # Lancer le traitement des données
    while True:
        # Traiter les données de température
        request_sensor_data(client, "temperature")
        if wait_for_data_with_retry(client, "temperature", timeout=10):
            with data_lock:
                calculate_and_save_data(client, temperature_data, "temperature")
                temperature_received = False

        # Traiter les données de CO2
        request_sensor_data(client, "co2")
        if wait_for_data_with_retry(client, "co2", timeout=10):
            with data_lock:
                calculate_and_save_data(client, co2_data, "co2")
                co2_received = False

        # Traiter les données de luminosité
        request_sensor_data(client, "light")
        if wait_for_data_with_retry(client, "light", timeout=10):
            with data_lock:
                calculate_and_save_data(client, light_data, "light")
                light_received = False

        # Attendre 3 minutes avant le prochain cycle
        logging.info("[INFO] Attente de 3 minutes avant le prochain cycle...")
        time.sleep(180)


if __name__ == "__main__":
    main()