/**
 * Allows the event organizer to run the lottery selection for their event.
 *
 * The organizer selects the number of winners using +/- controls, then taps
 * "Draw Winners". The lottery algorithm:
 *  1. Fetches the event's {@code waitingListIds} from Firestore.
 *  2. Shuffles the list randomly.
 *  3. Assigns {@code SELECTED} outcome to the first N entrants (winners) and
 *     {@code NOT_SELECTED} to the rest (losers).
 *  4. Sends an in-app notification to each loser via NotificationDB (respecting
 *     the user's notification preference).
 *  5. Posts a local Android notification on the device for each loss.
 *  6. Saves the LotteryPool results to LotteryDB.
 *
 * Outstanding issues:
 * - The lottery runs entirely on the client. Concurrent draws from multiple
 *   organizer devices are not prevented; a Cloud Function is the correct solution.
 * - {@code totalEntrants} is pre-loaded from the event document and used as a
 *   mock for the sample-rate display; it should be derived live from
 *   {@code waitingListIds.size()} at draw time.
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
    private String cachedEventName;
    private EventDB eventDB;
    private TextView tvWinnerCount, tvWaitingCount, tvSampleRate, tvEventName, tvEventSubtitle;
    private TextView tvDrawResultsSummary, tvWinnerList;
    private android.view.View cvDrawResults;
    private android.widget.Button btnDrawWinners, btnSendInvitations;
    private int winnersToSelect = 1;
    private long totalEntrants = 0;
    private long eventCapacity = Long.MAX_VALUE;
    /** Winners stored after draw runs, waiting for organizer to send invitations. */
    private java.util.List<String> pendingWinners = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lottery_draw);

        eventId = getIntent().getStringExtra("eventId");
        long cap = getIntent().getLongExtra("eventCapacity", -1L);
        if (cap > 0) eventCapacity = cap;
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
        tvEventName = findViewById(R.id.tv_lottery_event_name);
        tvEventSubtitle = findViewById(R.id.tv_lottery_event_subtitle);
        tvDrawResultsSummary = findViewById(R.id.tv_draw_results_summary);
        tvWinnerList = findViewById(R.id.tv_winner_list);
        cvDrawResults = findViewById(R.id.cv_draw_results);
        btnDrawWinners = findViewById(R.id.btn_draw_winners);
        btnSendInvitations = findViewById(R.id.btn_send_invitations);

        findViewById(R.id.btn_lottery_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_plus_winners).setOnClickListener(v -> {
            if (winnersToSelect < eventCapacity) {
                winnersToSelect++;
                updateUI();
            } else {
                Toast.makeText(this, "Cannot exceed event capacity (" + eventCapacity + ")", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_minus_winners).setOnClickListener(v -> {
            if (winnersToSelect > 1) {
                winnersToSelect--;
                updateUI();
            }
        });

<<<<<<< HEAD
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
=======
        btnDrawWinners.setOnClickListener(v -> runLottery());
        btnSendInvitations.setOnClickListener(v -> sendInvitations());
    }

    private void runLottery() {
        if (eventId == null) return;
        if (winnersToSelect <= 0) {
            Toast.makeText(this, "Select at least 1 winner", Toast.LENGTH_SHORT).show();
            return;
        }
        if (winnersToSelect > eventCapacity) {
            Toast.makeText(this, "Number of winners cannot exceed event capacity (" + eventCapacity + ")", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDrawWinners.setEnabled(false);
        Toast.makeText(this, "Running Lottery Draw...", Toast.LENGTH_SHORT).show();

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            if (!eventDoc.exists()) { btnDrawWinners.setEnabled(true); return; }

            java.util.List<String> waitingIds = (java.util.List<String>) eventDoc.get("waitingListIds");
            if (waitingIds == null || waitingIds.isEmpty()) {
                Toast.makeText(this, "No entrants on the waiting list", Toast.LENGTH_SHORT).show();
                btnDrawWinners.setEnabled(true);
                return;
            }

            cachedEventName = eventDoc.getString("name");

            // 1. Shuffle and split
            java.util.Collections.shuffle(waitingIds);
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
            int winCount = Math.min(winnersToSelect, waitingIds.size());
            java.util.List<String> winners = new java.util.ArrayList<>(waitingIds.subList(0, winCount));
            java.util.List<String> losers = new java.util.ArrayList<>(waitingIds.subList(winCount, waitingIds.size()));

<<<<<<< HEAD
            String eventName = eventDoc.getString("name");

            // 3. Update Winners
            for (String wId : winners) {
                updateEntrantOutcome(db, wId, eventId, "SELECTED", null);
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

=======
            // 2. Update outcomes in Firestore (no notifications yet)
            for (String wId : winners) {
                updateEntrantOutcome(db, wId, eventId, "SELECTED", null);
            }
            for (String lId : losers) {
                updateEntrantOutcome(db, lId, eventId, "NOT_SELECTED", () ->
                        checkAndSendLossNotification(db, lId, eventId, cachedEventName));
            }

            // 3. Save LotteryPool
            com.example.cmput301_app.database.LotteryDB lotteryDB = new com.example.cmput301_app.database.LotteryDB();
            com.example.cmput301_app.model.LotteryPool pool = new com.example.cmput301_app.model.LotteryPool(winnersToSelect);
            for (String wId : winners) pool.selectEntrant(wId);

            lotteryDB.createLotteryPool(eventId, pool, aVoid -> {
                pendingWinners = winners;
                // 4. Show results and "Send Invitations" button
                runOnUiThread(() -> showDrawResults(winners.size(), losers.size()));
            }, e -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving lottery results", Toast.LENGTH_SHORT).show();
                    btnDrawWinners.setEnabled(true);
                });
            });
        }).addOnFailureListener(e -> {
            btnDrawWinners.setEnabled(true);
            Toast.makeText(this, "Error running lottery", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDrawResults(int winnerCount, int loserCount) {
        btnDrawWinners.setVisibility(android.view.View.GONE);
        // Also hide +/- controls so the organizer can't re-draw
        findViewById(R.id.btn_plus_winners).setEnabled(false);
        findViewById(R.id.btn_minus_winners).setEnabled(false);

        if (tvDrawResultsSummary != null) {
            tvDrawResultsSummary.setText(
                    winnerCount + " entrant" + (winnerCount == 1 ? "" : "s") + " selected  •  "
                    + loserCount + " not selected");
        }
        if (cvDrawResults != null) cvDrawResults.setVisibility(android.view.View.VISIBLE);
        if (btnSendInvitations != null) btnSendInvitations.setVisibility(android.view.View.VISIBLE);

        // Fetch winner display names from Firestore
        if (pendingWinners != null && !pendingWinners.isEmpty() && tvWinnerList != null) {
            fetchWinnerNames(pendingWinners);
        }
    }

    private void fetchWinnerNames(java.util.List<String> winnerIds) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(winnerIds.size());
        java.util.Map<String, String> nameMap = new java.util.concurrent.ConcurrentHashMap<>();

        for (String uid : winnerIds) {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                String name = doc.getString("name");
                if (name == null || name.isEmpty()) name = doc.getString("username");
                if (name == null || name.isEmpty()) name = uid;
                nameMap.put(uid, name);
                if (remaining.decrementAndGet() == 0) {
                    // Build ordered list using original winnerIds order
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < winnerIds.size(); i++) {
                        sb.append(i + 1).append(". ").append(nameMap.getOrDefault(winnerIds.get(i), winnerIds.get(i)));
                        if (i < winnerIds.size() - 1) sb.append("\n");
                    }
                    runOnUiThread(() -> tvWinnerList.setText(sb.toString()));
                }
            }).addOnFailureListener(e -> {
                nameMap.put(uid, uid);
                if (remaining.decrementAndGet() == 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < winnerIds.size(); i++) {
                        sb.append(i + 1).append(". ").append(nameMap.getOrDefault(winnerIds.get(i), winnerIds.get(i)));
                        if (i < winnerIds.size() - 1) sb.append("\n");
                    }
                    runOnUiThread(() -> tvWinnerList.setText(sb.toString()));
                }
            });
        }
    }

    private void sendInvitations() {
        if (pendingWinners == null || pendingWinners.isEmpty()) {
            Toast.makeText(this, "No winners to notify", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSendInvitations.setEnabled(false);
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(pendingWinners.size());
        for (String wId : pendingWinners) {
            checkAndSendWinNotification(db, wId, eventId, cachedEventName, () -> {
                if (remaining.decrementAndGet() == 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this,
                                "Invitations sent to " + pendingWinners.size() + " entrant"
                                + (pendingWinners.size() == 1 ? "" : "s") + "!",
                                Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            });
        }
    }

>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
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

    private void checkAndSendLossNotification(com.google.firebase.firestore.FirebaseFirestore db, String userId, String evId, String eventName) {
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
<<<<<<< HEAD
            Boolean notificationsEnabled = userDoc.getBoolean("notificationsEnabled");
            if (notificationsEnabled != null && !notificationsEnabled) {
                // Opted out
                return;
            }

            // Create notification record in Firestore
            com.example.cmput301_app.database.NotificationDB notifDB = new com.example.cmput301_app.database.NotificationDB();
            com.example.cmput301_app.model.Notification n = new com.example.cmput301_app.model.Notification(
                    "", evId, com.google.firebase.auth.FirebaseAuth.getInstance().getUid(),
                    "You were not selected in the current draw. You may still be selected if a chosen entrant declines.",
=======
            // Respect the user's notification opt-out preference for loss notifications
            Boolean notificationsEnabled = userDoc.getBoolean("notificationsEnabled");
            if (Boolean.FALSE.equals(notificationsEnabled)) return;

            String displayName = eventName != null ? eventName : "an event";
            String message = "You were not selected for \"" + displayName + "\" in the current draw. "
                    + "You may still be selected if a chosen entrant declines.";

            com.example.cmput301_app.database.NotificationDB notifDB = new com.example.cmput301_app.database.NotificationDB();
            com.example.cmput301_app.model.Notification n = new com.example.cmput301_app.model.Notification(
                    "", evId, com.google.firebase.auth.FirebaseAuth.getInstance().getUid(),
                    message,
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
                    com.example.cmput301_app.model.Notification.NotificationType.LOTTERY_LOSS,
                    com.google.firebase.Timestamp.now()
            );
            n.addRecipient(userId);
<<<<<<< HEAD
            
            notifDB.createNotification(n, savedNotif -> {
                // Trigger local device notification if matching user or as a simulation
                triggerLocalNotification(evId, eventName);
=======

            notifDB.createNotification(n, savedNotif -> {
                triggerLocalNotification(evId, eventName, message);
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
            }, e -> {});
        });
    }

<<<<<<< HEAD
    private void triggerLocalNotification(String evId, String eventName) {
=======
    private void checkAndSendWinNotification(com.google.firebase.firestore.FirebaseFirestore db,
            String userId, String evId, String eventName, Runnable onComplete) {
        String message = "Congratulations! You have been selected for \""
                + (eventName != null ? eventName : "an event")
                + "\". Open the app to accept or decline your invitation.";

        // Always create the in-app notification record so the invitation appears in the bell
        com.example.cmput301_app.database.NotificationDB notifDB = new com.example.cmput301_app.database.NotificationDB();
        com.example.cmput301_app.model.Notification n = new com.example.cmput301_app.model.Notification(
                "", evId, com.google.firebase.auth.FirebaseAuth.getInstance().getUid(),
                message,
                com.example.cmput301_app.model.Notification.NotificationType.LOTTERY_WIN,
                com.google.firebase.Timestamp.now()
        );
        n.addRecipient(userId);

        notifDB.createNotification(n, savedNotif -> {
            // Only send the device push notification if the user has not opted out
            db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                Boolean notificationsEnabled = userDoc.getBoolean("notificationsEnabled");
                if (!Boolean.FALSE.equals(notificationsEnabled)) {
                    triggerLocalNotification(evId, eventName, message);
                }
                if (onComplete != null) onComplete.run();
            }).addOnFailureListener(e -> { if (onComplete != null) onComplete.run(); });
        }, e -> { if (onComplete != null) onComplete.run(); });
    }

    private void triggerLocalNotification(String evId, String eventName, String notificationMessage) {
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
        // Create intent to open Event Details
        android.content.Intent intent = new android.content.Intent(this, com.example.cmput301_app.entrant.EventDetailsActivity.class);
        intent.putExtra("eventId", evId);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                
        // Ensure channel exists
        android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "lottery_results", "Lottery Results", android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Check permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return; // cannot post
            }
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, "lottery_results")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Ensure valid icon or fallback
                .setContentTitle("Lottery Results: " + (eventName != null ? eventName : "Event"))
<<<<<<< HEAD
                .setContentText("You were not selected in the current draw. You may still be selected if a chosen entrant declines.")
                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText("You were not selected in the current draw. You may still be selected if a chosen entrant declines."))
=======
                .setContentText(notificationMessage)
                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText(notificationMessage))
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void loadEventData() {
        eventDB.getEvent(eventId, event -> {
            if (event != null) {
                totalEntrants = event.getWaitingListCount();
                cachedEventName = event.getName();
                if (event.getCapacity() > 0 && eventCapacity == Long.MAX_VALUE) {
                    eventCapacity = event.getCapacity();
                }
                // Clamp current selection to capacity
                if (winnersToSelect > eventCapacity) {
                    winnersToSelect = (int) eventCapacity;
                }
                if (tvEventName != null && event.getName() != null) {
                    tvEventName.setText(event.getName());
                }
                if (tvEventSubtitle != null) {
                    String sub = "";
                    if (event.getCategory() != null) sub += event.getCategory() + " ";
                    if (event.getLocation() != null) sub += "• " + event.getLocation();
                    tvEventSubtitle.setText(sub.trim());
                }
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