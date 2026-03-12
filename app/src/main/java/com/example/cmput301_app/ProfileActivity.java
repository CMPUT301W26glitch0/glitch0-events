package com.example.cmput301_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

        btnLogout.setOnClickListener(v -> logout());

        if (tvDeleteProfile != null) {
            tvDeleteProfile.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, DeleteProfileActivity.class);
                intent.putExtra("role", userRole);
                startActivity(intent);
            });
        }
    }

    private String getCurrentUid() {
        if (mAuth.getCurrentUser() == null) return null;
        return mAuth.getCurrentUser().getUid();
    }

    private void loadUserData() {
        String uid = getCurrentUid();
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
    }

    private void uploadImageAndSaveProfile() {
        String uid = getCurrentUid();
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
    }

    private void saveChanges(String photoUrl) {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String uid = getCurrentUid();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }
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
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}