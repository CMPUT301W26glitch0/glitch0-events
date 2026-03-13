/**
 * Shows the current entrant's personal event history.
 *
 * Fetches the user's {@code registrationHistory} array from Firestore via
 * EntrantDB, then resolves each event document via EventDB. The resolved events
 * are paired with their lottery outcome and displayed in a RecyclerView backed
 * by MyEventsAdapter, sorted by event date (most recent first).
 *
 * This activity is part of the entrant bottom navigation and is also reachable
 * from DashboardActivity's "My Events" tab, which duplicates this functionality
 * inside the tab container.
 *
 * Outstanding issues:
 * - DashboardActivity now hosts the My Events tab inline, making this standalone
 *   activity potentially redundant. The two implementations should be unified.
 */
package com.example.cmput301_app.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyEventsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EntrantDB entrantDB;
    private EventDB eventDB;
    private RecyclerView rvMyEvents;
    private MyEventsAdapter adapter;
    private List<MyEventsAdapter.MyEventItem> myEventItems;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_events);

        mAuth = FirebaseAuth.getInstance();
        entrantDB = new EntrantDB();
        eventDB = new EventDB();

        View mainView = findViewById(R.id.my_events_main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // set up RecyclerView
        rvMyEvents = findViewById(R.id.rv_my_events);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        rvMyEvents.setLayoutManager(new LinearLayoutManager(this));
        myEventItems = new ArrayList<>();
        adapter = new MyEventsAdapter(myEventItems, item -> openEventDetails(item));
        rvMyEvents.setAdapter(adapter);

        // bottom nav click handlers
        findViewById(R.id.nav_events).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });

        findViewById(R.id.nav_my_events).setOnClickListener(v -> {
            // already on My Events screen, do nothing
        });

        findViewById(R.id.nav_profile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });

        loadMyEvents();
    }

    private void loadMyEvents() {
        String uid = mAuth.getUid();
        if (uid == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        // step 1 — fetch the entrants profile to get registrationHistory
        entrantDB.getEntrant(uid, entrant -> {
            if (entrant == null || entrant.getRegistrationHistory() == null
                    || entrant.getRegistrationHistory().isEmpty()) {
                // no history — show empty state
                tvEmptyState.setVisibility(View.VISIBLE);
                rvMyEvents.setVisibility(View.GONE);
                return;
            }

            List<Entrant.RegistrationRecord> history = entrant.getRegistrationHistory();
            List<MyEventsAdapter.MyEventItem> items = new ArrayList<>();

            // step 2 — For each record, fetch the corresponding event details
            // use an array to track how many fetches have completed
            int[] completed = {0};
            int total = history.size();

            for (Entrant.RegistrationRecord record : history) {
                eventDB.getEvent(record.getEventId(), event -> {
                    if (event != null) {
                        items.add(new MyEventsAdapter.MyEventItem(event, record.getOutcome()));
                    }
                    completed[0]++;

                    // once all fetches are done, sort and display
                    if (completed[0] == total) {
                        // sort by event date, most recent first
                        Collections.sort(items, (a, b) -> {
                            if (a.event.getDate() == null || b.event.getDate() == null) return 0;
                            return b.event.getDate().compareTo(a.event.getDate());
                        });

                        myEventItems.clear();
                        myEventItems.addAll(items);
                        adapter.notifyDataSetChanged();

                        // show empty state if no valid events were found
                        if (myEventItems.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            rvMyEvents.setVisibility(View.GONE);
                        }
                    }
                }, e -> {
                    completed[0]++;
                    Toast.makeText(this, "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }, e -> {
            Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void openEventDetails(MyEventsAdapter.MyEventItem item) {
        if (item.event == null || item.event.getEventId() == null) return;
        
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra("eventId", item.event.getEventId());
        startActivity(intent);
    }
}
