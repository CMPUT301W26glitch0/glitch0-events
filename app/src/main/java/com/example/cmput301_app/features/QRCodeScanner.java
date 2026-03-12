package com.example.cmput301_app.features;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * Utility class for scanning QR codes using device camera.
 *
 * Purpose: Provides a simple interface for Activities to launch QR scanner
 * and handle scan results. Uses ZXing library for QR code detection.
 *
 */
public class QRCodeScanner {

    /**
     * Creates an ActivityResultLauncher for QR code scanning.
     * This should be called in the Activity's onCreate() method.
     *
     * @param activity The Activity that will handle scan results
     * @param callback Callback interface that receives scan results
     * @return ActivityResultLauncher configured for QR scanning
     */
    public static ActivityResultLauncher<ScanOptions> createScanner(
            Activity activity,
            ScanCallback callback) {

        return ((androidx.activity.ComponentActivity) activity).registerForActivityResult(
                new ScanContract(),
                result -> {
                    if (callback!= null) {
                        callback.onScanComplete(result);
                    }
                }
        );
    }

    /**
     * Launches the QR code scanner
     * Call this when user taps "Scan QR Code" button.
     *
     * @param launcher The ActivityResultLauncher created by createScanner()
     */
    public static void scan(ActivityResultLauncher<ScanOptions> launcher) {
        ScanOptions options= new ScanOptions();
        options.setPrompt("Scan an event QR code");
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(false);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);

        launcher.launch(options);
    }

    /**
     * Callback interface for handling QR scan results
     */
    public interface ScanCallback {
        /**
         * Called when QR scan completes(success or cancelled).
         *
         * @param result ScanIntentResult containing scanned content or null if scan was cancelled
         *
         */
        void onScanComplete(com.journeyapps.barcodescanner.ScanIntentResult result);
    }
}