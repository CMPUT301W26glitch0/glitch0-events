/*
 * Purpose: Manages user profile viewing and editing.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Patterns;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

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
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

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
    private SwitchMaterial switchNotifications;
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
        switchNotifications = findViewById(R.id.switch_notifications);

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
     *  2nd priority: SharedPreferences "last_uid" (device login)
     *  If neither resolves, the callback receives null and the caller should call logout().
     */
    private void resolveUid(java.util.function.Consumer<String> callback) {
        if (mAuth.getCurrentUser() != null) {
            callback.accept(mAuth.getCurrentUser().getUid());
            return;
        }
        String lastUid = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("last_uid", null);
        callback.accept(lastUid);
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

                    Boolean notifs = documentSnapshot.getBoolean("notificationsEnabled");
                    if (notifs != null) {
                        switchNotifications.setChecked(notifs);
                    } else {
                        switchNotifications.setChecked(true);
                    }

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

            // Read all bytes on the main thread (where the content URI grant is valid),
            // then upload via putBytes() to avoid -13010 errors from background thread URI access.
            byte[] imageBytes;
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                if (is == null) {
                    Toast.makeText(this, "Could not read image", Toast.LENGTH_SHORT).show();
                    return;
                }
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(chunk)) != -1) buffer.write(chunk, 0, bytesRead);
                is.close();
                imageBytes = buffer.toByteArray();
            } catch (Exception e) {
                Toast.makeText(this, "Could not read image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            StorageReference storageRef = mStorage.getReference().child("profile_pictures/" + uid + ".jpg");
            btnSave.setEnabled(false);
            btnSave.setText("Uploading...");
            storageRef.putBytes(imageBytes).addOnSuccessListener(taskSnapshot ->
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> saveChanges(uri.toString()))
            ).addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                btnSave.setText("Save Changes");
                android.util.Log.e("ProfileActivity", "Storage upload failed", e);
                Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                    "profileImageUrl", photoUrl != null ? photoUrl : "",
                    "notificationsEnabled", switchNotifications.isChecked()
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