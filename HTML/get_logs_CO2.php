<?php
header('Content-Type: application/json');
ob_clean(); // Nettoie tout contenu envoyé avant ce point

$host = 'localhost'; 
$dbname = 'Logs_Capteurs';
$username = 'Gestionnaire';
$password = 'L0calPassw@rd';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $sql = "SELECT * FROM Logs_CO2 ORDER BY Date_Heure DESC LIMIT 24";
    $stmt = $pdo->query($sql);

    $data = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode($data);
} catch (PDOException $e) {
    echo json_encode(['error' => $e->getMessage()]);
}