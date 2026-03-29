/*
 * Purpose: Main entry point of the application, routing users to their respective dashboards based on roles.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cmput301_app.admin.AdminDashboardActivity;
import com.example.cmput301_app.entrant.DashboardActivity;
import com.example.cmput301_app.organizer.OrganizerDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
<<<<<<< HEAD
import com.google.firebase.auth.FirebaseUser;
=======
import com.google.firebase.firestore.DocumentSnapshot;
>>>>>>> e647fe8311104827fc8216cccd333b98ab890bef
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

<<<<<<< HEAD
<<<<<<< HEAD
=======
        // Check if user is already logged in via standard Auth or Saved Device ID
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String savedUid = prefs.getString("user_uid", null);

        if (mAuth.getCurrentUser() != null || savedUid != null) {
            navigateToDashboard();
            return;
        }

>>>>>>> e647fe8311104827fc8216cccd333b98ab890bef
=======
        boolean isDarkMode = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getBoolean("darkModeEnabled", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initUI();

        // Auto-login: if Firebase still has an active session, skip the login screen
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserAndNavigate(currentUser.getUid());
        }
    }

    private void initUI() {
        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnDeviceLogin = findViewById(R.id.btn_device_login);
        TextView tvRegisterLink = findViewById(R.id.tv_register_link);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                String email = etEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(this, "Enter your email above first", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(unused ->
                                Toast.makeText(this, "Reset email sent to " + email, Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            });
        }

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            // Don't trim passwords as spaces can be valid
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        btnLogin.setEnabled(true);
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
<<<<<<< HEAD
                            checkUserAndNavigate(uid);
=======
                            saveUserLocally(uid);
                            linkDeviceIdToUser(uid);
                            navigateToDashboard();
>>>>>>> e647fe8311104827fc8216cccd333b98ab890bef
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(this, "Login Failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

<<<<<<< HEAD
<<<<<<< HEAD
        // Device login: use the last-used UID stored locally in SharedPreferences.
        // This is set on every successful login (email/password or device), so it
        // always refers to whoever most recently used the app on this device.
        if (btnDeviceLogin != null) {
            btnDeviceLogin.setOnClickListener(v -> {
                android.content.SharedPreferences prefs =
                        getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
=======
        if (btnDeviceLogin != null) {
            btnDeviceLogin.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
                String lastUid = prefs.getString("last_uid", null);
                if (lastUid != null) {
                    checkUserAndNavigate(lastUid);
                } else {
<<<<<<< HEAD
                    Toast.makeText(this,
                            "No previous session found. Please log in with your email and password first.",
                            Toast.LENGTH_LONG).show();
=======
                    Toast.makeText(this, "No previous session found. Log in with email first.", Toast.LENGTH_LONG).show();
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
                }
            });
        }
=======
        btnDeviceLogin.setOnClickListener(v -> {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            
            mDb.collection("users")
                    .whereEqualTo("deviceId", deviceId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                            saveUserLocally(userDoc.getId());
                            Toast.makeText(MainActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            navigateToDashboard();
                        } else {
                            Toast.makeText(MainActivity.this, "No account linked to this device. Please log in manually once.", Toast.LENGTH_LONG).show();
                        }
                    });
        });
>>>>>>> e647fe8311104827fc8216cccd333b98ab890bef

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }

<<<<<<< HEAD
    private void checkUserAndNavigate(String uid) {
        mDb.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            // Save UID for device login convenience
            getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit().putString("last_uid", uid).apply();

            if (doc.exists()) {
<<<<<<< HEAD
                // Persist this UID locally so Device ID login works after logout
                getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                        .edit().putString("last_uid", uid).apply();
=======
    private void saveUserLocally(String uid) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("user_uid", uid).apply();
    }

    private void linkDeviceIdToUser(String userId) {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        mDb.collection("users").document(userId).update("deviceId", deviceId);
    }
>>>>>>> e647fe8311104827fc8216cccd333b98ab890bef
=======
                // Restore dark mode preference from Firestore on login
                Boolean darkMode = doc.getBoolean("darkModeEnabled");
                boolean isDark = darkMode != null && darkMode;
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("darkModeEnabled", isDark).apply();
                AppCompatDelegate.setDefaultNightMode(
                        isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a

                String role = doc.getString("role");
                Intent intent;
                if ("organizer".equalsIgnoreCase(role)) {
                    intent = new Intent(this, OrganizerDashboardActivity.class);
                } else if ("admin".equalsIgnoreCase(role)) {
                    intent = new Intent(this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(this, DashboardActivity.class);
                }
                startActivity(intent);
                finish();
            } else {
<<<<<<< HEAD
=======
                // No profile found — clear stale SharedPreferences and send to registration
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .edit().remove("last_uid").apply();
>>>>>>> 2df83395a475e1f465ca98b60788454a30b2549a
                mAuth.signOut();
                Toast.makeText(this, "No account found. Please register.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Connection error. Please try again.", Toast.LENGTH_SHORT).show();
        });
    }
}
