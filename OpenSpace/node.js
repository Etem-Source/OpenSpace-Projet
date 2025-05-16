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

// ✅ Connexion au broker MQTT
const client = mqtt.connect("ws://192.168.29.55:9001");

client.on("connect", () => {
    console.log("🟢 Connecté à MQTT");

    // ✅ Souscription aux topics
    const topics = [
        "user/authentication/response",
        "Average/Temperature",
        "Average/CO2",
        "Average/Light",
        "Seuil/Temperature",
        "Seuil/CO2",
        "Seuil/Light",
        "logs" // ✅ Ajout du topic logs
    ];

    topics.forEach(topic => {
        client.subscribe(topic, (err) => {
            if (err) console.error(`❌ Erreur souscription à ${topic}:`, err);
            else console.log(`✅ Souscrit à ${topic}`);
        });
    });
});

// ✅ Stockage des seuils reçus
let thresholds = {
    temperature: "N/A",
    co2: "N/A",
    light: "N/A"
};

// ✅ Gestion des messages MQTT
client.on("message", (topic, message) => {
    try {
        console.log(`📩 Message reçu sur ${topic}: ${message.toString()}`);

        switch (topic) {
            case "Average/Temperature":
            case "Seuil/Temperature":
                thresholds.temperature = message.toString();
                break;
            case "Average/CO2":
            case "Seuil/CO2":
                thresholds.co2 = message.toString();
                break;
            case "Average/Light":
            case "Seuil/Light":
                thresholds.light = message.toString();
                break;
            case "logs": // ✅ Enregistrement des logs en base de données
                const logEntry = {
                    timestamp: new Date().toISOString(),
                    message: message.toString()
                };

                db.query("INSERT INTO logs (timestamp, message) VALUES (?, ?)", [logEntry.timestamp, logEntry.message], (err) => {
                    if (err) console.error("🔴 Erreur insertion log dans la DB:", err);
                    else console.log("✅ Log enregistré dans la DB:", logEntry);
                });

                break;
            case "user/authentication/response":
                const response = JSON.parse(message.toString());
                console.log(`🔹 Réponse authentification reçue: ${response.status}`);

                if (response.status === "success") {
                    console.log("✅ Authentification réussie !");
                } else {
                    console.log("🔴 Identifiants incorrects.");
                }
                break;
            default:
                console.log(`⚠️ Message reçu sur un topic non géré: ${topic}`);
        }
    } catch (e) {
        console.error("❌ Erreur parsing MQTT:", e);
    }
});

// ✅ API pour récupérer les logs via Qt
app.get("/get-logs", (req, res) => {
    db.query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 50", (err, results) => {
        if (err) {
            console.error("🔴 Erreur récupération des logs:", err);
            res.status(500).json({ status: "error", message: "Erreur interne" });
        } else {
            console.log("📜 Logs récupérés depuis la DB:", results);
            res.json(results);
        }
    });
});

// ✅ API pour récupérer les seuils via Qt
app.get("/get-thresholds", (req, res) => {
    res.json(thresholds);
});

// ✅ Routes API pour mettre à jour les seuils
app.post("/update-temperature", (req, res) => {
    const { temperature } = req.body;
    if (!temperature) return res.status(400).json({ status: "error", message: "Valeur manquante" });

    client.publish("Seuil/Temperature", temperature);
    thresholds.temperature = temperature;
    res.json({ status: "success", message: "Température mise à jour avec succès" });
});

app.post("/update-co2", (req, res) => {
    const { co2 } = req.body;
    if (!co2) return res.status(400).json({ status: "error", message: "Valeur manquante" });

    client.publish("Seuil/CO2", co2);
    thresholds.co2 = co2;
    res.json({ status: "success", message: "CO2 mis à jour avec succès" });
});

app.post("/update-light", (req, res) => {
    const { light } = req.body;
    if (!light) return res.status(400).json({ status: "error", message: "Valeur manquante" });

    client.publish("Seuil/Light", light);
    thresholds.light = light;
    res.json({ status: "success", message: "Luminosité mise à jour avec succès" });
});

// ✅ Route API pour l'authentification utilisateur
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
                res.json({ status: "error", message: "Erreur interne" });
            }
        }
    });
});

// ✅ Gestion des erreurs MQTT
client.on("error", (err) => {
    console.error("🔴 Erreur MQTT:", err.message);
});

// ✅ Démarrage du serveur Node.js
app.listen(3000, () => {
    console.log("🚀 Serveur Node.js démarré sur le port 3000");
});

