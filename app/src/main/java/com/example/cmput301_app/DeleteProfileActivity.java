/**
 * Handles permanent deletion of the current user's account.
 *
 * Presents the user with a confirmation form (they must type "DELETE" to proceed)
 * and a summary of what will be lost. On confirmation, this activity:
 *  - For organizers: deletes all events created by the organizer from Firestore,
 *    then deletes the user document.
 *  - For entrants: deletes only the user document.
 * In both cases the corresponding Firebase Authentication account is deleted and
 * SharedPreferences is cleared, returning the user to the login screen.
 *
 * The role is passed via intent extra "role" ("organizer" or "entrant").
 *
 * Outstanding issues:
 * - Entrant cleanup does not remove the user from waiting lists or
 *   registrationHistory arrays of events they joined.
 */
package com.example.cmput301_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.provider.Settings;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DeleteProfileActivity extends AppCompatActivity {

    private String role;
    private String deviceId;
    private EditText etConfirmDelete;
    private Button btnConfirmDelete;
    private LinearLayout llLossItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_delete_profile);

        role = getIntent().getStringExtra("role");
        if (role == null)
            role = "entrant";

<<<<<<< HEAD
        // Use Android ID as the user identifier — same as MainActivity / ProfileActivity
=======
        // Use Android ID as the user identifier — same as MainActivity /
        // ProfileActivity
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId == null) {
            finish();
            return;
        }

        etConfirmDelete = findViewById(R.id.et_confirm_delete);
        btnConfirmDelete = findViewById(R.id.btn_confirm_delete_profile);
        llLossItems = findViewById(R.id.ll_loss_items);

        View root = findViewById(R.id.delete_profile_root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        setupLossItems();
        setupValidation();

        findViewById(R.id.tv_cancel_delete).setOnClickListener(v -> finish());

        btnConfirmDelete.setOnClickListener(v -> deleteProfile());
    }

    private void setupLossItems() {
        llLossItems.removeAllViews();
        if ("organizer".equalsIgnoreCase(role)) {
            addLossItem(android.R.drawable.ic_menu_agenda, "All active events & lotteries");
            addLossItem(android.R.drawable.ic_menu_gallery, "Uploaded event posters");
            addLossItem(android.R.drawable.ic_menu_myplaces, "Organizer account status");
        } else {
            addLossItem(android.R.drawable.ic_input_add, "All active lottery registrations");
            addLossItem(android.R.drawable.ic_menu_recent_history, "Past event history & receipts");
            addLossItem(android.R.drawable.ic_menu_myplaces, "Verified community status");
        }
    }

    private void addLossItem(int iconRes, String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 12, 0, 12);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        icon.setColorFilter(Color.parseColor("#F04438"));

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#475467"));
        tv.setTextSize(16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(24, 0, 0, 0);
        tv.setLayoutParams(params);

        row.addView(icon);
        row.addView(tv);
        llLossItems.addView(row);
    }

    private void setupValidation() {
        etConfirmDelete.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isValid = "DELETE".equalsIgnoreCase(s.toString().trim());
                btnConfirmDelete.setEnabled(isValid);
                btnConfirmDelete.setAlpha(isValid ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void deleteProfile() {
        btnConfirmDelete.setEnabled(false);
        btnConfirmDelete.setText("Deleting...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("DeleteProfile", "Starting deletion for device: " + deviceId + " role: " + role);

        if ("organizer".equalsIgnoreCase(role)) {
            // For organizers: first delete all events they created, then delete user doc
            db.collection("events")
                    .whereEqualTo("organizerId", deviceId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Task<Void>> deleteTasks = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Log.d("DeleteProfile", "Deleting event: " + doc.getId());
                            deleteTasks.add(db.collection("events").document(doc.getId()).delete());
                        }
                        Log.d("DeleteProfile", "Deleting " + deleteTasks.size() + " events");

                        Tasks.whenAll(deleteTasks)
                                .addOnSuccessListener(aVoid -> deleteUserDocument(db))
                                .addOnFailureListener(e -> {
                                    Log.e("DeleteProfile", "Failed to delete events", e);
                                    // Proceed to delete user doc even if event deletion partially fails
                                    deleteUserDocument(db);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DeleteProfile", "Failed to query events", e);
                        // Proceed to delete user doc even if event query fails
                        deleteUserDocument(db);
                    });
        } else {
            deleteUserDocument(db);
        }
    }

    private void deleteUserDocument(FirebaseFirestore db) {
        db.collection("users").document(deviceId).delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("DeleteProfile", "Firestore user document deleted successfully");
                        finalizeDeletion();
                    } else {
                        Log.e("DeleteProfile", "Firestore deletion failed", task.getException());
                        Toast.makeText(this, "Failed to delete from database. Please check your connection.",
                                Toast.LENGTH_LONG).show();
                        btnConfirmDelete.setEnabled(true);
                        btnConfirmDelete.setText("Delete Profile");
                    }
                });
    }

    private void finalizeDeletion() {
        // Clear local preferences
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        Toast.makeText(this, "Your profile has been removed.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}