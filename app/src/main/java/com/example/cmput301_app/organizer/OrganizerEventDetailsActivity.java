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
import android.widget.EditText;
import android.widget.LinearLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.model.Comment;
import com.google.firebase.firestore.FieldValue;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
<<<<<<< HEAD
=======
import com.example.cmput301_app.util.ImageUtils;
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
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
    private TextView tvCoOrgCount, tvCoOrgHint;
    private android.widget.LinearLayout llCoOrganizers;
    private ImageView ivPoster, ivQrCode;
<<<<<<< HEAD
    private Button btnViewEntrants, btnManageLottery, btnEdit, btnDelete, btnDrawReplacement;
=======
    private Button btnViewEntrants, btnManageLottery, btnEdit, btnDelete, btnDrawReplacement,
            btnNotifyWaitingList, btnNotifySelectedEntrants, btnNotifyCancelledEntrants,
            btnInviteEntrant;
    private FloatingActionButton fabMainActions;
    private View viewDimOverlay;
    private LinearLayout llFabMenu;
    private boolean isFabMenuOpen = false;
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
    private String eventId;
    private EventDB eventDB;
    private NotificationDB notificationDB;
    private Event currentEvent;
    private Bitmap qrBitmap;
    private com.google.firebase.firestore.FirebaseFirestore db;
<<<<<<< HEAD
=======
    private Uri newPosterUri;
    private RecyclerView rvComments;
    private OrganizerCommentAdapter commentAdapter;
    private TextView tvNoComments;
    private EditText etCommentInput;
    private ImageButton btnPostComment;
    private com.google.firebase.firestore.ListenerRegistration eventListener;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPosterMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    newPosterUri = uri;
                    Glide.with(this).load(uri).centerCrop().into(ivPoster);
                    uploadPosterAndUpdate();
                }
            });
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a

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

        tvCoOrgCount = findViewById(R.id.tv_co_org_count);
        tvCoOrgHint = findViewById(R.id.tv_co_org_hint);
        llCoOrganizers = findViewById(R.id.ll_co_organizers);

        findViewById(R.id.btn_assign_co_organizer).setOnClickListener(v -> {
            Intent intent = new Intent(this, AssignCoOrganizerActivity.class);
            intent.putExtra("eventId", eventId);
            if (currentEvent != null) intent.putExtra("eventName", currentEvent.getName());
            startActivity(intent);
        });

        btnViewEntrants = findViewById(R.id.btn_view_entrants);
        btnManageLottery = findViewById(R.id.btn_manage_lottery);
        btnEdit = findViewById(R.id.btn_edit_event_details);
        btnDelete = findViewById(R.id.btn_delete_event_details);
        btnDrawReplacement = findViewById(R.id.btn_draw_replacement);
<<<<<<< HEAD

        btnDrawReplacement.setOnClickListener(v -> drawReplacement());
