package com.example.openspaceprealpha;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LogsMQTT";
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    private MqttClient mqttClient;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private CheckBox rememberMeCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        rememberMeCheckBox = findViewById(R.id.rememberMe);
        Button loginButton = findViewById(R.id.loginButton);

        loadPreferences();

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                boolean rememberMe = rememberMeCheckBox.isChecked();

                Log.d(TAG, "Login button clicked with username: " + username + ", password: " + password);
                if (rememberMe) {
                    savePreferences(username, password);
                } else {
                    clearPreferences();
                }
                sendMqttMessage(username, password);
            }
        });

        try {
            String broker = "tcp://192.168.29.55:1883";
            String clientId = MqttClient.generateClientId();
            Log.d(TAG, "Creating MQTT client with broker: " + broker + ", clientId: " + clientId);
            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "Connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Message arrived on topic: " + topic + ", message: " + payload);
                    String[] parts = payload.split(";");

                    for (int i = 0; i < parts.length; i++) {
                        Log.d(TAG, "parts[" + i + "]: " + parts[i]);
                    }

                    // Vérifiez si le message est une réponse de connexion réussie
                    if (parts.length >= 8 && parts[0].equals("GOS") && parts[1].equals("00") && parts[2].equals("success")) {
                        String mail = parts[3];
                        String age = parts[4];
                        String nom = parts[5];
                        String prenom = parts[6];
                        String username = parts[7];

                        Log.d(TAG, "Login successful, user: " + nom + " " + prenom);
                        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                        intent.putExtra("mail", mail);
                        intent.putExtra("age", age);
                        intent.putExtra("nom", nom);
                        intent.putExtra("prenom", prenom);
                        intent.putExtra("username", username);
                        startActivity(intent);
                    }
                    // Vérifiez si le message est une requête de connexion
                    else if (parts.length >= 4 && parts[0].equals("GOS") && parts[1].equals("01")) {
                        Log.d(TAG, "Login request message received, ignoring.");
                    }
                    // Si le message ne correspond à aucun des types précédents, c'est un échec de connexion
                    else {
                        Log.d(TAG, "Login failed: Invalid payload structure or status not 'success'");
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connexion échouée", Toast.LENGTH_SHORT).show());
                    }
                }



                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Delivery complete, token: " + token);
                }
            });

            mqttClient.connect(options);
            Log.d(TAG, "Connected to broker: " + broker);
            mqttClient.subscribe("auth/topic");
            Log.d(TAG, "Subscribed to topic: auth/topic");

        } catch (MqttException e) {
            Log.e(TAG, "MQTT Exception", e);
        }
    }

    private void sendMqttMessage(String username, String password) {
        try {
            String topic = "auth/topic";
            String payload = "GOS;01;" + username + ";" + password;
            Log.d(TAG, "Publishing message to topic: " + topic + ", payload: " + payload);
            MqttMessage message = new MqttMessage(payload.getBytes());
            mqttClient.publish(topic, message);
            Log.d(TAG, "Message published");
        } catch (MqttException e) {
            Log.e(TAG, "MQTT Exception", e);
        }
    }

    private void savePreferences(String username, String password) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_REMEMBER_ME, true);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.apply();
    }

    private void loadPreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean rememberMe = preferences.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String username = preferences.getString(KEY_USERNAME, "");
            String password = preferences.getString(KEY_PASSWORD, "");
            usernameEditText.setText(username);
            passwordEditText.setText(password);
            rememberMeCheckBox.setChecked(true);
        }
    }

    private void clearPreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
}
