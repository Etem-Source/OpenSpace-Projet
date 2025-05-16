import paho.mqtt.client as mqtt
import mysql.connector
import datetime

# Configuration MySQL
db_config = {
    "host": "localhost",
    "user": "Client",
    "password": "Cl1entP@ss105",
    "database": "Clients"
}

# Configuration MQTT
broker_address = "localhost"  # Change if Mosquitto broker is on a different machine
topic = "auth/topic"

def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))
    client.subscribe(topic)
    client.subscribe("reservation/topic")

def on_message(client, userdata, msg):
    message = msg.payload.decode()
    print(f"Received message: {message}")

    # Ne traiter que les messages de requête valides et ignorer les réponses
    if message.startswith("GOS;01;") or message.startswith("GOS;02;reunion") or \
       message.startswith("GOS;05;requestreservations") or message.startswith("GOS;04;askreservations") or \
       message.startswith("GOS;05;cancelreservation") or message.startswith("GOS;02;bureau") or message.startswith("GOS;07;askallreservations") or message.startswith("GOS;05;query;bureau"):
        response = handle_request(message)
        client.publish(msg.topic, response)
        print(f"Sent response: {response}")
    else:
        print(f"Ignored message: {message}")

def handle_request(trame):
    print(f"Handling request: {trame}")
    parts = trame.split(";")
    if len(parts) < 2 or parts[0] != "GOS":
        return "GOS;00;error;Invalid request format"

    action = parts[1]
    print(f"Action: {action}")

    if action == "01":  # Connexion
        if len(parts) < 4:
            return "GOS;00;error;Invalid login format"
        identifier, password = parts[2], parts[3]
        print(f"Login with identifier: {identifier}, password: {password}")
        return handle_login(identifier, password)
    elif action == "02" and len(parts) >= 5 and parts[2] == "reunion":  # Réservation réunion
        username, date, start_time, end_time = parts[3:7]
        print(f"Reservation with username: {username}, date: {date}, start_time: {start_time}, end_time: {end_time}")
        return handle_reservation(username, date, start_time, end_time)
    elif action == "02" and len(parts) >= 8 and parts[2] == "bureau":
        username, start_date, end_date, num_bureau, start_time, end_time = parts[3:9]
        print(f"Bureau Reservation for user: {username}, startDate: {start_date}, endDate: {end_date}, num_bureau: {num_bureau}, startTime: {start_time}, endTime: {end_time}")
        return handle_office_reservation(username, start_date, end_date, num_bureau, start_time, end_time)
    elif action == "04" and len(parts) >= 3 and parts[2] == "askreservations":  # Fetch reservations
        username = parts[3]
        return fetch_reservations(username)
    elif action == "05" and len(parts) >= 4 and parts[2] == "requestreservations":  # Fetch office reservations
        username = parts[3]
        return fetch_reservations_bureau(username)
    elif action == "05" and len(parts) >= 6 and parts[2] == "cancelreservation":  # Annuler réservation
        username, date, start_time, end_time = parts[3:7]
        print(f"Cancel reservation with username: {username}, date: {date}, start_time: {start_time}, end_time: {end_time}")
        return handle_cancel_reservation(username, date, start_time, end_time)
    elif action == "05" and len(parts) >= 4 and parts[2] == "query" and parts[3] == "bureau":  # Requête pour les réservations d'un bureau spécifique
        num_bureau = parts[4]
        print(f"Query reservations for office number: {num_bureau}")
        return fetch_office_reservations_by_number(num_bureau)
    elif action == "06" and len(parts) >= 3 and parts[2] == "requestfuturereservations":  # Récupérer les réservations futures
        username = parts[3]
        return fetch_future_reservations(username)
    elif action == "07" and len(parts) >= 4 and parts[2] == "askallreservations":  # Récupérer toutes les réservations
        reservation_type = parts[3]
        print(f"Fetching all {reservation_type} reservations")
        return fetch_all_reservations()

    else:
        return "GOS;00;error;Unknown action"

