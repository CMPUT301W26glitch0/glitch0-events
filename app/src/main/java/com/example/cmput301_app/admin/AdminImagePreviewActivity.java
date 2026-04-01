/*
 * Purpose: Full-size image preview for admin, with option to remove the poster.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.AdminDB;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.util.ImageUtils;

/**
 * Full-size preview of a single event poster image.
 *
 * Receives the eventId, event name, and organizer name via Intent extras.
 * Fetches the event from Firestore to load the posterUrl, then displays
 * the full-size image. Provides a Remove button that clears the posterUrl
 * field on the event document (the event itself is not deleted).
 */
public class AdminImagePreviewActivity extends AppCompatActivity {

    private static final String TAG = "AdminImagePreview";

    private AdminDB adminDB;
    private String eventId;
    private String eventName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_image_preview);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_image_preview_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adminDB = new AdminDB();

        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");
        String organizerName = getIntent().getStringExtra("organizerName");

        TextView tvEventName = findViewById(R.id.tv_preview_event_name);
        TextView tvOrganizerName = findViewById(R.id.tv_preview_organizer_name);
        ImageView ivFullImage = findViewById(R.id.iv_full_image);
        ProgressBar progressBar = findViewById(R.id.progress_preview);
        Button btnRemove = findViewById(R.id.btn_preview_remove);

        tvEventName.setText(eventName != null ? eventName : "Unnamed Event");
        tvOrganizerName.setText(organizerName != null ? organizerName : "Unknown organizer");

        findViewById(R.id.btn_preview_back).setOnClickListener(v -> finish());

        // Fetch the event to get its posterUrl
        if (eventId != null) {
            progressBar.setVisibility(View.VISIBLE);
            ivFullImage.setVisibility(View.GONE);

            EventDB eventDB = new EventDB();
            eventDB.getEvent(eventId,
                    event -> {
                        progressBar.setVisibility(View.GONE);
                        ivFullImage.setVisibility(View.VISIBLE);
                        if (event != null && event.getPosterUrl() != null
                                && !event.getPosterUrl().isEmpty()) {
                            ImageUtils.loadImage(this, event.getPosterUrl(), ivFullImage, false);
                        }
                    },
                    e -> {
                        Log.e(TAG, "Failed to load event", e);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
            );
        }

        btnRemove.setOnClickListener(v -> {
            String name = eventName != null ? eventName : "this event";
            new AlertDialog.Builder(this)
                    .setTitle("Remove Image")
                    .setMessage("Remove the poster image for \"" + name + "\"? The event itself will not be deleted.")
                    .setPositiveButton("Remove", (dialog, which) -> removeImage())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void removeImage() {
        if (eventId == null) return;
        adminDB.removeImage(
                eventId,
                unused -> {
                    Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> {
                    Log.e(TAG, "Failed to remove image", e);
                    Toast.makeText(this, "Failed to remove image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
        );
    }
}
