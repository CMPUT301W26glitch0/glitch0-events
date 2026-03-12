package com.example.cmput301_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.cmput301_app.database.EntrantDB;
import com.example.cmput301_app.model.Entrant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EntrantDB entrantDB;
    private FirebaseStorage mStorage;
    private EditText etName, etEmail, etPhone;
    private ImageView ivProfile;
    private Button btnSave, btnLogout;
    private View btnBack;
    private Uri imageUri;
    private Entrant currentEntrant;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(ivProfile);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        entrantDB = new EntrantDB();
        mStorage = FirebaseStorage.getInstance();

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etName = findViewById(R.id.et_profile_name);
        etEmail = findViewById(R.id.et_profile_email);
        etPhone = findViewById(R.id.et_profile_phone);
        ivProfile = findViewById(R.id.iv_profile_pic);
        btnSave = findViewById(R.id.btn_save_profile);
        btnLogout = findViewById(R.id.btn_logout);
        btnBack = findViewById(R.id.btn_profile_back);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View.OnClickListener pickImageListener = v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        };

        ivProfile.setOnClickListener(pickImageListener);
        findViewById(R.id.fab_edit_profile_pic).setOnClickListener(pickImageListener);

        loadUserData();

        btnSave.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImageAndSaveProfile();
            } else {
                saveChanges(null);
            }
        });

        btnLogout.setOnClickListener(v -> {
            // 1. Sign out of Firebase
            mAuth.signOut();
            
            // 2. Set logged out status in SharedPreferences
            SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            prefs.edit().putBoolean("is_logged_out", true).apply();
            
            // 3. Redirect to MainActivity
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        entrantDB.getEntrant(deviceId, entrant -> {
            if (entrant != null) {
                currentEntrant = entrant;
                etName.setText(entrant.getName());
                etEmail.setText(entrant.getEmail());
                etPhone.setText(entrant.getPhoneNumber());
                
                String photoUrl = entrant.getProfileImageUrl();
                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(ProfileActivity.this).load(photoUrl).circleCrop().into(ivProfile);
                }
            } else {
                Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
            }
        }, e -> {
            Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadImageAndSaveProfile() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        StorageReference storageRef = mStorage.getReference().child("profile_pictures/" + deviceId + ".jpg");

        btnSave.setEnabled(false);
        btnSave.setText("Uploading...");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveChanges(uri.toString());
                }))
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                    Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveChanges(String photoUrl) {
        if (currentEntrant == null) return;

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        currentEntrant.setName(name);
        currentEntrant.setEmail(email);
        currentEntrant.setPhoneNumber(phone);
        if (photoUrl != null) {
            currentEntrant.setProfileImageUrl(photoUrl);
        }

        entrantDB.updateEntrant(currentEntrant, aVoid -> {
            Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            btnSave.setText("Save Changes");
            finish();
        }, e -> {
            btnSave.setEnabled(true);
            btnSave.setText("Save Changes");
            Toast.makeText(ProfileActivity.this, "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}