/*
 * Purpose: Read-only admin view of a single user profile, with option to remove.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.example.cmput301_app.util.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Read-only profile detail view for the admin.
 *
 * Receives profile data via Intent extras and displays name, email, phone,
 * role, and join date. Provides a Remove button that prompts for confirmation
 * before deleting the profile via AdminDB.
 */
public class AdminProfileDetailActivity extends AppCompatActivity {

    private AdminDB adminDB;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_profile_detail_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adminDB = new AdminDB();

        deviceId = getIntent().getStringExtra("deviceId");
        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");
        String phone = getIntent().getStringExtra("phone");
        String role = getIntent().getStringExtra("role");
        String profileImageUrl = getIntent().getStringExtra("profileImageUrl");
        long joinDateMs = getIntent().getLongExtra("joinDateMs", -1);

        TextView tvName = findViewById(R.id.tv_detail_name);
        TextView tvEmail = findViewById(R.id.tv_detail_email);
        TextView tvPhone = findViewById(R.id.tv_detail_phone);
        TextView tvRole = findViewById(R.id.tv_detail_role);
        TextView tvJoinDate = findViewById(R.id.tv_detail_join_date);
        TextView tvInitial = findViewById(R.id.tv_detail_initial);
        ImageView ivAvatar = findViewById(R.id.iv_detail_avatar);
        Button btnRemove = findViewById(R.id.btn_detail_remove);

        String displayName = name != null ? name : "Unknown User";
        tvName.setText(displayName);
        tvEmail.setText(email != null ? email : "—");

        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText(phone);
            tvPhone.setVisibility(View.VISIBLE);
        } else {
            tvPhone.setVisibility(View.GONE);
        }

        // Role badge
        String displayRole = role != null ? capitalize(role) : "Entrant";
        tvRole.setText(displayRole);
        switch (role != null ? role.toLowerCase() : "") {
            case "organizer":
                tvRole.setBackgroundColor(0xFFEFF8FF);
                tvRole.setTextColor(0xFF026AA2);
                break;
            case "admin":
                tvRole.setBackgroundColor(0xFFF4F3FF);
                tvRole.setTextColor(0xFF5925DC);
                break;
            default:
                tvRole.setBackgroundColor(0xFFECFDF3);
                tvRole.setTextColor(0xFF027A48);
                break;
        }

        // Join date
        if (joinDateMs > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvJoinDate.setText("Joined " + sdf.format(new Date(joinDateMs)));
        } else {
            tvJoinDate.setText("Join date unknown");
        }

        // Avatar
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            ivAvatar.setVisibility(View.VISIBLE);
            tvInitial.setVisibility(View.GONE);
            ImageUtils.loadImage(this, profileImageUrl, ivAvatar, true);
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvInitial.setVisibility(View.VISIBLE);
            tvInitial.setText(displayName.length() > 0
                    ? String.valueOf(displayName.charAt(0)).toUpperCase() : "?");
        }

        findViewById(R.id.btn_detail_back).setOnClickListener(v -> finish());

        boolean isOrganizer = "organizer".equalsIgnoreCase(role);

        if (isOrganizer) {
            btnRemove.setText("Remove Organizer");
            btnRemove.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Remove Organizer")
                    .setMessage("Remove \"" + displayName + "\"? This will permanently delete their account, "
                            + "all events they created, all associated waiting lists, QR codes, and poster images. "
                            + "All entrants will receive a cancellation notification.")
                    .setPositiveButton("Remove", (dialog, which) -> removeOrganizer(displayName))
                    .setNegativeButton("Cancel", null)
                    .show());
        } else {
            btnRemove.setText("Remove Profile");
            btnRemove.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Remove Profile")
                    .setMessage("Remove \"" + displayName + "\"? This will delete their account and remove them from all waiting lists.")
                    .setPositiveButton("Remove", (dialog, which) -> removeUserProfile(displayName))
                    .setNegativeButton("Cancel", null)
                    .show());
        }
    }

    private void removeOrganizer(String displayName) {
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Invalid profile", Toast.LENGTH_SHORT).show();
            return;
        }
        adminDB.removeOrganizer(
                deviceId,
                unused -> {
                    Toast.makeText(this, displayName + " and all their events removed", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> Toast.makeText(this, "Failed to remove: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private void removeUserProfile(String displayName) {
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Invalid profile", Toast.LENGTH_SHORT).show();
            return;
        }
        adminDB.removeProfile(
                deviceId,
                unused -> {
                    Toast.makeText(this, displayName + " removed", Toast.LENGTH_SHORT).show();
                    finish();
                },
                e -> Toast.makeText(this, "Failed to remove: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
