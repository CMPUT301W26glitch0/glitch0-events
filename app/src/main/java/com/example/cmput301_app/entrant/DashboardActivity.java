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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

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
}