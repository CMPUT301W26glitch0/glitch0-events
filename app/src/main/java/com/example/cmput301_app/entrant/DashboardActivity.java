/*
 * Purpose: Main dashboard for the Entrant role, providing access to event discovery and profile.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.entrant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.MainActivity;
import com.example.cmput301_app.ProfileActivity;
import com.example.cmput301_app.RegisterActivity;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.database.EntrantDB;
import com.example.cmput301_app.model.Event;
import com.google.firebase.auth.FirebaseAuth;

import com.example.cmput301_app.entrant.NotificationHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard activity for entrants to browse events and scan QR codes
 *
 */
public class DashboardActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EventDB eventDB;
    private EntrantDB entrantDB;
    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> eventList;
    private String deviceId;
    private com.google.firebase.firestore.ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        NotificationHelper.createNotificationChannel(this);
        listenForNotifications();

        mAuth = FirebaseAuth.getInstance();
        eventDB = new EventDB();
        entrantDB = new EntrantDB();

        // Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        View dashboardMain = findViewById(R.id.dashboard_main);
        if (dashboardMain != null) {
            ViewCompat.setOnApplyWindowInsetsListener(dashboardMain, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        rvEvents = findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList, this);
        rvEvents.setAdapter(adapter);

        // Setup navigation buttons
        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
        });

        // Scan QR button
        findViewById(R.id.nav_scan_qr).setOnClickListener(v -> {
            checkProfileAndNavigateToScan();
        });

        findViewById(R.id.nav_my_events).setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, MyEventsActivity.class));
            finish();
        });
        loadEvents();
    }

    /**
     * Checks if user has a profile before allowing navigation to scan screen.
     * If no profile exists, prompts user to create one.
     */
    private void checkProfileAndNavigateToScan() {
        // Check if user is authenticated
        if (mAuth.getCurrentUser() != null) {
            // User is logged in, navigate to scan screen
            Intent intent = new Intent(this, ScanQRActivity.class);
            startActivity(intent);
        } else {
            // No profile, prompt to create one
            Toast.makeText(this, "Please create a profile first", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        }
    }


    private void loadEvents() {
        eventDB.getAllEvents(events -> {
            eventList.clear();
            eventList.addAll(events);
            adapter.notifyDataSetChanged();
        }, e -> {
            Toast.makeText(DashboardActivity.this,
                    "Error loading events: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Subscribes to real-time lottery notifications for the currently logged-in entrant.
     *
     * This method attaches a Firestore snapshot listener to the "notifications"
     * collection and filters documents where the current user is included in
     * the recipientIds array.
     *
     * When a new notification document appears:
     *
     * 1. The notification is checked against local SharedPreferences to prevent
     *    duplicate notifications from being shown multiple times.
     * 2. The associated event is verified to ensure it still exists.
     * 3. Depending on the notification type (LOTTERY_WIN or LOTTERY_LOSS),
     *    NotificationHelper is used to display an Android system notification.
     *
     * The listener is automatically removed in onDestroy() to prevent memory
     * leaks and background notification triggers.
     */

    private void listenForNotifications() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("notification_prefs", MODE_PRIVATE);

        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {

            String role = userDoc.getString("role");
            if (!"entrant".equals(role)) {
                return;
            }

            notificationListener = db.collection("notifications")
                    .whereArrayContains("recipientIds", uid)
                    .addSnapshotListener((snapshots, e) -> {

                        if (e != null || snapshots == null) return;

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {

                            String notificationId = doc.getId();
                            String type = doc.getString("type");
                            String eventId = doc.getString("eventId");

                            if (notificationId == null || type == null || eventId == null) continue;

                            boolean alreadyShown = prefs.getBoolean(notificationId, false);
                            if (alreadyShown) continue;

                            db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {

                                // Skip deleted events
                                if (!eventDoc.exists()) {
                                    prefs.edit().putBoolean(notificationId, true).apply();
                                    return;
                                }

                                String eventName = eventDoc.getString("name");
                                if (eventName == null || eventName.isEmpty()) {
                                    eventName = "Event";
                                }

                                if ("LOTTERY_WIN".equals(type)) {
                                    NotificationHelper.showLotteryWinNotification(
                                            DashboardActivity.this,
                                            eventName,
                                            eventId
                                    );
                                } else if ("LOTTERY_LOSS".equals(type)) {
                                    NotificationHelper.showLotteryLossNotification(
                                            DashboardActivity.this,
                                            eventName,
                                            eventId
                                    );
                                }

                                prefs.edit().putBoolean(notificationId, true).apply();
                            });
                        }
                    });
        });
    }

    /**
     * Cleans up the Firestore notification listener when the activity is destroyed.
     *
     * Removing the listener prevents the activity from continuing to receive
     * notification updates after the dashboard is closed
     */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
}