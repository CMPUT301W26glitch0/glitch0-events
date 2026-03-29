/*
 * Purpose: Activity to filter events by availability (day, time) and event capacity.
 * Implements: US 01.01.04 – Filter events based on availability and event capacity.
 */
package com.example.cmput301_app.entrant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.model.Event;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Full-screen filter activity for entrants to filter events by:
 * - Availability: preferred days of week and time of day
 * - Event capacity: waitlist availability and hide full events
 */
public class FilterEventsActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_DAYS = "selectedDays";
    public static final String EXTRA_MORNING = "morningChecked";
    public static final String EXTRA_AFTERNOON = "afternoonChecked";
    public static final String EXTRA_EVENING = "eveningChecked";
    public static final String EXTRA_WAITLIST_AVAILABILITY = "waitlistAvailability";
    public static final String EXTRA_HIDE_FULL = "hideFullEvents";

    private Chip chipMon, chipTue, chipWed, chipThu, chipFri, chipSat, chipSun;
    private CheckBox cbMorning, cbAfternoon, cbEvening;
    private SwitchMaterial switchWaitlist, switchHideFull;
    private Button btnShowMatches;
    private TextView tvCancel, tvApply, tvReset;

    private EventDB eventDB;
    private List<Event> allEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_events);

        eventDB = new EventDB();
        bindViews();
        restoreFilterState(getIntent());
        setupListeners();
        loadEventsAndCount();
    }

    private void bindViews() {
        tvCancel = findViewById(R.id.tv_cancel);
        tvApply = findViewById(R.id.tv_apply);
        tvReset = findViewById(R.id.tv_reset_filters);
        btnShowMatches = findViewById(R.id.btn_show_matches);

        chipMon = findViewById(R.id.chip_mon);
        chipTue = findViewById(R.id.chip_tue);
        chipWed = findViewById(R.id.chip_wed);
        chipThu = findViewById(R.id.chip_thu);
        chipFri = findViewById(R.id.chip_fri);
        chipSat = findViewById(R.id.chip_sat);
        chipSun = findViewById(R.id.chip_sun);

        cbMorning = findViewById(R.id.cb_morning);
        cbAfternoon = findViewById(R.id.cb_afternoon);
        cbEvening = findViewById(R.id.cb_evening);

        switchWaitlist = findViewById(R.id.switch_waitlist);
        switchHideFull = findViewById(R.id.switch_hide_full);
    }

    private void restoreFilterState(Intent intent) {
        if (intent == null) return;
        int[] days = intent.getIntArrayExtra(EXTRA_SELECTED_DAYS);
        if (days != null) {
            for (int day : days) chipForDay(day).setChecked(true);
        }
        cbMorning.setChecked(intent.getBooleanExtra(EXTRA_MORNING, false));
        cbAfternoon.setChecked(intent.getBooleanExtra(EXTRA_AFTERNOON, false));
        cbEvening.setChecked(intent.getBooleanExtra(EXTRA_EVENING, false));
        switchWaitlist.setChecked(intent.getBooleanExtra(EXTRA_WAITLIST_AVAILABILITY, false));
        switchHideFull.setChecked(intent.getBooleanExtra(EXTRA_HIDE_FULL, false));
    }

    private void setupListeners() {
        tvCancel.setOnClickListener(v -> { setResult(RESULT_CANCELED); finish(); });
        tvApply.setOnClickListener(v -> applyAndFinish());
        btnShowMatches.setOnClickListener(v -> applyAndFinish());
        tvReset.setOnClickListener(v -> resetAll());

        CompoundButton.OnCheckedChangeListener recalc = (btn, checked) -> updateMatchCount();
        cbMorning.setOnCheckedChangeListener(recalc);
        cbAfternoon.setOnCheckedChangeListener(recalc);
        cbEvening.setOnCheckedChangeListener(recalc);
        switchWaitlist.setOnCheckedChangeListener(recalc);
        switchHideFull.setOnCheckedChangeListener(recalc);

        ChipGroup chipGroup = findViewById(R.id.chip_group_days);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> updateMatchCount());
    }

    private void loadEventsAndCount() {
        eventDB.getAllEvents(events -> {
            allEvents.clear();
            allEvents.addAll(events);
            updateMatchCount();
        }, e -> Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show());
    }

    private void updateMatchCount() {
        List<Event> filtered = applyFilters(allEvents);
        btnShowMatches.setText("Show " + filtered.size() + " Matches");
    }

    private List<Event> applyFilters(List<Event> events) {
        int[] selectedDays = getSelectedDays();
        boolean morning = cbMorning.isChecked();
        boolean afternoon = cbAfternoon.isChecked();
        boolean evening = cbEvening.isChecked();
        boolean waitlistOnly = switchWaitlist.isChecked();
        boolean hideFull = switchHideFull.isChecked();
        boolean anyDaySelected = selectedDays.length > 0;
        boolean anyTimeSelected = morning || afternoon || evening;

        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (anyDaySelected) {
                int eventDay = event.getDayOfWeek();
                if (eventDay == -1) continue;
                boolean dayMatch = false;
                for (int d : selectedDays) { if (d == eventDay) { dayMatch = true; break; } }
                if (!dayMatch) continue;
            }
            if (anyTimeSelected) {
                int hour = event.getHourOfDay();
                if (hour == -1) continue;
                boolean timeMatch = false;
                if (morning && hour < 12) timeMatch = true;
                if (afternoon && hour >= 12 && hour < 16) timeMatch = true;
                if (evening && hour >= 16) timeMatch = true;
                if (!timeMatch) continue;
            }
            if (waitlistOnly && !event.hasWaitlistSpace()) continue;
            if (hideFull && event.isFull()) continue;
            result.add(event);
        }
        return result;
    }

    private int[] getSelectedDays() {
        List<Integer> days = new ArrayList<>();
        if (chipMon.isChecked()) days.add(Calendar.MONDAY);
        if (chipTue.isChecked()) days.add(Calendar.TUESDAY);
        if (chipWed.isChecked()) days.add(Calendar.WEDNESDAY);
        if (chipThu.isChecked()) days.add(Calendar.THURSDAY);
        if (chipFri.isChecked()) days.add(Calendar.FRIDAY);
        if (chipSat.isChecked()) days.add(Calendar.SATURDAY);
        if (chipSun.isChecked()) days.add(Calendar.SUNDAY);
        int[] result = new int[days.size()];
        for (int i = 0; i < days.size(); i++) result[i] = days.get(i);
        return result;
    }

    private Chip chipForDay(int calendarDay) {
        switch (calendarDay) {
            case Calendar.MONDAY:    return chipMon;
            case Calendar.TUESDAY:   return chipTue;
            case Calendar.WEDNESDAY: return chipWed;
            case Calendar.THURSDAY:  return chipThu;
            case Calendar.FRIDAY:    return chipFri;
            case Calendar.SATURDAY:  return chipSat;
            case Calendar.SUNDAY:    return chipSun;
            default: return chipMon;
        }
    }

    private void resetAll() {
        chipMon.setChecked(false); chipTue.setChecked(false); chipWed.setChecked(false);
        chipThu.setChecked(false); chipFri.setChecked(false); chipSat.setChecked(false);
        chipSun.setChecked(false);
        cbMorning.setChecked(false); cbAfternoon.setChecked(false); cbEvening.setChecked(false);
        switchWaitlist.setChecked(false); switchHideFull.setChecked(false);
        updateMatchCount();
    }

    private void applyAndFinish() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SELECTED_DAYS, getSelectedDays());
        resultIntent.putExtra(EXTRA_MORNING, cbMorning.isChecked());
        resultIntent.putExtra(EXTRA_AFTERNOON, cbAfternoon.isChecked());
        resultIntent.putExtra(EXTRA_EVENING, cbEvening.isChecked());
        resultIntent.putExtra(EXTRA_WAITLIST_AVAILABILITY, switchWaitlist.isChecked());
        resultIntent.putExtra(EXTRA_HIDE_FULL, switchHideFull.isChecked());
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
