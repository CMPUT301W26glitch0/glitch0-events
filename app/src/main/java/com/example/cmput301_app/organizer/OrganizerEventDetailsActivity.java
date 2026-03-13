/**
 * Management screen for a single event owned by the current organizer.
 *
 * Loads event data from Firestore via EventDB and displays key info (name,
 * category, date, location, price, capacity, description, poster, QR code).
 * Available actions:
 *  - Edit — opens CreateEventActivity pre-populated with the event's data.
 *  - Delete — removes the event from Firestore and navigates back.
 *  - View Entrants — opens EntrantListActivity for this event.
 *  - Share / Download QR — saves or shares the generated QR code image.
 *  - Draw Replacement — visible when at least one entrant has DECLINED or
 *    CANCELLED; picks a random WAITING entrant and marks them as SELECTED.
 *
 * The QR code is generated on-device from the event's {@code qrCode} field
 * using the ZXing library.
 *
 * Outstanding issues:
 * - The replacement draw is client-side and not atomic (see LotteryDrawActivity).
 * - The Manage Lottery button is present in the layout but navigates to
 *   LotteryDrawActivity rather than a dedicated per-event lottery manager.
 */
package com.example.cmput301_app.organizer;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.bumptech.glide.Glide;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.database.NotificationDB;
import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.model.Notification;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class OrganizerEventDetailsActivity extends AppCompatActivity {
    private static final String TAG = "OrganizerEventDetails";
    private TextView tvName, tvCategory, tvDate, tvLocation, tvPrice, tvCapacity, tvRegDates, tvDescription;
    private ImageView ivPoster, ivQrCode;
    private Button btnViewEntrants, btnManageLottery, btnEdit, btnDelete, btnDrawReplacement;
    private String eventId;
    private EventDB eventDB;
    private NotificationDB notificationDB;
    private Event currentEvent;
    private Bitmap qrBitmap;
    private com.google.firebase.firestore.FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_event_details);

        eventDB = new EventDB();
        notificationDB = new NotificationDB();
        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId");

        initViews();

        if (eventId != null) {
            loadEventDetails();
            loadReplacementStatus();
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        ivPoster = findViewById(R.id.iv_org_event_poster);
        ivQrCode = findViewById(R.id.iv_org_qr_code);
        tvName = findViewById(R.id.tv_org_event_name);
        tvCategory = findViewById(R.id.tv_org_event_category);
        tvDate = findViewById(R.id.tv_org_date);
        tvLocation = findViewById(R.id.tv_org_location);
        tvPrice = findViewById(R.id.tv_org_price);
        tvCapacity = findViewById(R.id.tv_org_capacity);
        tvRegDates = findViewById(R.id.tv_org_reg_dates);
        tvDescription = findViewById(R.id.tv_org_description);

        btnViewEntrants = findViewById(R.id.btn_view_entrants);
        btnManageLottery = findViewById(R.id.btn_manage_lottery);
        btnEdit = findViewById(R.id.btn_edit_event_details);
        btnDelete = findViewById(R.id.btn_delete_event_details);
        btnDrawReplacement = findViewById(R.id.btn_draw_replacement);

        btnDrawReplacement.setOnClickListener(v -> drawReplacement());

        findViewById(R.id.btn_org_back).setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizerDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateEventActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v ->
                eventDB.deleteEvent(eventId, aVoid -> {
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    finish();
                }, e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()));

        btnViewEntrants.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantListActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        // Share / Download button — shows a picker dialog
        findViewById(R.id.btn_share_qr_details).setOnClickListener(v -> showQROptions());

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
    }

    private void loadEventDetails() {
        eventDB.getEvent(eventId, event -> {
            if (event != null) {
                currentEvent = event;
                updateUI();
            }
        }, e -> {
            Log.e(TAG, "Failed to load event", e);
            Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUI() {
        tvName.setText(currentEvent.getName());
        tvCategory.setText(currentEvent.getCategory() != null ? currentEvent.getCategory().toUpperCase() : "GENERAL");
        tvLocation.setText(currentEvent.getLocation());
        tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", currentEvent.getPrice()));
        tvCapacity.setText(String.valueOf(currentEvent.getCapacity()));
        tvDescription.setText(currentEvent.getDescription());

        if (currentEvent.getPosterUrl() != null && !currentEvent.getPosterUrl().isEmpty()) {
            Glide.with(this).load(currentEvent.getPosterUrl()).into(ivPoster);
        }

        if (currentEvent.getQrCode() != null) {
            qrBitmap = generateQRCode(currentEvent.getQrCode());
            if (qrBitmap != null) ivQrCode.setImageBitmap(qrBitmap);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat fullFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        if (currentEvent.getDate() != null) {
            tvDate.setText(dateFormat.format(currentEvent.getDate().toDate()));
        }

        if (currentEvent.getRegistrationOpen() != null && currentEvent.getRegistrationClose() != null) {
            String open = fullFormat.format(currentEvent.getRegistrationOpen().toDate());
            String close = fullFormat.format(currentEvent.getRegistrationClose().toDate());
            tvRegDates.setText(open + " - " + close);
        }
    }

    private void loadReplacementStatus() {
        if (eventId == null) return;
        
        // Listen to all users who are on this event's waiting list
        db.collection("events").document(eventId).addSnapshotListener((eventDoc, e) -> {
            if (e != null || eventDoc == null || !eventDoc.exists()) return;
            
            java.util.List<String> waitingListIds = (java.util.List<String>) eventDoc.get("waitingListIds");
            if (waitingListIds == null || waitingListIds.isEmpty()) {
                btnDrawReplacement.setVisibility(View.GONE);
                return;
            }

            // We need to count how many are Cancelled/Declined vs how many were newly Selected as replacements.
            // For simplicity in this assignment: if there is >= 1 cancelled/declined, allow drawing.
            // A more robust implementation would track "replacementCapacity".
            java.util.concurrent.atomic.AtomicInteger cancelledCount = new java.util.concurrent.atomic.AtomicInteger(0);
            java.util.concurrent.atomic.AtomicInteger remainingCounter = new java.util.concurrent.atomic.AtomicInteger(waitingListIds.size());

            for (String userId : waitingListIds) {
                db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String outcome = getOutcomeForEvent(userDoc, eventId);
                        if ("CANCELLED".equals(outcome) || "DECLINED".equals(outcome)) {
                            cancelledCount.incrementAndGet();
                        }
                    }
                    if (remainingCounter.decrementAndGet() == 0) {
                        // If there are cancelled users, show the draw replacement button
                        runOnUiThread(() -> {
                            if (cancelledCount.get() > 0) {
                                btnDrawReplacement.setVisibility(View.VISIBLE);
                            } else {
                                btnDrawReplacement.setVisibility(View.GONE);
                            }
                        });
                    }
                });
            }
        });
    }

    private void drawReplacement() {
        if (eventId == null) return;
        btnDrawReplacement.setEnabled(false);

        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            java.util.List<String> waitingListIds = (java.util.List<String>) eventDoc.get("waitingListIds");
            if (waitingListIds == null || waitingListIds.isEmpty()) {
                Toast.makeText(this, "No replacements available", Toast.LENGTH_SHORT).show();
                btnDrawReplacement.setEnabled(true);
                return;
            }

            // Find all users currently WAITING
            java.util.List<com.google.firebase.firestore.DocumentSnapshot> waitingUsers = new java.util.ArrayList<>();
            java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(waitingListIds.size());

            for (String userId : waitingListIds) {
                db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String outcome = getOutcomeForEvent(userDoc, eventId);
                        if ("WAITING".equals(outcome)) {
                            synchronized (waitingUsers) { waitingUsers.add(userDoc); }
                        }
                    }
                    
                    if (remaining.decrementAndGet() == 0) {
                        if (waitingUsers.isEmpty()) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "No valid replacements remaining on waiting list", Toast.LENGTH_SHORT).show();
                                btnDrawReplacement.setEnabled(true);
                            });
                            return;
                        }

                        // Pick a random user
                        java.util.Random random = new java.util.Random();
                        com.google.firebase.firestore.DocumentSnapshot chosenUser = waitingUsers.get(random.nextInt(waitingUsers.size()));
                        
                        updateUserToSelected(chosenUser);
                    }
                });
            }
        });
    }

    private void updateUserToSelected(com.google.firebase.firestore.DocumentSnapshot userDoc) {
        String userId = userDoc.getId();
        String userName = userDoc.getString("name");
        
        // Construct new record payload
        java.util.Map<String, Object> newRecord = new java.util.HashMap<>();
        newRecord.put("eventId", eventId);
        newRecord.put("outcome", "SELECTED");
        newRecord.put("timestamp", com.google.firebase.Timestamp.now());

        // We can't easily update a specific item in an array in Firestore, 
        // so we remove the old WAITING record and add the new SELECTED record.
        java.util.Map<String, Object> oldRecord = new java.util.HashMap<>();
        oldRecord.put("eventId", eventId);
        oldRecord.put("outcome", "WAITING");

        db.collection("users").document(userId)
                .update(
                    "registrationHistory", com.google.firebase.firestore.FieldValue.arrayRemove(oldRecord)
                ).addOnSuccessListener(aVoid -> {
                    db.collection("users").document(userId)
                        .update("registrationHistory", com.google.firebase.firestore.FieldValue.arrayUnion(newRecord))
                        .addOnSuccessListener(aVoid2 -> {
                            // Notify the chosen entrant
                            Notification notif = new Notification(
                                    null,
                                    eventId,
                                    userId,
                                    "Congratulations! You've been selected as a replacement for this event.",
                                    Notification.NotificationType.LOTTERY_WIN_REDRAW,
                                    com.google.firebase.Timestamp.now()
                            );
                            notif.addRecipient(userId);
                            notificationDB.createNotification(notif, v -> {}, e -> {});

                            runOnUiThread(() -> {
                                Toast.makeText(this, "Replacement drawn: " + (userName != null ? userName : "Unknown") + " is now Selected!", Toast.LENGTH_LONG).show();
                                btnDrawReplacement.setEnabled(true);
                            });
                        });
                });
    }

    private String getOutcomeForEvent(com.google.firebase.firestore.DocumentSnapshot userDoc, String eventId) {
        Object rawHistory = userDoc.get("registrationHistory");
        if (rawHistory instanceof java.util.List) {
            for (Object item : (java.util.List<?>) rawHistory) {
                if (item instanceof java.util.Map) {
                    java.util.Map<?, ?> record = (java.util.Map<?, ?>) item;
                    if (eventId.equals(record.get("eventId"))) {
                        Object outcome = record.get("outcome");
                        if (outcome != null) return outcome.toString();
                    }
                }
            }
        }
        return "WAITING";
    }

    private Bitmap generateQRCode(String data) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 400, 400);
            int w = bitMatrix.getWidth(), h = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            for (int x = 0; x < w; x++)
                for (int y = 0; y < h; y++)
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            return bitmap;
        } catch (WriterException e) {
            Log.e(TAG, "QR Generation failed", e);
            return null;
        }
    }

    private void showQROptions() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle("QR Code")
                .setItems(new String[]{"Download to Gallery", "Share"}, (dialog, which) -> {
                    if (which == 0) downloadQR();
                    else shareQR();
                })
                .show();
    }

    private void downloadQR() {
        if (qrBitmap == null) return;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "event_qr_" + eventId + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/EventQRCodes");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);
                Toast.makeText(this, "✅ QR Code saved to gallery!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQR() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File qrDir = new File(getCacheDir(), "qr_codes");
            if (!qrDir.exists()) qrDir.mkdirs();

            File qrFile = new File(qrDir, "event_qr_" + eventId + ".png");
            try (FileOutputStream fos = new FileOutputStream(qrFile)) {
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            Uri shareUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    qrFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Scan this QR code to view the event!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));

        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
