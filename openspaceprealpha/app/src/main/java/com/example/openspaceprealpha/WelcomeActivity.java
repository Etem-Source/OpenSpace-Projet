package com.example.openspaceprealpha;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;


public class WelcomeActivity extends AppCompatActivity {

    private MqttClient mqttClient;
    private String username;
    private BottomSheetDialog officeDialog; // Déclaration de officeDialog
    private Spinner officeSpinner; // Déclaration de officeSpinner
    private TextView startDateTextView, endDateTextView; // Déclaration des TextViews pour les dates
    private boolean isReservationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DEBUG", "WelcomeActivity: onCreate called");
        setContentView(R.layout.activity_welcome);

        String mail = getIntent().getStringExtra("mail");
        String age = getIntent().getStringExtra("age");
        String nom = getIntent().getStringExtra("nom");
        String prenom = getIntent().getStringExtra("prenom");
        username = getIntent().getStringExtra("username");

        Log.d("DEBUG", "Received user details: mail=" + mail + ", username=" + username);

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        usernameTextView.setText(username);

        LinearLayout userInfoContainer = findViewById(R.id.user_info_container);
        userInfoContainer.setOnClickListener(v -> {
            Log.d("DEBUG", "Clic sur la section utilisateur pour afficher les détails");
            showUserDetails(mail, age, nom, prenom, username);
        });

        Button reserveMeetingButton = findViewById(R.id.reserveMeetingButton);
        Button reserveOfficeButton = findViewById(R.id.reserveOfficeButton);

        reserveMeetingButton.setOnClickListener(v -> {
            Log.d("DEBUG", "Clic sur 'Réserver une réunion'");
            showMeetingReservation();
        });

        reserveOfficeButton.setOnClickListener(v -> {
            Log.d("DEBUG", "Clic sur 'Réserver un bureau'");
            showOfficeReservation();
        });

        // Initialiser le client MQTT
        try {
            Log.d("DEBUG", "Initialisation du client MQTT");
            String broker = "tcp://192.168.29.55:1883";
            String clientId = MqttClient.generateClientId();
            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("ERROR", "Connexion MQTT perdue", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    Log.d("DEBUG", "Message MQTT reçu: topic=" + topic + ", payload=" + payload);

                    String[] parts = payload.split(";");
                    if (parts.length >= 3 && parts[0].equals("GOS") && parts[1].equals("02")) {
                        runOnUiThread(() -> {
                            Log.d("DEBUG", "Processing reservation response...");
                            if (parts[2].equals("success")) {
                                Log.d("DEBUG", "Réservation réussie");
                                Toast.makeText(WelcomeActivity.this, "Réservation réussie !", Toast.LENGTH_LONG).show();
                            } else if (parts[2].equals("error")) {
                                String errorMessage = parts.length > 3 ? parts[3] : "Erreur inconnue.";
                                Log.e("ERROR", "Échec de la réservation: " + errorMessage);
                                Toast.makeText(WelcomeActivity.this, "Échec de la réservation : " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Log.e("DEBUG", "Message MQTT non traité ou format incorrect.");
                    }
                }


                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("DEBUG", "Message MQTT livré avec succès");
                }
            });

            mqttClient.connect(options);
            Log.d("DEBUG", "Client MQTT connecté avec succès");
            mqttClient.subscribe("reservation/topic");
        } catch (MqttException e) {
            Log.e("ERROR", "Erreur lors de la connexion MQTT", e);
        }
    }

    private void showUserDetails(String mail, String age, String nom, String prenom, String username) {
        Log.d("DEBUG", "Affichage des détails utilisateur");
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(WelcomeActivity.this);
        View bottomSheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.user_details_bottom_sheet, findViewById(R.id.bottomSheetContainer));

        TextView usernameHeaderTextView = bottomSheetView.findViewById(R.id.usernameHeaderTextView);
        usernameHeaderTextView.setText(username);

        Button informationButton = bottomSheetView.findViewById(R.id.informationButton);
        Button meetingReservationButton = bottomSheetView.findViewById(R.id.meetingReservationButton);
        Button officeReservationButton = bottomSheetView.findViewById(R.id.officeReservationButton); // Nouveau bouton pour les réservations de bureaux
        Button logoutButton = bottomSheetView.findViewById(R.id.logoutButton);

        informationButton.setOnClickListener(v -> {
            Log.d("DEBUG", "Clic sur 'Informations utilisateur'");
            showUserInfo(mail, age, nom, prenom, username);
        });

        meetingReservationButton.setOnClickListener(v -> {
            Log.d("DEBUG", "Clic sur 'Réservations de réunions'");
            showReservations();
        });

        officeReservationButton.setOnClickListener(v -> {
            Log.d("DEBUG", "Clic sur 'Réservations de bureaux'");
            showOfficeReservations();
        });

        logoutButton.setOnClickListener(v -> {
            Log.d("DEBUG", "Déconnexion utilisateur et retour à l'écran principal");
            bottomSheetDialog.dismiss();

            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
        Log.d("DEBUG", "Détails utilisateur affichés dans le BottomSheet");
    }




    private void showUserInfo(String mail, String age, String nom, String prenom, String username) {
        BottomSheetDialog userInfoDialog = new BottomSheetDialog(WelcomeActivity.this);
        View userInfoView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.user_info_bottom_sheet, findViewById(R.id.bottomSheetContainer));

        TextView usernameHeaderTextView = userInfoView.findViewById(R.id.usernameHeaderTextView);
        TextView nomTextView = userInfoView.findViewById(R.id.nomTextView);
        TextView actualNomTextView = userInfoView.findViewById(R.id.actualNomTextView);
        TextView prenomTextView = userInfoView.findViewById(R.id.prenomTextView);
        TextView actualPrenomTextView = userInfoView.findViewById(R.id.actualPrenomTextView);
        TextView ageTextView = userInfoView.findViewById(R.id.ageTextView);
        TextView actualAgeTextView = userInfoView.findViewById(R.id.actualAgeTextView);
        TextView emailTextView = userInfoView.findViewById(R.id.emailTextView);
        TextView actualEmailTextView = userInfoView.findViewById(R.id.actualEmailTextView);
        TextView usernameTextView = userInfoView.findViewById(R.id.usernameTextView);
        TextView actualUsernameTextView = userInfoView.findViewById(R.id.actualUsernameTextView);

        usernameHeaderTextView.setText(username);
        nomTextView.setText("Nom:");
        actualNomTextView.setText(nom);
        prenomTextView.setText("Prénom:");
        actualPrenomTextView.setText(prenom);
        ageTextView.setText("Âge:");
        actualAgeTextView.setText(age);
        emailTextView.setText("Email:");
        actualEmailTextView.setText(mail);
        usernameTextView.setText("Utilisateur:");
        actualUsernameTextView.setText(username);

        userInfoDialog.setContentView(userInfoView);
        userInfoDialog.show();
    }

    private BottomSheetDialog reservationDialog;

    private void showReservations() {
        Log.d("DEBUG", "Clic sur 'Réservations de réunions'");

        if (reservationDialog != null && reservationDialog.isShowing()) {
            reservationDialog.dismiss();
        }

        reservationDialog = new BottomSheetDialog(WelcomeActivity.this);
        View reservationView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.reservation_bottom_sheet, null);

        LinearLayout reservationsContainer = reservationView.findViewById(R.id.reservationsContainer);
        TextView reservationMessage = reservationView.findViewById(R.id.reservationMessage);
        Button pastReservationsButton = reservationView.findViewById(R.id.pastReservationsButton);
        NestedScrollView scrollView = reservationView.findViewById(R.id.reservationsScrollView);

