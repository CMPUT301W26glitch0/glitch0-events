/*
 * Purpose: Main dashboard for the Admin role. Lists all events and allows removal.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.ProfileActivity;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.AdminDB;
import com.example.cmput301_app.model.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin dashboard that shows all events with the ability to remove any event.
 * Accessible only to users with the "admin" role.
 *
 * When an admin removes an event:
 * 1. A cancellation notification is sent to all entrants on the waiting list and confirmed attendees.
 * 2. All notifications associated with the event are deleted.
 * 3. The poster image is deleted from Firebase Storage (if present).
 * 4. The event document is deleted from Firestore.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";

    private AdminDB adminDB;
    private RecyclerView rvEvents;
    private AdminEventAdapter adapter;
    private List<Event> eventList;
    private TextView tvEmptyState;
    private TextView tvEventCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark mode preference before setContentView
        boolean isDarkMode = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getBoolean("darkModeEnabled", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_dashboard_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adminDB = new AdminDB();

        tvEmptyState = findViewById(R.id.tv_admin_empty);
        tvEventCount = findViewById(R.id.tv_event_count);

        rvEvents = findViewById(R.id.rv_admin_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        adapter = new AdminEventAdapter(eventList, this, this::onRemoveConfirmed);
        rvEvents.setAdapter(adapter);

        findViewById(R.id.nav_profile_admin).setOnClickListener(v ->
                startActivity(new Intent(AdminDashboardActivity.this, ProfileActivity.class)));

        loadAllEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllEvents();
    }

    private void loadAllEvents() {
        adminDB.getAllEvents(
                events -> {
                    eventList.clear();
                    eventList.addAll(events);
                    adapter.notifyDataSetChanged();

                    boolean isEmpty = eventList.isEmpty();
                    tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvEvents.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    tvEventCount.setText(eventList.size() + " event" + (eventList.size() == 1 ? "" : "s"));
                },
                e -> {
                    Log.e(TAG, "Failed to load events", e);
                    Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void onRemoveConfirmed(Event event, int position) {
        adminDB.removeEvent(
                event.getEventId(),
                unused -> {
                    Toast.makeText(this, "Event removed", Toast.LENGTH_SHORT).show();
                    eventList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, eventList.size());

                    boolean isEmpty = eventList.isEmpty();
                    tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvEvents.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    tvEventCount.setText(eventList.size() + " event" + (eventList.size() == 1 ? "" : "s"));
                },
                e -> {
                    Log.e(TAG, "Failed to remove event", e);
                    Toast.makeText(this, "Failed to remove event: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
        );
    }
}

