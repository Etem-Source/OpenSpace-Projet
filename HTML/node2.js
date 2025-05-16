const express = require('express');
const mysql = require('mysql2');
const app = express();
const port = 3000;

// Configuration de la connexion à la base de données MySQL
const connection = mysql.createConnection({
  host: '192.168.29.55',  // Remplace par l'adresse de ton serveur DB si nécessaire
  user: 'Gestionnaire',  // Remplace par ton nom d'utilisateur MySQL
  password: 'L0calPassw@rd',  // Remplace par ton mot de passe MySQL
  database: 'Logs_Capteurs'  // Nom de ta base de données
});

// Connexion à la base de données
connection.connect((err) => {
  if (err) {
    console.error('Erreur de connexion à la base de données : ' + err.stack);
    return;
  }
  console.log('Connecté à la base de données avec l\'ID ' + connection.threadId);
});

// Serveur pour récupérer les logs de la table Logs_Temperature
app.get('/logs', (req, res) => {
  connection.query('SELECT * FROM Logs_Temperature ORDER BY timestamp DESC', (err, results) => {
    if (err) {
      console.error('Erreur lors de la récupération des logs :', err);
      res.status(500).send('Erreur de la base de données');
    } else {
      res.json(results);
    }
  });
});

// Servir le fichier HTML
app.use(express.static('public'));

app.listen(port, () => {
  console.log(`Serveur Node.js en écoute sur http://localhost:${port}`);
});
