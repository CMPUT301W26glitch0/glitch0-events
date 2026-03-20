/*
 * Purpose: Handles new user registration and account creation.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app;

import android.content.Intent;
import android.os.Bundle;
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

        String[] roles = {"Entrant", "Organizer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spinnerRole != null) spinnerRole.setAdapter(adapter);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String role = spinnerRole != null ? spinnerRole.getSelectedItem().toString() : "Entrant";

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // checks if inputted email address is in proper format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRegister.setEnabled(false);

            // Sign out any previous session before creating a new account
            mAuth.signOut();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Use Firebase Auth UID as the Firestore document key
                            String uid = mAuth.getCurrentUser().getUid();

                            if ("Organizer".equals(role)) {
                                Organizer organizer = new Organizer(uid, name, email, null);
                                organizerDB.createOrganizer(organizer, aVoid -> {
                                    Toast.makeText(this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                                    navigateToDashboard("Organizer");
                                }, e -> {
                                    btnRegister.setEnabled(true);
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            } else {
                                Entrant entrant = new Entrant(uid, name, email, null);
                                entrantDB.createEntrant(entrant, aVoid -> {
                                    Toast.makeText(this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                                    navigateToDashboard("Entrant");
                                }, e -> {
                                    btnRegister.setEnabled(true);
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        } else {
                            btnRegister.setEnabled(true);
                            String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(this, "Registration Failed: " + msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
<<<<<<< HEAD

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("Organizer".equals(role)) {
            intent = new Intent(this, OrganizerDashboardActivity.class);
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }
=======
//
    private void navigateToDashboard() {
        Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
>>>>>>> e647fe8311104827fc8216cccd333b98ab890bef
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}