def handle_login(identifier, password):
    try:
        # Connexion à la base de données
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        # Vérifier si l'utilisateur se connecte avec un email ou un username
        if "@" in identifier:
            query = "SELECT * FROM Clients_Info WHERE mail = %s"
        else:
            query = "SELECT * FROM Clients_Info WHERE username = %s"

        cursor.execute(query, (identifier,))
        user = cursor.fetchone()

        if not user:
            return "GOS;00;error;User not found"

        # Vérification du mot de passe en clair
        if password == user["password"]:
            return f"GOS;00;success;{user['mail']};{user['age']};{user['name']};{user['firstname']};{user['username']}"
        else:
            return "GOS;00;error;Invalid password"
    except mysql.connector.Error as e:
        return f"GOS;00;error;Database error: {str(e)}"
    except Exception as e:
        return f"GOS;00;error;{str(e)}"

def handle_reservation(username, date, start_time, end_time):
    try:
        # Convertir la date de millisecondes à un format de date acceptable par MySQL
        date = datetime.datetime.fromtimestamp(int(date)/1000).strftime('%Y-%m-%d')

        # Connexion à la base de données
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        # Vérifier les conflits de réservation
        query = """
        SELECT * FROM Reservations_Meeting
        WHERE date = %s AND
              (start_time < %s AND end_time > %s)
        """
        cursor.execute(query, (date, end_time, start_time))
        if cursor.fetchone():
            return "GOS;02;error;Time slot already reserved"

        # Insérer la réservation dans la base de données
        query = """
        INSERT INTO Reservations_Meeting (username, date, start_time, end_time)
        VALUES (%s, %s, %s, %s)
        """
        cursor.execute(query, (username, date, start_time, end_time))

        conn.commit()

        return "GOS;02;success;Reservation successful"
    except mysql.connector.Error as e:
        return f"GOS;02;error;Database error: {str(e)}"
    except Exception as e:
        return f"GOS;02;error;{str(e)}"

def handle_office_reservation(username, start_date, end_date, num_bureau, start_time, end_time):
    try:
        # Convertir les dates de millisecondes à un format de date acceptable par MySQL
        start_date = datetime.datetime.fromtimestamp(int(start_date) / 1000).strftime('%Y-%m-%d')
        end_date = datetime.datetime.fromtimestamp(int(end_date) / 1000).strftime('%Y-%m-%d')

        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        # Vérifier si le bureau est disponible
        check_query = """
        SELECT * FROM Reservations_Office
        WHERE num_bureau = %s
          AND ((start_date <= %s AND end_date >= %s) OR (start_date <= %s AND end_date >= %s))
          AND ((start_time <= %s AND end_time >= %s) OR (start_time <= %s AND end_time >= %s));
        """
        cursor.execute(check_query, (num_bureau, start_date, start_date, end_date, end_date, start_time, start_time, end_time, end_time))
        if cursor.fetchone():
            # Bureau indisponible, proposer un autre bureau
            for new_bureau in range(1, 11):  # Numéros de bureaux de 1 à 10
                if new_bureau == int(num_bureau):
                    continue
                cursor.execute(check_query, (new_bureau, start_date, start_date, end_date, end_date, start_time, start_time, end_time, end_time))
                if not cursor.fetchone():
                    return f"GOS;02;error;Horraire déjà réservés"
            return "GOS;02;error;Tous les bureaux sont réservés pour les dates sélectionnées, veuillez choisir une autre date."

        # Insérer la réservation en base
        insert_query = """
        INSERT INTO Reservations_Office (username, start_date, end_date, num_bureau, start_time, end_time)
        VALUES (%s, %s, %s, %s, %s, %s)
        """
        cursor.execute(insert_query, (username, start_date, end_date, num_bureau, start_time, end_time))
        conn.commit()
        return "GOS;02;success;Réservation réussie"
    except Exception as e:
        return f"GOS;02;error;{str(e)}"

def handle_cancel_reservation(username, date, start_time, end_time):
    try:
        # Connexion à la base de données
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        # Supprimer la réservation de la base de données
        query = """
        DELETE FROM Reservations_Meeting
        WHERE username = %s AND date = %s AND start_time = %s AND end_time = %s
        """
        cursor.execute(query, (username, date, start_time, end_time))

        conn.commit()

        if cursor.rowcount == 0:
            return "GOS;05;error;No such reservation found"

        return "GOS;05;success;Reservation cancelled successfully"
    except mysql.connector.Error as e:
        return f"GOS;05;error;Database error: {str(e)}"
    except Exception as e:
        return f"GOS;05;error;{str(e)}"


