<?php
// filepath: /var/www/html/Luka/get_reservations_bureau.php
header('Content-Type: application/json');

$host = 'localhost'; // Adresse du serveur
$dbname = 'Clients'; // Nom de la base de données
$username = 'Gestionnaire'; // Nom d'utilisateur MySQL
$password = 'L0calPassw@rd'; // Mot de passe MySQL

try {
    // Connexion à la base de données
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Requête pour récupérer les réservations de bureau
    $stmt = $pdo->query('SELECT id, username, start_date, end_date, num_bureau, start_time, end_time FROM Reservations_Office');
    $reservations = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // Retourner les données au format JSON
    echo json_encode($reservations);
} catch (PDOException $e) {
    echo json_encode(['error' => $e->getMessage()]);
}
?>