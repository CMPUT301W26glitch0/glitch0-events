/*
 * Purpose: Displays a map with pins showing the locations from which entrants joined
 *          an event's waiting list. Only available for events with geolocation enabled.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.organizer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cmput301_app.R;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WaitingListMapActivity extends AppCompatActivity {

    private MapView mapView;
    private TextView tvNoLocationData;
    private String eventId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // osmdroid requires user-agent before setContentView
        Configuration.getInstance().setUserAgentValue(getPackageName());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_list_map);

        eventId = getIntent().getStringExtra("eventId");
        db = FirebaseFirestore.getInstance();

        mapView = findViewById(R.id.map_view);
        tvNoLocationData = findViewById(R.id.tv_no_location_data);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(3.0);
        mapView.getController().setCenter(new GeoPoint(20.0, 0.0));

        findViewById(R.id.btn_map_back).setOnClickListener(v -> finish());

        if (eventId != null) {
            loadLocations();
        } else {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadLocations() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) {
                        showNoDataMessage();
                        return;
                    }

                    Object rawLocations = eventDoc.get("waitingListLocations");
                    if (!(rawLocations instanceof Map) || ((Map<?, ?>) rawLocations).isEmpty()) {
                        showNoDataMessage();
                        return;
                    }

                    Map<String, Object> locations = (Map<String, Object>) rawLocations;
                    List<Marker> pendingMarkers = new ArrayList<>();
                    AtomicInteger remaining = new AtomicInteger(locations.size());

                    for (Map.Entry<String, Object> entry : locations.entrySet()) {
                        String deviceId = entry.getKey();
                        Object rawData = entry.getValue();

                        if (!(rawData instanceof Map)) {
                            if (remaining.decrementAndGet() == 0) {
                                runOnUiThread(() -> finalizeMap(pendingMarkers));
                            }
                            continue;
                        }

                        Map<String, Object> locationData = (Map<String, Object>) rawData;
                        double lat = toDouble(locationData.get("latitude"));
                        double lng = toDouble(locationData.get("longitude"));
                        com.google.firebase.Timestamp joinedAt =
                                locationData.get("joinedAt") instanceof com.google.firebase.Timestamp
                                        ? (com.google.firebase.Timestamp) locationData.get("joinedAt")
                                        : null;

                        db.collection("users").document(deviceId).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = (userDoc.exists() && userDoc.getString("name") != null)
                                            ? userDoc.getString("name") : "Unknown";

                                    String snippet = "";
                                    if (joinedAt != null) {
                                        SimpleDateFormat sdf = new SimpleDateFormat(
                                                "MMM dd, yyyy HH:mm", Locale.getDefault());
                                        snippet = "Joined: " + sdf.format(joinedAt.toDate());
                                    }

                                    Marker marker = new Marker(mapView);
                                    marker.setPosition(new GeoPoint(lat, lng));
                                    marker.setTitle(name);
                                    marker.setSnippet(snippet);
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                                    synchronized (pendingMarkers) {
                                        pendingMarkers.add(marker);
                                    }
                                    if (remaining.decrementAndGet() == 0) {
                                        runOnUiThread(() -> finalizeMap(pendingMarkers));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0) {
                                        runOnUiThread(() -> finalizeMap(pendingMarkers));
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> runOnUiThread(this::showNoDataMessage));
    }

    private void finalizeMap(List<Marker> markers) {
        if (markers.isEmpty()) {
            showNoDataMessage();
            return;
        }

        for (Marker marker : markers) {
            mapView.getOverlays().add(marker);
        }

        // Center map on the average position of all markers
        double avgLat = 0, avgLng = 0;
        for (Marker m : markers) {
            avgLat += m.getPosition().getLatitude();
            avgLng += m.getPosition().getLongitude();
        }
        avgLat /= markers.size();
        avgLng /= markers.size();

        mapView.getController().setZoom(markers.size() == 1 ? 12.0 : 8.0);
        mapView.getController().setCenter(new GeoPoint(avgLat, avgLng));
        mapView.invalidate();
    }

    private void showNoDataMessage() {
        if (tvNoLocationData != null) tvNoLocationData.setVisibility(View.VISIBLE);
        if (mapView != null) mapView.setVisibility(View.GONE);
    }

    private double toDouble(Object value) {
        if (value instanceof Double) return (Double) value;
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Float) return ((Float) value).doubleValue();
        return 0.0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