        // Afficher les réservations passées dans un autre BottomSheet
        pastReservationsButton.setOnClickListener(v -> showPastReservations());

        // Envoyer une demande pour récupérer les réservations via MQTT
        sendMqttRequestForReservations(username);

        // Ajouter un écouteur pour recevoir les réservations et les afficher
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Gérer la perte de connexion
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                String[] parts = payload.split(";");

                // Vérifier que le message est une réponse de réservation
                if (parts.length >= 3 && parts[0].equals("GOS") && parts[1].equals("04") && parts[2].equals("success")) {
                    runOnUiThread(() -> {
                        reservationsContainer.removeAllViews();

                        for (int i = 3; i < parts.length; i += 4) {
                            String date = parts[i];
                            String startTime = parts[i + 1];
                            String endTime = parts[i + 2];
                            String status = parts[i + 3];

                            if (!status.equals("past")) {  // Ne pas afficher les réservations passées ici
                                View reservationViewItem = LayoutInflater.from(getApplicationContext()).inflate(R.layout.reservation_item, reservationsContainer, false);
                                TextView dateTextView = reservationViewItem.findViewById(R.id.dateTextView);
                                TextView timeTextView = reservationViewItem.findViewById(R.id.timeTextView);
                                TextView statusTextView = reservationViewItem.findViewById(R.id.statusTextView);
                                ImageView iconStatus = reservationViewItem.findViewById(R.id.iconStatus);
                                Button cancelButton = reservationViewItem.findViewById(R.id.cancelButton);

                                // Convertir la date au format français
                                String[] dateParts = date.split("-");
                                String frenchDate = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];

                                dateTextView.setText("Date: " + frenchDate);
                                timeTextView.setText("Heure: " + startTime + " - " + endTime);

                                statusTextView.setText("Réservation à venir");
                                statusTextView.setTextColor(getResources().getColor(R.color.green));
                                iconStatus.setImageResource(R.drawable.ic_future);
                                cancelButton.setVisibility(View.VISIBLE);  // Afficher le bouton d'annulation pour les réservations futures

                                // Ajouter un écouteur de clic pour le bouton d'annulation
                                cancelButton.setOnClickListener(v -> {
                                    new AlertDialog.Builder(WelcomeActivity.this)
                                            .setTitle("Annuler la réservation")
                                            .setMessage("Etes-vous sûr de vouloir annuler cette réservation pour la date: " + frenchDate + " de " + startTime + " à " + endTime + " ?")
                                            .setPositiveButton("Oui", (dialog, which) -> {
                                                // Envoyer une demande d'annulation de réservation via MQTT
                                                sendMqttCancelReservation(username, date, startTime, endTime);

                                                // Recharger la liste des réservations après l'annulation
                                                showReservations();
                                            })
                                            .setNegativeButton("Non", null)
                                            .show();
                                });

                                reservationsContainer.addView(reservationViewItem);
                            }
                        }

                        if (reservationsContainer.getChildCount() == 0) {
                            reservationMessage.setText("Aucune réservation trouvée.");
                        } else {
                            reservationMessage.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                String[] topics = token.getTopics();
                if (topics != null) {
                    for (String topic : topics) {
                        System.out.println("Message delivered to topic: " + topic);
                    }
                }
                System.out.println("Delivery complete for message with ID: " + token.getMessageId());
            }
        });

        reservationDialog.setContentView(reservationView);

