/*
 * Purpose: Admin screen to browse and delete comments across all events.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.database.AdminDB;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin screen that lets an administrator browse all comments posted across all
 * events and delete any comment that violates app policy.
 *
 * Each deletion:
 * 1. Shows a confirmation dialog.
 * 2. Removes the comment from the event document via {@link AdminDB#deleteComment}.
 * 3. Writes an audit record to the {@code deleted_comments_log} Firestore collection.
 */
public class AdminCommentsActivity extends AppCompatActivity {

    private static final String TAG = "AdminComments";

    private AdminDB adminDB;
    private RecyclerView rvComments;
    private AdminCommentAdapter adapter;

    private final List<AdminCommentEntry> allEntries = new ArrayList<>();
    private final List<AdminCommentEntry> filtered   = new ArrayList<>();

    private TextView tvCount;
    private TextView tvEmptyState;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_comments);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.admin_comments_main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });

        adminDB = new AdminDB();

        tvCount      = findViewById(R.id.tv_comment_count);
        tvEmptyState = findViewById(R.id.tv_comments_empty);
        etSearch     = findViewById(R.id.et_search_comments);

        rvComments = findViewById(R.id.rv_admin_comments);
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminCommentAdapter(filtered, this, this::onDeleteClicked);
        rvComments.setAdapter(adapter);

        findViewById(R.id.btn_comments_back).setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadComments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadComments();
    }

    private void loadComments() {
        adminDB.getAllComments(
                entries -> {
                    allEntries.clear();
                    allEntries.addAll(entries);
                    applyFilter();
                },
                e -> {
                    Log.e(TAG, "Failed to load comments", e);
                    Toast.makeText(this, "Failed to load comments", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void applyFilter() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        filtered.clear();

        for (AdminCommentEntry entry : allEntries) {
            if (!query.isEmpty()) {
                String author = entry.getComment().getAuthorName() != null
                        ? entry.getComment().getAuthorName().toLowerCase() : "";
                String content = entry.getComment().getContent() != null
                        ? entry.getComment().getContent().toLowerCase() : "";
                String event = entry.getEventName().toLowerCase();
                if (!author.contains(query) && !content.contains(query) && !event.contains(query)) {
                    continue;
                }
            }
            filtered.add(entry);
        }

        adapter.notifyDataSetChanged();

        boolean isEmpty = filtered.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvComments.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvCount.setText(filtered.size() + " comment" + (filtered.size() == 1 ? "" : "s"));
    }

    private void onDeleteClicked(AdminCommentEntry entry, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment by \""
                        + entry.getComment().getAuthorName() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> performDelete(entry, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(AdminCommentEntry entry, int position) {
        adminDB.deleteComment(
                entry.getEventId(),
                entry.getComment(),
                entry.getEventName(),
                unused -> {
                    allEntries.remove(entry);
                    filtered.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, filtered.size());

                    boolean isEmpty = filtered.isEmpty();
                    tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    rvComments.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    tvCount.setText(filtered.size() + " comment" + (filtered.size() == 1 ? "" : "s"));

                    Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show();
                },
                e -> {
                    Log.e(TAG, "Failed to delete comment", e);
                    Toast.makeText(this, "Failed to delete comment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
        );
    }
}
