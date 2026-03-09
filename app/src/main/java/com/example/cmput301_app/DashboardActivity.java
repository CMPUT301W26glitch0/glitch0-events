package com.example.cmput301_app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Main dashboard for the application.
 * Updated to extend AppCompatActivity to prevent theme-related crashes.
 */
public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This ensures the activity uses your XML layout file
        setContentView(R.layout.activity_dashboard);
    }
}
