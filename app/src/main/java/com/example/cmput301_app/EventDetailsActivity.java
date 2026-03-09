package com.example.cmput301_app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity to show specific event details.
 */
public class EventDetailsActivity extends AppCompatActivity {

    @Override    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        // Retrieve the event ID sent from the Adapter
        String eventId = getIntent().getStringExtra("eventId");
    }
}