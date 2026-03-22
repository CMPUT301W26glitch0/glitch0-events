/*
 * Purpose: Displays detailed information for a specific event to an entrant, including joining the waitlist.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EntrantDB;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.model.Comment;
import com.example.cmput301_app.model.Entrant;
import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.organizer.CreateEventActivity;
import com.example.cmput301_app.organizer.OrganizerDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.firestore.FieldValue;

public class EventDetailsActivity extends AppCompatActivity {
    private static final String TAG = "EventDetails";
    private TextView tvTitle, tvDescription, tvDate, tvRegOpen, tvRegClose, tvCategory, tvLocation, tvPrice, tvCapacity;
    private Button btnJoin, btnEdit;
    private ImageView ivHeader;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String eventId;
    private Event currentEvent;
    private boolean isOnWaitingList = false;

    // Added for invitation actions
    private View llInvitationActions;
    private Button btnAccept, btnDecline;
    private TextView tvInvitationStatus;

    // Added for comments
    private RecyclerView rvComments;
    private CommentAdapter commentAdapter;
    private EditText etCommentInput;
    private ImageButton btnPostComment;
    private TextView tvNoComments;


    // added for joinWaitingList method
    private EntrantDB entrantDB;
    private EventDB eventDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        entrantDB = new EntrantDB();
        eventDB = new EventDB();

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
        
        llInvitationActions = findViewById(R.id.ll_invitation_actions);
        btnAccept = findViewById(R.id.btn_accept_invitation);
        btnDecline = findViewById(R.id.btn_decline_invitation);
        tvInvitationStatus = findViewById(R.id.tv_invitation_status);
        
        btnEdit = findViewById(R.id.btn_edit_event);
        ivHeader = findViewById(R.id.iv_header);

        rvComments = findViewById(R.id.rv_comments);
        etCommentInput = findViewById(R.id.et_comment_input);
        btnPostComment = findViewById(R.id.btn_post_comment);
        tvNoComments = findViewById(R.id.tv_no_comments);

        if (rvComments != null) {
            rvComments.setLayoutManager(new LinearLayoutManager(this));
            commentAdapter = new CommentAdapter(new java.util.ArrayList<>());
            rvComments.setAdapter(commentAdapter);
        }

        if (btnPostComment != null) {
            btnPostComment.setOnClickListener(v -> postComment());
        }

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

    /** Firebase Auth first; SharedPreferences last_uid fallback for device ID login. */
    private String resolveUid() {
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("last_uid", null);
    }

    private void loadEventDetails() {
        db.collection("events").document(eventId).addSnapshotListener((doc, e) -> {
            if (e != null) {
                Log.e("EventDetailsActivity", "Listen failed.", e);
                return;
            }
            if (doc != null && doc.exists()) {
                try {
                    currentEvent = doc.toObject(Event.class);
                    if (currentEvent != null) {
                        currentEvent.setEventId(doc.getId());
                        updateUI();
                        checkWaitingListStatus();
                    }
                } catch (Exception ex) {
                    Log.e("EventDetailsActivity", "Error parsing event", ex);
                }
            }
        });
    }

    private void postComment() {
        if (etCommentInput == null || currentEvent == null) return;
        String content = etCommentInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = resolveUid();
        if (uid == null) {
            Toast.makeText(this, "Must be logged in to comment.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPostComment.setEnabled(false);
        entrantDB.getEntrant(uid, entrant -> {
            String authorName = (entrant != null && entrant.getName() != null) ? entrant.getName() : "Unknown Entrant";
            Comment newComment = new Comment(content, authorName, com.google.firebase.Timestamp.now());

            db.collection("events").document(eventId)
                    .update("comments", FieldValue.arrayUnion(newComment))
                    .addOnSuccessListener(aVoid -> {
                        etCommentInput.setText("");
                        btnPostComment.setEnabled(true);
                        // The snapshot listener will automatically update the UI with the new comment
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show();
                        btnPostComment.setEnabled(true);
                    });
        }, e -> {
            Toast.makeText(this, "Failed to fetch user profile", Toast.LENGTH_SHORT).show();
            btnPostComment.setEnabled(true);
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

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId.equals(currentEvent.getOrganizerId())) {
            if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
            if (btnJoin != null) btnJoin.setVisibility(View.GONE);
        } else {
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
        }

        if (commentAdapter != null) {
            java.util.List<Comment> comments = currentEvent.getComments();
            if (comments == null || comments.isEmpty()) {
                rvComments.setVisibility(View.GONE);
                if (tvNoComments != null) tvNoComments.setVisibility(View.VISIBLE);
                commentAdapter.setComments(new java.util.ArrayList<>());
            } else {
                rvComments.setVisibility(View.VISIBLE);
                if (tvNoComments != null) tvNoComments.setVisibility(View.GONE);
                commentAdapter.setComments(comments);
            }
        }
    }

    private void checkWaitingListStatus() {
        String uid = resolveUid();
        if (uid == null || eventId == null) {
            setupJoinButton();
            return;
        }

        entrantDB.getEntrant(uid, entrant -> {
            isOnWaitingList = (entrant != null && entrant.isOnWaitingList(eventId));
            setupJoinButton();
        }, e -> {
            isOnWaitingList = false;
            setupJoinButton();
        });
    }

    private void setupJoinButton() {
        if (btnJoin != null) {
            String deviceId = resolveUid();
            if (deviceId == null) {
                showWaitlistButton();
                return;
            }

            // Fetch entrant to determine state
            entrantDB.getEntrant(deviceId, entrant -> {
                if (entrant != null) {
                    java.util.List<Entrant.RegistrationRecord> history = entrant.getRegistrationHistory();
                    Entrant.RegistrationRecord.Outcome currentOutcome = null;
                    if (history != null) {
                        for (Entrant.RegistrationRecord record : history) {
                            if (record.getEventId().equals(eventId)) {
                                currentOutcome = record.getOutcome();
                                break;
                            }
                        }
                    }

                    if (currentOutcome == Entrant.RegistrationRecord.Outcome.SELECTED) {
                        showInvitationActions();
                    } else if (currentOutcome == Entrant.RegistrationRecord.Outcome.ACCEPTED) {
                        showInvitationStatus("Invitation Accepted");
                    } else if (currentOutcome == Entrant.RegistrationRecord.Outcome.DECLINED) {
                        showInvitationStatus("Invitation Declined");
                    } else if (currentOutcome == Entrant.RegistrationRecord.Outcome.NOT_SELECTED) {
                        showInvitationStatus("Not Selected in Lottery");
                    } else if (currentOutcome == Entrant.RegistrationRecord.Outcome.WAITING) {
                        showWaitlistButton();
                    } else {
                        showWaitlistButton();
                    }
                } else {
                    showWaitlistButton();
                }
            }, e -> showWaitlistButton());
        }
    }

    private void showWaitlistButton() {
        if (llInvitationActions != null) llInvitationActions.setVisibility(View.GONE);
        if (tvInvitationStatus != null) tvInvitationStatus.setVisibility(View.GONE);

        if (currentEvent.checkIsRegistrationOpen()) {
            long limit = currentEvent.getWaitingListLimit();
            java.util.List<String> currentWaitlist = currentEvent.getWaitingListIds();
            int currentSize = (currentWaitlist != null) ? currentWaitlist.size() : 0;

            if (limit > 0 && currentSize >= limit) {
                btnJoin.setVisibility(View.VISIBLE);
                btnJoin.setEnabled(false);
                btnJoin.setText("Waiting List Full");
                btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBDBDBD));
            } else {
                btnJoin.setVisibility(View.VISIBLE);
                btnJoin.setEnabled(true);
                btnJoin.setText("Join Waiting List");
                btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));

                entrantDB.getEntrant(resolveUid(), entrant -> {
                    if (entrant != null && entrant.isOnWaitingList(eventId)) {
                        // Already joined — let them leave
                        btnJoin.setText("Leave Waiting List");
                        btnJoin.setEnabled(true);
                        btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5722));
                        btnJoin.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                                .setTitle("Leave Waiting List")
                                .setMessage("Are you sure you want to remove yourself from the waiting list?")
                                .setPositiveButton("Leave", (dialog, which) -> leaveWaitingList())
                                .setNegativeButton("Cancel", null)
                                .show());
                    } else {
                        btnJoin.setOnClickListener(v -> joinWaitingList());
                    }
                }, e -> btnJoin.setOnClickListener(v -> joinWaitingList()));
            }
        } else {
            btnJoin.setVisibility(View.VISIBLE);
            btnJoin.setEnabled(false);
            btnJoin.setText("Registration Closed");
            btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBDBDBD));
        }
    }

    private void showInvitationActions() {
        if (btnJoin != null) btnJoin.setVisibility(View.GONE);
        if (tvInvitationStatus != null) tvInvitationStatus.setVisibility(View.GONE);
        if (llInvitationActions != null) {
            llInvitationActions.setVisibility(View.VISIBLE);
            
            if (btnAccept != null) {
                btnAccept.setOnClickListener(v -> {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Accept Invitation")
                            .setMessage("Are you sure you want to accept this invitation?")
                            .setPositiveButton("Accept", (dialog, which) -> handleAccept())
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
            if (btnDecline != null) {
                btnDecline.setOnClickListener(v -> {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Decline Invitation")
                            .setMessage("Are you sure you want to decline this invitation? You will lose your spot.")
                            .setPositiveButton("Decline", (dialog, which) -> handleDecline())
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            }
        }
    }

    private void showInvitationStatus(String message) {
        if (btnJoin != null) btnJoin.setVisibility(View.GONE);
        if (llInvitationActions != null) llInvitationActions.setVisibility(View.GONE);
        if (tvInvitationStatus != null) {
            tvInvitationStatus.setVisibility(View.VISIBLE);
            tvInvitationStatus.setText(message);
        }
    }

    private void handleAccept() {
        String uid = resolveUid();
        if (uid == null) return;

        updateEntrantOutcome(uid, eventId, "SELECTED", "ACCEPTED", () -> {
            // Push their ID to the confirmedAttendees array on the Event
            eventDB.addToConfirmedAttendees(eventId, uid, aVoid -> {
                Toast.makeText(this, "Invitation Accepted!", Toast.LENGTH_SHORT).show();
                showInvitationStatus("Invitation Accepted");
            }, e -> {
                Toast.makeText(this, "Accepted, but failed to log attendee on event.", Toast.LENGTH_SHORT).show();
                showInvitationStatus("Invitation Accepted");
            });
        });
    }

    private void handleDecline() {
        String uid = resolveUid();
        if (uid == null) return;

        updateEntrantOutcome(uid, eventId, "SELECTED", "DECLINED", () -> {
            Toast.makeText(this, "Invitation Declined.", Toast.LENGTH_SHORT).show();
            showInvitationStatus("Invitation Declined");
            triggerAutomaticRedraw(eventId);
        });
    }

    private void updateEntrantOutcome(String userId, String targetEventId, String oldStatus, String newStatus, Runnable onSuccess) {
        java.util.Map<String, Object> oldRecord = new java.util.HashMap<>();
        oldRecord.put("eventId", targetEventId);
        oldRecord.put("outcome", oldStatus);

        java.util.Map<String, Object> newRecord = new java.util.HashMap<>();
        newRecord.put("eventId", targetEventId);
        newRecord.put("outcome", newStatus);
        newRecord.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .update("registrationHistory", FieldValue.arrayRemove(oldRecord))
                .addOnSuccessListener(a -> {
                    db.collection("users").document(userId)
                            .update("registrationHistory", FieldValue.arrayUnion(newRecord))
                            .addOnSuccessListener(a2 -> onSuccess.run());
                });
    }

    private void triggerAutomaticRedraw(String targetEventId) {
        db.collection("events").document(targetEventId).get().addOnSuccessListener(eventDoc -> {
            java.util.List<String> waitingListIds = (java.util.List<String>) eventDoc.get("waitingListIds");
            if (waitingListIds == null || waitingListIds.isEmpty()) return;

            java.util.List<com.google.firebase.firestore.DocumentSnapshot> waitingUsers = new java.util.ArrayList<>();
            java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(waitingListIds.size());

            for (String userId : waitingListIds) {
                db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        java.util.List<Map<String, Object>> history = (java.util.List<Map<String, Object>>) userDoc.get("registrationHistory");
                        String outcome = null;
                        if (history != null) {
                            for (Map<String, Object> rec : history) {
                                if (targetEventId.equals(rec.get("eventId"))) {
                                    outcome = (String) rec.get("outcome");
                                    break;
                                }
                            }
                        }

                        if ("WAITING".equals(outcome)) {
                            synchronized (waitingUsers) { waitingUsers.add(userDoc); }
                        }
                    }

                    if (remaining.decrementAndGet() == 0) {
                        if (waitingUsers.isEmpty()) return;

                        java.util.Random random = new java.util.Random();
                        com.google.firebase.firestore.DocumentSnapshot chosenUser = waitingUsers.get(random.nextInt(waitingUsers.size()));

                        updateEntrantOutcome(chosenUser.getId(), targetEventId, "WAITING", "SELECTED", () -> {
                            Log.d("AutoRedraw", "A new entrant has been chosen: " + chosenUser.getString("name"));
                            // Send LOTTERY_WIN_REDRAW notification to the replacement winner
                            sendRedrawWinNotification(chosenUser.getId(), targetEventId);
                        });
                    }
                });
            }
        });
    }

    private void joinWaitingList() {
        if (currentEvent == null || eventId == null) return;

        String deviceId = resolveUid();
        if (deviceId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnJoin.setEnabled(false);

        entrantDB.getEntrant(deviceId, entrant -> {
            if (entrant != null && entrant.isOnWaitingList(eventId)) {
                Toast.makeText(this, "Already joined the waiting list.", Toast.LENGTH_SHORT).show();
                isOnWaitingList = true;
                setupJoinButton();
                return;
            }

            entrantDB.addToWaitingList(deviceId, eventId, aVoid -> {
                eventDB.addToWaitingList(eventId, deviceId, aVoid2 -> {
                    Entrant.RegistrationRecord record = new Entrant.RegistrationRecord(
                            eventId,
                            Entrant.RegistrationRecord.Outcome.WAITING
                    );
                    entrantDB.addRegistrationRecord(deviceId, record, aVoid3 -> {
                        if (!isFinishing()) {
                            Toast.makeText(this, "Successfully joined waiting list!", Toast.LENGTH_SHORT).show();
                            isOnWaitingList = true;
                            setupJoinButton();
                        }
                    }, e -> {
                        if (!isFinishing()) {
                            Toast.makeText(this, "Joined but history not saved", Toast.LENGTH_SHORT).show();
                            isOnWaitingList = true;
                            setupJoinButton();
                        }
                    });
                }, e -> {
                    if (!isFinishing()) {
                        Toast.makeText(this, "Error updating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnJoin.setEnabled(true);
                    }
                });
            }, e -> {
                if (!isFinishing()) {
                    Toast.makeText(this, "Error joining waiting list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnJoin.setEnabled(true);
                }
            });
        }, e -> {
            if (!isFinishing()) {
                Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnJoin.setEnabled(true);
            }
        });
    }

    private void leaveWaitingList() {
        if (currentEvent == null || eventId == null) return;

        String deviceId = resolveUid();
        if (deviceId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnJoin.setEnabled(false);

        entrantDB.removeFromWaitingList(deviceId, eventId, aVoid -> {
            eventDB.removeFromWaitingList(eventId, deviceId, aVoid2 -> {
                if (!isFinishing()) {
                    Toast.makeText(this, "Left waiting list successfully", Toast.LENGTH_SHORT).show();
                    isOnWaitingList = false;
                    setupJoinButton();
                }
            }, e -> {
                if (!isFinishing()) {
                    Log.e(TAG, "Error removing from event", e);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnJoin.setEnabled(true);
                }
            });
        }, e -> {
            if (!isFinishing()) {
                Log.e(TAG, "Error removing from entrant", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnJoin.setEnabled(true);
            }
        });
    }

    /**
     * Sends a LOTTERY_WIN_REDRAW notification to a replacement entrant who was
     * auto-selected after another entrant declined.
     */
    private void sendRedrawWinNotification(String userId, String targetEventId) {
        db.collection("events").document(targetEventId).get().addOnSuccessListener(eventDoc -> {
            String eventName = eventDoc.getString("name");
            String message = "Congratulations! You have been selected as a replacement for "
                    + (eventName != null ? eventName : "an event")
                    + ". Open the app to accept or decline your invitation.";

            // Always create the Firestore notification record
            com.example.cmput301_app.database.NotificationDB notifDB = new com.example.cmput301_app.database.NotificationDB();
            com.example.cmput301_app.model.Notification n = new com.example.cmput301_app.model.Notification(
                    "", targetEventId, auth.getUid(),
                    message,
                    com.example.cmput301_app.model.Notification.NotificationType.LOTTERY_WIN_REDRAW,
                    com.google.firebase.Timestamp.now()
            );
            n.addRecipient(userId);

            notifDB.createNotification(n, savedNotif -> {
                // Check user's notification preference before local push
                db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                    Boolean notificationsEnabled = userDoc.getBoolean("notificationsEnabled");
                    if (notificationsEnabled != null && !notificationsEnabled) {
                        return; // Opted out of push
                    }

                    // Send local push notification
                    android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        android.app.NotificationChannel channel = new android.app.NotificationChannel(
                                "lottery_results", "Lottery Results", android.app.NotificationManager.IMPORTANCE_HIGH);
                        notificationManager.createNotificationChannel(channel);
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                                android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }

                    android.content.Intent intent = new android.content.Intent(this, EventDetailsActivity.class);
                    intent.putExtra("eventId", targetEventId);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                            this, 0, intent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

                    androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, "lottery_results")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("Lottery Results: " + (eventName != null ? eventName : "Event"))
                            .setContentText(message)
                            .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                });
            }, e -> Log.e(TAG, "Failed to create redraw notification", e));
        });
    }
}
