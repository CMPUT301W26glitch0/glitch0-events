package com.example.cmput301_app;

import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class    RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

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
        Button btnRegister = findViewById(R.id.btn_register);
        TextView tvLoginLink = findViewById(R.id.tv_login_link);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

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
                            String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                            if (userId != null) {
                                Map<String, Object> user = new HashMap<>();
                                user.put("name", name);
                                user.put("email", email);
                                user.put("role", "organizer");

                                mDb.collection("users").document(userId)
                                        .set(user)
                                        .addOnCompleteListener(firestoreTask -> {
                                            if (firestoreTask.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "Profile error: " + (firestoreTask.getException() != null ? firestoreTask.getException().getMessage() : "Unknown"), Toast.LENGTH_LONG).show();
                                            }
                                            navigateToDashboard();
                                        });
                            } else {
                                navigateToDashboard();
                            }
                        } else {
                            btnRegister.setEnabled(true);
                            String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}