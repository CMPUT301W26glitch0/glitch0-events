/**
 * Lightweight activity that opens the Google Play Services barcode scanner to
 * let an entrant scan an event QR code.
 *
 * On a successful scan it checks whether the raw value starts with the prefix
 * {@code "event_details:"} and, if so, launches {@link EventDetailsActivity}
 * with the extracted event ID. Any unrecognised QR content is silently rejected
 * with a Toast message.
 *
 * This activity finishes immediately after the scan result is handled.
 *
 * Outstanding issues:
 * - This activity is currently superseded by {@link ScanQRActivity}, which
 *   uses a live CameraX preview. The two implementations should be consolidated.
 */
package com.example.cmput301_app.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

/**
 * Launches the Google Play Services QR scanner and handles the result.
 * If the scanned QR code starts with "event_details:", it opens EventDetailsActivity.
 */
public class QRScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String raw = barcode.getRawValue();
                    if (raw != null && raw.startsWith("event_details:")) {
                        String eventId = raw.substring("event_details:".length());
                        Intent intent = new Intent(this, EventDetailsActivity.class);
                        intent.putExtra("eventId", eventId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Unrecognized QR code", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                })
                .addOnCanceledListener(this::finish)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Scan failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
