/**
 * Activity providing a live CameraX preview with real-time QR code detection.
 *
 * Uses the ML Kit Barcode Scanning API to continuously analyse camera frames and
 * automatically recognise QR codes without requiring the user to press a button.
 * When a code matching the {@code "event_details:"} prefix is detected, the
 * activity verifies the event exists in Firestore via EventDB and navigates to
 * EventDetailsActivity.
 *
 * Also supports scanning QR codes from the device gallery via
 * {@code ActivityResultContracts.GetContent}.
 *
 * The CameraX lifecycle is bound to the activity so that camera resources are
 * automatically released when the activity is destroyed.
 *
 * Outstanding issues:
 * - The "History" and "My Entries" bottom buttons are placeholder stubs and
 *   are not yet linked to real functionality.
 */
package com.example.cmput301_app.entrant;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.cmput301_app.ProfileActivity;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.features.QRCodeScanner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity with live CameraX preview and real-time QR code detection.
 * Matches storyboard design with in-app camera feed inside the card.
 *
 * Features:
 * - Live camera preview in card
 * - Real-time QR code detection with ML Kit
 * - Upload from gallery option
 * - Auto-detects QR codes in camera view
 */
public class ScanQRActivity extends AppCompatActivity {

    private static final String TAG = "ScanQRActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private PreviewView previewView;
    private ActivityResultLauncher<String> galleryPicker;
    private EventDB eventDB;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_qr);

        eventDB = new EventDB();
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize ML Kit barcode scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // Get PreviewView
        previewView = findViewById(R.id.camera_preview);

        // Initialize gallery picker
        initializeGalleryPicker();

        // Setup UI elements
        setupButtons();

        // Check camera permission and start camera
        checkCameraPermission();
    }

    /**
     * Check and request camera permission
     */
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required to scan QR codes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Start CameraX with live preview and QR detection
     */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Bind camera preview and image analysis (QR detection)
     */
    @OptIn(markerClass = ExperimentalGetImage.class)
    private void bindCameraPreview(ProcessCameraProvider cameraProvider) {
        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image analysis use case for QR detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        // Camera selector (back camera)
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to camera
            Camera camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis);

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    /**
     * Analyze camera frames for QR codes
     */
    @androidx.camera.core.ExperimentalGetImage
    private void analyzeImage(ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                mediaImage, imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String qrContent = barcode.getRawValue();
                        if (qrContent != null && !isProcessing) {
                            isProcessing = true;
                            runOnUiThread(() -> handleQRScan(qrContent));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Barcode scanning failed", e);
                })
                .addOnCompleteListener(task -> {
                    imageProxy.close();
                });
    }

    /**
     * Setup all button click listeners
     */
    private void setupButtons() {
        // Back button
        ImageView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Help button
        ImageView btnHelp = findViewById(R.id.btn_help);
        btnHelp.setOnClickListener(v -> showHelp());

        // Camera button - just shows toast (camera is already live)
        ImageView btnCamera = findViewById(R.id.btn_camera);
        btnCamera.setOnClickListener(v -> {
            Toast.makeText(this, "Camera is active - point at QR code to scan",
                    Toast.LENGTH_SHORT).show();
        });

        // Upload from Gallery button
        Button btnUploadGallery = findViewById(R.id.btn_upload_from_gallery);
        btnUploadGallery.setOnClickListener(v -> openGallery());

        // History button (placeholder)
        findViewById(R.id.btn_history).setOnClickListener(v -> {
            Toast.makeText(this, "History feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // My Entries button (placeholder)
        findViewById(R.id.btn_my_entries).setOnClickListener(v -> {
            Toast.makeText(this, "My Entries feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Shows help dialog
     */
    private void showHelp() {
        Toast.makeText(this,
                "Point your camera at an event QR code. It will be detected automatically.",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Initializes the gallery image picker
     */
    private void initializeGalleryPicker() {
        galleryPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        processImageFromGallery(uri);
                    }
                }
        );
    }

    /**
     * Opens gallery to select QR image
     */
    private void openGallery() {
        galleryPicker.launch("image/*");
    }

    /**
     * Process image from gallery for QR code
     */
    private void processImageFromGallery(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            InputImage image = InputImage.fromBitmap(bitmap, 0);

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.isEmpty()) {
                            Toast.makeText(this, "No QR code found in image", Toast.LENGTH_LONG).show();
                            isProcessing = false;
                        } else {
                            Barcode barcode = barcodes.get(0);
                            String qrContent = barcode.getRawValue();
                            handleQRScan(qrContent);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        isProcessing = false;
                    });

        } catch (IOException e) {
            Toast.makeText(this, "Error loading image", Toast.LENGTH_LONG).show();
            isProcessing = false;
        }
    }

    /**
     * Handle scanned QR code content
     */
    private void handleQRScan(String qrContent) {
        String eventId = QRCodeScanner.extractEventId(qrContent);

        if (eventId != null) {
            verifyAndNavigateToEvent(eventId);
        } else {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_LONG).show();
            isProcessing = false;
        }
    }

    /**
     * Verify event exists and navigate to details
     */
    private void verifyAndNavigateToEvent(String eventId) {
        eventDB.getEvent(eventId, event -> {
            if (event != null) {
                Intent intent = new Intent(this, EventDetailsActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Event not found", Toast.LENGTH_LONG).show();
                isProcessing = false;
            }
        }, error -> {
            Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            isProcessing = false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }
}