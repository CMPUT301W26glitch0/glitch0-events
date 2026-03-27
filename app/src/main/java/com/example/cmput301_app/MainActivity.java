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

import com.example.cmput301_app.entrant.DashboardActivity;
import com.example.cmput301_app.organizer.OrganizerDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

        boolean isDarkMode = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                .getBoolean("darkModeEnabled", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

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
                            checkUserAndNavigate(uid);
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(this, "Login Failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        if (btnDeviceLogin != null) {
            btnDeviceLogin.setOnClickListener(v -> {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                String lastUid = prefs.getString("last_uid", null);
                if (lastUid != null) {
                    checkUserAndNavigate(lastUid);
                } else {
                    Toast.makeText(this, "No previous session found. Log in with email first.", Toast.LENGTH_LONG).show();
                }
            });
        }

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }

    private void checkUserAndNavigate(String uid) {
        mDb.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            // Save UID for device login convenience
            getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit().putString("last_uid", uid).apply();

            if (doc.exists()) {
                // Restore dark mode preference from Firestore on login
                Boolean darkMode = doc.getBoolean("darkModeEnabled");
                boolean isDark = darkMode != null && darkMode;
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("darkModeEnabled", isDark).apply();
                AppCompatDelegate.setDefaultNightMode(
                        isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

                String role = doc.getString("role");
                Intent intent;
                if ("organizer".equalsIgnoreCase(role)) {
                    intent = new Intent(this, OrganizerDashboardActivity.class);
                } else if ("admin".equalsIgnoreCase(role)) {
                    intent = new Intent(this, DashboardActivity.class);
                    Toast.makeText(this, "Logged in as Admin", Toast.LENGTH_SHORT).show();
                } else {
                    intent = new Intent(this, DashboardActivity.class);
                }
                startActivity(intent);
                finish();
            } else {
                // No profile found — clear stale SharedPreferences and send to registration
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                        .edit().remove("last_uid").apply();
                mAuth.signOut();
                Toast.makeText(this, "No account found. Please register.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Connection error. Please try again.", Toast.LENGTH_SHORT).show();
        });
    }
}
