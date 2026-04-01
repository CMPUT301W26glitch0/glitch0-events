/*
 * Purpose: Admin event browsing screen — lists all events with search and tap-to-details.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.AdminDB;
import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.model.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin screen for browsing all events in the system.
 *
 * Shows every event regardless of status, with a search bar that filters by
 * event name or organizer name. Tapping an event navigates to the event
 * details view.
 */
public class AdminEventsActivity extends AppCompatActivity {

    private static final String TAG = "AdminEvents";

    private AdminDB adminDB;
    private RecyclerView rvEvents;
    private AdminEventBrowseAdapter adapter;

    /** Full unfiltered list loaded from Firestore. */
    private final List<Event> allEvents = new ArrayList<>();
    /** Filtered list shown in the RecyclerView. */
    private final List<Event> filteredEvents = new ArrayList<>();
    /** Maps organizer UID → display name for search and display. */
    private final Map<String, String> organizerNames = new HashMap<>();

    private TextView tvEmptyState;
    private TextView tvEventCount;
    private EditText etSearch;

    /** Tracks how many async loads are still pending (events + profiles = 2). */
    private int pendingLoads = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_events);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_events_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adminDB = new AdminDB();

        tvEmptyState = findViewById(R.id.tv_events_empty);
        tvEventCount = findViewById(R.id.tv_events_count);
        etSearch = findViewById(R.id.et_search_events);

        rvEvents = findViewById(R.id.rv_admin_events_browse);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminEventBrowseAdapter(filteredEvents, organizerNames, this, this::onEventTapped);
        rvEvents.setAdapter(adapter);

        findViewById(R.id.btn_events_back).setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pendingLoads = 2;
        allEvents.clear();
        organizerNames.clear();
        loadData();
    }

    private void loadData() {
        // Load events and profiles in parallel; render once both complete.
        adminDB.getAllEvents(
                events -> {
                    allEvents.clear();
                    allEvents.addAll(events);
                    onLoadComplete();
                },
                e -> {
                    Log.e(TAG, "Failed to load events", e);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show());
                    onLoadComplete();
                }
        );

        adminDB.getAllProfiles(
                profiles -> {
                    organizerNames.clear();
                    for (Profile p : profiles) {
                        if (p.getDeviceId() != null && p.getName() != null) {
                            organizerNames.put(p.getDeviceId(), p.getName());
                        }
                    }
                    onLoadComplete();
                },
                e -> {
                    Log.e(TAG, "Failed to load profiles", e);
                    onLoadComplete();
                }
        );
    }

    private synchronized void onLoadComplete() {
        pendingLoads--;
        if (pendingLoads == 0) {
            runOnUiThread(() -> applyFilter(etSearch.getText().toString()));
        }
    }

    private void applyFilter(String query) {
        filteredEvents.clear();
        String trimmed = query.trim().toLowerCase();

        for (Event event : allEvents) {
            if (trimmed.isEmpty()) {
                filteredEvents.add(event);
                continue;
            }
            // Match against event name
            if (event.getName() != null && event.getName().toLowerCase().contains(trimmed)) {
                filteredEvents.add(event);
                continue;
            }
            // Match against organizer name
            String orgName = organizerNames.get(event.getOrganizerId());
            if (orgName != null && orgName.toLowerCase().contains(trimmed)) {
                filteredEvents.add(event);
            }
        }

        adapter.notifyDataSetChanged();

        boolean isEmpty = filteredEvents.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvEvents.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEventCount.setText(filteredEvents.size() + " event" + (filteredEvents.size() == 1 ? "" : "s"));
    }

    private void onEventTapped(Event event) {
        if (event.getEventId() == null) return;
        Intent intent = new Intent(this, com.example.cmput301_app.entrant.EventDetailsActivity.class);
        intent.putExtra("eventId", event.getEventId());
        startActivity(intent);
    }
}
