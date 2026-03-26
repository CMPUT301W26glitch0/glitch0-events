/**
 * Displays a co-organizer invitation for an entrant to accept or decline.
 *
 * Receives the eventId and notificationId via intent extras. Fetches both the
 * event and the notification from Firestore to populate the UI with the event
 * name, inviting organizer's name, event date, and location.
 *
 * Accepting the invitation:
 *  - Moves the entrant from pendingCoOrganizerInvites to coOrganizerIds in the event doc.
 *  - Removes the entrant from waitingListIds (removes them from the entrant pool).
 *  - Navigates to EventDetailsActivity for the accepted event.
 *
 * Declining the invitation:
 *  - Removes the entrant from pendingCoOrganizerInvites in the event doc.
 *  - Finishes the activity.
 */
package com.example.cmput301_app.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.database.NotificationDB;
import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.model.Notification;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class CoOrganizerInvitationActivity extends AppCompatActivity {

    private static final String TAG = "CoOrgInvitation";

    private TextView tvEventName, tvInviterName, tvInviterInitial, tvEventDate, tvEventLocation;
    private Button btnAccept, btnDecline;
    private ProgressBar progressLoading;

    private EventDB eventDB;
    private NotificationDB notificationDB;

    private String eventId;
    private String notificationId;
    private String deviceId;
    private Event currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_co_organizer_invitation);

        eventId = getIntent().getStringExtra("eventId");
        notificationId = getIntent().getStringExtra("notificationId");

        eventDB = new EventDB();
        notificationDB = new NotificationDB();

        deviceId = resolveDeviceId();

        initViews();
        loadInvitationDetails();
    }

    private void initViews() {
        tvEventName = findViewById(R.id.tv_event_name);
        tvInviterName = findViewById(R.id.tv_inviter_name);
        tvInviterInitial = findViewById(R.id.tv_inviter_initial);
        tvEventDate = findViewById(R.id.tv_event_date);
        tvEventLocation = findViewById(R.id.tv_event_location);
        btnAccept = findViewById(R.id.btn_accept);
        btnDecline = findViewById(R.id.btn_decline);
        progressLoading = findViewById(R.id.progress_loading);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnAccept.setOnClickListener(v -> handleAccept());
        btnDecline.setOnClickListener(v -> handleDecline());
    }

    private void loadInvitationDetails() {
        if (eventId == null) {
            Toast.makeText(this, "Invalid invitation", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        btnAccept.setEnabled(false);
        btnDecline.setEnabled(false);

        // Load event and notification in parallel, render when both ready
        final Event[] loadedEvent = {null};
        final String[] loadedInviterName = {null};
        final int[] pendingLoads = {2};

        eventDB.getEvent(eventId, event -> {
            loadedEvent[0] = event;
            pendingLoads[0]--;
            if (pendingLoads[0] == 0) {
                runOnUiThread(() -> renderUI(loadedEvent[0], loadedInviterName[0]));
            }
        }, e -> {
            Log.e(TAG, "Failed to load event", e);
            pendingLoads[0]--;
            runOnUiThread(() -> {
                Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show();
                finish();
            });
        });

        if (notificationId != null) {
            notificationDB.getNotification(notificationId, notification -> {
                if (notification != null && notification.getInviterName() != null) {
                    loadedInviterName[0] = notification.getInviterName();
                }
                pendingLoads[0]--;
                if (pendingLoads[0] == 0) {
                    runOnUiThread(() -> renderUI(loadedEvent[0], loadedInviterName[0]));
                }
            }, e -> {
                pendingLoads[0]--;
                if (pendingLoads[0] == 0) {
                    runOnUiThread(() -> renderUI(loadedEvent[0], loadedInviterName[0]));
                }
            });
        } else {
            pendingLoads[0]--;
        }
    }

    private void renderUI(Event event, String inviterName) {
        progressLoading.setVisibility(View.GONE);
        btnAccept.setEnabled(true);
        btnDecline.setEnabled(true);

        if (event == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentEvent = event;

        tvEventName.setText(event.getName() != null ? event.getName() : "Unknown Event");

        String displayInviter = inviterName != null ? inviterName : "An organizer";
        tvInviterName.setText(displayInviter);
        tvInviterInitial.setText(
                displayInviter.length() > 0
                        ? String.valueOf(displayInviter.charAt(0)).toUpperCase()
                        : "O");

        if (event.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvEventDate.setText(sdf.format(event.getDate().toDate()));
        } else {
            tvEventDate.setText("Date TBD");
        }

        tvEventLocation.setText(
                event.getLocation() != null && !event.getLocation().isEmpty()
                        ? event.getLocation()
                        : "Location TBD");
    }

    private void handleAccept() {
        if (deviceId == null || eventId == null) {
            Toast.makeText(this, "Cannot process invitation", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAccept.setEnabled(false);
        btnDecline.setEnabled(false);

        boolean wasOnWaitingList = currentEvent != null
                && currentEvent.getWaitingListIds() != null
                && currentEvent.getWaitingListIds().contains(deviceId);
        eventDB.acceptCoOrganizerInvite(eventId, deviceId, wasOnWaitingList, aVoid -> {
            Toast.makeText(this, "You are now a co-organizer!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("eventId", eventId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }, e -> {
            Log.e(TAG, "Failed to accept invitation", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Failed to accept invitation", Toast.LENGTH_SHORT).show();
                btnAccept.setEnabled(true);
                btnDecline.setEnabled(true);
            });
        });
    }

    private void handleDecline() {
        if (deviceId == null || eventId == null) {
            finish();
            return;
        }

        btnAccept.setEnabled(false);
        btnDecline.setEnabled(false);

        eventDB.declineCoOrganizerInvite(eventId, deviceId, aVoid -> {
            Toast.makeText(this, "Invitation declined", Toast.LENGTH_SHORT).show();
            finish();
        }, e -> {
            Log.e(TAG, "Failed to decline invitation", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Failed to decline invitation", Toast.LENGTH_SHORT).show();
                btnAccept.setEnabled(true);
                btnDecline.setEnabled(true);
            });
        });
    }

    private String resolveDeviceId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("last_uid", null);
    }
}
