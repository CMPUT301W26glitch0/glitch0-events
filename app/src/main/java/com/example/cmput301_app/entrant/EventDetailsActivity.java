package com.example.cmput301_app.entrant;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cmput301_app.R;
import com.example.cmput301_app.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.Map;

public class EventDetailsActivity extends AppCompatActivity {
    private TextView tvTitle, tvDescription, tvCount;
    private Button btnJoin;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String eventId;
    private Event currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        View mainView = findViewById(R.id.event_details_main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        tvTitle = findViewById(R.id.tv_event_title);
        tvDescription = findViewById(R.id.tv_event_description);
        tvCount = findViewById(R.id.tv_waiting_list_count);
        btnJoin = findViewById(R.id.btn_join_waiting_list);

        eventId = getIntent().getStringExtra("eventId");

        if (eventId != null) {
            loadEventDetails();
        } else {
            Toast.makeText(this, "No event ID provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadEventDetails() {
        db.collection("events").document(eventId).addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Toast.makeText(this, "Error loading event details", Toast.LENGTH_SHORT).show();
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                try {
                    currentEvent = documentSnapshot.toObject(Event.class);
                    if (currentEvent != null) {
                        currentEvent.setEventId(documentSnapshot.getId());
                        updateUI();
                    }
                } catch (Exception ex) {
                    Toast.makeText(this, "Data format error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Event not found in database", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (currentEvent == null) return;
        
        tvTitle.setText(currentEvent.getTitle() != null ? currentEvent.getTitle() : "No Title");
        tvDescription.setText(currentEvent.getDescription() != null ? currentEvent.getDescription() : "No Description");
        tvCount.setText("Waiting List: " + currentEvent.getWaitingListCount() + " entrants");

        if (currentEvent.isRegistrationOpen()) {
            btnJoin.setVisibility(View.VISIBLE);
            btnJoin.setOnClickListener(v -> joinWaitingList());
        } else {
            btnJoin.setVisibility(View.GONE);
        }
    }

    private void joinWaitingList() {
        String userId = auth.getUid();
        if (userId == null) {
            Toast.makeText(this, "Please log in to join", Toast.LENGTH_SHORT).show();
            return;
        }

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitingListRef = eventRef.collection("waiting_list").document(userId);

        btnJoin.setEnabled(false);

        waitingListRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                Toast.makeText(this, "You are already on the waiting list", Toast.LENGTH_SHORT).show();
                btnJoin.setEnabled(true);
            } else {
                db.runTransaction((Transaction.Function<Void>) transaction -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", userId);
                    data.put("deviceId", deviceId);
                    data.put("joinedAt", FieldValue.serverTimestamp());

                    transaction.set(waitingListRef, data);
                    transaction.update(eventRef, "waitingListCount", FieldValue.increment(1));
                    return null;
                }).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Successfully joined!", Toast.LENGTH_LONG).show();
                    btnJoin.setEnabled(true);
                }).addOnFailureListener(err -> {
                    Toast.makeText(this, "Error joining: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                    btnJoin.setEnabled(true);
                });
            }
        });
    }
}