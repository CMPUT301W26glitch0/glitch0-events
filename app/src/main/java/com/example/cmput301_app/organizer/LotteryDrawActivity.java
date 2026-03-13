/*
 * Purpose: Allows organizers to execute the lottery selection for their event.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.model.Event;

public class LotteryDrawActivity extends AppCompatActivity {

    private String eventId;
    private EventDB eventDB;
    private TextView tvWinnerCount, tvWaitingCount, tvSampleRate;
    private int winnersToSelect = 20;
    private long totalEntrants = 142; // Mock

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lottery_draw);

        eventId = getIntent().getStringExtra("eventId");
        eventDB = new EventDB();

        initViews();
        if (eventId != null) {
            loadEventData();
        }
    }

    private void initViews() {
        tvWinnerCount = findViewById(R.id.tv_winner_count);
        tvWaitingCount = findViewById(R.id.tv_lottery_waiting_count);
        tvSampleRate = findViewById(R.id.tv_sample_rate);

        findViewById(R.id.btn_lottery_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_plus_winners).setOnClickListener(v -> {
            winnersToSelect++;
            updateUI();
        });

        findViewById(R.id.btn_minus_winners).setOnClickListener(v -> {
            if (winnersToSelect > 1) {
                winnersToSelect--;
                updateUI();
            }
        });

        findViewById(R.id.btn_draw_winners).setOnClickListener(v -> {
            runLottery();
        });
    }

    private void runLottery() {
        if (eventId == null || totalEntrants == 0) return;
        
        // Show a quick loader/toast
        Toast.makeText(this, "Running Lottery Draw...", Toast.LENGTH_SHORT).show();

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            if (!eventDoc.exists()) return;
            
            java.util.List<String> waitingIds = (java.util.List<String>) eventDoc.get("waitingListIds");
            if (waitingIds == null || waitingIds.isEmpty()) return;

            // 1. Shuffle to randomize
            java.util.Collections.shuffle(waitingIds);

            // 2. Split into winners and losers
            int winCount = Math.min(winnersToSelect, waitingIds.size());
            java.util.List<String> winners = new java.util.ArrayList<>(waitingIds.subList(0, winCount));
            java.util.List<String> losers = new java.util.ArrayList<>(waitingIds.subList(winCount, waitingIds.size()));

            String eventName = eventDoc.getString("name");

            // 3. Update Winners & Send Notifications
            for (String wId : winners) {
                updateEntrantOutcome(db, wId, eventId, "SELECTED", () -> {
                    checkAndSendWinNotification(db, wId, eventId, eventName);
                });
            }

            // 4. Update Losers & Send Notifications
            for (String lId : losers) {
                updateEntrantOutcome(db, lId, eventId, "NOT_SELECTED", () -> {
                    checkAndSendLossNotification(db, lId, eventId, eventName);
                });
            }



            // 5. Build and Save LotteryPool to LotteryDB
            com.example.cmput301_app.database.LotteryDB lotteryDB = new com.example.cmput301_app.database.LotteryDB();
            com.example.cmput301_app.model.LotteryPool pool = new com.example.cmput301_app.model.LotteryPool(winnersToSelect);
            for (String wId : winners) pool.selectEntrant(wId);
            
            lotteryDB.createLotteryPool(eventId, pool, aVoid -> {
                // Done
                Toast.makeText(this, "Lottery Complete!", Toast.LENGTH_SHORT).show();
                finish();
            }, e -> {
                Toast.makeText(this, "Error saving lottery results", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateEntrantOutcome(com.google.firebase.firestore.FirebaseFirestore db, String userId, String evId, String newStatus, Runnable onComplete) {
        java.util.Map<String, Object> oldRecord = new java.util.HashMap<>();
        oldRecord.put("eventId", evId);
        oldRecord.put("outcome", "WAITING");

        java.util.Map<String, Object> newRecord = new java.util.HashMap<>();
        newRecord.put("eventId", evId);
        newRecord.put("outcome", newStatus);
        newRecord.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .update("registrationHistory", com.google.firebase.firestore.FieldValue.arrayRemove(oldRecord))
                .addOnSuccessListener(a -> {
                    db.collection("users").document(userId)
                            .update("registrationHistory", com.google.firebase.firestore.FieldValue.arrayUnion(newRecord))
                            .addOnSuccessListener(a2 -> {
                                if (onComplete != null) onComplete.run();
                            });
                });
    }

    private void checkAndSendLossNotification(com.google.firebase.firestore.FirebaseFirestore db,
                                              String userId,
                                              String evId,
                                              String eventName) {
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            Boolean notificationsEnabled = userDoc.getBoolean("notificationsEnabled");
            if (notificationsEnabled != null && !notificationsEnabled) {
                return;
            }

            com.example.cmput301_app.database.NotificationDB notifDB =
                    new com.example.cmput301_app.database.NotificationDB();

            com.example.cmput301_app.model.Notification n =
                    new com.example.cmput301_app.model.Notification(
                            "",
                            evId,
                            com.google.firebase.auth.FirebaseAuth.getInstance().getUid(),
                            "You were not selected for " + eventName + " in the current draw. You may still be selected if a chosen entrant declines.",
                            com.example.cmput301_app.model.Notification.NotificationType.LOTTERY_LOSS,
                            com.google.firebase.Timestamp.now()
                    );

            n.addRecipient(userId);

            notifDB.createNotification(n, savedNotif -> {
            }, e -> {});
        });
    }

    private void checkAndSendWinNotification(com.google.firebase.firestore.FirebaseFirestore db,
                                             String userId,
                                             String evId,
                                             String eventName) {

        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {

            Boolean notificationsEnabled = userDoc.getBoolean("notificationsEnabled");

            if (notificationsEnabled != null && !notificationsEnabled) {
                return; // user opted out
            }

            com.example.cmput301_app.database.NotificationDB notifDB =
                    new com.example.cmput301_app.database.NotificationDB();

            com.example.cmput301_app.model.Notification n =
                    new com.example.cmput301_app.model.Notification(
                            "",
                            evId,
                            com.google.firebase.auth.FirebaseAuth.getInstance().getUid(),
                            "You were selected for " + eventName + ". Tap to view your invitation.",
                            com.example.cmput301_app.model.Notification.NotificationType.LOTTERY_WIN,
                            com.google.firebase.Timestamp.now()
                    );

            n.addRecipient(userId);

            notifDB.createNotification(n, savedNotif -> {
            }, e -> {});
        });
    }

    private void loadEventData() {
        eventDB.getEvent(eventId, event -> {
            if (event != null) {
                totalEntrants = event.getWaitingListCount();
                updateUI();
            }
        }, e -> {});
    }

    private void updateUI() {
        tvWinnerCount.setText(String.valueOf(winnersToSelect));
        tvWaitingCount.setText(totalEntrants + " Entrants");
        
        if (totalEntrants > 0) {
            int rate = (int) ((winnersToSelect * 100.0) / totalEntrants);
            tvSampleRate.setText("~" + rate + "%");
        }
    }
}