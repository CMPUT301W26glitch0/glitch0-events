package com.example.cmput301_app.features;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Utility class for generating QR codes for events.
 *
 * Purpose: Generates QR code bitmaps that encode event IDs
 * Organizers can display/share these QR codes for entrants to scan
 *
 */
public class QRCodeGenerator {

    /**
     * Generates a QR code bitmap containing the event ID
     * The QR code encodes a simple string in format: "EVENT:eventId"
     *
     * @param eventId The Firebase document ID of the event
     * @param width Width of the QR code image in pixels
     * @param height Height of the QR code image in pixels
     * @return Bitmap containing the QR code, or null if generation fails
     */
    public static Bitmap generateQRCode(String eventId, int width, int height) {
        if (eventId == null|| eventId.isEmpty()) {
            return null;
        }

        try {
            // Create QR code content with EVENT: prefix
            String qrContent = "EVENT:" + eventId;

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, width, height);

            // convert BitMatrix to Bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a standard 512x512 QR code for an event.
     * Convenience method using default size.
     *
     * @param eventId The Firebase document ID of the event
     * @return Bitmap containing the QR code, or null if generation fails
     */
    public static Bitmap generateQRCode(String eventId) {
        return generateQRCode(eventId, 512, 512);
    }

    /**
     * Validates if a scanned QR code content is a valid event QR code.
     * Checks if content starts with "EVENT:" prefix.
     *
     * @param qrContent The raw content decoded from QR code
     * @return true if content is valid event QR format, false otherwise
     */
    public static boolean isValidEventQR(String qrContent) {
        return qrContent != null && qrContent.startsWith("EVENT:");
    }

    /**
     * Extracts the event ID from a scanned QR code content.
     *
     * @param qrContent The raw content decoded from QR code (format: "EVENT:eventId")
     * @return The event ID string, or null if invalid format
     */
    public static String extractEventId(String qrContent) {
        if (isValidEventQR(qrContent)) {
            return qrContent.substring(6); // Remove "EVENT:" prefix
        }
        return null;
    }
}