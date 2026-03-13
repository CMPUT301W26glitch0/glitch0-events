package com.example.cmput301_app.features;

import android.app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * Utility class for scanning QR codes using device camera.
 *
 * Purpose: Provides a simple interface for Activities to launch QR scanner
 * and handle scan results. Uses ZXing library for QR code detection
 * Works with the "event_details:eventId" format used by QRDisplayActivity
 *
 */
public class QRCodeScanner {

    /**
     * Creates an ActivityResultLauncher for QR code scanning.
     * This must be called in the Activity's onCreate() method before the Activity is created.
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
                    if (callback != null) {
                        callback.onScanComplete(result);
                    }
                }
        );
    }

    /**
     * Launches the QR code scanner camera interface.
     * Call this when user taps a "Scan QR Code" button.
     *
     * @param launcher The ActivityResultLauncher created by createScanner()
     */
    public static void scan(ActivityResultLauncher<ScanOptions> launcher) {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Scan an event QR code");
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(false);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);

        launcher.launch(options);
    }

    /**
     * Validates if a scanned QR code content is a valid event QR code.
     * Checks for the "event_details:" prefix used by the team's QRDisplayActivity.
     *
     * @param qrContent The raw content decoded from QR code
     * @return true if content is valid event QR format, false otherwise
     */
    public static boolean isValidEventQR(String qrContent) {
        return qrContent != null && qrContent.startsWith("event_details:");
    }

    /**
     * Extracts the event ID from a scanned QR code content.
     * Expects format: "event_details:eventId"
     *
     * @param qrContent The raw content decoded from QR code
     * @return The event ID string, or null if invalid format
     *
     */
    public static String extractEventId(String qrContent) {
        if (isValidEventQR(qrContent)) {
            return qrContent.substring(14);
        }
        return null;
    }

    /**
     * Callback interface for handling QR scan results.
     */
    public interface ScanCallback {
        /**
         * Called when QR scan completes (whether successful or cancelled).
         *
         * @param result ScanIntentResult containing scanned content and metadata.
         *               Will be null if scan was cancelled.
         *               Check result.getContents() for the actual QR code data.
         */
        void onScanComplete(com.journeyapps.barcodescanner.ScanIntentResult result);
    }
}