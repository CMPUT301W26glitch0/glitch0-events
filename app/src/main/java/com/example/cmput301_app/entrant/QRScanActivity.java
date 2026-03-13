/*
 * Purpose: Activity responsible for opening the camera to scan event QR codes.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
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
