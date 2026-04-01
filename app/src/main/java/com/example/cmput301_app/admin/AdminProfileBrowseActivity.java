/*
 * Purpose: Admin profile browsing screen — lists all users with search and tap-to-details.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.content.Intent;
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
import com.example.cmput301_app.model.Profile;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen for browsing all user profiles in the system.
 *
 * Displays every profile with name, email, and join date. A search bar filters
 * by name or email. Tapping a profile navigates to the read-only detail view.
 */
public class AdminProfileBrowseActivity extends AppCompatActivity {

    private static final String TAG = "AdminProfileBrowse";

    private AdminDB adminDB;
    private RecyclerView rvProfiles;
    private AdminProfileBrowseAdapter adapter;

    private final List<Profile> allProfiles = new ArrayList<>();
    private final List<Profile> filteredProfiles = new ArrayList<>();

    private TextView tvEmptyState;
    private TextView tvProfileCount;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile_browse);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_profile_browse_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adminDB = new AdminDB();

        tvEmptyState = findViewById(R.id.tv_profile_browse_empty);
        tvProfileCount = findViewById(R.id.tv_profile_browse_count);
        etSearch = findViewById(R.id.et_search_profiles);

        rvProfiles = findViewById(R.id.rv_admin_profiles_browse);
        rvProfiles.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminProfileBrowseAdapter(filteredProfiles, this, this::onProfileTapped);
        rvProfiles.setAdapter(adapter);

        findViewById(R.id.btn_profile_browse_back).setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadProfiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfiles();
    }

    private void loadProfiles() {
        adminDB.getAllProfiles(
                profiles -> {
                    allProfiles.clear();
                    allProfiles.addAll(profiles);
                    applyFilter(etSearch.getText().toString());
                },
                e -> {
                    Log.e(TAG, "Failed to load profiles", e);
                    Toast.makeText(this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void applyFilter(String query) {
        filteredProfiles.clear();
        String trimmed = query.trim().toLowerCase();

        for (Profile p : allProfiles) {
            if (trimmed.isEmpty()) {
                filteredProfiles.add(p);
                continue;
            }
            if (p.getName() != null && p.getName().toLowerCase().contains(trimmed)) {
                filteredProfiles.add(p);
                continue;
            }
            if (p.getEmail() != null && p.getEmail().toLowerCase().contains(trimmed)) {
                filteredProfiles.add(p);
            }
        }

        adapter.notifyDataSetChanged();

        boolean isEmpty = filteredProfiles.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvProfiles.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvProfileCount.setText(filteredProfiles.size() + " user"
                + (filteredProfiles.size() == 1 ? "" : "s"));
    }

    private void onProfileTapped(Profile profile) {
        Intent intent = new Intent(this, AdminProfileDetailActivity.class);
        intent.putExtra("deviceId", profile.getDeviceId());
        intent.putExtra("name", profile.getName());
        intent.putExtra("email", profile.getEmail());
        intent.putExtra("phone", profile.getPhoneNumber());
        intent.putExtra("role", profile.getRole());
        intent.putExtra("profileImageUrl", profile.getProfileImageUrl());
        if (profile.getJoinDate() != null) {
            intent.putExtra("joinDateMs", profile.getJoinDate().toDate().getTime());
        }
        startActivity(intent);
    }
}
