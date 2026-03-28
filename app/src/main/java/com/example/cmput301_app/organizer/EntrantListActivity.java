/**
 * Displays a unified, searchable list of all entrants on a specific event's waiting list.
 *
 * The activity receives an {@code eventId} via intent extra and attaches a real-time
 * Firestore snapshot listener to the event document. For each user ID in
 * {@code waitingListIds}, it fetches the user document, reads the
 * {@code registrationHistory} array to determine the entrant's current outcome
 * for this event, and maps that outcome to a display status string used by
 * EntrantAdapter.
 *
 * A search bar allows the organizer to filter the list by name. The total count
 * badge always reflects the unfiltered master list size.
 *
 * The "Run Lottery" button is enabled only when at least one entrant has
 * WAITING status, and navigates to LotteryDrawActivity.
 *
 * Outstanding issues:
 * - The list is rebuilt from scratch on every snapshot update. For large waiting
 *   lists this may be expensive; a DiffUtil-based approach would be more efficient.
 */
package com.example.cmput301_app.organizer;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;

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
import com.example.cmput301_app.model.Entrant;
import com.example.cmput301_app.model.Notification;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import java.util.concurrent.atomic.AtomicInteger;

public class EntrantListActivity extends AppCompatActivity {

    private static final String TAG = "EntrantListActivity";

    private String eventId;
    private String eventName;
    private FirebaseFirestore db;
    private EventDB eventDB;
    private NotificationDB notificationDB;

    private RecyclerView rvEntrants;
    private EntrantAdapter adapter;
    private List<Entrant> displayList;

    private TextView tvTotalCount;
    private TextView tvEmptyState;
    private TextView tvRecentLabel;
    private TextView tvEventNameHeader;
    private TextView tvEventSubtitle;
    private EditText etSearch;
    private android.widget.Button btnRunLottery;
    private android.widget.Button btnViewMap;
    private android.widget.Button btnExportCsv;
    private com.google.android.material.tabs.TabLayout tabLayout;

    /** 0 = All Entrants, 1 = Cancelled, 2 = Enrolled */
    private int selectedTab = 0;

    /** Unified master list of all entrants. */
    private final List<Entrant> masterList = new ArrayList<>();

