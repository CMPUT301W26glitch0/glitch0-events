package com.example.cmput301_app.organizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.model.Entrant;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class EntrantListActivity extends AppCompatActivity {

    private String eventId;
    private EventDB eventDB;
    private RecyclerView rvEntrants;
    private EntrantAdapter adapter;
    private List<Entrant> entrantList;
    private TextView tvTotalCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_entrant_list);

        eventId = getIntent().getStringExtra("eventId");
        eventDB = new EventDB();

        initViews();
        setupRecyclerView();
        
        if (eventId != null) {
            loadEntrants();
        }
    }

    private void initViews() {
        tvTotalCount = findViewById(R.id.tv_total_count);
        findViewById(R.id.btn_entrants_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_run_lottery).setOnClickListener(v -> {
            Intent intent = new Intent(this, LotteryDrawActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupRecyclerView() {
        rvEntrants = findViewById(R.id.rv_entrants);
        rvEntrants.setLayoutManager(new LinearLayoutManager(this));
        entrantList = new ArrayList<>();
        // Note: For now adding mock data to match UI
        entrantList.add(new Entrant("1", "Sarah Jenkins", "s.jenkins@example.com", null));
        entrantList.add(new Entrant("2", "Michael Chen", "m.chen88@provider.net", null));
        adapter = new EntrantAdapter(entrantList);
        rvEntrants.setAdapter(adapter);
    }

    private void loadEntrants() {
        // Logic to fetch entrants from the event's waiting list sub-collection
        tvTotalCount.setText(String.valueOf(entrantList.size()));
    }
}