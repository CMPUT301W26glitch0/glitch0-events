/*
 * Purpose: Admin notification log — shows all notifications sent by organizers.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
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
import com.example.cmput301_app.database.AdminDB;
import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.model.Notification;
import com.example.cmput301_app.model.Profile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin screen that shows a reverse-chronological log of all notifications
 * sent in the system.
 *
 * Each entry shows the organizer who sent it, the message, the notification
 * type (target audience), the associated event, and the timestamp.
 *
 * A text search bar narrows results by organizer name or event name.
 * Two date pickers allow narrowing results to a specific date range.
 */
public class AdminNotificationLogActivity extends AppCompatActivity {

    private static final String TAG = "AdminNotifLog";
    private static final SimpleDateFormat DATE_LABEL_FMT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private AdminDB adminDB;
    private RecyclerView rvLog;
    private AdminNotificationLogAdapter adapter;

    private final List<Notification> allNotifications = new ArrayList<>();
    private final List<Notification> filtered = new ArrayList<>();
    private final Map<String, String> organizerNames = new HashMap<>();
    private final Map<String, String> eventNames = new HashMap<>();

    private TextView tvCount;
    private TextView tvEmptyState;
    private EditText etSearch;
    private TextView tvFromDate;
    private TextView tvToDate;

    /** Null means no lower/upper bound. */
    private Date filterFrom = null;
    private Date filterTo = null;

    private int pendingLoads = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_notification_log);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.admin_notif_log_main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });

        adminDB = new AdminDB();

        tvCount = findViewById(R.id.tv_notif_count);
        tvEmptyState = findViewById(R.id.tv_notif_empty);
        etSearch = findViewById(R.id.et_search_notif);
        tvFromDate = findViewById(R.id.tv_filter_from);
        tvToDate = findViewById(R.id.tv_filter_to);

        rvLog = findViewById(R.id.rv_notif_log);
        rvLog.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminNotificationLogAdapter(filtered, organizerNames, eventNames, this);
        rvLog.setAdapter(adapter);

        findViewById(R.id.btn_notif_back).setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        tvFromDate.setOnClickListener(v -> pickDate(true));
        tvToDate.setOnClickListener(v -> pickDate(false));

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pendingLoads = 3;
        allNotifications.clear();
        organizerNames.clear();
        eventNames.clear();
        loadData();
    }

    private void loadData() {
        adminDB.getNotificationLog(
                notifications -> {
                    allNotifications.clear();
                    allNotifications.addAll(notifications);
                    onLoadComplete();
                },
                e -> {
                    Log.e(TAG, "Failed to load notifications", e);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show());
                    onLoadComplete();
                }
        );

        adminDB.getAllProfiles(
                profiles -> {
                    organizerNames.clear();
                    for (Profile p : profiles) {
                        if (p.getDeviceId() != null && p.getName() != null) {
                            organizerNames.put(p.getDeviceId(), p.getName());
                        }
                    }
                    onLoadComplete();
                },
                e -> {
                    Log.e(TAG, "Failed to load profiles", e);
                    onLoadComplete();
                }
        );

        adminDB.getAllEvents(
                events -> {
                    eventNames.clear();
                    for (Event ev : events) {
                        if (ev.getEventId() != null && ev.getName() != null) {
                            eventNames.put(ev.getEventId(), ev.getName());
                        }
                    }
                    onLoadComplete();
                },
                e -> {
                    Log.e(TAG, "Failed to load events", e);
                    onLoadComplete();
                }
        );
    }

    private synchronized void onLoadComplete() {
        pendingLoads--;
        if (pendingLoads == 0) {
            runOnUiThread(this::applyFilter);
        }
    }

    private void applyFilter() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        filtered.clear();

        for (Notification n : allNotifications) {
            // Text filter: match organizer name or event name
            if (!query.isEmpty()) {
                String orgName = organizerNames.getOrDefault(n.getOrganizerId(), "").toLowerCase();
                String evName = eventNames.getOrDefault(n.getEventId(), "").toLowerCase();
                // Also check eventName field stored on the notification itself
                String storedEvName = n.getEventName() != null ? n.getEventName().toLowerCase() : "";
                if (!orgName.contains(query) && !evName.contains(query) && !storedEvName.contains(query)) {
                    continue;
                }
            }

            // Date range filter
            if (n.getTimestamp() != null) {
                Date ts = n.getTimestamp().toDate();
                if (filterFrom != null && ts.before(filterFrom)) continue;
                if (filterTo != null && ts.after(filterTo)) continue;
            }

            filtered.add(n);
        }

        adapter.notifyDataSetChanged();

        boolean isEmpty = filtered.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvLog.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvCount.setText(filtered.size() + " notification" + (filtered.size() == 1 ? "" : "s"));
    }

    private void pickDate(boolean isFrom) {
        Calendar cal = Calendar.getInstance();
        // Initialise picker at existing selection if set
        if (isFrom && filterFrom != null) cal.setTime(filterFrom);
        if (!isFrom && filterTo != null) cal.setTime(filterTo);

        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day);
            if (!isFrom) {
                // Inclusive end-of-day
                selected.set(Calendar.HOUR_OF_DAY, 23);
                selected.set(Calendar.MINUTE, 59);
                selected.set(Calendar.SECOND, 59);
            } else {
                selected.set(Calendar.HOUR_OF_DAY, 0);
                selected.set(Calendar.MINUTE, 0);
                selected.set(Calendar.SECOND, 0);
            }
            if (isFrom) {
                filterFrom = selected.getTime();
                tvFromDate.setText("From: " + DATE_LABEL_FMT.format(filterFrom));
            } else {
                filterTo = selected.getTime();
                tvToDate.setText("To: " + DATE_LABEL_FMT.format(filterTo));
            }
            applyFilter();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
}
