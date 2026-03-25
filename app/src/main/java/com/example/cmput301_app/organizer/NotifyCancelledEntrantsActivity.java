/*
 * Purpose: Allows an organizer to compose and send a custom broadcast message
 *          to all entrants whose lottery outcome is DECLINED or CANCELLED for
 *          this event, excluding those who have opted out of organizer notifications.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.organizer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.NotificationDB;
import com.example.cmput301_app.model.Notification;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class NotifyCancelledEntrantsActivity extends AppCompatActivity {

    private String eventId;
    private String eventName;
    private TextInputEditText etMessage;
    private Button btnSend;
    private FirebaseFirestore db;
    private NotificationDB notificationDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notify_waiting_list);   // reuse same layout

        eventId   = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        db = FirebaseFirestore.getInstance();
        notificationDB = new NotificationDB();

        etMessage = findViewById(R.id.et_message);
        btnSend   = findViewById(R.id.btn_send_notification);

        TextView tvNotifyEventName = findViewById(R.id.tv_notify_event_name);
        if (eventName != null) tvNotifyEventName.setText(eventName);

        TextView tvRecipientCount = findViewById(R.id.tv_recipient_count);
        tvRecipientCount.setText("Sending to all declined and cancelled entrants");

        findViewById(R.id.btn_notify_back).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendNotification());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void sendNotification() {
        String message = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (message.isEmpty()) {
            etMessage.setError("Message cannot be empty");
            etMessage.requestFocus();
            return;
        }

        btnSend.setEnabled(false);

        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    List<String> waitingListIds = (List<String>) eventDoc.get("waitingListIds");
                    if (waitingListIds == null || waitingListIds.isEmpty()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "No entrants on this event", Toast.LENGTH_SHORT).show();
                            btnSend.setEnabled(true);
                        });
                        return;
                    }
                    collectEligibleRecipients(waitingListIds, message);
                })
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to load event data", Toast.LENGTH_SHORT).show();
                    btnSend.setEnabled(true);
                }));
    }

    /**
     * For each user on the waiting list, includes them only if:
     *  1. Their registrationHistory outcome for this event is DECLINED or CANCELLED, AND
     *  2. Their notificationsEnabled is not explicitly false.
     */
    private void collectEligibleRecipients(List<String> waitingListIds, String message) {
        List<String> eligibleIds = new ArrayList<>();
        AtomicInteger remaining = new AtomicInteger(waitingListIds.size());

        for (String userId : waitingListIds) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(userDoc -> {
                        boolean isDeclinedOrCancelled = false;
                        Object rawHistory = userDoc.get("registrationHistory");
                        if (rawHistory instanceof List) {
                            for (Object item : (List<?>) rawHistory) {
                                if (item instanceof Map) {
                                    Map<?, ?> record = (Map<?, ?>) item;
                                    if (eventId.equals(record.get("eventId"))) {
                                        String outcome = record.get("outcome") != null
                                                ? record.get("outcome").toString() : "";
                                        isDeclinedOrCancelled =
                                                "DECLINED".equals(outcome) || "CANCELLED".equals(outcome);
                                        break;
                                    }
                                }
                            }
                        }

                        if (isDeclinedOrCancelled) {
                            Boolean notificationsEnabled = userDoc.getBoolean("notificationsEnabled");
                            if (notificationsEnabled == null || notificationsEnabled) {
                                synchronized (eligibleIds) {
                                    eligibleIds.add(userDoc.getId());
                                }
                            }
                        }

                        if (remaining.decrementAndGet() == 0) {
                            dispatchNotification(eligibleIds, message);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (remaining.decrementAndGet() == 0) {
                            dispatchNotification(eligibleIds, message);
                        }
                    });
        }
    }

    private void dispatchNotification(List<String> recipientIds, String message) {
        if (recipientIds.isEmpty()) {
            runOnUiThread(() -> {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("No Recipients")
                        .setMessage("There are no declined or cancelled entrants to notify, or all have opted out of notifications.")
                        .setPositiveButton("OK", null)
                        .show();
                btnSend.setEnabled(true);
            });
            return;
        }

        String organizerDeviceId = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("last_uid", null);

        Notification notif = new Notification(
                null,
                eventId,
                organizerDeviceId,
                message,
                Notification.NotificationType.ORGANIZER_BROADCAST,
                com.google.firebase.Timestamp.now()
        );
        for (String id : recipientIds) {
            notif.addRecipient(id);
        }

        final int count = recipientIds.size();
        notificationDB.createNotification(notif,
                savedNotif -> runOnUiThread(() -> showConfirmationAndFinish(count)),
                e -> runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show();
                    btnSend.setEnabled(true);
                }));
    }

    private void showConfirmationAndFinish(int count) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Notification Sent")
                .setMessage("Your message was sent to " + count
                        + " declined/cancelled entrant" + (count == 1 ? "" : "s") + ".")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}
