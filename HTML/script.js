// Connexion au broker MQTT
const client = mqtt.connect("ws://192.168.29.55:9001");

// Variables globales pour stocker les seuils et leur date de réception
let seuilTemperature = null;
let seuilTemperatureTimestamp = null;

let seuilLight = null;
let seuilLightTimestamp = null;

let seuilCO2 = null;
let seuilCO2Timestamp = null;

// Flags pour éviter alertes répétées
let alerteTemperatureEnvoyee = false;
let alerteLightEnvoyee = false;
let alerteCO2Envoyee = false;

// Durée de validité des seuils en ms (1 heure)
const SEUIL_VALIDITY_DURATION = 60 * 60 * 1000; // 3600000 ms

// Fonction pour vérifier si un seuil est toujours valide
function isSeuilValide(timestamp) {
    if (!timestamp) return false;
    const now = Date.now();
    return (now - timestamp) <= SEUIL_VALIDITY_DURATION;
}

// Fonction générique pour gérer les alertes
function checkAlert(topic, value, seuil, seuilTimestamp, alerteEnvoyeeFlagName, alertMessage) {
    if (seuil !== null && !isNaN(value)) {
        if (!isSeuilValide(seuilTimestamp)) {
            // Seuil expiré, on ignore l'alerte
            console.log(`Le seuil ${topic} a expiré, alerte ignorée.`);
            window[alerteEnvoyeeFlagName] = false; // reset flag pour futur seuil valide
            return;
        }

        console.log(`Comparaison ${topic}: ${value} > ${seuil} ?`);
        if (value > seuil) {
            console.log(`Seuil ${topic} dépassé !`);
            if (!window[alerteEnvoyeeFlagName]) {
                alert(alertMessage);
                window[alerteEnvoyeeFlagName] = true;
            }
        } else {
            window[alerteEnvoyeeFlagName] = false;
        }
    }
}

client.on('connect', function () {
    console.log('Connecté au broker MQTT');
    // Abonnement aux topics nécessaires, incluant les seuils
    client.subscribe("user/authentication/response");
    client.subscribe("Average/Temperature");
    client.subscribe("Average/Light");
    client.subscribe("Average/CO2");
    client.subscribe("Average/PeopleCount");
    client.subscribe("Seuil/Temperature");
    client.subscribe("Seuil/Light");
    client.subscribe("Seuil/CO2");
});

client.on('message', function (topic, message) {
    if (topic === "user/authentication/response") {
        const response = JSON.parse(message.toString());
        if (response.status === "success") {
            // Envoi la demande des données puis redirige après 1s
            sendPageMonitoringRequest();
            setTimeout(() => {
                window.location.href = "monitoring.html";
            }, 1000);
        } else {
            const loginError = document.getElementById('loginError');
            if (loginError) {
                loginError.textContent = 'Identifiants incorrects.';
            }
        }
    } 
    else if (topic === "Seuil/Temperature") {
        seuilTemperature = Number(message.toString());
        seuilTemperatureTimestamp = Date.now();
        console.log("Seuil Température reçu :", seuilTemperature, typeof seuilTemperature);
    } 
    else if (topic === "Seuil/Light") {
        seuilLight = Number(message.toString());
        seuilLightTimestamp = Date.now();
        console.log("Seuil Luminosité reçu :", seuilLight, typeof seuilLight);
    } 
    else if (topic === "Seuil/CO2") {
        seuilCO2 = Number(message.toString());
        seuilCO2Timestamp = Date.now();
        console.log("Seuil CO2 reçu :", seuilCO2, typeof seuilCO2);
    } 
    else if (topic === "Average/Temperature") {
        const temperatureValue = Number(message.toString());
        console.log("Temperature reçue :", temperatureValue, typeof temperatureValue);

        const temperatureElement = document.getElementById("temperature");
        if (temperatureElement) {
            temperatureElement.textContent = temperatureValue;
        }

        checkAlert(
            "Température",
            temperatureValue,
            seuilTemperature,
            seuilTemperatureTimestamp,
            "alerteTemperatureEnvoyee",
            `Alerte Température ! La température (${temperatureValue}°C) dépasse le seuil autorisé (${seuilTemperature}°C).`
        );
    } 
    else if (topic === "Average/Light") {
        const lightValue = Number(message.toString());
        console.log("Luminosité reçue :", lightValue, typeof lightValue);

        const lightElement = document.getElementById("light");
        if (lightElement) {
            lightElement.textContent = lightValue;
        }

        checkAlert(
            "Luminosité",
            lightValue,
            seuilLight,
            seuilLightTimestamp,
            "alerteLightEnvoyee",
            `Alerte Luminosité ! Le niveau de luminosité (${lightValue} lux) est trop élevé (seuil : ${seuilLight} lux).`
        );
    } 
    else if (topic === "Average/CO2") {
        const co2Value = Number(message.toString());
        console.log("CO2 reçu :", co2Value, typeof co2Value);

        const co2Element = document.getElementById("co2");
        if (co2Element) {
            co2Element.textContent = co2Value;
        }

        checkAlert(
            "CO2",
            co2Value,
            seuilCO2,
            seuilCO2Timestamp,
            "alerteCO2Envoyee",
            `Alerte CO₂ ! Le niveau de CO₂ (${co2Value}) a dépassé le seuil autorisé (${seuilCO2}).`
        );
    } 
    else if (topic === "Average/PeopleCount") {
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
function sendSignalSeuils() {
    if (client.connected) {
        client.publish("Signal/Seuils", JSON.stringify({}), function(err) {
            if (err) {
                console.error("Erreur lors de l'envoi du message 'Signal/Seuils' :", err);
            } else {
                console.log("Message 'Signal/Seuils' publié");
                // Dès que le message est publié, on lance la demande de monitoring (après 5s)
                sendPageMonitoringRequest();
            }
        });
    } else {
        console.error("Le client MQTT n'est pas connecté. Réessai dans 1 seconde...");
        setTimeout(sendSignalSeuils, 1000);
    }
}

function sendPageMonitoringRequest() {
    setTimeout(() => {
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
            setTimeout(sendPageMonitoringRequest, 1000);
        }
    }, 5000);
}



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

function updateCO2() {
    const co2Threshold = document.getElementById('co2Threshold').value;

    if (!co2Threshold) {
        console.warn("Veuillez saisir une valeur pour le CO2.");
        return;
    }

    const co2Value = parseFloat(co2Threshold);

    if (isNaN(co2Value)) {
        console.error("La valeur saisie pour le CO2 n'est pas un nombre valide.");
        return;
    }

    if (client.connected) {
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

// Gestionnaires d'événements pour les boutons de seuils (si présents)
const tempButton = document.getElementById('updateTemperatureBtn');
if (tempButton) {
    tempButton.addEventListener('click', updateTemperature);
}

const co2Button = document.getElementById('updateCO2Btn');
if (co2Button) {
    co2Button.addEventListener('click', updateCO2);
}

const lightButton = document.getElementById('updateLightBtn');
if (lightButton) {
    lightButton.addEventListener('click', updateLight);
}
