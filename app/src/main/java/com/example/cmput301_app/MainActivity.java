package com.example.cmput301_app;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        String extracted = com.example.cmput301_app.features.QRCodeGenerator.extractEventId("EVENT:event-test-123");
        android.util.Log.d("QR", "Extracted ID: " + extracted);
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

        // Check if user is already logged in (standard Firebase session)
        if (mAuth.getCurrentUser() != null) {
            navigateToDashboard();
            return;
        }

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

        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        Button btnDeviceLogin = findViewById(R.id.btn_device_login);
        TextView tvRegisterLink = findViewById(R.id.tv_register_link);

        // Standard Login
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Link device ID upon successful manual login
                            linkDeviceIdToUser(mAuth.getCurrentUser().getUid());
                            navigateToDashboard();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(MainActivity.this, "Login Failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Device ID Login
        btnDeviceLogin.setOnClickListener(v -> {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            
            mDb.collection("users")
                    .whereEqualTo("deviceId", deviceId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // User found with this device ID
                            // if not you have to login at least once.
                            Toast.makeText(MainActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            navigateToDashboard();
                        } else {
                            Toast.makeText(MainActivity.this, "No account linked to this device. Please log in manually once.", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }

    private void linkDeviceIdToUser(String userId) {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        mDb.collection("users").document(userId).update("deviceId", deviceId);
    }

    private void navigateToDashboard() {
        startActivity(new Intent(MainActivity.this, DashboardActivity.class));
        finish();
    }
}