=======
        btnNotifyWaitingList = findViewById(R.id.btn_notify_waiting_list);

        rvComments = findViewById(R.id.rv_org_comments);
        tvNoComments = findViewById(R.id.tv_org_no_comments);
        etCommentInput = findViewById(R.id.et_org_comment_input);
        btnPostComment = findViewById(R.id.btn_org_post_comment);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new OrganizerCommentAdapter(new java.util.ArrayList<>(), this::confirmAndDeleteComment);
        rvComments.setAdapter(commentAdapter);
        btnPostComment.setOnClickListener(v -> postComment());

        fabMainActions = findViewById(R.id.fab_main_actions);
        viewDimOverlay = findViewById(R.id.view_dim_overlay);
        llFabMenu = findViewById(R.id.ll_fab_menu);

        fabMainActions.setOnClickListener(v -> toggleFabMenu());
        viewDimOverlay.setOnClickListener(v -> closeFabMenu());

        btnDrawReplacement.setOnClickListener(v -> {
            closeFabMenu();
            drawReplacement();
        });

        btnNotifyWaitingList.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(this, NotifyWaitingListActivity.class);
            intent.putExtra("eventId", eventId);
            if (currentEvent != null) intent.putExtra("eventName", currentEvent.getName());
            startActivity(intent);
        });

        btnNotifySelectedEntrants = findViewById(R.id.btn_notify_selected_entrants);
        btnNotifySelectedEntrants.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(this, NotifySelectedEntrantsActivity.class);
            intent.putExtra("eventId", eventId);
            if (currentEvent != null) intent.putExtra("eventName", currentEvent.getName());
            startActivity(intent);
        });

        btnNotifyCancelledEntrants = findViewById(R.id.btn_notify_cancelled_entrants);
        btnNotifyCancelledEntrants.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(this, NotifyCancelledEntrantsActivity.class);
            intent.putExtra("eventId", eventId);
            if (currentEvent != null) intent.putExtra("eventName", currentEvent.getName());
            startActivity(intent);
        });

        btnInviteEntrant = findViewById(R.id.btn_invite_entrant);
        btnInviteEntrant.setOnClickListener(v -> {
            closeFabMenu();
            String orgId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("last_uid", null);
            if (orgId != null) {
                db.collection("users").document(orgId).get()
                        .addOnSuccessListener(doc -> {
                            String orgName = doc.getString("name");
                            launchInviteScreen(orgName != null ? orgName : "The organizer");
                        })
                        .addOnFailureListener(e -> launchInviteScreen("The organizer"));
            } else {
                launchInviteScreen("The organizer");
            }
        });

        findViewById(R.id.fab_change_poster).setOnClickListener(v ->
                pickPosterMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a

        findViewById(R.id.btn_org_back).setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizerDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnEdit.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(this, CreateEventActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

<<<<<<< HEAD
        btnDelete.setOnClickListener(v ->
                eventDB.deleteEvent(eventId, aVoid -> {
                    Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    finish();
                }, e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()));
=======
        btnDelete.setOnClickListener(v -> {
            closeFabMenu();
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Event")
                    .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) ->
                            eventDB.deleteEvent(eventId, aVoid -> {
                                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            }, e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a

        btnViewEntrants.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(this, EntrantListActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

<<<<<<< HEAD
        // Share / Download button — shows a picker dialog
        findViewById(R.id.btn_share_qr_details).setOnClickListener(v -> showQROptions());
=======
        btnManageLottery.setOnClickListener(v -> {
            closeFabMenu();
            Intent intent = new Intent(this, LotteryDrawActivity.class);
            intent.putExtra("eventId", eventId);
            if (currentEvent != null) {
                intent.putExtra("eventCapacity", currentEvent.getCapacity());
            }
            startActivity(intent);
        });
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a

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
        eventListener = db.collection("events").document(eventId).addSnapshotListener((doc, e) -> {
            if (e != null) {
                Log.e(TAG, "Failed to load event", e);
                return;
            }
            if (doc != null && doc.exists()) {
                try {
                    currentEvent = doc.toObject(Event.class);
                    if (currentEvent != null) {
                        currentEvent.setEventId(doc.getId());
                        updateUI();
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error parsing event", ex);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventListener != null) eventListener.remove();
    }

    private void updateUI() {
        tvName.setText(currentEvent.getName());
        tvCategory.setText(currentEvent.getCategory() != null ? currentEvent.getCategory().toUpperCase() : "GENERAL");
        tvLocation.setText(currentEvent.getLocation());
        tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", currentEvent.getPrice()));
        tvCapacity.setText(String.valueOf(currentEvent.getCapacity()));
        tvDescription.setText(currentEvent.getDescription());

        if (currentEvent.getPosterUrl() != null && !currentEvent.getPosterUrl().isEmpty()) {
            ImageUtils.loadImage(this, currentEvent.getPosterUrl(), ivPoster, false);
        }

<<<<<<< HEAD
        if (currentEvent.getQrCode() != null) {
=======
        // Hide QR code section for private events
        int qrVisibility = currentEvent.isPrivate() ? View.GONE : View.VISIBLE;
        findViewById(R.id.tv_qr_label).setVisibility(qrVisibility);
        findViewById(R.id.cv_qr_code).setVisibility(qrVisibility);
        findViewById(R.id.btn_share_qr_details).setVisibility(qrVisibility);

        if (!currentEvent.isPrivate() && currentEvent.getQrCode() != null) {
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
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

        // Show "Draw Lottery" only after registration has closed
        if (currentEvent.getRegistrationClose() != null
                && currentEvent.getRegistrationClose().toDate().before(new java.util.Date())) {
            btnManageLottery.setVisibility(View.VISIBLE);
        } else {
            btnManageLottery.setVisibility(View.GONE);
        }

        // Show "Invite Entrant" button only for private events
        btnInviteEntrant.setVisibility(currentEvent.isPrivate() ? View.VISIBLE : View.GONE);

        loadCoOrganizers();
        displayComments();
    }

<<<<<<< HEAD
=======
    private void postComment() {
        if (etCommentInput == null || currentEvent == null) return;
        String content = etCommentInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String orgId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("last_uid", null);
        btnPostComment.setEnabled(false);

        Runnable postWithName = () -> db.collection("users")
                .document(orgId != null ? orgId : "")
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String authorName = (name != null && !name.isEmpty()) ? name : "Organizer";
                    Comment comment = new Comment(
                            java.util.UUID.randomUUID().toString(),
                            content, authorName,
                            com.google.firebase.Timestamp.now(),
                            true);
                    db.collection("events").document(eventId)
                            .update("comments", FieldValue.arrayUnion(comment))
                            .addOnSuccessListener(aVoid -> {
                                etCommentInput.setText("");
                                btnPostComment.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show();
                                btnPostComment.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch profile", Toast.LENGTH_SHORT).show();
                    btnPostComment.setEnabled(true);
                });

        if (orgId != null) {
            postWithName.run();
        } else {
            // No user ID — still post with generic label
            Comment comment = new Comment(
                    java.util.UUID.randomUUID().toString(),
                    content, "Organizer",
                    com.google.firebase.Timestamp.now(),
                    true);
            db.collection("events").document(eventId)
                    .update("comments", FieldValue.arrayUnion(comment))
                    .addOnSuccessListener(aVoid -> {
                        etCommentInput.setText("");
                        btnPostComment.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show();
                        btnPostComment.setEnabled(true);
                    });
        }
    }

    private void displayComments() {
        java.util.List<Comment> comments = currentEvent.getComments();
        if (comments == null || comments.isEmpty()) {
            rvComments.setVisibility(View.GONE);
            tvNoComments.setVisibility(View.VISIBLE);
        } else {
            tvNoComments.setVisibility(View.GONE);
            rvComments.setVisibility(View.VISIBLE);
            java.util.List<Comment> sorted = new java.util.ArrayList<>(comments);
            sorted.sort((a, b) -> {
                if (a.getTimestamp() == null) return -1;
                if (b.getTimestamp() == null) return 1;
                return a.getTimestamp().compareTo(b.getTimestamp());
            });
            commentAdapter.setComments(sorted);
        }
    }

    private void confirmAndDeleteComment(Comment comment) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) ->
                        eventDB.deleteComment(eventId, comment,
                                aVoid -> Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(this, "Failed to delete comment", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadCoOrganizers() {
        if (llCoOrganizers == null || currentEvent == null) return;

        java.util.List<String> coOrgIds = currentEvent.getCoOrganizerIds();
        llCoOrganizers.removeAllViews();

        if (coOrgIds == null || coOrgIds.isEmpty()) {
            if (tvCoOrgCount != null) tvCoOrgCount.setText("0");
            if (tvCoOrgHint != null) tvCoOrgHint.setVisibility(android.view.View.VISIBLE);
            return;
        }

        if (tvCoOrgCount != null) tvCoOrgCount.setText(String.valueOf(coOrgIds.size()));
        if (tvCoOrgHint != null) tvCoOrgHint.setVisibility(android.view.View.GONE);

        for (String userId : coOrgIds) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("name");
                        if (name == null) name = "Unknown";
                        final String displayName = name;

                        runOnUiThread(() -> {
                            // Build initials avatar + name row
                            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
                            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                            android.widget.LinearLayout.LayoutParams rowParams =
                                    new android.widget.LinearLayout.LayoutParams(
                                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                            rowParams.setMargins(0, 0, 0, 10);
                            row.setLayoutParams(rowParams);

                            // Avatar circle
                            TextView avatar = new TextView(this);
                            int size = (int) (40 * getResources().getDisplayMetrics().density);
                            android.widget.LinearLayout.LayoutParams avatarParams =
                                    new android.widget.LinearLayout.LayoutParams(size, size);
                            avatarParams.setMarginEnd((int) (10 * getResources().getDisplayMetrics().density));
                            avatar.setLayoutParams(avatarParams);
                            avatar.setGravity(android.view.Gravity.CENTER);
                            avatar.setText(displayName.substring(0, 1).toUpperCase());
                            avatar.setTextColor(0xFFFFFFFF);
                            avatar.setTextSize(16f);
                            android.graphics.drawable.GradientDrawable circle =
                                    new android.graphics.drawable.GradientDrawable();
                            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                            circle.setColor(0xFF7F56D9);
                            avatar.setBackground(circle);

                            // Name text
                            TextView tvName = new TextView(this);
                            tvName.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                            tvName.setText(displayName);
                            tvName.setTextColor(getResources().getColor(R.color.dark_blue, getTheme()));
                            tvName.setTextSize(14f);

                            // Purple "Co-Organizer" badge
                            TextView badge = new TextView(this);
                            android.widget.LinearLayout.LayoutParams badgeParams =
                                    new android.widget.LinearLayout.LayoutParams(
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                            badge.setLayoutParams(badgeParams);
                            badge.setText("Co-Org");
                            badge.setTextColor(0xFF7F56D9);
                            badge.setTextSize(11f);
                            int hPad = (int) (8 * getResources().getDisplayMetrics().density);
                            int vPad = (int) (3 * getResources().getDisplayMetrics().density);
                            badge.setPadding(hPad, vPad, hPad, vPad);
                            android.graphics.drawable.GradientDrawable badgeBg =
                                    new android.graphics.drawable.GradientDrawable();
                            badgeBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                            badgeBg.setCornerRadius(50f);
                            badgeBg.setColor(0xFFF4F3FF);
                            badge.setBackground(badgeBg);

                            // Remove button
                            android.widget.Button btnRemove = new android.widget.Button(this);
                            android.widget.LinearLayout.LayoutParams removeParams =
                                    new android.widget.LinearLayout.LayoutParams(
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                            btnRemove.setLayoutParams(removeParams);
                            btnRemove.setText("Remove");
                            btnRemove.setTextSize(11f);
                            btnRemove.setAllCaps(false);
                            btnRemove.setTextColor(0xFFD92D20);
                            btnRemove.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                            btnRemove.setOnClickListener(v -> confirmRemoveCoOrganizer(userId, displayName));

                            row.addView(avatar);
                            row.addView(tvName);
                            row.addView(badge);
                            row.addView(btnRemove);
                            llCoOrganizers.addView(row);
                        });
                    });
        }
    }

    private void confirmRemoveCoOrganizer(String userId, String displayName) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Remove Co-Organizer")
                .setMessage("Remove " + displayName + " as co-organizer?")
                .setPositiveButton("Remove", (dialog, which) ->
                        eventDB.removeCoOrganizer(eventId, userId,
                                aVoid -> Toast.makeText(this, displayName + " removed as co-organizer",
                                        Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(this, "Failed to remove co-organizer",
                                        Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void launchInviteScreen(String organizerName) {
        Intent intent = new Intent(this, InviteEntrantToWaitingListActivity.class);
        intent.putExtra("eventId", eventId);
        if (currentEvent != null) intent.putExtra("eventName", currentEvent.getName());
        intent.putExtra("organizerName", organizerName);
        startActivity(intent);
    }

>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
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

<<<<<<< HEAD
=======
    private void toggleFabMenu() {
        isFabMenuOpen = !isFabMenuOpen;
        if (isFabMenuOpen) {
            viewDimOverlay.setVisibility(View.VISIBLE);
            llFabMenu.setVisibility(View.VISIBLE);
            fabMainActions.animate().rotation(45f).setDuration(200).start();
        } else {
            viewDimOverlay.setVisibility(View.GONE);
            llFabMenu.setVisibility(View.GONE);
            fabMainActions.animate().rotation(0f).setDuration(200).start();
        }
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;
        viewDimOverlay.setVisibility(View.GONE);
        llFabMenu.setVisibility(View.GONE);
        fabMainActions.animate().rotation(0f).setDuration(200).start();
    }

>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
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
<<<<<<< HEAD
                        if ("WAITING".equals(outcome)) {
=======
                        if ("WAITING".equals(outcome) || "NOT_SELECTED".equals(outcome)) {
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
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

<<<<<<< HEAD
        // We can't easily update a specific item in an array in Firestore, 
        // so we remove the old WAITING record and add the new SELECTED record.
        java.util.Map<String, Object> oldRecord = new java.util.HashMap<>();
        oldRecord.put("eventId", eventId);
        oldRecord.put("outcome", "WAITING");
=======
        String oldOutcome = getOutcomeForEvent(userDoc, eventId);

        // We can't easily update a specific item in an array in Firestore, 
        // so we remove the old record and add the new SELECTED record.
        java.util.Map<String, Object> oldRecord = new java.util.HashMap<>();
        oldRecord.put("eventId", eventId);
        oldRecord.put("outcome", oldOutcome);
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a

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

<<<<<<< HEAD
=======
    private void uploadPosterAndUpdate() {
        if (newPosterUri == null || currentEvent == null) return;

        // Compress to ~800×800 px at 55% JPEG quality and store as Base64 in Firestore
        String base64 = ImageUtils.compressToBase64(this, newPosterUri, 800, 55);
        if (base64 == null) {
            Toast.makeText(this, "Could not process image", Toast.LENGTH_SHORT).show();
            return;
        }
        currentEvent.setPosterUrl(base64);
        eventDB.updateEvent(currentEvent,
                aVoid -> Toast.makeText(this, "Event photo updated", Toast.LENGTH_SHORT).show(),
                e -> Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show());
    }

>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
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
