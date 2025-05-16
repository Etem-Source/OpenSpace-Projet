# 🏢 Open Space Management System

Projet technique – BTS Cybersécurité, Informatique & Réseaux  
Session 2025
**Projet interne / Nouveau**

---

## 📑 Table des matières

1. [Contexte et Objectif](#contexte-et-objectif)
2. [Domaine d’Activité](#domaine-dactivité)
3. [Spécifications & Expression du Besoin](#spécifications--expression-du-besoin)
4. [Architecture & Technologies](#architecture--technologies)
5. [Contraintes de Réalisation](#contraintes)
6. [Répartition des Fonctions](#répartition-des-fonctions)
7. [Planification & Évaluation](#planification--évaluation)
8. [Ressources à Disposition](#ressources-à-disposition)
9. [Gestion de projet & Documentation](#gestion-de-projet--documentation)

---

## 1. 🎯 Contexte et Objectif

Développer un système permettant la gestion intelligente d’un open space :
- **Surveillance et régulation** des paramètres environnementaux (température, CO₂, éclairage)
- **Comptage/gestion des personnes** pour des raisons de sécurité
- **Gestion des réservations** des espaces (bureaux/salles, etc.)
- **Ouverture à des utilisateurs externes** (ex : start-ups)
- **Supervision centralisée et alertes** (PC de gestion, mobile)

---

## 2. 🌐 Domaine d’activité

- ☑️ Télécommunications, téléphonie, réseaux
- ☑️ Informatique, réseaux, infrastructures
- ☑️ Electronique, informatique médicale
- ☑️ Mesure, instrumentation, micro-systèmes
- ☑️ Automatique, robotique

---

## 3. 🛠️ Spécifications & Expression du Besoin

### 🔒 Sécurité & Comptage

- Comptage des personnes par double accès, via barrière optique (1 personne/accès à la fois)
- Remise à zéro automatique du compteur chaque nuit
- Historisation sur plages horaires de 1h, centralisé en base de données

### 🌡️ Paramètres Environnementaux

- 3 zones :
  - **Zone 1** : bureaux
  - **Zone 2** : détente
  - **Zone 3** : réunion

- Température (chaque zone, toutes les 3 min, calcul moyenne)  
  - > seuil max : chauffage coupé  
  - < seuil min : chauffage activé

- CO₂ : capteur central, si seuil critique → ventilation + alerte sonore
- Éclairement, par zone, ajustement dynamique par dimming

### 📆 Réservations

- Interface web/mobile pour réservations par utilisateurs externes
- Statistiques d’occupation

### 📦 Base de Données

- Stocke : seuils, historiques, réservations…

---

## 4. ⚙️ Architecture & Technologies

### 🧰 Matériel

- **ESP8266** (x3) pour la mesure/réaction locale
- **Raspberry Pi** (gestion et coordination)
- **PC** (gestion centralisée, double boot Win10/Linux Mint)
- **Capteurs** : DHT11 (temp.), Grove CO₂ (101020067), Grove Light Sensor, PIR HC-SR501, IR Grove
- **Module relais** (chauffage, ventilation, éclairage, etc.)
- **Mobile Android/iOS** (client réservations)

### 💾 Logiciel

- **Langages :** C/C++ (embarqué), Python (Raspberry Pi), SQL
- **Environnement :** Arduino IDE, Dev C++, Qt, Android Studio/Xamarin
- **BDD :** MySQL
- **Com :** sockets / protocoles IP/HTTP
- **Libres de droits privilégiés**

---

## 5. 📏 Contraintes

- **Financières :** usage de matériel existant et de logiciels libres autant que possible
- **Sécurité :** gestion des accès, alertes, traçabilité
- **Interopérabilité** : protocoles unifiés pour les échanges entre PC/mobile/RPi/ESP/BDD
- **Règlement :** respect des consignes et du calendrier E6-2

---

## 6. 👥 Répartition des Fonctions

| Étudiant IR | Fonctions Principales |
|-------------|----------------------|
| **1** | Prise d’info lumineuse/température, régulation du chauffage, dimming lumière |
| **2** | Supervision et interface principale PC, gestion des alertes, communication RPi ↔️ PC/mobile/ESP/BDD |
| **3** | Développement app mobile (Android/iOS – réservations), historique et interface utilisateur, communication avec la BDD |
| **4** | Systèmes de comptage, conception et remplissage de la base de données, scripts de mise à jour et d’interrogation |

*NB : Toutes les fonctions sont documentées dans le dossier projet.*

---

## 7. 🗓️ Planification & Évaluation

- Diagramme de Gantt détaillé (à voir dans dossier)
- Revue régulière avec référents & commission
- Synthèse projet à chaque étape-clé (analyse, conception, dev, tests)
- Évaluation selon :
  - Disponibilité & conformité des équipements
  - Atteinte des objectifs client
  - Documentation complète

---

## 8. 📚 Ressources à Disposition

- PC et réseau du labo (accès internet limité)
- IDE et utilitaires (Qt Creator, Android Studio, outils Arduino, DevCpp…)
- Datasheets, guides en ligne, forums OSS
- Outils de prototypage rapide (breadboards, câblage, etc.)
- Encadrement par professeurs responsables

---

## 9. 🗂️ Gestion de projet & Documentation

- **Diagrammes** : déploiement, séquence, états/transitions, classes, cas d’utilisation
- **Docs** :
  - Cahier des charges
  - Manuels technique & utilisateur
  - Protocoles de test, scripts BDD
- **Gestion collaborative** : Git/GitHub pour suivi et versionnage
- **Avenants** : toute modification documentée, validée par la commission

---

> ⚡️ Projet innovant, pluridisciplinaire et concret pour la gestion intelligente des environnements professionnels !

---
