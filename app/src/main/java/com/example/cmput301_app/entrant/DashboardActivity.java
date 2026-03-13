package com.example.cmput301_app.entrant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.model.Event;
import com.google.firebase.auth.FirebaseAuth;

import com.example.cmput301_app.entrant.NotificationHelper;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EventDB eventDB;
    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        NotificationHelper.createNotificationChannel(this);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notificationsEnabled", true);

        if (notificationsEnabled) {
            NotificationHelper.requestNotificationPermissionAndShowDemo(this, "Demo Event");
            NotificationHelper.requestNotificationPermissionAndShowLossDemo(this, "Demo Event");
        }

        mAuth = FirebaseAuth.getInstance();
        eventDB = new EventDB();

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

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
        });

        findViewById(R.id.nav_my_events).setOnClickListener(v -> {
            Toast.makeText(this, "My Events Clicked", Toast.LENGTH_SHORT).show();
        });

        loadEvents();
    }

    private void loadEvents() {
        eventDB.getAllEvents(events -> {
            eventList.clear();
            eventList.addAll(events);
            adapter.notifyDataSetChanged();
        }, e -> {
            Toast.makeText(DashboardActivity.this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        NotificationHelper.handlePermissionResult(this, requestCode, grantResults, "Demo Event");
    }
}