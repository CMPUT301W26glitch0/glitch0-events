package com.example.cmput301_app;

import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;
    private FirebaseStorage mStorage;
    private EditText etName, etEmail, etPhone;
    private ImageView ivProfile;
    private Button btnSave, btnLogout;
    private View btnBack;
    private Uri imageUri;

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
        mDb = FirebaseFirestore.getInstance();
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
            mAuth.signOut();
            finish();
        });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
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
        }
    }

    private void uploadImageAndSaveProfile() {
        String userId = mAuth.getCurrentUser().getUid();
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
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Required fields missing", Toast.LENGTH_SHORT).show();
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
        }
    }
}