const express = require("express");
const bodyParser = require("body-parser");
const mqtt = require("mqtt");
const mysql = require("mysql2");

const app = express();
app.use(bodyParser.json());

const db = mysql.createConnection({
    host: "192.168.29.55",
    user: "GSeuil",
    password: "L0calPassw@rd",
    database: "Logs_Capteurs"
});

db.connect((err) => {
    if (err) {
        console.error("🔴 Erreur de connexion à la DB:", err);
    } else {
        console.log("🟢 Connecté à la base de données !");
    }
});

const client = mqtt.connect("ws://192.168.29.55:9001");

client.on("connect", () => {
    console.log("🟢 Connecté à MQTT");

    const topics = [
        "user/authentication/response",
        "Average/Temperature",
        "Average/CO2",
        "Average/Light",
        "Seuil/Temperature",
        "Seuil/CO2",
        "Seuil/Light",
        "Alert/Temperature",
        "Alert/CO2",
        "Alert/Light",
        "logs"
    ];

    topics.forEach(topic => {
        client.subscribe(topic, (err) => {
            if (err) console.error(`❌ Erreur souscription à ${topic}:`, err);
            else console.log(`✅ Souscrit à ${topic}`);
        });
    });
});

// === Données globales ===
let thresholds = {
    temperature: "N/A",
    co2: "N/A",
    light: "N/A"
};

let averages = {
    temperature: "N/A",
    co2: "N/A",
    light: "N/A"
};

let pendingAlerts = [];

// === Gestion des messages MQTT ===
client.on("message", (topic, message) => {
    try {
        const value = message.toString();
        console.log(`📩 Message reçu sur ${topic}: ${value}`);

        const floatValue = parseFloat(value);

        switch (topic) {
            case "Average/Temperature":
                averages.temperature = value;
                if (thresholds.temperature !== "N/A" && floatValue > parseFloat(thresholds.temperature)) {
                    const alertMsg = `Alerte: Température moyenne ${value} dépasse seuil ${thresholds.temperature}`;
                    client.publish("Alert/Temperature", alertMsg);
                    console.warn(`⚠️ ${alertMsg}`);
                    pendingAlerts.push({ message: alertMsg });
                }
                break;

            case "Average/CO2":
                averages.co2 = value;
                if (thresholds.co2 !== "N/A" && floatValue > parseFloat(thresholds.co2)) {
                    const alertMsg = `Alerte: CO2 moyen ${value} dépasse seuil ${thresholds.co2}`;
                    client.publish("Alert/CO2", alertMsg);
                    console.warn(`⚠️ ${alertMsg}`);
                    pendingAlerts.push({ message: alertMsg });
                }
                break;

            case "Average/Light":
                averages.light = value;
                if (thresholds.light !== "N/A" && floatValue > parseFloat(thresholds.light)) {
                    const alertMsg = `Alerte: Luminosité moyenne ${value} dépasse seuil ${thresholds.light}`;
                    client.publish("Alert/Light", alertMsg);
                    console.warn(`⚠️ ${alertMsg}`);
                    pendingAlerts.push({ message: alertMsg });
                }
                break;

            case "Seuil/Temperature":
                thresholds.temperature = value;
                break;

            case "Seuil/CO2":
                thresholds.co2 = value;
                break;

            case "Seuil/Light":
                thresholds.light = value;
                break;

            case "logs":
                const logEntry = {
                    timestamp: new Date().toISOString(),
                    message: value
                };

                db.query("INSERT INTO logs (timestamp, message) VALUES (?, ?)", [logEntry.timestamp, logEntry.message], (err) => {
                    if (err) console.error("🔴 Erreur insertion log dans la DB:", err);
                    else console.log("✅ Log enregistré dans la DB:", logEntry);
                });
                break;

            case "user/authentication/response":
                const response = JSON.parse(value);
                console.log(`🔹 Réponse authentification reçue: ${response.status}`);

                if (response.status === "success") {
                    console.log("✅ Authentification réussie !");
                    console.log("📡 Signal/Seuils publié");

                    setTimeout(() => {
                        //client.publish("Signal/PageMonitoring", JSON.stringify({ request: "data" }));
                        console.log("📡 Requêtes de moyennes publiées après 5s");
                    }, 5000);
                } else {
                    console.log("🔴 Identifiants incorrects.");
                }
                break;

            case "Alert/Temperature":
            case "Alert/CO2":
            case "Alert/Light":
                // Ajout des alertes manuelles venant du broker
                pendingAlerts.push({ message: value });
                break;

            default:
                console.log(`⚠️ Message sur topic inconnu: ${topic}`);
        }
    } catch (e) {
        console.error("❌ Erreur traitement message MQTT:", e);
    }
});

