# Importation des bibliothèques nécessaires
import paho.mqtt.client as mqtt  # Bibliothèque pour la communication MQTT
import mysql.connector  # Bibliothèque pour la connexion à la base de données MySQL
from dotenv import load_dotenv  # Bibliothèque pour charger les variables d'environnement
import os  # Bibliothèque pour les fonctions système
import time  # Bibliothèque pour la gestion du temps
import threading  # Bibliothèque pour la gestion des threads
import json  # Bibliothèque pour manipuler les données JSON

# Charger les variables d'environnement
load_dotenv()

# Configuration de la base de données des capteurs
DB_CONFIG_SENSORS = {
    "host": os.getenv("DB_HOST_SENSORS"),
    "user": os.getenv("DB_USER_SENSORS"),
    "password": os.getenv("DB_PASSWORD_SENSORS"),
    "database": os.getenv("DB_NAME_SENSORS")
}

# Configuration de la base de données du gestionnaire
DB_CONFIG_MANAGER = {
    "host": os.getenv("DB_HOST_MANAGER"),
    "user": os.getenv("DB_USER_MANAGER"),
    "password": os.getenv("DB_PASSWORD_MANAGER"),
    "database": os.getenv("DB_NAME_MANAGER")
}

# Configuration du broker MQTT
BROKER = os.getenv("MQTT_BROKER")
PORT = int(os.getenv("MQTT_PORT"))

TOPICS = [
    ("Signal/RequestTemperature", 0),  # Topic pour les demandes de température
    ("Signal/RequestCO2", 0),  # Topic pour les demandes de CO2
    ("Signal/RequestLight", 0),  # Topic pour les demandes de luminosité
    ("Reunion_Temp/topic", 0),  # Topic pour les données de température de la zone de réunion
    ("Bureau_Temp/topic", 0),  # Topic pour les données de température de la zone de bureau
    ("Detente_Temp/topic", 0),  # Topic pour les données de température de la zone de détente
    ("Reunion_AirQuality/topic", 0),  # Topic pour les données de qualité de l'air de la zone de réunion
    ("Bureau_AirQuality/topic", 0),  # Topic pour les données de qualité de l'air de la zone de bureau
    ("Detente_AirQuality/topic", 0),  # Topic pour les données de qualité de l'air de la zone de détente
    ("Reunion_Light/topic", 0),  # Topic pour les données de luminosité de la zone de réunion
    ("Bureau_Light/topic", 0),  # Topic pour les données de luminosité de la zone de bureau
    ("Detente_Light/topic", 0),  # Topic pour les données de luminosité de la zone de détente
    ("user/authentication", 0),  # Topic pour l'authentification de l'utilisateur
    ("Signal/PageMonitoring", 0)  # Topic pour l'ouverture de la page de monitoring
]