        // Configurer le comportement du BottomSheet après qu'il soit affiché
        reservationDialog.setOnShowListener(dialog -> {
            try {
                // Récupérer le BottomSheet
                FrameLayout bottomSheet = reservationDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                    // Configurer le NestedScrollView pour contrôler le glissement du BottomSheet
                    scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                        behavior.setDraggable(scrollY == 0);
                    });
                }
            } catch (Exception e) {
                Log.e("DEBUG", "Erreur lors de la configuration du BottomSheetBehavior", e);
            }
        });

        reservationDialog.show();
    }




    private void sendMqttRequestForReservations(String username) {
        try {
            String topic = "reservation/topic";
            String payload = "GOS;04;askreservations;" + username;
            MqttMessage message = new MqttMessage(payload.getBytes());
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void sendMqttCancelReservation(String username, String date, String startTime, String endTime) {
        try {
            String topic = "reservation/topic";
            String payload = "GOS;05;cancelreservation;" + username + ";" + date + ";" + startTime + ";" + endTime;
            MqttMessage message = new MqttMessage(payload.getBytes());
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }





    private void showPastReservations() {
        BottomSheetDialog pastReservationDialog = new BottomSheetDialog(WelcomeActivity.this);
        View pastReservationView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.reservation_bottom_sheet, null);

        LinearLayout reservationsContainer = pastReservationView.findViewById(R.id.reservationsContainer);
        TextView reservationMessage = pastReservationView.findViewById(R.id.reservationMessage);
        NestedScrollView scrollView = pastReservationView.findViewById(R.id.reservationsScrollView);

        // Changer le titre pour indiquer qu'il s'agit des réservations passées
        TextView reservationTitle = pastReservationView.findViewById(R.id.reservationTitle);
        if (reservationTitle != null) {
            reservationTitle.setText("  Réunions passées");
        }

        // Cacher le bouton des réservations passées car nous sommes déjà dans cette vue
        Button pastReservationsButton = pastReservationView.findViewById(R.id.pastReservationsButton);
        if (pastReservationsButton != null) {
            pastReservationsButton.setVisibility(View.GONE);
        }

        // Envoyer une demande pour récupérer les réservations via MQTT
        sendMqttRequestForReservations(username);

        // Ajouter un écouteur pour recevoir les réservations et les afficher
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Gérer la perte de connexion
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                String[] parts = payload.split(";");

                // Vérifier que le message est une réponse de réservation
                if (parts.length >= 3 && parts[0].equals("GOS") && parts[1].equals("04") && parts[2].equals("success")) {
                    runOnUiThread(() -> {
                        reservationsContainer.removeAllViews();

                        for (int i = 3; i < parts.length; i += 4) {
                            String date = parts[i];
                            String startTime = parts[i + 1];
                            String endTime = parts[i + 2];
                            String status = parts[i + 3];

                            if (status.equals("past")) {
                                View reservationViewItem = LayoutInflater.from(getApplicationContext()).inflate(R.layout.reservation_item, reservationsContainer, false);
                                TextView dateTextView = reservationViewItem.findViewById(R.id.dateTextView);
                                TextView timeTextView = reservationViewItem.findViewById(R.id.timeTextView);
                                TextView statusTextView = reservationViewItem.findViewById(R.id.statusTextView);
                                ImageView iconStatus = reservationViewItem.findViewById(R.id.iconStatus);
                                Button cancelButton = reservationViewItem.findViewById(R.id.cancelButton);

                                // Convertir la date au format français
                                String[] dateParts = date.split("-");
                                String frenchDate = dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];

                                dateTextView.setText("Date: " + frenchDate);
                                timeTextView.setText("Heure: " + startTime + " - " + endTime);

                                statusTextView.setText("Réservation passée");
                                statusTextView.setTextColor(getResources().getColor(R.color.red));
                                iconStatus.setImageResource(R.drawable.ic_past);
                                cancelButton.setVisibility(View.GONE);  // Ne pas afficher le bouton d'annulation pour les réservations passées

                                reservationsContainer.addView(reservationViewItem);
                            }
                        }

                        if (reservationsContainer.getChildCount() == 0) {
                            reservationMessage.setText("Aucune réservation passée trouvée.");
                        } else {
                            reservationMessage.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                String[] topics = token.getTopics();
                if (topics != null) {
                    for (String topic : topics) {
                        System.out.println("Message delivered to topic: " + topic);
                    }
                }
                System.out.println("Delivery complete for message with ID: " + token.getMessageId());
            }
        });

        pastReservationDialog.setContentView(pastReservationView);

        // Configurer le comportement du BottomSheet après qu'il soit affiché
        pastReservationDialog.setOnShowListener(dialog -> {
            try {
                // Récupérer le BottomSheet
                FrameLayout bottomSheet = pastReservationDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                    // Configurer le NestedScrollView pour contrôler le glissement du BottomSheet
                    scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                        behavior.setDraggable(scrollY == 0);
                    });
                }
            } catch (Exception e) {
                Log.e("DEBUG", "Erreur lors de la configuration du BottomSheetBehavior", e);
            }
        });

        pastReservationDialog. show();
    }






    private void sendMqttRequestForAllReservations() {
        try {
            String topic = "reservation/topic";
            String payload = "GOS;07;askallreservations;meeting";
            MqttMessage message = new MqttMessage(payload.getBytes());
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void showMeetingReservation() {
        Log.d("DEBUG", "Opening the meeting reservation dialog");

        BottomSheetDialog meetingDialog = new BottomSheetDialog(WelcomeActivity.this);
        View meetingView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.meeting_reservation_bottom_sheet, findViewById(R.id.bottomSheetContainer));

        MaterialCalendarView calendarView = meetingView.findViewById(R.id.calendarView);
        Spinner hourSpinnerStart = meetingView.findViewById(R.id.hourSpinnerStart);
        Spinner hourSpinnerEnd = meetingView.findViewById(R.id.hourSpinnerEnd);
        Button confirmReservationButton = meetingView.findViewById(R.id.confirmReservationButton);

        Log.d("DEBUG", "Views initialized successfully");

        // Créer une liste des heures pour les Spinners (de 08:00 à 20:00)
        String[] hours = {"08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"};
        ArrayAdapter<String> defaultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, hours);
        hourSpinnerStart.setAdapter(defaultAdapter);
        hourSpinnerEnd.setAdapter(defaultAdapter);

        Log.d("DEBUG", "Adapters set for spinners");

        // Stockage de la date sélectionnée
        final Calendar[] selectedDate = {Calendar.getInstance()}; // Initialisation à aujourd'hui
        final Map<String, List<String>> reservationsByDate = new HashMap<>();

        // Configuration du calendrier
        // Désactiver les dates passées
        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                Calendar dayCalendar = Calendar.getInstance();
                dayCalendar.set(day.getYear(), day.getMonth(), day.getDay(), 0, 0, 0);
                dayCalendar.set(Calendar.MILLISECOND, 0);

                return dayCalendar.before(today);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setDaysDisabled(true);
                view.addSpan(new ForegroundColorSpan(ContextCompat.getColor(WelcomeActivity.this, R.color.gray)));
            }
        });

        // Sélectionner la date du jour par défaut
        CalendarDay today = CalendarDay.today();
        calendarView.setSelectedDate(today);
        // Déclencher manuellement l'événement de changement de date
        String todayFormatted = String.format("%d-%02d-%02d", today.getYear(), today.getMonth() + 1, today.getDay());
        Log.d("DEBUG", "Setting default date: " + todayFormatted);

        // Listener pour détecter la date sélectionnée dans le calendrier
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            Log.d("DEBUG", "Date selected: " + date);

            // Formater la date comme dans la Map
            selectedDate[0] = Calendar.getInstance();
            selectedDate[0].set(date.getYear(), date.getMonth(), date.getDay());
            String selectedDateStr = String.format("%d-%02d-%02d", date.getYear(), date.getMonth() + 1, date.getDay());
            Log.d("DEBUG", "Formatted selected date: " + selectedDateStr);

            updateHourSpinners(selectedDateStr, hours, hourSpinnerStart, hourSpinnerEnd, reservationsByDate);
        });

        // Configurer le callback MQTT pour recevoir les réservations
        setupMqttCallbackForReservations(calendarView, reservationsByDate, hours, hourSpinnerStart, hourSpinnerEnd, today);

        // Requête pour récupérer les réservations et décorer le calendrier
        Log.d("DEBUG", "Sending MQTT request to fetch all reservations");
        sendMqttRequestForAllReservations();

        confirmReservationButton.setOnClickListener(v -> {
            Log.d("DEBUG", "Confirm button clicked");

            // Vérifier qu'une date est sélectionnée
            if (selectedDate[0] == null) {
                Toast.makeText(this, "Aucune date sélectionnée. Veuillez choisir une date dans le calendrier.", Toast.LENGTH_LONG).show();
                Log.e("DEBUG", "No date selected. Cannot proceed with reservation.");
                return;
            }

            String selectedStartTime = hourSpinnerStart.getSelectedItem().toString();
            String selectedEndTime = hourSpinnerEnd.getSelectedItem().toString();

            // Vérifier que l'heure de début est avant l'heure de fin
            if (Integer.parseInt(selectedStartTime.split(":")[0]) >= Integer.parseInt(selectedEndTime.split(":")[0])) {
                Toast.makeText(this, "L'heure de début doit être avant l'heure de fin.", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d("DEBUG", "Selected Date for Reservation (Millis): " + selectedDate[0].getTimeInMillis());
            Log.d("DEBUG", "Selected Date for Reservation (Formatted): " +
                    new SimpleDateFormat("yyyy-MM-dd").format(selectedDate[0].getTime()));

            // Envoi de la réservation avec la date correcte
            sendMqttReservation(username, selectedDate[0].getTimeInMillis(), selectedStartTime, selectedEndTime);

            // Attente d'une réponse MQTT
            setupMqttCallbackForReservationResponse(meetingDialog);
        });

        meetingDialog.setContentView(meetingView);
        meetingDialog.show();

        // Déclencher immédiatement la mise à jour des heures pour aujourd'hui
        updateHourSpinners(todayFormatted, hours, hourSpinnerStart, hourSpinnerEnd, reservationsByDate);
    }

    // Méthode pour mettre à jour les spinners d'heures
    private void updateHourSpinners(String selectedDateStr, String[] hours, Spinner hourSpinnerStart,
                                    Spinner hourSpinnerEnd, Map<String, List<String>> reservationsByDate) {
        List<String> reservedHoursRaw = reservationsByDate.getOrDefault(selectedDateStr, new ArrayList<>());
        List<String> formattedReservedHours = computeReservedHours(reservedHoursRaw);
        Log.d("DEBUG", "Computed Reserved Hours for " + selectedDateStr + ": " + formattedReservedHours);

        // Adapter personnalisé pour mettre à jour les couleurs en fonction de la disponibilité
        ArrayAdapter<String> customAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, hours) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                String hour = getItem(position);

                if (formattedReservedHours.contains(hour)) {
                    textView.setTextColor(ContextCompat.getColor(getContext(), R.color.red)); // Rouge si réservé
                } else {
                    textView.setTextColor(ContextCompat.getColor(getContext(), R.color.green)); // Vert si libre
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return getView(position, convertView, parent);
            }
        };

        hourSpinnerStart.setAdapter(customAdapter);
        hourSpinnerEnd.setAdapter(customAdapter);
    }

    // Configuration du callback MQTT pour les réservations
    private void setupMqttCallbackForReservations(MaterialCalendarView calendarView,
                                                  Map<String, List<String>> reservationsByDate,
                                                  String[] hours, Spinner hourSpinnerStart,
                                                  Spinner hourSpinnerEnd, CalendarDay today) {
        List<CalendarDay> orangeDays = new ArrayList<>();
        List<CalendarDay> redDays = new ArrayList<>();

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("DEBUG", "Connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Log.d("MQTT", "Message received: " + new String(message.getPayload()));
                String payload = new String(message.getPayload());
                String[] parts = payload.split(";");

                if (parts.length >= 3 && parts[0].equals("GOS") && parts[1].equals("07") && parts[2].equals("success")) {
                    runOnUiThread(() -> {
                        Log.d("DEBUG", "Updating calendar with reservation data");
                        try {
                            // Vider les listes
                            orangeDays.clear();
                            redDays.clear();
                            reservationsByDate.clear();

                            for (int i = 3; i < parts.length; i += 4) { // Process blocks of 4
                                if (parts.length <= i + 3) continue;

                                String username = parts[i];
                                String date = parts[i + 1];
                                String startTime = parts[i + 2];
                                String endTime = parts[i + 3];

                                // Ajouter les heures réservées à la Map
                                reservationsByDate.putIfAbsent(date, new ArrayList<>());
                                reservationsByDate.get(date).add(startTime);
                                reservationsByDate.get(date).add(endTime);
                                Log.d("DEBUG", "Reserved hours for date " + date + ": " + reservationsByDate.get(date));

                                // Check si un jour est complètement réservé (08:00 à 20:00)
                                if (isDayFullyReserved(reservationsByDate.get(date))) {
                                    // Marque le jour comme rouge
                                    String[] dateParts = date.split("-");
                                    CalendarDay day = CalendarDay.from(
                                            Integer.parseInt(dateParts[0]),
                                            Integer.parseInt(dateParts[1]) - 1,
                                            Integer.parseInt(dateParts[2])
                                    );
                                    redDays.add(day);
                                } else {
                                    // Sinon, marque-le en orange
                                    String[] dateParts = date.split("-");
                                    CalendarDay day = CalendarDay.from(
                                            Integer.parseInt(dateParts[0]),
                                            Integer.parseInt(dateParts[1]) - 1,
                                            Integer.parseInt(dateParts[2])
                                    );
                                    orangeDays.add(day);
                                }
                            }

                            // Supprimer anciens décorateurs et ajouter les nouveaux
                            calendarView.removeDecorators();

                            // Remettre le décorateur pour les dates passées
                            calendarView.addDecorator(new DayViewDecorator() {
                                @Override
                                public boolean shouldDecorate(CalendarDay day) {
                                    Calendar today = Calendar.getInstance();
                                    today.set(Calendar.HOUR_OF_DAY, 0);
                                    today.set(Calendar.MINUTE, 0);
                                    today.set(Calendar.SECOND, 0);
                                    today.set(Calendar.MILLISECOND, 0);

                                    Calendar dayCalendar = Calendar.getInstance();
                                    dayCalendar.set(day.getYear(), day.getMonth(), day.getDay(), 0, 0, 0);
                                    dayCalendar.set(Calendar.MILLISECOND, 0);

                                    return dayCalendar.before(today);
                                }

                                @Override
                                public void decorate(DayViewFacade view) {
                                    view.setDaysDisabled(true);
                                    view.addSpan(new ForegroundColorSpan(ContextCompat.getColor(WelcomeActivity.this, R.color.gray)));
                                }
                            });

                            calendarView.addDecorator(new DayColorDecorator(orangeDays, R.color.orange, WelcomeActivity.this));
                            calendarView.addDecorator(new DayColorDecorator(redDays, R.color.red, WelcomeActivity.this));
                            calendarView.invalidateDecorators();

                            // Mettre à jour les spinners d'heures pour la date actuellement sélectionnée
                            CalendarDay selectedDate = calendarView.getSelectedDate();
                            if (selectedDate != null) {
                                String selectedDateStr = String.format("%d-%02d-%02d", selectedDate.getYear(), selectedDate.getMonth() + 1, selectedDate.getDay());
                                updateHourSpinners(selectedDateStr, hours, hourSpinnerStart, hourSpinnerEnd, reservationsByDate);
                            } else {
                                // Si aucune date n'est sélectionnée, utiliser la date d'aujourd'hui
                                String todayStr = String.format("%d-%02d-%02d", today.getYear(), today.getMonth() + 1, today.getDay());
                                updateHourSpinners(todayStr, hours, hourSpinnerStart, hourSpinnerEnd, reservationsByDate);
                            }
                        } catch (Exception e) {
                            Log.e("DEBUG", "Error while updating calendar", e);
                        }
                    });
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("DEBUG", "Message delivered");
            }
        });
    }

    // Configuration du callback MQTT pour la réponse de réservation
    private void setupMqttCallbackForReservationResponse(BottomSheetDialog dialog) {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("DEBUG", "Connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                String[] parts = payload.split(";");
                runOnUiThread(() -> {
                    if (parts.length >= 3 && parts[2].equals("success")) {
                        Toast.makeText(WelcomeActivity.this, "Réservation confirmée !", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else if (parts.length >= 3 && parts[2].equals("error")) {
                        String errorMessage = parts.length > 3 ? parts[3] : "Erreur inconnue.";
                        Toast.makeText(WelcomeActivity.this, "Réservation refusée : Horraire déjà réservée", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("DEBUG", "Message MQTT livré");
            }
        });
    }


    // Fonction pour vérifier si une journée est complètement réservée
    private boolean isDayFullyReserved(List<String> reservedHoursRaw) {
        List<String> formattedReservedHours = computeReservedHours(reservedHoursRaw);
        List<String> fullDayHours = Arrays.asList(
                "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00",
                "16:00", "17:00", "18:00", "19:00", "20:00"
        );
        return formattedReservedHours.containsAll(fullDayHours);
    }



    // Calculer les heures complètes d'une plage réservée
    private List<String> computeReservedHours(List<String> reservedHoursRaw) {
        List<String> computedHours = new ArrayList<>();
        for (int i = 0; i < reservedHoursRaw.size(); i += 2) {
            String startHour = reservedHoursRaw.get(i).substring(0, 5);
            String endHour = reservedHoursRaw.get(i + 1).substring(0, 5);

            int start = Integer.parseInt(startHour.split(":")[0]);
            int end = Integer.parseInt(endHour.split(":")[0]);
            for (int h = start; h <= end; h++) {
                computedHours.add(String.format("%02d:00", h));
            }
        }
        return computedHours;
    }

    // Vérifier si une heure est la fin d'une plage réservée
    private boolean isEndOfReservation(String hour, List<String> reservedHoursRaw) {
        for (int i = 1; i < reservedHoursRaw.size(); i += 2) {
            String endHour = reservedHoursRaw.get(i).substring(0, 5);
            if (endHour.equals(hour)) {
                return true;
            }
        }
        return false;
    }

    private void sendMqttReservation(String username, long date, String startTime, String endTime) {
        try {
            String topic = "reservation/topic";
            String payload = "GOS;02;reunion;" + username + ";" + date + ";" + startTime + ";" + endTime;
            MqttMessage message = new MqttMessage(payload.getBytes());
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    // Méthode pour afficher l'image du plan
    private void showPlanImage() {
        // Créez une boîte de dialogue pour afficher l'image
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Plan des bureaux");

        // Créer un ImageView pour afficher l'image
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.office_plan); // Remplacez 'office_plan' par le nom de votre image
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Ajouter l'ImageView à la boîte de dialogue
        builder.setView(imageView);

        // Ajouter un bouton pour fermer la boîte de dialogue
        builder.setPositiveButton("Fermer", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // Afficher la boîte de dialogue
        AlertDialog dialog = builder.create();
        dialog.show();
    }



    private void showOfficeReservation() {
        Log.d("DEBUG", "Opening the office reservation dialog");

        BottomSheetDialog officeDialog = new BottomSheetDialog(WelcomeActivity.this);
        View officeView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.desk_reservation_bottom_sheet, findViewById(R.id.bottomSheetContainer));

        // Récupérer les layouts
        LinearLayout officeSelectionLayout = officeView.findViewById(R.id.officeSelectionLayout);
        LinearLayout reservationDetailsLayout = officeView.findViewById(R.id.reservationDetailsLayout);

        // Récupérer les éléments UI
        TextView startDateTextView = officeView.findViewById(R.id.startDateTextView);
        TextView endDateTextView = officeView.findViewById(R.id.endDateTextView);
        MaterialCalendarView calendarView = officeView.findViewById(R.id.calendarView);
        Spinner hourSpinner = officeView.findViewById(R.id.hourSpinner);
        Spinner officeSpinner = officeView.findViewById(R.id.officeSpinner);
        Button confirmReservationButton = officeView.findViewById(R.id.confirmOfficeReservationButton);
        Button continueButton = officeView.findViewById(R.id.continueButton);
        Button viewPlanButton = officeView.findViewById(R.id.viewPlanButton);


        viewPlanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlanImage();
            }
        });

        // Initialiser les listes pour les Spinner
        String[] hours = {"08:00-12:00", "13:00-17:00"};
        String[] officeNumbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};

        // Adapter pour les heures
        ArrayAdapter<String> hourAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, hours) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(getResources().getColor(android.R.color.black)); // Forcer la couleur noire
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(getResources().getColor(android.R.color.black)); // Forcer la couleur noire
                return view;
            }
        };
        hourSpinner.setAdapter(hourAdapter);

        // Adapter standard pour le Spinner des bureaux
        ArrayAdapter<String> officeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, officeNumbers) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(getResources().getColor(android.R.color.black)); // Forcer la couleur noire
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(getResources().getColor(android.R.color.black)); // Forcer la couleur noire
                return view;
            }
        };
        officeSpinner.setAdapter(officeAdapter);

        // Variables pour stocker les dates et réservations
        final String[] selectedOffice = {null};
        final Calendar[] startDate = {null};
        final Calendar[] endDate = {null};
        final Map<String, Integer> occupancyLevels = new HashMap<>();

        // Désactiver les dates passées
        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                Calendar dayCalendar = Calendar.getInstance();
                dayCalendar.set(day.getYear(), day.getMonth(), day.getDay(), 0, 0, 0);
                dayCalendar.set(Calendar.MILLISECOND, 0);

                return dayCalendar.before(today);
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.setDaysDisabled(true);
                view.addSpan(new ForegroundColorSpan(ContextCompat.getColor(WelcomeActivity.this, R.color.gray)));
            }
        });

        // Configuration du mode de sélection multiple pour le calendrier
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_RANGE);

        // Listener pour détecter les dates sélectionnées dans le calendrier
        calendarView.setOnRangeSelectedListener((widget, dates) -> {
            if (dates.size() > 0) {
                startDate[0] = Calendar.getInstance();
                startDate[0].set(dates.get(0).getYear(), dates.get(0).getMonth(), dates.get(0).getDay());
                String startDateStr = String.format("%d/%02d/%02d",
                        dates.get(0).getDay(), dates.get(0).getMonth() + 1, dates.get(0).getYear());
                startDateTextView.setText(startDateStr);

                endDate[0] = Calendar.getInstance();
                CalendarDay lastDate = dates.get(dates.size() - 1);
                endDate[0].set(lastDate.getYear(), lastDate.getMonth(), lastDate.getDay());
                endDateTextView.setText(String.format("%d/%02d/%02d",
                        lastDate.getDay(), lastDate.getMonth() + 1, lastDate.getYear()));

                // Mettre à jour les horaires disponibles avec la liste de dates sélectionnées
                updateOfficeHourSpinner(startDateStr, hourSpinner, occupancyLevels, dates);
            }
        });

        // Bouton Continuer après sélection de bureau
        continueButton.setOnClickListener(v -> {
            selectedOffice[0] = officeSpinner.getSelectedItem().toString();
            Log.d("DEBUG", "Selected office: " + selectedOffice[0]);

            // Masquer la sélection de bureau et afficher le formulaire de réservation
            officeSelectionLayout.setVisibility(View.GONE);
            reservationDetailsLayout.setVisibility(View.VISIBLE);

            // Demander les réservations existantes pour le bureau sélectionné
            getMqttOfficeReservations(selectedOffice[0], calendarView, occupancyLevels, hourSpinner);
        });

        // Configuration du bouton de confirmation
        confirmReservationButton.setOnClickListener(v -> {
            // Validation des entrées utilisateur
            if (startDate[0] == null || endDate[0] == null) {
                Toast.makeText(this, "Veuillez sélectionner les dates de début et de fin.", Toast.LENGTH_LONG).show();
                return;
            }

            if (startDate[0].after(endDate[0])) {
                Toast.makeText(this, "La date de début doit précéder la date de fin.", Toast.LENGTH_LONG).show();
                return;
            }

            long diff = endDate[0].getTimeInMillis() - startDate[0].getTimeInMillis();
            long daysBetween = TimeUnit.MILLISECONDS.toDays(diff);
            if (daysBetween > 30) {
                Toast.makeText(this, "La réservation ne peut pas excéder un mois.", Toast.LENGTH_LONG).show();
                return;
            }

            String selectedHourRange = hourSpinner.getSelectedItem().toString();
            String startTime = selectedHourRange.split("-")[0];
            String endTime = selectedHourRange.split("-")[1];

            // Envoyer la réservation
            sendMqttOfficeReservation(username, startDate[0], endDate[0], selectedOffice[0], startTime, endTime);

            // Configurer le callback pour la réponse
            setupMqttCallbackForOfficeReservationResponse(officeDialog);
        });

        // Afficher la boîte de dialogue
        officeDialog.setContentView(officeView);
        officeDialog.show();
    }

    // Méthode pour mettre à jour le spinner d'horaires en fonction des réservations
    private void updateOfficeHourSpinner(String selectedDateStr, Spinner hourSpinner, Map<String, Integer> occupancyLevels, List<CalendarDay> selectedDates) {
        String[] hours = {"08:00-12:00", "13:00-17:00"};

        // Variables pour suivre si les créneaux sont réservés
        final boolean[] isMorningReservedAnywhere = {false};
        final boolean[] isAfternoonReservedAnywhere = {false};

        // Débug: Afficher toutes les réservations connues
        Log.i("CRITICAL_DEBUG", "=== OCCUPANCY MAP ===");
        for (Map.Entry<String, Integer> entry : occupancyLevels.entrySet()) {
            Log.i("CRITICAL_DEBUG", "Date: " + entry.getKey() + ", Occupancy: " + entry.getValue());
        }

        // Pour une plage de dates, vérifier chaque date manuellement
        if (selectedDates != null && selectedDates.size() > 0) {
            Log.i("CRITICAL_DEBUG", "Selected " + selectedDates.size() + " dates");

            CalendarDay firstDay = selectedDates.get(0);
            CalendarDay lastDay = selectedDates.get(selectedDates.size() - 1);

            Log.i("CRITICAL_DEBUG", "Date range: from " +
                    firstDay.getYear() + "-" + (firstDay.getMonth() + 1) + "-" + firstDay.getDay() +
                    " to " +
                    lastDay.getYear() + "-" + (lastDay.getMonth() + 1) + "-" + lastDay.getDay());

            // Parcourons toutes les dates dans l'intervalle
            Calendar currentDate = Calendar.getInstance();
            currentDate.set(firstDay.getYear(), firstDay.getMonth(), firstDay.getDay());

            Calendar endDate = Calendar.getInstance();
            endDate.set(lastDay.getYear(), lastDay.getMonth(), lastDay.getDay());
            endDate.add(Calendar.DATE, 1); // Ajouter 1 jour pour inclure la dernière date

            Log.i("CRITICAL_DEBUG", "Checking all dates in range:");
            while (currentDate.before(endDate)) {
                int year = currentDate.get(Calendar.YEAR);
                int month = currentDate.get(Calendar.MONTH) + 1; // Les mois commencent à 0
                int day = currentDate.get(Calendar.DAY_OF_MONTH);

                // ESSAYER AVEC LES DEUX FORMATS POSSIBLES
                // Format avec les zéros (2025-04-05)
                String dateKeyWithZeros = String.format("%d-%02d-%02d", year, month, day);
                // Format sans les zéros (2025-4-5)
                String dateKeyWithoutZeros = String.format("%d-%d-%d", year, month, day);

                Log.i("CRITICAL_DEBUG", "  - Checking date: " + dateKeyWithZeros);
                Log.i("CRITICAL_DEBUG", "    Also checking alternative format: " + dateKeyWithoutZeros);

                // Vérifier les deux formats possibles
                Integer occupancyWithZeros = occupancyLevels.get(dateKeyWithZeros);
                Integer occupancyWithoutZeros = occupancyLevels.get(dateKeyWithoutZeros);

                // Utiliser l'occupation trouvée dans l'un ou l'autre format
                Integer occupancy = (occupancyWithZeros != null) ? occupancyWithZeros : occupancyWithoutZeros;

                if (occupancy != null) {
                    Log.i("CRITICAL_DEBUG", "    Found occupancy: " + occupancy + " for date");

                    if (occupancy >= 1) {
                        isMorningReservedAnywhere[0] = true;
                        Log.i("CRITICAL_DEBUG", "    Morning is reserved!");
                    }

                    if (occupancy >= 2) {
                        isAfternoonReservedAnywhere[0] = true;
                        Log.i("CRITICAL_DEBUG", "    Afternoon is reserved!");
                    }
                } else {
                    Log.i("CRITICAL_DEBUG", "    No occupancy for this date in either format");
                }

                // Passer au jour suivant
                currentDate.add(Calendar.DATE, 1);
            }
        }

        Log.i("CRITICAL_DEBUG", "Final availability - Morning: " +
                (isMorningReservedAnywhere[0] ? "RESERVED" : "AVAILABLE") +
                ", Afternoon: " + (isAfternoonReservedAnywhere[0] ? "RESERVED" : "AVAILABLE"));

        // Adapter personnalisé pour les horaires avec des couleurs DIRECTES (pas les ressources)
        ArrayAdapter<String> customAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, hours) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) { // 08:00-12:00
                    if (isMorningReservedAnywhere[0]) {
                        textView.setTextColor(Color.RED);
                        Log.i("CRITICAL_DEBUG", "Setting morning color to RED");
                    } else {
                        textView.setTextColor(Color.GREEN);
                        Log.i("CRITICAL_DEBUG", "Setting morning color to GREEN");
                    }
                } else { // 13:00-17:00
                    if (isAfternoonReservedAnywhere[0]) {
                        textView.setTextColor(Color.RED);
                        Log.i("CRITICAL_DEBUG", "Setting afternoon color to RED");
                    } else {
                        textView.setTextColor(Color.GREEN);
                        Log.i("CRITICAL_DEBUG", "Setting afternoon color to GREEN");
                    }
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;

                if (position == 0) { // 08:00-12:00
                    if (isMorningReservedAnywhere[0]) {
                        textView.setTextColor(Color.RED);
                    } else {
                        textView.setTextColor(Color.GREEN);
                    }
                } else { // 13:00-17:00
                    if (isAfternoonReservedAnywhere[0]) {
                        textView.setTextColor(Color.RED);
                    } else {
                        textView.setTextColor(Color.GREEN);
                    }
                }
                return view;
            }
        };

        hourSpinner.setAdapter(customAdapter);
    }








    // Méthode pour obtenir les réservations par bureau via MQTT
    private void getMqttOfficeReservations(String officeNumber, MaterialCalendarView calendarView,
                                           Map<String, Integer> occupancyLevels, Spinner hourSpinner) {
        try {
            Log.d("DEBUG", "Requesting reservations for office: " + officeNumber);
            String topic = "reservation/topic";

            // Mettre à jour le callback MQTT pour traiter la réponse
            setupMqttCallbackForOfficeInfo(calendarView, officeNumber, occupancyLevels, hourSpinner);

            // Format: GOS;05;query;bureau;numéro_bureau
            String payload = "GOS;05;query;bureau;" + officeNumber;
            mqttClient.publish(topic, new MqttMessage(payload.getBytes()));
        } catch (MqttException e) {
            Log.e("DEBUG", "Error requesting office reservations", e);
            e.printStackTrace();
        }
    }

    // Configuration du callback MQTT pour recevoir les informations du bureau
    private void setupMqttCallbackForOfficeInfo(MaterialCalendarView calendarView, String officeNumber,
                                                Map<String, Integer> occupancyLevels, Spinner hourSpinner) {
        final List<CalendarDay> partiallyReservedDays = new ArrayList<>();
        final List<CalendarDay> fullyReservedDays = new ArrayList<>();

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("DEBUG", "Connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Log.d("MQTT", "Message received: " + new String(message.getPayload()));
                String payload = new String(message.getPayload());
                String[] parts = payload.split(";");

                // Format attendu: GOS;06;bureau;officeNumber;date1;date2;...
                if (parts.length >= 4 && parts[0].equals("GOS") && parts[1].equals("06") && parts[2].equals("bureau")) {
                    runOnUiThread(() -> {
                        Log.d("DEBUG", "Updating calendar with office reservation data");
                        try {
                            // Vider les listes précédentes et la Map
                            occupancyLevels.clear();
                            partiallyReservedDays.clear();
                            fullyReservedDays.clear();

                            // Traiter les dates réservées
                            for (int i = 4; i < parts.length; i++) {
                                try {
                                    // Format date: yyyy-MM-dd
                                    String dateStr = parts[i];
                                    String[] dateParts = dateStr.split("-");

                                    if (dateParts.length == 3) {
                                        int year = Integer.parseInt(dateParts[0]);
                                        int month = Integer.parseInt(dateParts[1]) - 1; // Months are 0-based
                                        int day = Integer.parseInt(dateParts[2]);

                                        // Incrementer le compteur d'occupation pour cette date
                                        String dateKey = year + "-" + (month+1) + "-" + day;
                                        occupancyLevels.put(dateKey, occupancyLevels.getOrDefault(dateKey, 0) + 1);
                                        Log.d("DEBUG", "Added occupation for " + dateKey + ": " + occupancyLevels.get(dateKey));
                                    }
                                } catch (NumberFormatException e) {
                                    Log.e("DEBUG", "Error parsing date: " + parts[i], e);
                                }
                            }

                            // Classer les jours selon leur niveau d'occupation
                            for (Map.Entry<String, Integer> entry : occupancyLevels.entrySet()) {
                                String[] dateParts = entry.getKey().split("-");
                                int year = Integer.parseInt(dateParts[0]);
                                int month = Integer.parseInt(dateParts[1]) - 1;
                                int day = Integer.parseInt(dateParts[2]);

                                CalendarDay calDay = CalendarDay.from(year, month, day);

                                if (entry.getValue() >= 2) {
                                    fullyReservedDays.add(calDay); // Journée complète réservée
                                } else {
                                    partiallyReservedDays.add(calDay); // Mi-journée réservée
                                }
                            }

                            // Supprimer anciens décorateurs et ajouter les nouveaux
                            calendarView.removeDecorators();

                            // Remettre le décorateur pour les dates passées
                            calendarView.addDecorator(new DayViewDecorator() {
                                @Override
                                public boolean shouldDecorate(CalendarDay day) {
                                    Calendar today = Calendar.getInstance();
                                    today.set(Calendar.HOUR_OF_DAY, 0);
                                    today.set(Calendar.MINUTE, 0);
                                    today.set(Calendar.SECOND, 0);
                                    today.set(Calendar.MILLISECOND, 0);

                                    Calendar dayCalendar = Calendar.getInstance();
                                    dayCalendar.set(day.getYear(), day.getMonth(), day.getDay(), 0, 0, 0);
                                    dayCalendar.set(Calendar.MILLISECOND, 0);

                                    return dayCalendar.before(today);
                                }

                                @Override
                                public void decorate(DayViewFacade view) {
                                    view.setDaysDisabled(true);
                                    view.addSpan(new ForegroundColorSpan(ContextCompat.getColor(WelcomeActivity.this, R.color.gray)));
                                }
                            });

                            // Ajouter les décorateurs pour jours partiellement et complètement réservés
                            if (!partiallyReservedDays.isEmpty()) {
                                calendarView.addDecorator(new DayColorDecorator(partiallyReservedDays, R.color.orange, WelcomeActivity.this));
                            }

                            if (!fullyReservedDays.isEmpty()) {
                                calendarView.addDecorator(new DayColorDecorator(fullyReservedDays, R.color.red, WelcomeActivity.this));
                            }

                            calendarView.invalidateDecorators();

                            // Si une date est déjà sélectionnée, mettre à jour le spinner des heures
                            List<CalendarDay> selectedDates = calendarView.getSelectedDates();
                            if (selectedDates != null && !selectedDates.isEmpty()) {
                                String selectedDateStr = String.format("%d/%02d/%02d",
                                        selectedDates.get(0).getDay(), selectedDates.get(0).getMonth() + 1, selectedDates.get(0).getYear());
                                updateOfficeHourSpinner(selectedDateStr, hourSpinner, occupancyLevels, selectedDates);
                            }

                        } catch (Exception e) {
                            Log.e("DEBUG", "Error while updating calendar", e);
                        }
                    });
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("DEBUG", "Message delivered");
            }
        });
    }




    // Méthode pour envoyer la réservation de bureau via MQTT
    private void sendMqttOfficeReservation(String username, Calendar startDate, Calendar endDate,
                                           String officeNumber, String startTime, String endTime) {
        try {
            String topic = "reservation/topic";

            // Formatage des dates en millisecondes
            long startDateMillis = startDate.getTimeInMillis();
            long endDateMillis = endDate.getTimeInMillis();

            // Payload MQTT
            String payload = "GOS;02;bureau;" + username + ";" + startDateMillis + ";" +
                    endDateMillis + ";" + officeNumber + ";" + startTime + ";" + endTime;

            Log.d("DEBUG", "Sending office reservation: " + payload);
            mqttClient.publish(topic, new MqttMessage(payload.getBytes()));
        } catch (MqttException e) {
            Log.e("DEBUG", "Error sending office reservation", e);
            e.printStackTrace();
        }
    }

    // Configuration du callback MQTT pour la réponse à la réservation
    private void setupMqttCallbackForOfficeReservationResponse(BottomSheetDialog dialog) {
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("DEBUG", "Connection lost", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                String[] parts = payload.split(";");

                Log.d("DEBUG", "Received response for office reservation: " + payload);

                runOnUiThread(() -> {
                    // Format attendu: GOS;08;success ou GOS;08;error;message
                    if (parts.length >= 3 && parts[0].equals("GOS") && parts[1].equals("02")) {
                        if (parts[2].equals("success")) {
                            Toast.makeText(WelcomeActivity.this, "Réservation de bureau confirmée !", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else if (parts[2].equals("error")) {
                            String errorMessage = parts.length > 3 ? parts[3] : "Erreur inconnue.";
                            Toast.makeText(WelcomeActivity.this, "Réservation refusée : " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("DEBUG", "Message MQTT livré");
            }
        });
    }






    private void sendMqttRequestForOfficeReservations(String username) {
        try {
            String topic = "reservation/topic";
            String payload = "GOS;05;requestreservations;" + username;
            MqttMessage message = new MqttMessage(payload.getBytes());
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void showOfficeReservations() {
        Log.d("DEBUG", "Affichage des réservations de bureau...");

        // Nettoyage de l'ancien dialogue s'il existe
        if (reservationDialog != null && reservationDialog.isShowing()) {
            reservationDialog.dismiss();
            reservationDialog = null;
        }

        // Création du BottomSheetDialog
        reservationDialog = new BottomSheetDialog(WelcomeActivity.this);
        View reservationView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.reservation2_bottom_sheet, findViewById(R.id.bottomSheetContainer));

        LinearLayout reservationsContainer = reservationView.findViewById(R.id.reservationsContainer);
        TextView reservationMessage = reservationView.findViewById(R.id.reservationMessage);
        Button pastReservationsButton = reservationView.findViewById(R.id.pastReservationsButton);

        // Configuration du bouton pour afficher les réservations passées
        pastReservationsButton.setOnClickListener(v -> {
            Log.d("DEBUG", "Clic sur 'Réservations passées'.");
            showPastOfficeReservations();
        });

        // Envoi de la requête MQTT pour récupérer les réservations
        Log.d("DEBUG", "Envoi de la requête MQTT pour récupérer les réservations de bureau pour l'utilisateur : " + username);
        sendMqttRequestForOfficeReservations(username);

        // Gestion du callback MQTT
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("DEBUG", "Connexion MQTT perdue.", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                Log.d("DEBUG", "Message reçu sur le topic : " + topic + ", contenu : " + payload);

                String[] parts = payload.split(";");
                if (parts.length >= 3 && parts[0].equals("GOS") && parts[1].equals("05") && parts[2].equals("success")) {
                    runOnUiThread(() -> {
                        try {
                            Log.d("DEBUG", "Traitement des données reçues pour les réservations...");
                            reservationsContainer.removeAllViews(); // Nettoyage de l'ancien contenu

                            boolean hasFutureReservations = false;

                            for (int i = 3; i < parts.length; i += 6) {
                                if (i + 5 >= parts.length) {
                                    Log.e("DEBUG", "Données de réservation incomplètes à l'index : " + i + ". Payload : " + payload);
                                    continue;
                                }

                                String numBureau = parts[i];
                                String startDate = parts[i + 1];
                                String startTime = parts[i + 2];
                                String endDate = parts[i + 3];
                                String endTime = parts[i + 4];
                                String status = parts[i + 5].trim();

                                Log.d("DEBUG", "Traitement - NumBureau : " + numBureau + ", StartDate : " + startDate + ", EndDate : " + endDate + ", Status : " + status);

                                if (status.equals("future")) {
                                    hasFutureReservations = true;
                                    Log.d("DEBUG", "Test 1");
                                    // Création de la vue pour chaque réservation future
                                    View reservationViewItem = LayoutInflater.from(getApplicationContext())
                                            .inflate(R.layout.reservation2_item, reservationsContainer, false);
                                    Log.d("DEBUG", "Test 1.2");
                                    TextView startDateTextView = reservationViewItem.findViewById(R.id.startDateTextView);
                                    TextView endDateTextView = reservationViewItem.findViewById(R.id.endDateTextView);
                                    TextView numBureauTextView = reservationViewItem.findViewById(R.id.numBureauTextView);
                                    TextView timeTextView = reservationViewItem.findViewById(R.id.timeTextView);
                                    TextView statusTextView = reservationViewItem.findViewById(R.id.statusTextView);
                                    ImageView iconStatus = reservationViewItem.findViewById(R.id.iconStatus);
                                    Button cancelButton = reservationViewItem.findViewById(R.id.cancelButton);
                                    Log.d("DEBUG", "Test 1.3");
                                    // Conversion des dates au format français
                                    String frenchStartDate = convertDateToFrenchFormat(startDate);
                                    String frenchEndDate = convertDateToFrenchFormat(endDate);

                                    startDateTextView.setText("Début: " + frenchStartDate);
                                    endDateTextView.setText("Fin: " + frenchEndDate);
                                    numBureauTextView.setText("Bureau: " + numBureau);
                                    timeTextView.setText("Heure: " + startTime + " - " + endTime);

                                    statusTextView.setText("Bureau réservé");
                                    Log.d("DEBUG", "Test 1.4");
                                    statusTextView.setTextColor(getResources().getColor(R.color.green));
                                    iconStatus.setImageResource(R.drawable.ic_future);
                                    cancelButton.setVisibility(View.VISIBLE);
                                    Log.d("DEBUG", "Test 2");
                                    cancelButton.setOnClickListener(v -> {
                                        Log.d("DEBUG", "Clic sur 'Annuler la réservation' pour le bureau : " + numBureau);
                                        new AlertDialog.Builder(WelcomeActivity.this)
                                                .setTitle("Annuler la réservation de bureau")
                                                .setMessage("Etes-vous sûr de vouloir annuler cette réservation pour le bureau : " + numBureau + " de " + frenchStartDate + " à " + frenchEndDate + " ?")
                                                .setPositiveButton("Oui", (dialog, which) -> {
                                                    sendMqttCancelOfficeReservation(username, startDate, endDate, numBureau, startTime, endTime);
                                                    showOfficeReservations();
                                                })
                                                .setNegativeButton("Non", null)
                                                .show();
                                    });
                                    Log.d("DEBUG", "Test 3");

                                    reservationsContainer.addView(reservationViewItem);
                                    Log.d("DEBUG", "Réservation future ajoutée avec succès : " + numBureau);
                                }
                            }

                            if (!hasFutureReservations) {
                                Log.d("DEBUG", "Aucune réservation future trouvée.");
                                reservationMessage.setText("Aucune réservation de bureau trouvée.");
                                reservationMessage.setVisibility(View.VISIBLE);
                            } else {
                                reservationMessage.setVisibility(View.GONE);
                            }
                        } catch (Exception e) {
                            Log.e("DEBUG", "Erreur lors du traitement des réservations : ", e);
                        }
                    });
                } else {
                    Log.e("DEBUG", "Message non reconnu ou statut d'échec : " + payload);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("DEBUG", "Message MQTT livré avec succès.");
            }
        });

        reservationDialog.setContentView(reservationView);
        reservationDialog.show();
        Log.d("DEBUG", "Affichage du dialogue des réservations de bureau.");
    }

    // Méthode pour convertir une date au format français
    private String convertDateToFrenchFormat(String date) {
        try {
            String[] dateParts = date.split("-");
            return dateParts[2] + "-" + dateParts[1] + "-" + dateParts[0];
        } catch (Exception e) {
            Log.e("ERROR", "Erreur lors de la conversion de la date : " + date, e);
            return date; // Retourne la date originale si une erreur survient
        }
    }






    private void showPastOfficeReservations() {
        Log.e("DEBUG", "Entrée dans la fonction showPastOfficeReservations");
        BottomSheetDialog pastReservationDialog = new BottomSheetDialog(WelcomeActivity.this);
        View pastReservationView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.reservation2_bottom_sheet, findViewById(R.id.bottomSheetContainer));

        LinearLayout reservationsContainer = pastReservationView.findViewById(R.id.reservationsContainer);
        TextView reservationMessage = pastReservationView.findViewById(R.id.reservationMessage);

        sendMqttRequestForOfficeReservations(username); // Requête MQTT

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.e("DEBUG", "Connexion MQTT perdue.", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                Log.d("DEBUG", "Message reçu : " + payload);
                String[] parts = payload.split(";");

                if (parts.length >= 3 && parts[0].equals("GOS") && parts[1].equals("05") && parts[2].equals("success")) {
                    runOnUiThread(() -> {
                        reservationsContainer.removeAllViews();
                        boolean hasPastReservations = false;

                        for (int i = 3; i < parts.length; i += 6) {
                            if (i + 5 >= parts.length) {
                                Log.e("DEBUG", "Réservation incomplète ignorée.");
                                continue;
                            }

                            String numBureau = parts[i];
                            String startDate = parts[i + 1];
                            String startTime = parts[i + 2];
                            String endDate = parts[i + 3];
                            String endTime = parts[i + 4];
                            String status = parts[i + 5].trim();

                            if (status.equals("past")) {
                                hasPastReservations = true;
                                View reservationViewItem = LayoutInflater.from(getApplicationContext())
                                        .inflate(R.layout.reservation2_item, reservationsContainer, false);

                                // Remplissage des données
                                TextView startDateTextView = reservationViewItem.findViewById(R.id.startDateTextView);
                                TextView endDateTextView = reservationViewItem.findViewById(R.id.endDateTextView);
                                TextView numBureauTextView = reservationViewItem.findViewById(R.id.numBureauTextView);
                                TextView timeTextView = reservationViewItem.findViewById(R.id.timeTextView);
                                TextView statusTextView = reservationViewItem.findViewById(R.id.statusTextView);
                                ImageView iconStatus = reservationViewItem.findViewById(R.id.iconStatus);

                                startDateTextView.setText("Début: " + convertDateToFrenchFormat(startDate));
                                endDateTextView.setText("Fin: " + convertDateToFrenchFormat(endDate));
                                numBureauTextView.setText("Bureau: " + numBureau);
                                timeTextView.setText("Heure: " + startTime + " - " + endTime);
                                statusTextView.setText("Réservation passée");
                                statusTextView.setTextColor(getResources().getColor(R.color.red));
                                iconStatus.setImageResource(R.drawable.ic_past);

                                // Le bouton d'annulation est explicitement masqué
                                Button cancelButton = reservationViewItem.findViewById(R.id.cancelButton);
                                cancelButton.setVisibility(View.GONE);

                                reservationsContainer.addView(reservationViewItem);
                            }
                        }

                        if (!hasPastReservations) {
                            reservationMessage.setText("Aucune réservation passée trouvée.");
                        } else {
                            reservationMessage.setVisibility(View.GONE);
                        }
                    });
                } else {
                    Log.e("DEBUG", "Message non reconnu ou mal formé : " + payload);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("DEBUG", "Message MQTT livré avec succès.");
            }
        });

        pastReservationDialog.setContentView(pastReservationView);
        pastReservationDialog.show();
    }



    private void sendMqttCancelOfficeReservation(String username, String startDate, String endDate, String numBureau, String startTime, String endTime) {
        try {
            String topic = "reservation/topic";
            String payload = "GOS;05;cancelreservationoffice;" + username + ";" + startDate + ";" + endDate + ";" + numBureau + ";" + startTime + ";" + endTime;
            MqttMessage message = new MqttMessage(payload.getBytes());
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


}

