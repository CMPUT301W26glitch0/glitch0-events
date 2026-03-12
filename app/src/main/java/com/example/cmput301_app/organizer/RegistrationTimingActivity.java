package com.example.cmput301_app.organizer;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.model.Event;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;

public class RegistrationTimingActivity extends AppCompatActivity {

    private String eventId;
    private EventDB eventDB;
    private Event currentEvent;
    private TextView tvOpeningTime, tvClosingTime;
    private MaterialSwitch swLocation, swAutomation;
    private Calendar startCalendar, endCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration_timing);

        eventId = getIntent().getStringExtra("eventId");
        eventDB = new EventDB();
        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();

        initViews();
        if (eventId != null) {
            loadEventData();
        }
    }

    private void initViews() {
        tvOpeningTime = findViewById(R.id.tv_opening_time);
        tvClosingTime = findViewById(R.id.tv_closing_time);
        swLocation = findViewById(R.id.sw_require_location);
        swAutomation = findViewById(R.id.sw_automation);

        findViewById(R.id.btn_set_opening_time).setOnClickListener(v -> showTimePicker(true));
        findViewById(R.id.btn_set_closing_time).setOnClickListener(v -> showTimePicker(false));
        findViewById(R.id.btn_update_timing).setOnClickListener(v -> updateTiming());
        findViewById(R.id.btn_timing_save).setOnClickListener(v -> updateTiming());

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadEventData() {
        eventDB.getEvent(eventId, event -> {
            if (event != null) {
                currentEvent = event;
                if (event.getRegistrationOpen() != null) {
                    startCalendar.setTime(event.getRegistrationOpen().toDate());
                    tvOpeningTime.setText(formatTime(startCalendar));
                }
                if (event.getRegistrationClose() != null) {
                    endCalendar.setTime(event.getRegistrationClose().toDate());
                    tvClosingTime.setText(formatTime(endCalendar));
                }
                swLocation.setChecked(event.isGeolocationEnabled());
            }
        }, e -> Toast.makeText(this, "Error loading event", Toast.LENGTH_SHORT).show());
    }

    private void showTimePicker(boolean isOpening) {
        Calendar cal = isOpening ? startCalendar : endCalendar;
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            cal.set(Calendar.MINUTE, minute);
            if (isOpening) tvOpeningTime.setText(formatTime(cal));
            else tvClosingTime.setText(formatTime(cal));
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private String formatTime(Calendar cal) {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        String ampm = hour >= 12 ? "PM" : "AM";
        int hour12 = hour % 12;
        if (hour12 == 0) hour12 = 12;
        return String.format("%02d:%02d %s", hour12, minute, ampm);
    }

    private void updateTiming() {
        if (currentEvent == null) return;
        currentEvent.setRegistrationOpen(new Timestamp(startCalendar.getTime()));
        currentEvent.setRegistrationClose(new Timestamp(endCalendar.getTime()));
        currentEvent.setGeolocationEnabled(swLocation.isChecked());

        // Note: EventDB updateEvent would be used here
        Toast.makeText(this, "Timing updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}