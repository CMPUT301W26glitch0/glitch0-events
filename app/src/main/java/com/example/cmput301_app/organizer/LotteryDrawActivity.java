package com.example.cmput301_app.organizer;

import android.os.Bundle;
import android.view.View;
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

public class LotteryDrawActivity extends AppCompatActivity {

    private String eventId;
    private EventDB eventDB;
    private TextView tvWinnerCount, tvWaitingCount, tvSampleRate;
    private int winnersToSelect = 20;
    private long totalEntrants = 142; // Mock

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lottery_draw);

        eventId = getIntent().getStringExtra("eventId");
        eventDB = new EventDB();

        initViews();
        if (eventId != null) {
            loadEventData();
        }
    }

    private void initViews() {
        tvWinnerCount = findViewById(R.id.tv_winner_count);
        tvWaitingCount = findViewById(R.id.tv_lottery_waiting_count);
        tvSampleRate = findViewById(R.id.tv_sample_rate);

        findViewById(R.id.btn_lottery_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_plus_winners).setOnClickListener(v -> {
            winnersToSelect++;
            updateUI();
        });

        findViewById(R.id.btn_minus_winners).setOnClickListener(v -> {
            if (winnersToSelect > 1) {
                winnersToSelect--;
                updateUI();
            }
        });

        findViewById(R.id.btn_draw_winners).setOnClickListener(v -> {
            Toast.makeText(this, "Lottery initiated! Notifying " + winnersToSelect + " winners...", Toast.LENGTH_LONG).show();
            finish();
        });

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
                totalEntrants = event.getWaitingListCount();
                updateUI();
            }
        }, e -> {});
    }

    private void updateUI() {
        tvWinnerCount.setText(String.valueOf(winnersToSelect));
        tvWaitingCount.setText(totalEntrants + " Entrants");
        
        if (totalEntrants > 0) {
            int rate = (int) ((winnersToSelect * 100.0) / totalEntrants);
            tvSampleRate.setText("~" + rate + "%");
        }
    }
}