// === ROUTES API ===
app.get("/get-logs", (req, res) => {
    db.query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 50", (err, results) => {
        if (err) {
            console.error("🔴 Erreur récupération des logs:", err);
            res.status(500).json({ status: "error", message: "Erreur interne" });
        } else {
            res.json(results);
        }
    });
});

app.get("/get-thresholds", (req, res) => {
    res.json(thresholds);
});

app.get("/get-averages", (req, res) => {
    res.json(averages);
});

app.get("/get-data", (req, res) => {
    res.json({
        thresholds,
        averages
    });
});

app.get("/alerts", (req, res) => {
    res.json(pendingAlerts);
    pendingAlerts = [];
});

app.post("/add-alert", (req, res) => {
    const { message } = req.body;
    if (!message) {
        return res.status(400).json({ status: "error", message: "Message d'alerte manquant" });
    }

    const alertMsg = {
        timestamp: new Date().toISOString(),
        message
    };

    pendingAlerts.push(alertMsg);

    db.query("INSERT INTO logs (timestamp, message) VALUES (?, ?)", [alertMsg.timestamp, `ALERTE: ${message}`], (err) => {
        if (err) {
            console.error("🔴 Erreur lors de l'insertion de l'alerte :", err);
        } else {
            console.log("✅ Alerte enregistrée dans la DB");
        }
    });

    res.json({ status: "success", message: "Alerte ajoutée" });
});

app.post("/update-temperature", (req, res) => {
    const { temperature } = req.body;
    if (!temperature) return res.status(400).json({ status: "error", message: "Valeur manquante" });

    client.publish("Seuil/Temperature", temperature);
    thresholds.temperature = temperature;
    res.json({ status: "success", message: "Température mise à jour" });
});

app.post("/update-co2", (req, res) => {
    const { co2 } = req.body;
    if (!co2) return res.status(400).json({ status: "error", message: "Valeur manquante" });

    client.publish("Seuil/CO2", co2);
    thresholds.co2 = co2;
    res.json({ status: "success", message: "CO2 mis à jour" });
});

app.post("/update-light", (req, res) => {
    const { light } = req.body;
    if (!light) return res.status(400).json({ status: "error", message: "Valeur manquante" });

    client.publish("Seuil/Light", light);
    thresholds.light = light;
    res.json({ status: "success", message: "Luminosité mise à jour" });
});

app.post("/authenticate", (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) {
        return res.status(400).json({ status: "error", message: "Champs manquants" });
    }

    client.publish("user/authentication", JSON.stringify({ username, password }));

    client.once("message", (topic, message) => {
        if (topic === "user/authentication/response") {
            try {
                const response = JSON.parse(message.toString());
                res.json(response);
            } catch (e) {
                res.status(500).json({ status: "error", message: "Erreur parsing réponse" });
            }
        }
    });
});

client.on("error", (err) => {
    console.error("🔴 Erreur MQTT:", err.message);
});

app.listen(3000, () => {
    console.log("🚀 Serveur Node.js démarré sur le port 3000");
});
