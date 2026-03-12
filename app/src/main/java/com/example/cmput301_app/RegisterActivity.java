package com.example.cmput301_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cmput301_app.database.EntrantDB;
import com.example.cmput301_app.database.OrganizerDB;
import com.example.cmput301_app.entrant.DashboardActivity;
import com.example.cmput301_app.organizer.OrganizerDashboardActivity;
import com.example.cmput301_app.model.Entrant;
import com.example.cmput301_app.model.Organizer;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EntrantDB entrantDB;
    private OrganizerDB organizerDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        entrantDB = new EntrantDB();
        organizerDB = new OrganizerDB();

        View registerMain = findViewById(R.id.register_main);
        if (registerMain != null) {
            ViewCompat.setOnApplyWindowInsetsListener(registerMain, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        EditText etName = findViewById(R.id.et_name);
        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        Spinner spinnerRole = findViewById(R.id.spinner_role);
        Button btnRegister = findViewById(R.id.btn_register);
        TextView tvLoginLink = findViewById(R.id.tv_login_link);

        // Setup Role Spinner
        String[] roles = {"Entrant", "Organizer", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerRole != null) spinnerRole.setAdapter(adapter);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String role = spinnerRole != null ? spinnerRole.getSelectedItem().toString() : "Entrant";

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            String firebaseUid = mAuth.getCurrentUser().getUid();
                            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                            
                            if ("Organizer".equals(role)) {
                                Organizer organizer = new Organizer(deviceId, name, email, null);
                                organizerDB.createOrganizer(organizer, aVoid -> {
                                    saveUserLocally(firebaseUid, deviceId);
                                    Toast.makeText(RegisterActivity.this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                                    navigateToDashboard("Organizer");
                                }, e -> {
                                    btnRegister.setEnabled(true);
                                    Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            } else {
                                Entrant entrant = new Entrant(deviceId, name, email, null);
                                entrantDB.createEntrant(entrant, aVoid -> {
                                    saveUserLocally(firebaseUid, deviceId);
                                    Toast.makeText(RegisterActivity.this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                                    navigateToDashboard("Entrant");
                                }, e -> {
                                    btnRegister.setEnabled(true);
                                    Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        } else {
                            btnRegister.setEnabled(true);
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });
    }

    private void saveUserLocally(String uid, String deviceId) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        prefs.edit()
            .putString("user_uid", uid)
            .putString("user_device_id", deviceId)
            .putBoolean("is_logged_out", false) // Ensure auto-login works after registration
            .apply();
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("Organizer".equals(role)) {
            intent = new Intent(RegisterActivity.this, OrganizerDashboardActivity.class);
        } else {
            intent = new Intent(RegisterActivity.this, DashboardActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}