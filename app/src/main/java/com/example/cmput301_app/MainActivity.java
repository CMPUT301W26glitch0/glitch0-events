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
                // Login was successful in Auth, but profile doc is missing.
                // Redirect to dashboard (entrant view) so they can at least see the app
                // and potentially fill out their profile.
                Toast.makeText(this, "Login successful. No profile doc found.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            }
        }).addOnFailureListener(e -> {
            // Fallback for connectivity issues
            if (mAuth.getCurrentUser() != null) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
            }
        });
    }
}