# Liste des topics de demande et des tables de logs correspondantes
REQUEST_TOPICS = [
    ("Signal/RequestTemperature", "Logs_Temperature"),  # Topic de demande de température et table de logs correspondante
    ("Signal/RequestCO2", "Logs_CO2"),  # Topic de demande de CO2 et table de logs correspondante
    ("Signal/RequestLight", "Logs_Luminosity")  # Topic de demande de luminosité et table de logs correspondante
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

# Variables pour stocker le temps de la dernière mise à jour pour chaque type de données
last_update_temp = None
last_update_co2 = None
last_update_light = None

# Variables pour stocker les moyennes
average_temperature = None
average_co2 = None
average_light = None

# Variable pour indiquer si la demande provient de la page de monitoring
from_page_monitoring = False

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
        print(f"[ERREUR] {e}")
        return False

# Fonction de rappel pour la réception des messages MQTT
def on_message(client, userdata, msg):
    global last_update_temp, last_update_co2, last_update_light, from_page_monitoring
    topic = msg.topic  # Récupération du topic du message
    payload = msg.payload.decode("utf-8")  # Décodage du payload du message

    print(f"[MQTT] Reçu : {topic} -> {payload}")  # Affichage du message reçu

    # Vérification des identifiants de connexion
    if topic == "user/authentication":
        print("[INFO] Vérification des identifiants de connexion...")
        credentials = json.loads(payload)
        username = credentials["username"]
        password = credentials["password"]
        print(f"[INFO] Vérification des identifiants pour l'utilisateur : {username}")
        if verify_credentials(username, password):
            print("[INFO] Identifiants de connexion valides.")
            client.publish("user/authentication/response", json.dumps({"status": "success"}))
        else:
            print("[ERREUR] Identifiants de connexion invalides.")
            client.publish("user/authentication/response", json.dumps({"status": "failure"}))

    # Traitement des valeurs de température
    elif topic == "Reunion_Temp/topic":
        temperature_data["Zone_Reunion"] = float(payload)
    elif topic == "Bureau_Temp/topic":
        temperature_data["Zone_Bureau"] = float(payload)
    elif topic == "Detente_Temp/topic":
        temperature_data["Zone_Detente"] = float(payload)
    
    # Traitement des valeurs de CO2
    elif topic == "Reunion_AirQuality/topic":
        co2_data["Zone_Reunion"] = float(payload)
    elif topic == "Bureau_AirQuality/topic":
        co2_data["Zone_Bureau"] = float(payload)
    elif topic == "Detente_AirQuality/topic":
        co2_data["Zone_Detente"] = float(payload)

    # Traitement des valeurs de luminosité
    elif topic == "Reunion_Light/topic":
        light_data["Zone_Reunion"] = float(payload)
    elif topic == "Bureau_Light/topic":
        light_data["Zone_Bureau"] = float(payload)
    elif topic == "Detente_Light/topic":
        light_data["Zone_Detente"] = float(payload)

    # Traitement de l'ouverture de la page de monitoring
    elif topic == "Signal/PageMonitoring":
        print("[INFO] Page de monitoring ouverte. Demande des données aux capteurs...")
        from_page_monitoring = True
        request_sensor_data()

    # Déclenchement du premier message pour chaque type de données
    if topic.startswith("Reunion_Temp") or topic.startswith("Bureau_Temp") or topic.startswith("Detente_Temp"):
        if last_update_temp is None:
            last_update_temp = time.time()

    if topic.startswith("Reunion_AirQuality") or topic.startswith("Bureau_AirQuality") or topic.startswith("Detente_AirQuality"):
        if last_update_co2 is None:
            last_update_co2 = time.time()

    if topic.startswith("Reunion_Light") or topic.startswith("Bureau_Light") or topic.startswith("Detente_Light"):
        if last_update_light is None:
            last_update_light = time.time()

# Fonction pour demander les données aux capteurs
def request_sensor_data():
    for topic, _ in REQUEST_TOPICS:
        client.publish(topic, "Request")
        print(f"[MQTT] Demande envoyée : {topic} -> Request")

# Fonction de surveillance et d'enregistrement des données
def check_and_save():
    global last_update_temp, last_update_co2, last_update_light, from_page_monitoring
    while True:
        time.sleep(0.5)  # Vérification toutes les 500 ms

        # Enregistrement des températures
        if last_update_temp and time.time() - last_update_temp >= 2:
            if all(v is not None for v in temperature_data.values()):
                if from_page_monitoring:
                    # Si la demande provient de la page web, envoyer uniquement les moyennes
                    calculate_and_publish_average(temperature_data, "temperature")
                else:
                    # Sinon, enregistrer dans la base de données
                    save_to_db(temperature_data, "Logs_Temperature")
                last_update_temp = None  # Réinitialisation après traitement
                for key in temperature_data:
                    temperature_data[key] = None  # Réinitialisation des valeurs
            else:
                print("[ATTENTE] Température : toutes les valeurs ne sont pas encore reçues.")

        # Enregistrement du CO2
        if last_update_co2 and time.time() - last_update_co2 >= 2:
            if all(v is not None for v in co2_data.values()):
                if from_page_monitoring:
                    # Si la demande provient de la page web, envoyer uniquement les moyennes
                    calculate_and_publish_average(co2_data, "co2")
                else:
                    # Sinon, enregistrer dans la base de données
                    save_to_db(co2_data, "Logs_CO2")
                last_update_co2 = None  # Réinitialisation après traitement
                for key in co2_data:
                    co2_data[key] = None  # Réinitialisation des valeurs
            else:
                print("[ATTENTE] CO2 : toutes les valeurs ne sont pas encore reçues.")

        # Enregistrement de la luminosité
        if last_update_light and time.time() - last_update_light >= 2:
            if all(v is not None for v in light_data.values()):
                if from_page_monitoring:
                    # Si la demande provient de la page web, envoyer uniquement les moyennes
                    calculate_and_publish_average(light_data, "light")
                else:
                    # Sinon, enregistrer dans la base de données
                    save_to_db(light_data, "Logs_Luminosity")
                last_update_light = None  # Réinitialisation après traitement
                for key in light_data:
                    light_data[key] = None  # Réinitialisation des valeurs
            else:
                print("[ATTENTE] Luminosité : toutes les valeurs ne sont pas encore reçues.")

        # Réinitialiser la variable from_page_monitoring après traitement
        if from_page_monitoring:
            from_page_monitoring = False

# Fonction pour calculer et publier les moyennes sans les enregistrer dans la base de données
def calculate_and_publish_average(data, data_type):
    valid_values = [v for v in data.values() if v is not None]
    if valid_values:
        average = round(sum(valid_values) / len(valid_values), 2)  # Calcul de la moyenne avec 2 chiffres après la virgule
        print(f"[INFO] Moyenne de {data_type} : {average}")
        client.publish(AVERAGE_TOPICS[data_type], average)  # Publication de la moyenne
    else:
        print(f"[INFO] Aucune valeur valide pour {data_type}.")

# Fonction pour enregistrer les données dans la base de données
def save_to_db(data, table_name):
    global average_temperature, average_co2, average_light
    try:
        conn = mysql.connector.connect(**DB_CONFIG_SENSORS)  # Connexion à la base de données des capteurs
        cursor = conn.cursor()  # Création d'un curseur pour exécuter les requêtes SQL
        sql = f"""
            INSERT INTO {table_name} (Zone_Reunion, Zone_Bureau, Zone_Detente, Date_Heure, Moy) 
            VALUES (%s, %s, %s, NOW(), %s)
        """
        # Remplacer les valeurs manquantes par -1
        values = tuple(v if v is not None else -1 for v in data.values())
        
        # Calcul de la moyenne en fonction du type de données
        if table_name == "Logs_Temperature":
            valid_values = [v for v in data.values() if v is not None]
            if valid_values:
                average_temperature = round(sum(valid_values) / len(valid_values), 2)  # Calcul de la moyenne avec 2 chiffres après la virgule
                print(f"Moyenne de la température : {average_temperature}")
                client.publish(AVERAGE_TOPICS["temperature"], average_temperature)  # Publication de la moyenne
            else:
                average_temperature = -1  # Si aucune valeur valide, mettre -1
            values += (average_temperature,)  # Ajouter la moyenne aux valeurs

        elif table_name == "Logs_CO2":
            valid_values = [v for v in data.values() if v is not None]
            if valid_values:
                average_co2 = round(sum(valid_values) / len(valid_values), 2)  # Calcul de la moyenne avec 2 chiffres après la virgule
                print(f"Moyenne du CO2 : {average_co2}")
                client.publish(AVERAGE_TOPICS["co2"], average_co2)  # Publication de la moyenne
            else:
                average_co2 = -1  # Si aucune valeur valide, mettre -1
            values += (average_co2,)  # Ajouter la moyenne aux valeurs

        elif table_name == "Logs_Luminosity":
            valid_values = [v for v in data.values() if v is not None]
            if valid_values:
                average_light = round(sum(valid_values) / len(valid_values), 2)  # Calcul de la moyenne avec 2 chiffres après la virgule
                print(f"Moyenne de la luminosité : {average_light}")
                client.publish(AVERAGE_TOPICS["light"], average_light)  # Publication de la moyenne
            else:
                average_light = -1  # Si aucune valeur valide, mettre -1
            values += (average_light,)  # Ajouter la moyenne aux valeurs

        cursor.execute(sql, values)  # Exécution de la requête SQL
        conn.commit()  # Validation de la transaction
        cursor.close()  # Fermeture du curseur
        conn.close()  # Fermeture de la connexion
        print(f"[SUCCÈS] Données enregistrées dans {table_name}.")

    except Exception as e:
        print(f"[ERREUR] {e}")

# Fonction pour envoyer des demandes MQTT périodiquement
def send_mqtt_requests():
    global client
    while True:
        for topic, log_table in REQUEST_TOPICS:
            client.publish(topic, "Request")
            print(f"[MQTT] Envoyé : {topic} -> Request")
            time.sleep(5)  # Attendre 5 secondes entre chaque type de capteur

        time.sleep(175)  # Compléter le cycle pour atteindre 3 minutes

# Connexion au broker MQTT
client = mqtt.Client()
client.on_message = on_message  # Définir la fonction de rappel pour la réception des messages
client.connect(BROKER, PORT, 60)  # Connexion au broker MQTT
client.subscribe(TOPICS)  # S'abonner aux topics

# Lancement de la surveillance des messages en arrière-plan
threading.Thread(target=send_mqtt_requests, daemon=True).start()
threading.Thread(target=check_and_save, daemon=True).start()

print("[INFO] En attente des messages MQTT...")
client.loop_forever()  # Boucle infinie pour maintenir la connexion MQTT

# Fin du programme