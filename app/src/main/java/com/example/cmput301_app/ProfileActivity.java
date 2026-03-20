/*
 * Purpose: Manages user profile viewing and editing.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Patterns;
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
import com.example.cmput301_app.database.OrganizerDB;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EntrantDB entrantDB;
    private OrganizerDB organizerDB;
    private FirebaseStorage mStorage;
    private FirebaseFirestore db;
    private EditText etName, etEmail, etPhone;
    private ImageView ivProfile;
    private Button btnSave, btnLogout;
    private TextView tvDeleteProfile;
    private View btnBack;
    private Uri imageUri;
    private String userRole = "entrant"; // Default

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
        organizerDB = new OrganizerDB();
        mStorage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserData();
    }

    private void initViews() {
        etName = findViewById(R.id.et_profile_name);
        etEmail = findViewById(R.id.et_profile_email);
        etPhone = findViewById(R.id.et_profile_phone);
        ivProfile = findViewById(R.id.iv_profile_pic);
        btnSave = findViewById(R.id.btn_save_profile);
        btnLogout = findViewById(R.id.btn_logout);
        btnBack = findViewById(R.id.btn_profile_back);
        tvDeleteProfile = findViewById(R.id.tv_delete_profile_btn);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View.OnClickListener pickImageListener = v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        };
        ivProfile.setOnClickListener(pickImageListener);
        findViewById(R.id.fab_edit_profile_pic).setOnClickListener(pickImageListener);

        btnSave.setOnClickListener(v -> {
            if (imageUri != null) uploadImageAndSaveProfile();
            else saveChanges(null);
        });

<<<<<<< HEAD
        btnLogout.setOnClickListener(v -> logout());

        if (tvDeleteProfile != null) {
            tvDeleteProfile.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, DeleteProfileActivity.class);
                intent.putExtra("role", userRole);
                startActivity(intent);
            });
        }
    }

    /** Resolves the current user's Firestore document ID.
     *  1st priority: Firebase Auth UID (email/password login)
     *  2nd priority: Firestore query by deviceId == ANDROID_ID (device login)
     *  If neither resolves, the callback receives null and the caller should call logout().
     */
    private void resolveUid(java.util.function.Consumer<String> callback) {
        if (mAuth.getCurrentUser() != null) {
            callback.accept(mAuth.getCurrentUser().getUid());
            return;
        }
        String androidId = android.provider.Settings.Secure.getString(
                getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        db.collection("users")
                .whereEqualTo("deviceId", androidId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        callback.accept(snap.getDocuments().get(0).getId());
                    } else {
                        callback.accept(null);
                    }
                })
                .addOnFailureListener(e -> callback.accept(null));
    }

    private void loadUserData() {
        resolveUid(uid -> {
            if (uid == null) { logout(); return; }
            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userRole = documentSnapshot.getString("role");
                    etName.setText(documentSnapshot.getString("name"));
                    etEmail.setText(documentSnapshot.getString("email"));
                    etPhone.setText(documentSnapshot.getString("phoneNumber"));

                    String photoUrl = documentSnapshot.getString("profileImageUrl");
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this).load(photoUrl).circleCrop().into(ivProfile);
                    }
                }
            });
        });
    }

    private void uploadImageAndSaveProfile() {
        resolveUid(uid -> {
            if (uid == null) { logout(); return; }
            StorageReference storageRef = mStorage.getReference().child("profile_pictures/" + uid + ".jpg");
            btnSave.setEnabled(false);
            btnSave.setText("Uploading...");
            storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> saveChanges(uri.toString()))
            ).addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
                Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void saveChanges(String photoUrl) {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
=======
        btnLogout.setOnClickListener(v -> {
            // 1. Sign out of Firebase
            mAuth.signOut();
            
            // 2. Clear local storage (Device ID login session)
            SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            prefs.edit().remove("user_uid").apply();
            
            // 3. Clear activity task and redirect to Login
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        // Check for either Firebase Auth user or locally saved UID (for device login)
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String userId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : prefs.getString("user_uid", null);

        if (userId != null) {
            mDb.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phone = documentSnapshot.getString("phone");
                            String photoUrl = documentSnapshot.getString("profilePictureUrl");

                            if (name != null) etName.setText(name);
                            if (email != null) etEmail.setText(email);
                            if (phone != null) etPhone.setText(phone);
                            
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(this).load(photoUrl).circleCrop().into(ivProfile);
                            }
                        }
                    });
        } else {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void uploadImageAndSaveProfile() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String userId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : prefs.getString("user_uid", null);
        
        if (userId == null) return;

        StorageReference storageRef = mStorage.getReference().child("profile_pictures/" + userId + ".jpg");

        btnSave.setEnabled(false);
        btnSave.setText("Uploading...");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveChanges(uri.toString());
                }))
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                    Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveChanges(String photoUrl) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String userId = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : prefs.getString("user_uid", null);

        if (userId != null) {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Required fields missing", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("email", email);
            updates.put("phone", phone);
            if (photoUrl != null) updates.put("profilePictureUrl", photoUrl);

            mDb.collection("users").document(userId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                    });
>>>>>>> e647fe8311104827fc8216cccd333b98ab890bef
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        resolveUid(uid -> {
            if (uid == null) { logout(); return; }
            db.collection("users").document(uid).update(
                    "name", name,
                    "email", email,
                    "phoneNumber", phone,
                    "profileImageUrl", photoUrl != null ? photoUrl : ""
            ).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}