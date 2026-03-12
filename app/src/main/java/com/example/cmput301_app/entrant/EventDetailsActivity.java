package com.example.cmput301_app.entrant;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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
import com.example.cmput301_app.model.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EventDetailsActivity extends AppCompatActivity {
    private static final String TAG = "EventDetails";
    private TextView tvTitle, tvDescription, tvDate, tvTime;
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
                // We keep top padding 0 for the immersive header image, but add bottom padding for navigation
                v.setPadding(0, 0, 0, systemBars.bottom);
                return insets;
            });
        }

        tvTitle = findViewById(R.id.tv_event_title);
        tvDescription = findViewById(R.id.tv_event_description);
        tvDate = findViewById(R.id.tv_event_date);
        tvTime = findViewById(R.id.tv_event_time);
        btnJoin = findViewById(R.id.btn_join_waiting_list);

        // Back button functionality
        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        eventId = getIntent().getStringExtra("eventId");

        if (eventId != null) {
            loadEventDetails();
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadEventDetails() {
        db.collection("events").document(eventId).addSnapshotListener((doc, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed.", e);
                return;
            }
            if (doc != null && doc.exists()) {
                try {
                    currentEvent = doc.toObject(Event.class);
                    if (currentEvent != null) {
                        currentEvent.setEventId(doc.getId());
                        updateUI();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error parsing event", ex);
                }
            }
        });
    }

    private void updateUI() {
        if (currentEvent == null) return;
        
        if (tvTitle != null) tvTitle.setText(currentEvent.getTitle());
        if (tvDescription != null) tvDescription.setText(currentEvent.getDescription());

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        if (currentEvent.getRegistrationStart() != null) {
            if (tvDate != null) tvDate.setText(dateFormat.format(currentEvent.getRegistrationStart()));
            if (tvTime != null) {
                String end = currentEvent.getRegistrationEnd() != null ? timeFormat.format(currentEvent.getRegistrationEnd()) : "End";
                tvTime.setText(timeFormat.format(currentEvent.getRegistrationStart()) + " - \n" + end);
            }
        }

        // Acceptance Criteria: The button is only shown when the event registration period is open
        if (btnJoin != null) {
            if (currentEvent.isRegistrationOpen()) {
                btnJoin.setVisibility(View.VISIBLE);
                btnJoin.setOnClickListener(v -> joinWaitingList());
            } else {
                // For development/debugging, you might want to keep it visible but disabled
                // btnJoin.setVisibility(View.GONE);
                btnJoin.setVisibility(View.VISIBLE);
                btnJoin.setEnabled(false);
                btnJoin.setText("Registration Closed");
                btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBDBDBD));
            }
        }
    }

    private void joinWaitingList() {
        if (currentEvent == null || eventId == null) return;
        String uid = auth.getUid();
        if (uid == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnJoin.setEnabled(false);
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        
        DocumentReference eventRef = db.collection("events").document(eventId);
        DocumentReference waitRef = eventRef.collection("waiting_list").document(uid);

        waitRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                Toast.makeText(EventDetailsActivity.this, "Already joined the waiting list.", Toast.LENGTH_SHORT).show();
                btnJoin.setEnabled(true);
            } else {
                db.runTransaction(transaction -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", uid);
                    data.put("deviceId", deviceId);
                    data.put("joinedAt", FieldValue.serverTimestamp());

                    transaction.set(waitRef, data);
                    transaction.update(eventRef, "waitingListCount", FieldValue.increment(1));
                    return null;
                }).addOnSuccessListener(aVoid -> {
                    if (!isFinishing()) {
                        Toast.makeText(EventDetailsActivity.this, "Joined successfully!", Toast.LENGTH_SHORT).show();
                        btnJoin.setEnabled(true);
                        btnJoin.setText("Already Joined");
                    }
                }).addOnFailureListener(e -> {
                    if (!isFinishing()) {
                        Log.e(TAG, "Transaction failed", e);
                        Toast.makeText(EventDetailsActivity.this, "Error joining: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnJoin.setEnabled(true);
                    }
                });
            }
        });
    }
}