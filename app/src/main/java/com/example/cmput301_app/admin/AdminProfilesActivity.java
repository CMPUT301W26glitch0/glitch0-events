/*
 * Purpose: Profile management screen for the Admin role.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.AdminDB;
import com.example.cmput301_app.model.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen that lists all user profiles with the ability to remove any profile.
 *
 * When an admin removes a profile:
 * 1. The user is removed from all event waiting lists.
 * 2. Their profile image is deleted from Firebase Storage.
 * 3. Their Firestore user document is deleted.
 * 4. Active devices are signed out on the next app resume (handled by dashboard activities).
 */
public class AdminProfilesActivity extends AppCompatActivity {

    private static final String TAG = "AdminProfiles";

    private AdminDB adminDB;
    private RecyclerView rvProfiles;
    private AdminProfileAdapter adapter;
    private List<Profile> profileList;
    private TextView tvEmptyState;
    private TextView tvProfileCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean isDarkMode = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getBoolean("darkModeEnabled", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profiles);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_profiles_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adminDB = new AdminDB();

        tvEmptyState = findViewById(R.id.tv_profiles_empty);
        tvProfileCount = findViewById(R.id.tv_profile_count);

        rvProfiles = findViewById(R.id.rv_admin_profiles);
        rvProfiles.setLayoutManager(new LinearLayoutManager(this));
        profileList = new ArrayList<>();
        adapter = new AdminProfileAdapter(profileList, this, this::onRemoveConfirmed);
        rvProfiles.setAdapter(adapter);

        findViewById(R.id.btn_profiles_back).setOnClickListener(v -> finish());

        loadAllProfiles();
    }

    private void loadAllProfiles() {
        adminDB.getAllProfiles(
                profiles -> {
                    profileList.clear();
                    profileList.addAll(profiles);
                    adapter.notifyDataSetChanged();

                    boolean isEmpty = profileList.isEmpty();
                    tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvProfiles.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    tvProfileCount.setText(profileList.size() + " user"
                            + (profileList.size() == 1 ? "" : "s"));
                },
                e -> {
                    Log.e(TAG, "Failed to load profiles", e);
                    Toast.makeText(this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void onRemoveConfirmed(Profile profile, int position) {
        String deviceId = profile.getDeviceId();
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Invalid profile ID", Toast.LENGTH_SHORT).show();
            return;
        }

        adminDB.removeProfile(
                deviceId,
                unused -> {
                    Toast.makeText(this, "Profile removed", Toast.LENGTH_SHORT).show();
                    profileList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, profileList.size());

                    boolean isEmpty = profileList.isEmpty();
                    tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvProfiles.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    tvProfileCount.setText(profileList.size() + " user"
                            + (profileList.size() == 1 ? "" : "s"));
                },
                e -> {
                    Log.e(TAG, "Failed to remove profile", e);
                    Toast.makeText(this, "Failed to remove profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
        );
    }
}