def fetch_reservations(username):
    try:
        # Connexion à la base de données
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        query = "SELECT date, start_time, end_time FROM Reservations_Meeting WHERE username = %s"
        cursor.execute(query, (username,))
        reservations = cursor.fetchall()

        response = "GOS;04;success;"
        for reservation in reservations:
            date, start_time, end_time = reservation
            # Convertir l'objet date en chaîne avant de l'utiliser avec strptime
            date_str = date.strftime('%Y-%m-%d')
            status = "past" if datetime.datetime.strptime(date_str, '%Y-%m-%d') < datetime.datetime.now() else "future"
            response += f"{date_str};{start_time};{end_time};{status};"
        
        return response.strip(';')
    except mysql.connector.Error as e:
        return f"GOS;04;error;Database error: {str(e)}"
    except Exception as e:
        return f"GOS;04;error;{str(e)}"

def fetch_reservations_bureau(username):
    try:
        # Connexion à la base de données
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        query = "SELECT start_date, end_date, start_time, end_time, num_bureau FROM Reservations_Office WHERE username = %s"
        cursor.execute(query, (username,))
        reservations = cursor.fetchall()

        response = "GOS;05;success;"
        for reservation in reservations:
            start_date, end_date, start_time, end_time, num_bureau = reservation
            # Convertir l'objet date en chaîne avant de l'utiliser avec strptime
            start_date_str = start_date.strftime('%Y-%m-%d')
            end_date_str = end_date.strftime('%Y-%m-%d')

            status = "past" if datetime.datetime.strptime(end_date_str, '%Y-%m-%d') < datetime.datetime.now() else "future"
            response += f"Bureau {num_bureau};{start_date_str};{start_time};{end_date_str};{end_time};{status};"
        
        return response.strip(';')
    except mysql.connector.Error as e:
        return f"GOS;05;error;Database error: {str(e)}"
    except Exception as e:
        return f"GOS;05;error;{str(e)}"
        
def fetch_office_reservations_by_number(num_bureau):
    """
    Récupère toutes les réservations futures pour un numéro de bureau spécifique
    """
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        # Date du jour
        today = datetime.date.today()

        # Requête SQL pour obtenir uniquement les réservations futures du bureau spécifié
        query = """
        SELECT start_date, end_date, start_time, end_time
        FROM Reservations_Office
        WHERE num_bureau = %s AND end_date >= %s
        """
        cursor.execute(query, (num_bureau, today))

        reservations = cursor.fetchall()
        conn.close()

        if not reservations:
            return f"GOS;06;bureau;{num_bureau};noreservation"

        # Création de la réponse
        response = f"GOS;06;bureau;{num_bureau}"

        # Générer la liste des dates pour chaque réservation
        for res in reservations:
            start_date = res[0]
            end_date = res[1]
            start_time = res[2]
            end_time = res[3]

            # Générer toutes les dates entre start_date et end_date
            current_date = start_date
            while current_date <= end_date:
                # Ne prendre que les dates futures ou égales à aujourd'hui
                if current_date >= today:
                    response += f";{current_date.strftime('%Y-%m-%d')}"
                # Passer au jour suivant
                current_date += datetime.timedelta(days=1)

        return response

    except mysql.connector.Error as err:
        print(f"Database error: {err}")
        return f"GOS;06;error;Database error: {err}"
    except Exception as e:
        print(f"Error: {e}")
        return f"GOS;06;error;Error: {e}"

def fetch_all_reservations():
    print("Fetching all meeting reservations")
    try:
        # Connexion à la base de données MySQL
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        # Requête pour récupérer uniquement les réservations futures
        query = """
        SELECT * FROM Reservations_Meeting
        WHERE date >= %s
        """
        today = datetime.date.today()
        cursor.execute(query, (today,))
        rows = cursor.fetchall()

        # Construire une réponse GOS
        if not rows:
            return "GOS;07;success;No reservations available"

        # Construire le format avec des points-virgules
        response = "GOS;07;success;" + ";".join(
            [f"{row[1]};{row[2]};{row[3]};{row[4]}" for row in rows]  # `username;date;start_time;end_time`
        )
        return response

    except mysql.connector.Error as err:
        print(f"Database error: {err}")
        return "GOS;07;error;Database query failed"
    finally:
        if conn.is_connected():
            cursor.close()
            conn.close()

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

client.connect(broker_address, 1883, 60)
client.loop_forever()

