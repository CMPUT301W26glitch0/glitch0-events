/*
 * Purpose: Main dashboard for the Entrant role, providing access to event discovery and profile.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.ProfileActivity;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EntrantDB;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.model.Entrant;
import com.example.cmput301_app.model.Event;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dashboard activity for entrants to browse events and scan QR codes.
 * Contains inline Browse and My Events tabs.
 */
public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EventDB eventDB;
    private EntrantDB entrantDB;

    // Browse tab
    private RecyclerView rvEvents;
    private EventAdapter browseAdapter;
    private List<Event> eventList;          // displayed (possibly filtered)
    private List<Event> masterEventList;    // all events from DB

    // My Events tab
    private RecyclerView rvMyEvents;
    private MyEventsAdapter myEventsAdapter;
    private List<MyEventsAdapter.MyEventItem> myEventItems;
    private TextView tvEmptyState;

    // Tab views
    private TextView tvTabBrowse, tvTabMyEvents;
    private View clBrowseContent, clMyEventsContent;

    private boolean myEventsLoaded = false;

    // Filter state (preserved for re-opening the filter screen)
    private int[] filterDays = null;
    private boolean filterMorning = false;
    private boolean filterAfternoon = false;
    private boolean filterEvening = false;
    private boolean filterWaitlist = false;
    private boolean filterHideFull = false;

    private ActivityResultLauncher<Intent> filterLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        eventDB = new EventDB();
        entrantDB = new EntrantDB();

        View dashboardMain = findViewById(R.id.dashboard_main);
        if (dashboardMain != null) {
            ViewCompat.setOnApplyWindowInsetsListener(dashboardMain, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // --- Filter launcher ---
        filterLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        filterDays = data.getIntArrayExtra(FilterEventsActivity.EXTRA_SELECTED_DAYS);
                        filterMorning = data.getBooleanExtra(FilterEventsActivity.EXTRA_MORNING, false);
                        filterAfternoon = data.getBooleanExtra(FilterEventsActivity.EXTRA_AFTERNOON, false);
                        filterEvening = data.getBooleanExtra(FilterEventsActivity.EXTRA_EVENING, false);
                        filterWaitlist = data.getBooleanExtra(FilterEventsActivity.EXTRA_WAITLIST_AVAILABILITY, false);
                        filterHideFull = data.getBooleanExtra(FilterEventsActivity.EXTRA_HIDE_FULL, false);
                        applyLocalFilters();
                    }
                });

        // --- Browse RecyclerView ---
        rvEvents = findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        masterEventList = new ArrayList<>();
        browseAdapter = new EventAdapter(eventList, this);
        rvEvents.setAdapter(browseAdapter);

        // --- My Events RecyclerView ---
        rvMyEvents = findViewById(R.id.rv_my_events);
        rvMyEvents.setLayoutManager(new LinearLayoutManager(this));
        myEventItems = new ArrayList<>();
        myEventsAdapter = new MyEventsAdapter(myEventItems, item -> {
            if (item.event != null && item.event.getEventId() != null) {
                Intent intent = new Intent(this, EventDetailsActivity.class);
                intent.putExtra("eventId", item.event.getEventId());
                startActivity(intent);
            }
        });
        rvMyEvents.setAdapter(myEventsAdapter);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        // --- Tab views ---
        tvTabBrowse = findViewById(R.id.tv_tab_browse);
        tvTabMyEvents = findViewById(R.id.tv_tab_my_events);
        clBrowseContent = findViewById(R.id.cl_browse_content);
        clMyEventsContent = findViewById(R.id.cl_my_events_content);

        tvTabBrowse.setOnClickListener(v -> showTab(false));
        tvTabMyEvents.setOnClickListener(v -> showTab(true));
        showTab(false);

        // --- Filter button ---
        View btnFilterSort = findViewById(R.id.btn_filter_sort);
        if (btnFilterSort != null) {
            btnFilterSort.setOnClickListener(v -> openFilterScreen());
        }

        // --- Bottom nav ---
        View navProfile = findViewById(R.id.nav_profile);
        if (navProfile != null) navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        View navScanQr = findViewById(R.id.nav_scan_qr);
        if (navScanQr != null) navScanQr.setOnClickListener(v ->
                startActivity(new Intent(this, ScanQRActivity.class)));

        View navEvents = findViewById(R.id.nav_events);
        if (navEvents != null) navEvents.setOnClickListener(v -> showTab(false));

        loadBrowseEvents();
    }

    private void openFilterScreen() {
        Intent intent = new Intent(this, FilterEventsActivity.class);
        if (filterDays != null) intent.putExtra(FilterEventsActivity.EXTRA_SELECTED_DAYS, filterDays);
        intent.putExtra(FilterEventsActivity.EXTRA_MORNING, filterMorning);
        intent.putExtra(FilterEventsActivity.EXTRA_AFTERNOON, filterAfternoon);
        intent.putExtra(FilterEventsActivity.EXTRA_EVENING, filterEvening);
        intent.putExtra(FilterEventsActivity.EXTRA_WAITLIST_AVAILABILITY, filterWaitlist);
        intent.putExtra(FilterEventsActivity.EXTRA_HIDE_FULL, filterHideFull);
        filterLauncher.launch(intent);
    }

    private void applyLocalFilters() {
        boolean anyDay = filterDays != null && filterDays.length > 0;
        boolean anyTime = filterMorning || filterAfternoon || filterEvening;

        eventList.clear();
        for (Event event : masterEventList) {
            if (anyDay) {
                int eventDay = event.getDayOfWeek();
                if (eventDay == -1) continue;
                boolean match = false;
                for (int d : filterDays) { if (d == eventDay) { match = true; break; } }
                if (!match) continue;
            }
            if (anyTime) {
                int hour = event.getHourOfDay();
                if (hour == -1) continue;
                boolean timeMatch = false;
                if (filterMorning && hour < 12) timeMatch = true;
                if (filterAfternoon && hour >= 12 && hour < 16) timeMatch = true;
                if (filterEvening && hour >= 16) timeMatch = true;
                if (!timeMatch) continue;
            }
            if (filterWaitlist && !event.hasWaitlistSpace()) continue;
            if (filterHideFull && event.isFull()) continue;
            eventList.add(event);
        }
        browseAdapter.notifyDataSetChanged();
    }

    private void showTab(boolean showMyEvents) {
        if (showMyEvents) {
            clBrowseContent.setVisibility(View.GONE);
            clMyEventsContent.setVisibility(View.VISIBLE);
            tvTabBrowse.setAlpha(0.5f);
            tvTabMyEvents.setAlpha(1.0f);
            if (!myEventsLoaded) { myEventsLoaded = true; loadMyEvents(); }
        } else {
            clBrowseContent.setVisibility(View.VISIBLE);
            clMyEventsContent.setVisibility(View.GONE);
            tvTabBrowse.setAlpha(1.0f);
            tvTabMyEvents.setAlpha(0.5f);
        }
    }

    private String resolveUid() {
        if (mAuth.getCurrentUser() != null) return mAuth.getCurrentUser().getUid();
        return getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("last_uid", null);
    }

    private void loadBrowseEvents() {
        eventDB.getAllEvents(events -> {
            masterEventList.clear();
            masterEventList.addAll(events);
            applyLocalFilters();
        }, e -> Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadMyEvents() {
        String uid = resolveUid();
        if (uid == null) {
            if (tvEmptyState != null) tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        entrantDB.getEntrant(uid, entrant -> {
            if (entrant == null || entrant.getRegistrationHistory() == null
                    || entrant.getRegistrationHistory().isEmpty()) {
                if (tvEmptyState != null) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvMyEvents.setVisibility(View.GONE);
                }
                return;
            }

            List<Entrant.RegistrationRecord> history = entrant.getRegistrationHistory();
            List<MyEventsAdapter.MyEventItem> items = new ArrayList<>();
            int[] completed = {0};
            int total = history.size();

            for (Entrant.RegistrationRecord record : history) {
                eventDB.getEvent(record.getEventId(), event -> {
                    if (event != null) {
                        items.add(new MyEventsAdapter.MyEventItem(event, record.getOutcome()));
                    }
                    completed[0]++;
                    if (completed[0] == total) {
                        Collections.sort(items, (a, b) -> {
                            if (a.event.getDate() == null || b.event.getDate() == null) return 0;
                            return b.event.getDate().compareTo(a.event.getDate());
                        });
                        myEventItems.clear();
                        myEventItems.addAll(items);
                        myEventsAdapter.notifyDataSetChanged();
                        if (tvEmptyState != null) {
                            tvEmptyState.setVisibility(myEventItems.isEmpty() ? View.VISIBLE : View.GONE);
                            rvMyEvents.setVisibility(myEventItems.isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    }
                }, e -> completed[0]++);
            }
        }, e -> Toast.makeText(this, "Error loading your events: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}