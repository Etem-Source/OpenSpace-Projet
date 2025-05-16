// Connexion au broker MQTT
const client = mqtt.connect("ws://192.168.29.55:9001");

client.on('connect', function () {
    console.log('Connecté au broker MQTT');
    // Abonnement aux topics nécessaires
    client.subscribe("user/authentication/response");
    client.subscribe("Average/Temperature");
    client.subscribe("Average/Light");
    client.subscribe("Average/CO2");
    client.subscribe("Average/PeopleCount");
});

client.on('message', function (topic, message) {
    if (topic === "user/authentication/response") {
        const response = JSON.parse(message.toString());
        if (response.status === "success") {
            // Redirection vers la page de monitoring
            window.location.href = "monitoring.html";
        } else {
            const loginError = document.getElementById('loginError');
            if (loginError) {
                loginError.textContent = 'Identifiants incorrects.';
            }
        }
    } else if (topic === "Average/Temperature") {
        const temperatureElement = document.getElementById("temperature");
        if (temperatureElement) {
            temperatureElement.textContent = message.toString();
        }
    } else if (topic === "Average/Light") {
        const lightElement = document.getElementById("light");
        if (lightElement) {
            lightElement.textContent = message.toString();
        }
    } else if (topic === "Average/CO2") {
        const co2Element = document.getElementById("co2");
        if (co2Element) {
            co2Element.textContent = message.toString();
        }
    } else if (topic === "Average/PeopleCount") {
        const peopleCountElement = document.getElementById("peopleCount");
        if (peopleCountElement) {
            peopleCountElement.textContent = message.toString();
        }
    }
});

client.on('error', function (err) {
    console.error('Erreur de connexion au broker MQTT :', err.message);
    const loginError = document.getElementById('loginError');
    if (loginError) {
        loginError.textContent = 'Erreur de connexion: ' + err.message;
    }
});

// Fonction pour envoyer les informations d'authentification
function loginUser(username, password) {
    const loginData = {
        username: username,
        password: password
    };

    if (client.connected) {
        client.publish("user/authentication", JSON.stringify(loginData), function (err) {
            if (err) {
                console.error("Erreur lors de l'envoi des informations d'authentification :", err);
            } else {
                console.log("Informations d'authentification envoyées :", loginData);
            }
        });
    } else {
        console.error("Le client MQTT n'est pas connecté au broker.");
        const loginError = document.getElementById('loginError');
        if (loginError) {
            loginError.textContent = "Impossible de se connecter au serveur.";
        }
    }
}

// Fonction pour demander les données de monitoring
function sendPageMonitoringRequest() {
    if (client.connected) {
        console.log("Envoi de la demande de monitoring...");
        client.publish("Signal/PageMonitoring", JSON.stringify({ request: "data" }), function (err) {
            if (err) {
                console.error("Erreur lors de l'envoi de la demande de monitoring :", err);
            } else {
                console.log("Demande de monitoring envoyée");
            }
        });
    } else {
        console.error("Le client MQTT n'est pas connecté au broker. Réessai dans 1 seconde...");
        setTimeout(sendPageMonitoringRequest, 1000); // Réessayer après 1 seconde
    }
}

// Fonction pour publier la température actualisée
function updateTemperature() {
    const temperatureThreshold = document.getElementById('temperatureThreshold').value;

    if (!temperatureThreshold) {
        console.warn("Veuillez saisir une valeur pour la température.");
        return;
    }

    if (client.connected) {
        client.publish("Seuil/Temperature", temperatureThreshold, function (err) {
            if (err) {
                console.error("Erreur lors de l'envoi de la température :", err);
            } else {
                console.log(`Température actualisée publiée : ${temperatureThreshold}`);
            }
        });
    } else {
        console.error("Le client MQTT n'est pas connecté au broker.");
    }
}

// Fonction pour publier le CO2 actualisé et afficher l'alerte si le seuil est dépassé
function updateCO2() {
    // Récupérer la valeur du seuil de CO₂
    const co2Threshold = document.getElementById('co2Threshold').value;

    // Vérifier si la valeur de CO₂ est définie
    if (!co2Threshold) {
        console.warn("Veuillez saisir une valeur pour le CO2.");
        return;
    }

    // Convertir la valeur de CO₂ en nombre (au cas où l'utilisateur entre une valeur sous forme de chaîne)
    const co2Value = parseFloat(co2Threshold);

    // Vérifier si la valeur de CO₂ est valide
    if (isNaN(co2Value)) {
        console.error("La valeur saisie pour le CO2 n'est pas un nombre valide.");
        return;
    }

    // Vérifier si le seuil de CO₂ dépasse 25 ppm et afficher une alerte
    if (co2Value > 25) {
        alert(`Alerte : Le niveau de CO₂ (${co2Value} ppm) dépasse le seuil de 25 ppm.`);
        
        // Rediriger vers la page d'alerte
        window.location.href = 'alerte.html';  // Redirection vers la page alerte.html
    } else {
        // Masquer l'alerte si le CO₂ est inférieur ou égal à 25 ppm
        document.getElementById('alert').style.display = 'none';
    }

    // Vérifier la connexion MQTT
    if (client.connected) {
        // Publier la valeur du seuil de CO₂ sur le broker MQTT
        client.publish("Seuil/CO2", co2Threshold, function (err) {
            if (err) {
                console.error("Erreur lors de l'envoi du CO2 :", err);
            } else {
                console.log(`CO2 actualisé publié : ${co2Threshold}`);
            }
        });
    } else {
        console.error("Le client MQTT n'est pas connecté au broker.");
    }
}


// Fonction pour publier la luminosité actualisée
function updateLight() {
    const lightThreshold = document.getElementById('lightThreshold').value;

    if (!lightThreshold) {
        console.warn("Veuillez saisir une valeur pour la luminosité.");
        return;
    }

    if (client.connected) {
        client.publish("Seuil/Light", lightThreshold, function (err) {
            if (err) {
                console.error("Erreur lors de l'envoi de la luminosité :", err);
            } else {
                console.log(`Luminosité actualisée publiée : ${lightThreshold}`);
            }
        });
    } else {
        console.error("Le client MQTT n'est pas connecté au broker.");
    }
}

// Gestionnaire d'événement pour le formulaire de connexion
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', function (event) {
        event.preventDefault();
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        loginUser(username, password);
    });
}

// Gestionnaires d'événements pour les boutons de seuils
const updateTemperatureButton = document.getElementById('updateTemperatureButton');
if (updateTemperatureButton) {
    updateTemperatureButton.addEventListener('click', function () {
        updateTemperature();
    });
}

const updateCO2Button = document.getElementById('updateCO2Button');
if (updateCO2Button) {
    updateCO2Button.addEventListener('click', function () {
        updateCO2();
    });
}

const updateLightButton = document.getElementById('updateLightButton');
if (updateLightButton) {
    updateLightButton.addEventListener('click', function () {
        updateLight();
    });
}

