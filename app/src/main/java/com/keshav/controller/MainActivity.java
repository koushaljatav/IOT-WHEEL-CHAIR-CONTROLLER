package com.keshav.controller;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import soup.neumorphism.NeumorphButton;
import soup.neumorphism.NeumorphImageButton;
import soup.neumorphism.NeumorphTextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private final String espIp = "192.168.97.177"; // Replace with your ESP IP
    private NeumorphTextView tvDistance;

    private NeumorphButton speed1, speed2, speed3, speed4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        fetchDistancePeriodically();
        sendSpeedCommand(1);

        // Initialize UI components
        NeumorphButton btnConnect = findViewById(R.id.btnConnect);
        NeumorphImageButton btnForward = findViewById(R.id.btnForward);
        NeumorphImageButton btnBackward = findViewById(R.id.btnBackward);
        NeumorphImageButton btnLeft = findViewById(R.id.btnLeft);
        NeumorphImageButton btnRight = findViewById(R.id.btnRight);
        NeumorphButton btnStop = findViewById(R.id.immediateStop);
        tvDistance = findViewById(R.id.distance);

        speed1 = findViewById(R.id.speed1);
        speed2 = findViewById(R.id.speed2);
        speed3 = findViewById(R.id.speed3);
        speed4 = findViewById(R.id.speed4);

        // Set up button click listeners
        btnConnect.setOnClickListener(view -> checkConnection());
        btnStop.setOnClickListener(view -> sendCommandToESP("/EmergencyStop"));

        setActiveSpeedButton(speed1);

        setupTouchListener(btnForward, "/forward");
        setupTouchListener(btnBackward, "/backward");
        setupTouchListener(btnLeft, "/left");
        setupTouchListener(btnRight, "/right");

        setupSpeedButton(speed1, 1);
        setupSpeedButton(speed2, 2);
        setupSpeedButton(speed3, 3);
        setupSpeedButton(speed4, 4);
    }

    // Adds touch listener for movement commands with shape style changes
    private void setupTouchListener(NeumorphImageButton button, String command) {
        button.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN: // Button pressed
                    view.setPressed(true); // Preserve the pressed state for styling
                    sendMovementCommand(command); // Send the movement command
                    return true; // Indicate that the event has been handled

                case MotionEvent.ACTION_UP: // Button released
                case MotionEvent.ACTION_CANCEL: // Handle cancel events as well
                    view.setPressed(false); // Restore the normal state for styling
                    sendMovementCommand("/stop"); // Send stop command
                    return true; // Indicate that the event has been handled
            }
            return false; // For other touch events, don't consume the event
        });
    }

    private void sendMovementCommand(String command) {
        String endpoint = "/command?cmd=" + command;
        sendCommandToESP(endpoint);
    }

    // Handles speed button clicks
    private void setupSpeedButton(NeumorphButton button, int speedLevel) {
        button.setOnClickListener(view -> {
            resetSpeedButtons(); // Reset all buttons to default state
            setActiveSpeedButton(button); // Set the clicked button as active
            sendSpeedCommand(speedLevel); // Send the speed command
        });
    }

    // Resets all speed buttons to default style
    private void resetSpeedButtons() {
        setDefaultButtonStyle(speed1);
        setDefaultButtonStyle(speed2);
        setDefaultButtonStyle(speed3);
        setDefaultButtonStyle(speed4);
    }

    // Applies default style to a button
    private void setDefaultButtonStyle(NeumorphButton button) {
        button.setBackgroundColor(getResources().getColor(android.R.color.transparent)); // Default background
        button.setTextColor(getResources().getColor(android.R.color.black)); // Default text color
    }

    // Sets the active style for the selected button
    private void setActiveSpeedButton(NeumorphButton button) {
        button.setBackgroundColor(getResources().getColor(R.color.teal_200)); // Active background
        button.setTextColor(getResources().getColor(android.R.color.white)); // Active text color
    }

    // Sends commands to set speed
    private void sendSpeedCommand(int speedLevel) {
        String endpoint = "/setSpeed?level=" + speedLevel;
        sendCommandToESP(endpoint);
    }

    // Generic function to send HTTP GET requests to ESP
    private void sendCommandToESP(String endpoint) {
        new Thread(() -> {
            try {
                String urlString = "http://" + espIp + endpoint;
                Log.d("ESP Command URL", "Requesting: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = reader.readLine();
                reader.close();

                Log.d("ESP Response Code", "Response Code: " + responseCode);

                if (responseCode == 200) {
                    runOnUiThread(() -> Toast.makeText(this, response, Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to send command. Response Code: " + responseCode, Toast.LENGTH_SHORT).show());
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Checks connection to ESP and retrieves distance or status
    private void checkConnection() {
        new Thread(() -> {
            try {
                String urlString = "http://" + espIp + "/status";
                Log.d("ESP Connection URL", "Requesting: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String response = reader.readLine();
                    reader.close();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Connected: " + response, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to connect to ESP", Toast.LENGTH_SHORT).show());
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void fetchDistancePeriodically() {
        new Thread(() -> {
            while (true) {
                try {
                    String urlString = "http://" + espIp + "/distance";
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    int responseCode = connection.getResponseCode();

                    if (responseCode == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String response = reader.readLine();
                        reader.close();

                        runOnUiThread(() -> tvDistance.setText("Distance: " + response)); // Update UI
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> tvDistance.setText("Error fetching distance"));
                }

                try {
                    Thread.sleep(1000); // Wait for 1 second before fetching again
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break; // Exit the loop if interrupted
                }
            }
        }).start();
    }
}
