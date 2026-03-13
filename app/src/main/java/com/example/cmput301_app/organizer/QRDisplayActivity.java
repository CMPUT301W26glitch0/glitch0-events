/**
 * Displays the QR code for a specific event and allows the organizer to
 * download or share it.
 *
 * The activity receives an {@code eventId} via intent extra, loads the event
 * from Firestore via EventDB, and generates a QR code bitmap on-device from
 * the string {@code "event_details:<eventId>"} using the ZXing library.
 *
 * The organizer can:
 *  - Download: saves the QR code to the device gallery under
 *    {@code Pictures/EventQRCodes/} via the MediaStore API (no permission
 *    required on API 29+).
 *  - Share: writes the bitmap to app cache and shares it via the Android
 *    share sheet using FileProvider.
 *
 * Outstanding issues: None.
 */
package com.example.cmput301_app.organizer;

import android.content.ContentValues;
import android.content.Intent;
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
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
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
    private Bitmap qrBitmap;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_qr_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_download_qr).setOnClickListener(v -> downloadQR());
        findViewById(R.id.btn_share_qr).setOnClickListener(v -> shareQR());

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

                qrBitmap = generateQR("event_details:" + eventId);
                if (qrBitmap != null) ivQrCode.setImageBitmap(qrBitmap);
            }
        }, e -> Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }

    private Bitmap generateQR(String data) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 500, 500);
            int w = bitMatrix.getWidth(), h = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            for (int x = 0; x < w; x++)
                for (int y = 0; y < h; y++)
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Saves QR to the device gallery using MediaStore (API 29+, no permission needed). */
    private void downloadQR() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "event_qr_" + eventId + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/EventQRCodes");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                // Mark as done so gallery can see it
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);

                Toast.makeText(this, "✅ QR Code saved to gallery!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** Shares QR via Android share sheet using FileProvider (works with all apps). */
    private void shareQR() {
        if (qrBitmap == null) {
            Toast.makeText(this, "QR not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Write to app cache so FileProvider can share it
            File qrDir = new File(getCacheDir(), "qr_codes");
            if (!qrDir.exists()) qrDir.mkdirs();

            File qrFile = new File(qrDir, "event_qr_" + eventId + ".png");
            try (FileOutputStream fos = new FileOutputStream(qrFile)) {
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }

            Uri shareUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    qrFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Scan this QR code to view the event!");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));

        } catch (Exception e) {
            Toast.makeText(this, "Share failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}