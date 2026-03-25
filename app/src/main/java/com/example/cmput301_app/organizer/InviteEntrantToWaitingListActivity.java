/*
 * Purpose: Allows an organizer to search all registered users and invite
 *          specific entrants to join the waiting list for a private event.
 *          On invite, a WAITING_LIST_INVITATION in-app notification is created
 *          in Firestore so the entrant sees it the next time they open the app.
 *          Organizers can also view pending invitations and cancel them before
 *          the entrant accepts.
 *
 *          Note: Push delivery when the app is in the background requires
 *          Firebase Cloud Messaging (FCM), which is not yet integrated in this
 *          project. The Firestore notification persists and will appear in the
 *          entrant's notification bell on next app launch.
 *
 * Design Pattern: Standard Android structure
 * Outstanding Issues: FCM push delivery not yet implemented.
 */
package com.example.cmput301_app.organizer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.database.NotificationDB;
import com.example.cmput301_app.model.Notification;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InviteEntrantToWaitingListActivity extends AppCompatActivity {

    /** Simple data holder for a user row in the search list. */
    public static class UserRow {
        public final String deviceId;
        public final String name;
        public final String email;
        public final String phone;

        UserRow(String deviceId, String name, String email, String phone) {
            this.deviceId = deviceId;
            this.name = name != null ? name : "Unknown";
            this.email = email != null ? email : "";
            this.phone = phone != null ? phone : "";
        }
    }

    /** Data holder for an already-invited entrant row. */
    public static class InvitedRow {
        public final String deviceId;
        public final String name;
        public final String email;
        public final String phone;
        public final boolean isPending; // true = PENDING_INVITE, can be cancelled

        InvitedRow(String deviceId, String name, String email, String phone, boolean isPending) {
            this.deviceId = deviceId;
            this.name = name != null ? name : "Unknown";
            this.email = email != null ? email : "";
            this.phone = phone != null ? phone : "";
            this.isPending = isPending;
        }
    }

    private String eventId;
    private String eventName;
    private String organizerName;

    private FirebaseFirestore db;
    private EventDB eventDB;
    private NotificationDB notificationDB;

    // Search mode
    private EditText etSearch;
    private RecyclerView rvUsers;
    private TextView tvEmpty;
    private final List<UserRow> allUsers = new ArrayList<>();
    private final List<UserRow> filteredUsers = new ArrayList<>();
    private InviteUserAdapter searchAdapter;

    // Invited mode
    private final List<InvitedRow> invitedList = new ArrayList<>();
    private InvitedEntrantAdapter invitedAdapter;

    // Tracks device IDs currently pending so they are excluded from search results
    private final Set<String> pendingInviteIds = new HashSet<>();

    // UI mode: true = search, false = invited
    private boolean inSearchMode = true;
    private Button btnModeSearch;
    private Button btnModeInvited;
    private View tilSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_invite_entrant);

        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");
        organizerName = getIntent().getStringExtra("organizerName");
        if (organizerName == null) organizerName = "The organizer";

        db = FirebaseFirestore.getInstance();
        eventDB = new EventDB();
        notificationDB = new NotificationDB();

        TextView tvTitle = findViewById(R.id.tv_invite_event_name);
        if (tvTitle != null && eventName != null) tvTitle.setText(eventName);

        etSearch = findViewById(R.id.et_invite_search);
        rvUsers = findViewById(R.id.rv_invite_users);
        tvEmpty = findViewById(R.id.tv_invite_empty);
        tilSearch = findViewById(R.id.til_invite_search);
        btnModeSearch = findViewById(R.id.btn_mode_search);
        btnModeInvited = findViewById(R.id.btn_mode_invited);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        searchAdapter = new InviteUserAdapter(filteredUsers, this::onInviteClicked);
        invitedAdapter = new InvitedEntrantAdapter(invitedList, this::onCancelInviteClicked);

        rvUsers.setAdapter(searchAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnModeSearch.setOnClickListener(v -> switchMode(true));
        btnModeInvited.setOnClickListener(v -> switchMode(false));

        findViewById(R.id.btn_invite_back).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadAllData();
    }

    private void switchMode(boolean searchMode) {
        inSearchMode = searchMode;

        if (searchMode) {
            btnModeSearch.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(this, R.color.primary_blue));
            btnModeSearch.setTextColor(0xFFFFFFFF);
            btnModeInvited.setBackgroundTintList(null);
            btnModeInvited.setTextColor(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primary_blue));
            tilSearch.setVisibility(View.VISIBLE);
            rvUsers.setAdapter(searchAdapter);
            filterUsers(etSearch.getText() != null ? etSearch.getText().toString() : "");
        } else {
            btnModeInvited.setBackgroundTintList(
                    androidx.core.content.ContextCompat.getColorStateList(this, R.color.primary_blue));
            btnModeInvited.setTextColor(0xFFFFFFFF);
            btnModeSearch.setBackgroundTintList(null);
            btnModeSearch.setTextColor(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primary_blue));
            tilSearch.setVisibility(View.GONE);
            rvUsers.setAdapter(invitedAdapter);
            updateEmptyState(invitedList.isEmpty(), "No pending invitations");
        }
    }

    private void loadAllData() {
        String currentOrgId = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("last_uid", null);

        // Load all users and the event's invitedUserIds in parallel
        db.collection("users").get().addOnSuccessListener(snapshots -> {
            allUsers.clear();
            List<QueryDocumentSnapshot> docs = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                if (doc.getId().equals(currentOrgId)) continue;
                docs.add(doc);
            }

            // Build invited list by checking registrationHistory for each user
            pendingInviteIds.clear();
            invitedList.clear();

            // Load the event to get invitedUserIds
            db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
                List<String> invitedIds = (List<String>) eventDoc.get("invitedUserIds");
                if (invitedIds == null) invitedIds = new ArrayList<>();
                final Set<String> invitedSet = new HashSet<>(invitedIds);

                for (QueryDocumentSnapshot doc : docs) {
                    String id = doc.getId();
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String phone = doc.getString("phoneNumber");

                    if (invitedSet.contains(id)) {
                        // Determine if this user's outcome for this event is PENDING_INVITE
                        String outcome = getOutcomeForEvent(doc, eventId);
                        boolean isPending = "PENDING_INVITE".equals(outcome);
                        invitedList.add(new InvitedRow(id, name, email, phone, isPending));
                        if (isPending) pendingInviteIds.add(id);
                    } else {
                        allUsers.add(new UserRow(id, name, email, phone));
                    }
                }

                runOnUiThread(() -> {
                    invitedAdapter.notifyDataSetChanged();
                    updateInvitedBadge();
                    filterUsers(etSearch.getText() != null ? etSearch.getText().toString() : "");
                });
            }).addOnFailureListener(e ->
                    runOnUiThread(() -> {
                        // Fallback: add everyone to allUsers
                        for (QueryDocumentSnapshot doc : docs) {
                            allUsers.add(new UserRow(doc.getId(), doc.getString("name"),
                                    doc.getString("email"), doc.getString("phoneNumber")));
                        }
                        filterUsers("");
                    }));
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show());
    }

    private String getOutcomeForEvent(QueryDocumentSnapshot doc, String eventId) {
        Object rawHistory = doc.get("registrationHistory");
        if (rawHistory instanceof List) {
            for (Object item : (List<?>) rawHistory) {
                if (item instanceof java.util.Map) {
                    java.util.Map<?, ?> record = (java.util.Map<?, ?>) item;
                    if (eventId.equals(record.get("eventId"))) {
                        Object outcome = record.get("outcome");
                        if (outcome != null) return outcome.toString();
                    }
                }
            }
        }
        return null;
    }

    private void filterUsers(String query) {
        filteredUsers.clear();
        String lower = query.trim().toLowerCase();
        for (UserRow row : allUsers) {
            // Exclude users already pending
            if (pendingInviteIds.contains(row.deviceId)) continue;
            if (lower.isEmpty()
                    || row.name.toLowerCase().contains(lower)
                    || row.email.toLowerCase().contains(lower)
                    || row.phone.toLowerCase().contains(lower)) {
                filteredUsers.add(row);
            }
        }
        searchAdapter.notifyDataSetChanged();
        if (inSearchMode) updateEmptyState(filteredUsers.isEmpty(), "No users found");
    }

    private void updateEmptyState(boolean empty, String message) {
        if (tvEmpty != null) {
            tvEmpty.setText(message);
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    private void updateInvitedBadge() {
        long pendingCount = invitedList.stream().filter(r -> r.isPending).count();
        if (btnModeInvited != null) {
            btnModeInvited.setText("Invited (" + pendingCount + ")");
        }
    }

    // -------------------------------------------------------------------------
    // Invite flow
    // -------------------------------------------------------------------------

    private void onInviteClicked(UserRow user) {
        eventDB.addInvitedUser(eventId, user.deviceId,
                aVoid -> {
                    db.collection("users").document(user.deviceId).get()
                            .addOnSuccessListener(userDoc -> {
                                Boolean notificationsEnabled = userDoc.getBoolean("notificationsEnabled");
                                if (notificationsEnabled != null && !notificationsEnabled) {
                                    addToPendingLocally(user, false);
                                    runOnUiThread(() ->
                                            Toast.makeText(this,
                                                    user.name + " invited (notifications opted out)",
                                                    Toast.LENGTH_SHORT).show());
                                    return;
                                }
                                sendInvitationNotification(user);
                            })
                            .addOnFailureListener(e -> sendInvitationNotification(user));
                },
                e -> runOnUiThread(() ->
                        Toast.makeText(this, "Failed to invite " + user.name, Toast.LENGTH_SHORT).show()));
    }

    private void sendInvitationNotification(UserRow user) {
        db.collection("users").document(user.deviceId).get()
                .addOnSuccessListener(userDoc -> {
                    List<java.util.Map<String, Object>> history =
                            (List<java.util.Map<String, Object>>) userDoc.get("registrationHistory");
                    if (history == null) history = new ArrayList<>();

                    history.removeIf(rec -> eventId.equals(rec.get("eventId")));

                    java.util.Map<String, Object> newRecord = new java.util.HashMap<>();
                    newRecord.put("eventId", eventId);
                    newRecord.put("outcome", "PENDING_INVITE");
                    newRecord.put("timestamp", com.google.firebase.Timestamp.now());
                    history.add(newRecord);

                    final List<java.util.Map<String, Object>> finalHistory = history;
                    db.collection("users").document(user.deviceId)
                            .update("registrationHistory", finalHistory)
                            .addOnSuccessListener(aVoid -> createAndSendNotification(user))
                            .addOnFailureListener(e -> createAndSendNotification(user));
                })
                .addOnFailureListener(e -> createAndSendNotification(user));
    }

    private void createAndSendNotification(UserRow user) {
        String message = "You've been invited to join the waiting list for \""
                + eventName + "\" by " + organizerName + ".";

        Notification notif = new Notification(
                null,
                eventId,
                getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("last_uid", null),
                message,
                Notification.NotificationType.WAITING_LIST_INVITATION,
                com.google.firebase.Timestamp.now()
        );
        notif.addRecipient(user.deviceId);
        notif.setInviterName(organizerName);
        notif.setEventName(eventName);

        notificationDB.createNotification(notif,
                saved -> {
                    addToPendingLocally(user, true);
                    runOnUiThread(() ->
                            Toast.makeText(this, user.name + " invited!", Toast.LENGTH_SHORT).show());
                },
                e -> {
                    addToPendingLocally(user, true);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Invited but notification failed for " + user.name,
                                    Toast.LENGTH_SHORT).show());
                });
    }

    /** Moves a user from the search list to the invited list locally. */
    private void addToPendingLocally(UserRow user, boolean notificationSent) {
        pendingInviteIds.add(user.deviceId);
        allUsers.removeIf(u -> u.deviceId.equals(user.deviceId));
        invitedList.add(new InvitedRow(user.deviceId, user.name, user.email, user.phone, true));
        runOnUiThread(() -> {
            invitedAdapter.notifyDataSetChanged();
            updateInvitedBadge();
            filterUsers(etSearch.getText() != null ? etSearch.getText().toString() : "");
        });
    }

    // -------------------------------------------------------------------------
    // Cancel invitation flow
    // -------------------------------------------------------------------------

    private void onCancelInviteClicked(InvitedRow row) {
        if (!row.isPending) return;

        // 1. Remove from event's invitedUserIds
        db.collection("events").document(eventId)
                .update("invitedUserIds", FieldValue.arrayRemove(row.deviceId))
                .addOnSuccessListener(aVoid -> removePendingInviteRecord(row))
                .addOnFailureListener(e -> runOnUiThread(() ->
                        Toast.makeText(this, "Failed to cancel invitation", Toast.LENGTH_SHORT).show()));
    }

    private void removePendingInviteRecord(InvitedRow row) {
        // 2. Remove PENDING_INVITE record from user's registrationHistory
        db.collection("users").document(row.deviceId).get()
                .addOnSuccessListener(userDoc -> {
                    List<java.util.Map<String, Object>> history =
                            (List<java.util.Map<String, Object>>) userDoc.get("registrationHistory");
                    if (history != null) {
                        history.removeIf(rec -> eventId.equals(rec.get("eventId"))
                                && "PENDING_INVITE".equals(rec.get("outcome")));
                        db.collection("users").document(row.deviceId)
                                .update("registrationHistory", history);
                    }
                    removePendingLocally(row);
                })
                .addOnFailureListener(e -> removePendingLocally(row));
    }

    /** Moves a cancelled user back to the search list. */
    private void removePendingLocally(InvitedRow row) {
        pendingInviteIds.remove(row.deviceId);
        invitedList.removeIf(r -> r.deviceId.equals(row.deviceId));
        allUsers.add(new UserRow(row.deviceId, row.name, row.email, row.phone));
        runOnUiThread(() -> {
            invitedAdapter.notifyDataSetChanged();
            updateInvitedBadge();
            filterUsers(etSearch.getText() != null ? etSearch.getText().toString() : "");
            updateEmptyState(invitedList.isEmpty(), "No pending invitations");
            Toast.makeText(this, "Invitation cancelled for " + row.name, Toast.LENGTH_SHORT).show();
        });
    }
}
