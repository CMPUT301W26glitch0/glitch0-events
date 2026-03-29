/*
 * Purpose: Main dashboard for the Organizer role, displaying their created events.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerDashboardActivity extends AppCompatActivity {
    private static final String TAG = "OrganizerDashboard";
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;
    private EventDB eventDB;
    private RecyclerView rvEvents;
    private OrganizerEventAdapter adapter;
    private List<Event> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();
        eventDB = new EventDB();

        View mainView = findViewById(R.id.organizer_dashboard_main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        rvEvents = findViewById(R.id.rv_organizer_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        adapter = new OrganizerEventAdapter(eventList, this);
        rvEvents.setAdapter(adapter);

        Button btnCreateEvent = findViewById(R.id.btn_create_event);
        btnCreateEvent.setOnClickListener(v -> {
            startActivity(new Intent(OrganizerDashboardActivity.this, CreateEventActivity.class));
        });

        findViewById(R.id.nav_profile_organizer).setOnClickListener(v -> {
            startActivity(new Intent(OrganizerDashboardActivity.this, ProfileActivity.class));
        });

        loadOrganizedEvents();
    }

    /**
     * Checks on each start whether the current user's Firestore document still exists.
     * If an admin has deleted this profile, the document will be gone and we sign the user out.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        mDb.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        mAuth.signOut();
                        getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                .edit().remove("last_uid").apply();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
    }

    private void loadOrganizedEvents() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();
        Log.d(TAG, "Loading events for organizer: " + uid);

        eventDB.getEventsByOrganizer(uid, (value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error fetching events: ", error);
                Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                Log.d(TAG, "Received " + value.size() + " events from Firestore");
                eventList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    try {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(doc.getId());
                            // Client-side safety net: only show this organizer's own events
                            if (uid.equals(event.getOrganizerId())) {
                                eventList.add(event);
                                Log.d(TAG, "Loaded event: " + event.getName() + " (ID: " + event.getEventId() + ")");
                            } else {
                                Log.w(TAG, "Skipping event with mismatched organizerId: " + doc.getId());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing event document: " + doc.getId(), e);
                    }
                }
                adapter.notifyDataSetChanged();

                if (eventList.isEmpty()) {
                    Log.d(TAG, "No events found for this organizer.");
                }
            }
        });
    }
}
