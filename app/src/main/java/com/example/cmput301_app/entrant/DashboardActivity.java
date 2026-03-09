package com.example.cmput301_app.entrant;

import android.content.Intent;
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
import com.example.cmput301_app.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;
    private RecyclerView rvEvents;
    private EventAdapter adapter;
    private List<Event> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

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
        mDb.collection("events").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                eventList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Event event = doc.toObject(Event.class);
                    event.setEventId(doc.getId());
                    eventList.add(event);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}