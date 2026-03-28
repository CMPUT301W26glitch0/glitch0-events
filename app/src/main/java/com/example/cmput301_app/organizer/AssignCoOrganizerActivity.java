package com.example.cmput301_app.organizer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.database.NotificationDB;
import com.example.cmput301_app.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets the organizer search all users and directly assign one as co-organizer.
 * - Excludes the organizer themselves and any already-assigned co-organizers.
 * - On assignment: adds to coOrganizerIds, removes from waitingListIds if present,
 *   and sends a CO_ORGANIZER_INVITATION notification.
 */
public class AssignCoOrganizerActivity extends AppCompatActivity {

    private String eventId;
    private String eventName;
    private FirebaseFirestore db;
    private EventDB eventDB;
    private NotificationDB notificationDB;

    private RecyclerView rvUsers;
    private InviteUserAdapter adapter;
    private TextView tvEmpty;
    private EditText etSearch;

    /** All eligible users (not yet co-organizers, not the organizer). */
    private final List<InviteEntrantToWaitingListActivity.UserRow> allUsers = new ArrayList<>();
    /** Filtered list shown in the RecyclerView. */
    private final List<InviteEntrantToWaitingListActivity.UserRow> displayList = new ArrayList<>();

    private List<String> currentCoOrganizerIds = new ArrayList<>();
    private List<String> currentWaitingListIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_co_organizer);

        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        db = FirebaseFirestore.getInstance();
        eventDB = new EventDB();
        notificationDB = new NotificationDB();

        TextView tvTitle = findViewById(R.id.tv_assign_event_name);
        if (eventName != null) tvTitle.setText("Assign Co-Organizer — " + eventName);

        findViewById(R.id.btn_assign_back).setOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tv_assign_empty);
        rvUsers = findViewById(R.id.rv_assign_users);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InviteUserAdapter(displayList, user -> confirmAssign(user), "Assign");
        rvUsers.setAdapter(adapter);

        etSearch = findViewById(R.id.et_assign_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterUsers(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        if (eventId != null) loadData();
    }

    private void loadData() {
        String orgId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("last_uid", null);

        // Load event to get coOrganizerIds and waitingListIds
        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            List<?> coOrgs = (List<?>) eventDoc.get("coOrganizerIds");
            currentCoOrganizerIds = new ArrayList<>();
            if (coOrgs != null) for (Object id : coOrgs) currentCoOrganizerIds.add(id.toString());

            List<?> waiting = (List<?>) eventDoc.get("waitingListIds");
            currentWaitingListIds = new ArrayList<>();
            if (waiting != null) for (Object id : waiting) currentWaitingListIds.add(id.toString());

            // Now load all users
            db.collection("users").get().addOnSuccessListener(snap -> {
                allUsers.clear();
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                    String uid = doc.getId();
                    // Exclude organizer and existing co-organizers
                    if (uid.equals(orgId)) continue;
                    if (currentCoOrganizerIds.contains(uid)) continue;

                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String phone = doc.getString("phoneNumber");
                    allUsers.add(new InviteEntrantToWaitingListActivity.UserRow(
                            uid,
                            name != null ? name : "Unknown",
                            email != null ? email : "",
                            phone));
                }
                filterUsers(etSearch.getText().toString());
            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show());
    }

    private void filterUsers(String query) {
        displayList.clear();
        String q = query.trim().toLowerCase();
        for (InviteEntrantToWaitingListActivity.UserRow user : allUsers) {
            if (q.isEmpty()
                    || user.name.toLowerCase().contains(q)
                    || user.email.toLowerCase().contains(q)) {
                displayList.add(user);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
        rvUsers.setVisibility(displayList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void confirmAssign(InviteEntrantToWaitingListActivity.UserRow user) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Assign Co-Organizer")
                .setMessage("Assign " + user.name + " as co-organizer for this event?\n\n"
                        + "They will be removed from the waiting list if already joined.")
                .setPositiveButton("Assign", (dialog, which) -> doAssign(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doAssign(InviteEntrantToWaitingListActivity.UserRow user) {
        boolean wasOnWaitingList = currentWaitingListIds.contains(user.deviceId);
        String orgId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("last_uid", null);

        eventDB.assignCoOrganizer(eventId, user.deviceId, wasOnWaitingList, aVoid -> {
            sendCoOrganizerNotification(user.deviceId, user.name, orgId);
            // Remove from local lists so they don't reappear
            currentCoOrganizerIds.add(user.deviceId);
            allUsers.removeIf(u -> u.deviceId.equals(user.deviceId));
            filterUsers(etSearch.getText().toString());
            Toast.makeText(this, user.name + " assigned as co-organizer", Toast.LENGTH_SHORT).show();
        }, e -> Toast.makeText(this, "Failed to assign co-organizer", Toast.LENGTH_SHORT).show());
    }

    private void sendCoOrganizerNotification(String recipientId, String recipientName, String orgId) {
        String resolvedOrgId = orgId != null ? orgId : "";
        db.collection("users").document(resolvedOrgId).get().addOnSuccessListener(orgDoc -> {
            String orgName = orgDoc.getString("name");
            if (orgName == null) orgName = "An organizer";
            String displayEvent = eventName != null ? eventName : "an event";
            String message = "You've been assigned as a co-organizer for \"" + displayEvent
                    + "\" by " + orgName + ".";

            Notification notif = new Notification(
                    null, eventId, resolvedOrgId, message,
                    Notification.NotificationType.CO_ORGANIZER_INVITATION,
                    com.google.firebase.Timestamp.now());
            notif.setInviterName(orgName);
            notif.addRecipient(recipientId);
            notificationDB.createNotification(notif, v -> {}, e -> {});
        }).addOnFailureListener(e -> {
            // Notification failure is non-critical; assignment already succeeded
        });
    }
}