    /** Indicates if there's at least one entrant with WAITING status */
    private boolean hasWaitingEntrants = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_list);

        eventId = getIntent().getStringExtra("eventId");
        db = FirebaseFirestore.getInstance();
        eventDB = new EventDB();
        notificationDB = new NotificationDB();

        initViews();
        setupRecyclerView();
        setupTabs();

        if (eventId != null) {
            loadEventHeader();
            loadEntrants();
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        tvTotalCount      = findViewById(R.id.tv_total_count);
        tvEmptyState      = findViewById(R.id.tv_empty_state);
        tvRecentLabel     = findViewById(R.id.tv_recent_label);
        tvEventNameHeader = findViewById(R.id.tv_event_name_header);
        etSearch          = findViewById(R.id.et_search_entrants);
        tabLayout         = findViewById(R.id.tab_layout_entrants);

        findViewById(R.id.btn_entrants_back).setOnClickListener(v -> finish());

        btnRunLottery = findViewById(R.id.btn_run_lottery);
        btnRunLottery.setOnClickListener(v -> {
            Intent intent = new Intent(this, LotteryDrawActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        btnViewMap = findViewById(R.id.btn_view_map);
        btnViewMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, WaitingListMapActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        btnExportCsv = findViewById(R.id.btn_export_csv);
        btnExportCsv.setOnClickListener(v -> exportCsv());

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupRecyclerView() {
        rvEntrants = findViewById(R.id.rv_entrants);
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        displayList = new ArrayList<>();
        adapter = new EntrantAdapter(displayList, entrant -> showInviteDialog(entrant), entrant -> showCancelConfirmDialog(entrant));
        rvEntrants.setAdapter(adapter);
    }

    private void showInviteDialog(Entrant entrant) {
        String name = entrant.getName() != null ? entrant.getName() : "this entrant";
        new android.app.AlertDialog.Builder(this)
                .setTitle("Invite as Co-Organizer")
                .setMessage("Invite " + name + " to be a co-organizer for this event?")
                .setPositiveButton("Invite", (dialog, which) -> sendCoOrganizerInvite(entrant))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendCoOrganizerInvite(Entrant entrant) {
        String organizerDeviceId = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("last_uid", null);
        if (organizerDeviceId == null) {
            Toast.makeText(this, "Could not identify organizer", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(organizerDeviceId).get()
                .addOnSuccessListener(organizerDoc -> {
                    String organizerName = organizerDoc.getString("name");
                    if (organizerName == null) organizerName = "An organizer";

                    final String finalOrganizerName = organizerName;
                    String displayEventName = eventName != null ? eventName : "an event";
                    String message = "You've been invited by " + finalOrganizerName
                            + " to co-organize \"" + displayEventName + "\"";

                    Notification notif = new Notification(
                            null,
                            eventId,
                            organizerDeviceId,
                            message,
                            Notification.NotificationType.CO_ORGANIZER_INVITATION,
                            com.google.firebase.Timestamp.now()
                    );
                    notif.setInviterName(finalOrganizerName);
                    notif.addRecipient(entrant.getDeviceId());

                    eventDB.addPendingCoOrganizerInvite(eventId, entrant.getDeviceId(),
                            aVoid -> notificationDB.createNotification(notif,
                                    savedNotif -> runOnUiThread(() -> Toast.makeText(this,
                                            "Invitation sent to " + entrant.getName(),
                                            Toast.LENGTH_SHORT).show()),
                                    e -> runOnUiThread(() -> Toast.makeText(this,
                                            "Failed to send notification", Toast.LENGTH_SHORT).show())),
                            e -> runOnUiThread(() -> Toast.makeText(this,
                                    "Failed to update event", Toast.LENGTH_SHORT).show()));
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Could not load organizer info", Toast.LENGTH_SHORT).show());
    }

    /** Loads event name and geolocation flag into the toolbar header. */
    private void loadEventHeader() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String name = doc.getString("name");
                    eventName = name;
                    if (tvEventNameHeader != null) {
                        tvEventNameHeader.setText(name != null ? name : "Event");
                    }
                    Boolean geolocationEnabled = doc.getBoolean("geolocationEnabled");
                    if (Boolean.TRUE.equals(geolocationEnabled) && btnViewMap != null) {
                        btnViewMap.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load event header", e));
    }

    private void setupTabs() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateDisplay();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (tabLayout != null) {
            tabLayout.addTab(tabLayout.newTab().setText("All Entrants"));
            tabLayout.addTab(tabLayout.newTab().setText("Cancelled"));
            tabLayout.addTab(tabLayout.newTab().setText("Enrolled"));
            tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    selectedTab = tab.getPosition();
                    updateDisplay();
                }
                @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            });
        }
    }

    /**
     * Fetches the event document, reads waitingListIds, then fetches each user
     * document and buckets them by their registrationHistory outcome for this event.
     * Uses a real-time snapshot listener on the event for live updates.
     */
    private void loadEntrants() {
        db.collection("events").document(eventId)
                .addSnapshotListener((eventDoc, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Event listen failed", e);
                        return;
                    }
                    if (eventDoc == null || !eventDoc.exists()) return;

                    List<String> waitingListIds = (List<String>) eventDoc.get("waitingListIds");
                    if (waitingListIds == null || waitingListIds.isEmpty()) {
                        clearAll();
                        updateDisplay();
                        return;
                    }

                    // Fetch all user docs by ID and classify them
                    clearAll();
                    hasWaitingEntrants = false;
                    AtomicInteger remaining = new AtomicInteger(waitingListIds.size());

                    for (String userId : waitingListIds) {
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String name   = userDoc.getString("name");
                                        String email  = userDoc.getString("email");
                                        String phone  = userDoc.getString("phoneNumber");
                                        String id     = userDoc.getId();

                                        String outcome = getOutcomeForEvent(userDoc, eventId);
                                        Entrant entrant = new Entrant(id, name != null ? name : "Unknown", email != null ? email : "", null);
                                        entrant.setPhoneNumber(phone != null ? phone : "");

                                        // Map the outcome to a specific real UI status string
                                        switch (outcome) {
                                            case "ACCEPTED":
                                                entrant.setStatus("ACCEPTED");
                                                entrant.setGeolocation(getTimestampForEvent(userDoc, eventId));
                                                break;
                                            case "SELECTED":
                                                entrant.setStatus("AWAITING RESPONSE");
                                                entrant.setGeolocation("Invited recently"); // Or fetch timestamp if available
                                                break;
                                            case "NOT_SELECTED":
                                                entrant.setStatus("NOT SELECTED");
                                                entrant.setGeolocation("Not selected in draw");
                                                break;
                                            case "DECLINED":
                                                entrant.setStatus("DECLINED");
                                                entrant.setGeolocation("Declined invitation");
                                                break;
                                            case "CANCELLED":
                                                entrant.setStatus("CANCELLED");
                                                entrant.setGeolocation("Cancelled by organizer");
                                                break;
                                            default: // WAITING
                                                entrant.setStatus("WAITING");
                                                entrant.setGeolocation("Joined Waiting List");
                                                hasWaitingEntrants = true;
                                                break;
                                        }

                                        synchronized (masterList) { masterList.add(entrant); }
                                    }
                                    if (remaining.decrementAndGet() == 0) {
                                        runOnUiThread(this::updateDisplay);
                                    }
                                })
                                .addOnFailureListener(err -> {
                                    Log.e(TAG, "Failed to load user " + userId, err);
                                    if (remaining.decrementAndGet() == 0) {
                                        runOnUiThread(this::updateDisplay);
                                    }
                                });
                    }
                });
    }

    /**
     * Reads the registrationHistory array on a user document and returns
     * the outcome string for the given eventId, or "WAITING" if not found.
     */
    private String getOutcomeForEvent(DocumentSnapshot userDoc, String eventId) {
        Object rawHistory = userDoc.get("registrationHistory");
        if (rawHistory instanceof List) {
            for (Object item : (List<?>) rawHistory) {
                if (item instanceof Map) {
                    Map<?, ?> record = (Map<?, ?>) item;
                    if (eventId.equals(record.get("eventId"))) {
                        Object outcome = record.get("outcome");
                        if (outcome != null) return outcome.toString();
                    }
                }
            }
        }
        return "WAITING";
    }

    /** Reads the timestamp for a specific event from the user's registrationHistory. */
    private String getTimestampForEvent(DocumentSnapshot userDoc, String eventId) {
        Object rawHistory = userDoc.get("registrationHistory");
        if (rawHistory instanceof List) {
            for (Object item : (List<?>) rawHistory) {
                if (item instanceof Map) {
                    Map<?, ?> record = (Map<?, ?>) item;
                    if (eventId.equals(record.get("eventId"))) {
                        Object ts = record.get("timestamp");
                        if (ts instanceof com.google.firebase.Timestamp) {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                    "MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
                            return "Accepted " + sdf.format(((com.google.firebase.Timestamp) ts).toDate());
                        }
                    }
                }
            }
        }
        return "Accepted";
    }

    private void clearAll() {
        masterList.clear();
    }

    /** Updates the RecyclerView content based on the active tab and search query. */
    private void updateDisplay() {
        String query = etSearch != null ? etSearch.getText().toString().toLowerCase().trim() : "";

        // Apply tab filter first
        List<Entrant> tabFiltered = new ArrayList<>();
        int enrolledCount = 0;
        for (Entrant e : masterList) {
            if ("ACCEPTED".equals(e.getStatus())) enrolledCount++;
            if (selectedTab == 1) {
                String s = e.getStatus();
                if ("DECLINED".equals(s) || "CANCELLED".equals(s)) {
                    tabFiltered.add(e);
                }
            } else if (selectedTab == 2) {
                if ("ACCEPTED".equals(e.getStatus())) {
                    tabFiltered.add(e);
                }
            } else {
                tabFiltered.add(e);
            }
        }

        // Show Export CSV button only on the Enrolled tab; enable only when there are enrolled entrants
        if (btnExportCsv != null) {
            if (selectedTab == 2) {
                btnExportCsv.setVisibility(View.VISIBLE);
                btnExportCsv.setEnabled(enrolledCount > 0);
                btnExportCsv.setAlpha(enrolledCount > 0 ? 1.0f : 0.4f);
            } else {
                btnExportCsv.setVisibility(View.GONE);
            }
        }

        // Then apply search query
        List<Entrant> filteredList = new ArrayList<>();
        for (Entrant e : tabFiltered) {
            if (query.isEmpty() || (e.getName() != null && e.getName().toLowerCase().contains(query))) {
                filteredList.add(e);
            }
        }

        // Count badge always reflects total (all tabs, no search filter)
        int totalCount = masterList.size();
        if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(totalCount));

        // Update section label to match active tab
        if (tvRecentLabel != null) {
            if (selectedTab == 2) tvRecentLabel.setText("ENROLLED ENTRANTS");
            else if (selectedTab == 1) tvRecentLabel.setText("CANCELLED / DECLINED");
            else tvRecentLabel.setText("ENTRANT LIST");
        }

        // Swap adapter data
        displayList.clear();
        displayList.addAll(filteredList);
        adapter.notifyDataSetChanged();

        // Enable/disable lottery button based on whether anyone has "WAITING" status
        if (btnRunLottery != null) {
            btnRunLottery.setEnabled(hasWaitingEntrants);
            btnRunLottery.setAlpha(hasWaitingEntrants ? 1.0f : 0.4f);
        }

        // Show/hide empty state
        if (tabFiltered.isEmpty()) {
            rvEntrants.setVisibility(View.GONE);
            if (selectedTab == 2) {
                showEmptyState("No entrants have enrolled yet. Export is unavailable.");
            } else if (selectedTab == 1) {
                showEmptyState("No entrants have been cancelled or declined.");
            } else {
                showEmptyState("No entrants have joined the waiting list.");
            }
        } else if (filteredList.isEmpty()) {
            rvEntrants.setVisibility(View.GONE);
            showEmptyState("No entrants match that name.");
        } else {
            rvEntrants.setVisibility(View.VISIBLE);
            hideEmptyState();
        }
    }

    private void showEmptyState(String message) {
        if (tvEmptyState != null) {
            tvEmptyState.setText(message);
            tvEmptyState.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmptyState() {
        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);
    }

    private void showCancelConfirmDialog(Entrant entrant) {
        String name = entrant.getName() != null ? entrant.getName() : "this entrant";
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancel Invitation")
                .setMessage("Cancel the invitation for " + name + "? They will be notified that their invitation has been revoked.")
                .setPositiveButton("Cancel Invitation", (dialog, which) -> cancelEntrantInvitation(entrant))
                .setNegativeButton("Keep", null)
                .show();
    }

    private void cancelEntrantInvitation(Entrant entrant) {
        String userId = entrant.getDeviceId();
        if (userId == null || eventId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    List<Map<String, Object>> history =
                            (List<Map<String, Object>>) doc.get("registrationHistory");
                    if (history == null) history = new ArrayList<>();

                    for (int i = 0; i < history.size(); i++) {
                        Map<String, Object> rec = history.get(i);
                        if (eventId.equals(rec.get("eventId")) && "SELECTED".equals(rec.get("outcome"))) {
                            Map<String, Object> updated = new HashMap<>(rec);
                            updated.put("outcome", "CANCELLED");
                            updated.put("timestamp", com.google.firebase.Timestamp.now());
                            history.set(i, updated);
                            break;
                        }
                    }

                    final List<Map<String, Object>> finalHistory = history;
                    db.collection("users").document(userId)
                            .update("registrationHistory", finalHistory)
                            .addOnSuccessListener(aVoid -> sendCancellationNotification(userId))
                            .addOnFailureListener(e -> runOnUiThread(() ->
                                    Toast.makeText(this, "Failed to cancel invitation", Toast.LENGTH_SHORT).show()));
                })
                .addOnFailureListener(e -> runOnUiThread(() ->
                        Toast.makeText(this, "Failed to load entrant data", Toast.LENGTH_SHORT).show()));
    }

    private void exportCsv() {
        List<Entrant> enrolled = new ArrayList<>();
        for (Entrant e : masterList) {
            if ("ACCEPTED".equals(e.getStatus())) enrolled.add(e);
        }

        if (enrolled.isEmpty()) {
            Toast.makeText(this, "No enrolled entrants to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Name,Email,Phone Number\n");
        for (Entrant e : enrolled) {
            csv.append(escapeCsvField(e.getName() != null ? e.getName() : "")).append(",")
               .append(escapeCsvField(e.getEmail() != null ? e.getEmail() : "")).append(",")
               .append(escapeCsvField(e.getPhoneNumber() != null ? e.getPhoneNumber() : "")).append("\n");
        }

        String fileName = "enrolled_" + eventId + "_" + System.currentTimeMillis() + ".csv";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri itemUri = getContentResolver().insert(collection, values);

        if (itemUri == null) {
            Toast.makeText(this, "Failed to create export file", Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream out = getContentResolver().openOutputStream(itemUri)) {
            out.write(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        getContentResolver().update(itemUri, values, null, null);
        Toast.makeText(this, "Exported " + enrolled.size() + " entrant(s) to Downloads/" + fileName, Toast.LENGTH_LONG).show();
    }

    private String escapeCsvField(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private void sendCancellationNotification(String userId) {
        String organizerDeviceId = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("last_uid", null);
        String displayEventName = eventName != null ? eventName : "an event";
        String message = "Your invitation to \"" + displayEventName
                + "\" has been cancelled by the organizer.";

        Notification notif = new Notification(
                null,
                eventId,
                organizerDeviceId,
                message,
                Notification.NotificationType.INVITATION_CANCELLED,
                com.google.firebase.Timestamp.now()
        );
        notif.addRecipient(userId);

        notificationDB.createNotification(notif,
                savedNotif -> runOnUiThread(() ->
                        Toast.makeText(this, "Invitation cancelled and entrant notified", Toast.LENGTH_SHORT).show()),
                e -> runOnUiThread(() ->
                        Toast.makeText(this, "Invitation cancelled (notification failed)", Toast.LENGTH_SHORT).show()));
    }
}