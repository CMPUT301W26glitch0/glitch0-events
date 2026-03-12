package com.example.cmput301_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.cmput301_app.organizer.OrganizerDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
   // Firebase Authentication and Firestore instances
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

        // Check for manual logout flag
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean isLoggedOut = prefs.getBoolean("is_logged_out", false);

        // Auto-login only if user didn't manually logout
        if (!isLoggedOut) {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            checkUserAndNavigate(deviceId, false);
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
                Toast.makeText(MainActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            setLoggedOutStatus(false);
                            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                            checkUserAndNavigate(deviceId, true);
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(MainActivity.this, "Login Failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnDeviceLogin.setOnClickListener(v -> {
            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            setLoggedOutStatus(false);
            checkUserAndNavigate(deviceId, true);
        });

        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }

    private void setLoggedOutStatus(boolean status) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("is_logged_out", status).apply();
    }

    private void checkUserAndNavigate(String deviceId, boolean showToastOnFail) {
        mDb.collection("users").document(deviceId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String role = documentSnapshot.getString("role");
                Intent intent;
                

                if ("organizer".equalsIgnoreCase(role)) {
                    intent = new Intent(MainActivity.this, OrganizerDashboardActivity.class);
                } else {
                    intent = new Intent(MainActivity.this, DashboardActivity.class);
                }
                startActivity(intent);
                finish();
            } else if (showToastOnFail) {
                Toast.makeText(this, "No profile found for this device. Please register.", Toast.LENGTH_LONG).show();
            }
        });
    }
}