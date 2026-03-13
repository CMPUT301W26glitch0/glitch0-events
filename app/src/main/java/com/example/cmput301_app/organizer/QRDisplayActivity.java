package com.example.cmput301_app.organizer;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import java.io.OutputStream;

/**
 * Activity to display and download event QR codes.
 */
public class QRDisplayActivity extends AppCompatActivity {

    private String eventId;
    private String eventName;
    private EventDB eventDB;
    private ImageView ivQrCode, ivPreviewPoster;
    private TextView tvEventTitle, tvPreviewName, tvPreviewDesc, tvPreviewLocation, tvEventId, tvEventIdSecondary;
    private Bitmap qrCodeBitmap;

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
        findViewById(R.id.btn_download_qr).setOnClickListener(v -> downloadQRCode());
        findViewById(R.id.btn_share_qr).setOnClickListener(v ->
                Toast.makeText(this, "Share feature coming soon", Toast.LENGTH_SHORT).show());

        if (eventId != null) {
            loadEventData();
        }
    }

    private void loadEventData() {
        eventDB.getEvent(eventId, event -> {
            if (event != null) {
                eventName = event.getName();
                tvEventTitle.setText("QR Code for " + eventName);
                tvPreviewName.setText(eventName);
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
            qrCodeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrCodeBitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ivQrCode.setImageBitmap(qrCodeBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Download QR code to gallery
     */
    private void downloadQRCode() {
        if (qrCodeBitmap == null) {
            Toast.makeText(this, "QR code not generated yet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = "QR_" + (eventName != null ? eventName.replaceAll("[^a-zA-Z0-9]", "_") : "Event")
                    + "_" + System.currentTimeMillis() + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EventQRCodes");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(this, "QR code saved to Gallery", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Failed to save QR code", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving QR code: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}