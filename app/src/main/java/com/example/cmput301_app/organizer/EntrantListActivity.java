/*
 * Purpose: Displays the unified waitlist and accepted entrants for a specific event.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.example.cmput301_app.model.Entrant;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import java.util.concurrent.atomic.AtomicInteger;

public class EntrantListActivity extends AppCompatActivity {

    private static final String TAG = "EntrantListActivity";

    private String eventId;
    private FirebaseFirestore db;

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

        findViewById(R.id.btn_entrants_back).setOnClickListener(v -> finish());

        btnRunLottery = findViewById(R.id.btn_run_lottery);
        btnRunLottery.setOnClickListener(v -> {
            Intent intent = new Intent(this, LotteryDrawActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

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
        adapter = new EntrantAdapter(displayList);
        rvEntrants.setAdapter(adapter);
    }

    /** Loads event name and location into the toolbar header. */
    private void loadEventHeader() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && tvEventNameHeader != null) {
                        String name = doc.getString("name");
                        String location = doc.getString("location");
                        tvEventNameHeader.setText(name != null ? name : "Event");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load event header", e));
    }

    private void setupTabs() {
        // Tab layout removed. We now setup the Search Bar TextWatcher instead.
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateDisplay();
                }
                @Override public void afterTextChanged(Editable s) {}
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
                                        String id     = userDoc.getId();

                                        String outcome = getOutcomeForEvent(userDoc, eventId);
                                        Entrant entrant = new Entrant(id, name != null ? name : "Unknown", email != null ? email : "", null);

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

    /** Updates the RecyclerView content based on master list and search query. */
    private void updateDisplay() {
        String query = etSearch != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        
        List<Entrant> filteredList = new ArrayList<>();
        for (Entrant e : masterList) {
            if (query.isEmpty() || (e.getName() != null && e.getName().toLowerCase().contains(query))) {
                filteredList.add(e);
            }
        }

        // Count badge reflecting Total Entrants (not filtered)
        int totalCount = masterList.size();
        if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(totalCount));

        // Swap adapter data
        displayList.clear();
        displayList.addAll(filteredList);
        adapter.notifyDataSetChanged();

        // Enable/disable lottery button based on whether anyone has "WAITING" status
        if (btnRunLottery != null) {
            btnRunLottery.setEnabled(hasWaitingEntrants);
            btnRunLottery.setAlpha(hasWaitingEntrants ? 1.0f : 0.4f);
        }

        // Show/hide empty state based on search results vs total list
        if (totalCount == 0) {
            rvEntrants.setVisibility(View.GONE);
            showEmptyState("No entrants have joined the waiting list.");
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
}