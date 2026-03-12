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

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

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

        // Device login now just acts as a shortcut if the user is already signed in
        if (btnDeviceLogin != null) {
            btnDeviceLogin.setOnClickListener(v -> {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    checkUserAndNavigate(user.getUid());
                } else {
                    Toast.makeText(this, "Please log in with your email and password.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RegisterActivity.class)));
    }

    private void checkUserAndNavigate(String uid) {
        mDb.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String role = doc.getString("role");
                Intent intent;
                if ("organizer".equalsIgnoreCase(role)) {
                    intent = new Intent(this, OrganizerDashboardActivity.class);
                } else {
                    intent = new Intent(this, DashboardActivity.class);
                }
                startActivity(intent);
                finish();
            } else {
                // Signed in to Firebase Auth but no Firestore profile — send to register
                mAuth.signOut();
                Toast.makeText(this, "No profile found. Please register.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error loading profile. Check your connection.", Toast.LENGTH_SHORT).show());
    }
}