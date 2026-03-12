package com.example.cmput301_app.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.bumptech.glide.Glide;
import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.organizer.CreateEventActivity;
import com.example.cmput301_app.organizer.OrganizerDashboardActivity;
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
    private TextView tvTitle, tvDescription, tvDate, tvRegOpen, tvRegClose, tvCategory, tvLocation, tvPrice, tvCapacity;
    private Button btnJoin, btnEdit;
    private ImageView ivHeader;
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

        initViews();
        
        eventId = getIntent().getStringExtra("eventId");

        if (eventId != null) {
            loadEventDetails();
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_event_title);
        tvDescription = findViewById(R.id.tv_event_description);
        tvDate = findViewById(R.id.tv_event_date);
        tvRegOpen = findViewById(R.id.tv_reg_open);
        tvRegClose = findViewById(R.id.tv_reg_close);
        tvCategory = findViewById(R.id.tv_category);
        tvLocation = findViewById(R.id.tv_event_location);
        tvPrice = findViewById(R.id.tv_event_price);
        tvCapacity = findViewById(R.id.tv_event_capacity);
        btnJoin = findViewById(R.id.btn_join_waiting_list);
        btnEdit = findViewById(R.id.btn_edit_event);
        ivHeader = findViewById(R.id.iv_header);

        View mainView = findViewById(R.id.event_details_main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(0, 0, 0, systemBars.bottom);
                return insets;
            });
        }

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // If the user is the organizer, go back to the Organizer Dashboard
                String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                if (currentEvent != null && deviceId.equals(currentEvent.getOrganizerId())) {
                    Intent intent = new Intent(this, OrganizerDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                finish();
            });
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(this, CreateEventActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
            });
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
        
        if (tvTitle != null) tvTitle.setText(currentEvent.getName());
        if (tvDescription != null) tvDescription.setText(currentEvent.getDescription());
        if (tvCategory != null) {
            String category = currentEvent.getCategory();
            tvCategory.setText(category != null ? category.toUpperCase() : "GENERAL");
        }
        if (tvLocation != null) tvLocation.setText(currentEvent.getLocation());
        
        if (tvPrice != null) {
            tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", currentEvent.getPrice()));
        }
        
        if (tvCapacity != null) {
            tvCapacity.setText(String.valueOf(currentEvent.getCapacity()));
        }

        if (ivHeader != null && currentEvent.getPosterUrl() != null && !currentEvent.getPosterUrl().isEmpty()) {
            Glide.with(this)
                 .load(currentEvent.getPosterUrl())
                 .placeholder(android.R.drawable.ic_menu_gallery)
                 .into(ivHeader);
        }

        SimpleDateFormat fullFormat = new SimpleDateFormat("EEE, MMM dd HH:mm:ss", Locale.getDefault());

        if (currentEvent.getDate() != null && tvDate != null) {
            tvDate.setText("Event Date: " + fullFormat.format(currentEvent.getDate().toDate()));
        }

        if (currentEvent.getRegistrationOpen() != null && tvRegOpen != null) {
            tvRegOpen.setText("Registration Open:\n" + fullFormat.format(currentEvent.getRegistrationOpen().toDate()));
        }
        
        if (currentEvent.getRegistrationClose() != null && tvRegClose != null) {
            tvRegClose.setText("Registration Close:\n" + fullFormat.format(currentEvent.getRegistrationClose().toDate()));
        }

        // Check ownership
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId.equals(currentEvent.getOrganizerId())) {
            if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
            if (btnJoin != null) btnJoin.setVisibility(View.GONE);
        } else {
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            setupJoinButton();
        }
    }

    private void setupJoinButton() {
        if (btnJoin != null) {
            if (currentEvent.checkIsRegistrationOpen()) {
                btnJoin.setVisibility(View.VISIBLE);
                btnJoin.setEnabled(true);
                btnJoin.setText("Join Waiting List");
                btnJoin.setOnClickListener(v -> joinWaitingList());
            } else {
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
