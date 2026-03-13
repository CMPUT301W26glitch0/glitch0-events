package com.example.cmput301_app.organizer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.bumptech.glide.Glide;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.model.Event;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class OrganizerEventDetailsActivity extends AppCompatActivity {
    private static final String TAG = "OrganizerEventDetails";
    private TextView tvName, tvCategory, tvDate, tvLocation, tvPrice, tvCapacity, tvRegDates, tvDescription;
    private ImageView ivPoster, ivQrCode;
    private Button btnViewEntrants, btnManageLottery, btnEdit, btnDelete;
    private String eventId;
    private EventDB eventDB;
    private Event currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_event_details);

        eventDB = new EventDB();
        eventId = getIntent().getStringExtra("eventId");

        initViews();
        
        if (eventId != null) {
            loadEventDetails();
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        ivPoster = findViewById(R.id.iv_org_event_poster);
        ivQrCode = findViewById(R.id.iv_org_qr_code);
        tvName = findViewById(R.id.tv_org_event_name);
        tvCategory = findViewById(R.id.tv_org_event_category);
        tvDate = findViewById(R.id.tv_org_date);
        tvLocation = findViewById(R.id.tv_org_location);
        tvPrice = findViewById(R.id.tv_org_price);
        tvCapacity = findViewById(R.id.tv_org_capacity);
        tvRegDates = findViewById(R.id.tv_org_reg_dates);
        tvDescription = findViewById(R.id.tv_org_description);

        btnViewEntrants = findViewById(R.id.btn_view_entrants);
        btnManageLottery = findViewById(R.id.btn_manage_lottery);
        btnEdit = findViewById(R.id.btn_edit_event_details);
        btnDelete = findViewById(R.id.btn_delete_event_details);

        findViewById(R.id.btn_org_back).setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizerDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateEventActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> {
            eventDB.deleteEvent(eventId, aVoid -> {
                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                finish();
            }, e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
        });

        btnViewEntrants.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantListActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        findViewById(R.id.btn_share_qr_details).setOnClickListener(v -> {
            Toast.makeText(this, "QR Sharing not implemented yet", Toast.LENGTH_SHORT).show();
        });

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
    }

    private void loadEventDetails() {
        eventDB.getEvent(eventId, event -> {
            if (event != null) {
                currentEvent = event;
                updateUI();
            }
        }, e -> {
            Log.e(TAG, "Failed to load event", e);
            Toast.makeText(this, "Failed to load event details", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUI() {
        tvName.setText(currentEvent.getName());
        tvCategory.setText(currentEvent.getCategory() != null ? currentEvent.getCategory().toUpperCase() : "GENERAL");
        tvLocation.setText(currentEvent.getLocation());
        tvPrice.setText(String.format(Locale.getDefault(), "$%.2f", currentEvent.getPrice()));
        tvCapacity.setText(String.valueOf(currentEvent.getCapacity()));
        tvDescription.setText(currentEvent.getDescription());

        if (currentEvent.getPosterUrl() != null && !currentEvent.getPosterUrl().isEmpty()) {
            Glide.with(this).load(currentEvent.getPosterUrl()).into(ivPoster);
        }

        // Generate and display QR Code
        if (currentEvent.getQrCode() != null) {
            generateQRCode(currentEvent.getQrCode());
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat fullFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        if (currentEvent.getDate() != null) {
            tvDate.setText(dateFormat.format(currentEvent.getDate().toDate()));
        }

        if (currentEvent.getRegistrationOpen() != null && currentEvent.getRegistrationClose() != null) {
            String open = fullFormat.format(currentEvent.getRegistrationOpen().toDate());
            String close = fullFormat.format(currentEvent.getRegistrationClose().toDate());
            tvRegDates.setText(open + " - " + close);
        }
    }

    private void generateQRCode(String data) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 400, 400);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Log.e(TAG, "QR Generation failed", e);
        }
    }
}
