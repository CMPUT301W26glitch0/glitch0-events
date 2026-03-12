package com.example.cmput301_app.organizer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.model.Event;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class QRDisplayActivity extends AppCompatActivity {

    private String eventId;
    private EventDB eventDB;
    private ImageView ivQrCode, ivPreviewPoster;
    private TextView tvEventTitle, tvPreviewName, tvPreviewDesc, tvPreviewLocation, tvEventId, tvEventIdSecondary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_display);

        eventId = getIntent().getStringExtra("eventId");
        eventDB = new EventDB();

        ivQrCode = findViewById(R.id.iv_qr_code_large);
        ivPreviewPoster = findViewById(R.id.iv_preview_poster);
        tvEventTitle = findViewById(R.id.tv_qr_title);
        tvPreviewName = findViewById(R.id.tv_preview_name);
        tvPreviewDesc = findViewById(R.id.tv_preview_desc);
        tvPreviewLocation = findViewById(R.id.tv_preview_location);
        tvEventId = findViewById(R.id.tv_qr_event_id);
        tvEventIdSecondary = findViewById(R.id.tv_qr_event_id_secondary);

        findViewById(R.id.btn_qr_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_download_qr).setOnClickListener(v -> Toast.makeText(this, "QR Code downloaded to gallery", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_share_qr).setOnClickListener(v -> Toast.makeText(this, "Sharing options opened", Toast.LENGTH_SHORT).show());

        if (eventId != null) {
            loadEventData();
        }
    }

    private void loadEventData() {
        eventDB.getEvent(eventId, event -> {
            if (event != null) {
                tvEventTitle.setText("QR Code for " + event.getName());
                tvPreviewName.setText(event.getName());
                tvPreviewDesc.setText(event.getDescription());
                tvPreviewLocation.setText(event.getLocation());
                
                if (tvEventId != null) tvEventId.setText(eventId);
                if (tvEventIdSecondary != null) tvEventIdSecondary.setText("ID: " + eventId);
                
                if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                    Glide.with(this).load(event.getPosterUrl()).into(ivPreviewPoster);
                }

                generateQR("event_details:" + eventId);
            }
        }, e -> Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }

    private void generateQR(String data) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 500, 500);
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
            e.printStackTrace();
        }
    }
}