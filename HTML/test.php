<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Logs de Température</title>
    <style>
        table {
            width: 100%;
            border-collapse: collapse;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: center;
        }
        th {
            background-color: #f2f2f2;
        }
    </style>
</head>
<body>
    <h1>Dernières 24 données enregistrées</h1>
    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Température</th>
                <th>Zone Réunion</th>
                <th>Zone Bureau</th>
                <th>Zone Détente</th>
                <th>Date et Heure</th>
            </tr>
        </thead>
        <tbody>
            <?php
            // Connexion à la base de données
            $host = '192.168.29.55'; // Adresse du serveur
            $dbname = 'Logs_Capteurs'; // Nom de la base de données
            $username = 'Gestionnaire'; // Nom d'utilisateur
            $password = 'L0calPassw@rd'; // Mot de passe

            try {
                // Création de la connexion PDO
                $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
                $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

                // Vérification des droits avec une requête simple
                $sql = "SELECT 1 FROM Logs_Temperature LIMIT 1";
                $pdo->query($sql);

                // Requête pour récupérer les 24 dernières entrées
                $sql = "SELECT * FROM Logs_Temperature ORDER BY Date_Heure DESC LIMIT 24";
                $stmt = $pdo->query($sql);

                // Affichage des données dans le tableau
                while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                    echo "<tr>";
                    echo "<td>" . htmlspecialchars($row['Temp_ID']) . "</td>";
                    echo "<td>" . htmlspecialchars($row['Temp_Int']) . "</td>";
                    echo "<td>" . htmlspecialchars($row['Zone_Reunion']) . "</td>";
                    echo "<td>" . htmlspecialchars($row['Zone_Bureau']) . "</td>";
                    echo "<td>" . htmlspecialchars($row['Zone_Detente']) . "</td>";
                    echo "<td>" . htmlspecialchars($row['Date_Heure']) . "</td>";
                    echo "</tr>";
                }
            } catch (PDOException $e) {
                // Gestion des erreurs
                if (strpos($e->getMessage(), 'Access denied') !== false) {
                    echo "<tr><td colspan='6'>Erreur : Accès refusé. Vérifiez les droits de l'utilisateur.</td></tr>";
                } else {
                    echo "<tr><td colspan='6'>Erreur : " . htmlspecialchars($e->getMessage()) . "</td></tr>";
                }
            } catch (Exception $e) {
                // Gestion des autres erreurs
                echo "<tr><td colspan='6'>Erreur inattendue : " . htmlspecialchars($e->getMessage()) . "</td></tr>";
            }
            ?>
        </tbody>
    </table>
</body>
</html>