/*
 * Purpose: Admin screen for browsing event poster images.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.AdminDB;
import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.model.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin screen that shows a grid of all event poster images.
 *
 * Loads events (filtered to those with a non-empty posterUrl) and organizer
 * display names in parallel. Tapping an image opens AdminImagePreviewActivity
 * where the admin can view the full-size image and remove it if needed.
 */
public class AdminImagesActivity extends AppCompatActivity {

    private static final String TAG = "AdminImages";

    private AdminDB adminDB;
    private RecyclerView rvImages;
    private AdminImageAdapter adapter;
    private final List<Event> imageList = new ArrayList<>();
    private final Map<String, String> organizerNames = new HashMap<>();
    private TextView tvEmptyState;
    private TextView tvImageCount;

    /** Tracks how many async loads are still pending (events + profiles = 2). */
    private int pendingLoads = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_images);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_images_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adminDB = new AdminDB();

        tvEmptyState = findViewById(R.id.tv_images_empty);
        tvImageCount = findViewById(R.id.tv_image_count);

        rvImages = findViewById(R.id.rv_admin_images);
        rvImages.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AdminImageAdapter(imageList, organizerNames, this, this::onImageTapped);
        rvImages.setAdapter(adapter);

        findViewById(R.id.btn_images_back).setOnClickListener(v -> finish());

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pendingLoads = 2;
        imageList.clear();
        organizerNames.clear();
        loadData();
    }

    private void loadData() {
        adminDB.getAllImages(
                events -> {
                    imageList.clear();
                    imageList.addAll(events);
                    onLoadComplete();
                },
                e -> {
                    Log.e(TAG, "Failed to load images", e);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show());
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
    }

    private synchronized void onLoadComplete() {
        pendingLoads--;
        if (pendingLoads == 0) {
            runOnUiThread(this::renderList);
        }
    }

    private void renderList() {
        adapter.notifyDataSetChanged();
        boolean isEmpty = imageList.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvImages.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvImageCount.setText(imageList.size() + " image" + (imageList.size() == 1 ? "" : "s"));
    }

    private void onImageTapped(Event event) {
        String organizerName = organizerNames.getOrDefault(event.getOrganizerId(), null);
        Intent intent = new Intent(this, AdminImagePreviewActivity.class);
        intent.putExtra("eventId", event.getEventId());
        intent.putExtra("eventName", event.getName());
        intent.putExtra("organizerName", organizerName);
        startActivity(intent);
    }
}
