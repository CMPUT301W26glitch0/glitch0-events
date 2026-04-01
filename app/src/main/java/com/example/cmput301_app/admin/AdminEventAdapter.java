/*
 * Purpose: RecyclerView adapter for the Admin event management screen.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Event;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying all events in the admin dashboard.
 * Each item has a "Remove" button that shows a confirmation dialog before deletion.
 */
public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.ViewHolder> {

    /** Callback invoked when the admin confirms removal of an event. */
    public interface OnRemoveClickListener {
        void onRemoveConfirmed(Event event, int position);
    }

    private final List<Event> eventList;
    private final Context context;
    private final OnRemoveClickListener removeListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public AdminEventAdapter(List<Event> eventList, Context context, OnRemoveClickListener removeListener) {
        this.eventList = eventList;
        this.context = context;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);

        holder.tvTitle.setText(event.getName() != null ? event.getName() : "Untitled Event");

        String eventId = event.getEventId();
        String displayId = (eventId != null && eventId.length() >= 4)
                ? eventId.substring(0, 4) : "0000";
        holder.tvId.setText("ID: #" + displayId);

        // Date
        if (event.getDate() != null) {
            holder.tvDate.setText(dateFormat.format(event.getDate().toDate()));
        } else {
            holder.tvDate.setText("Date TBD");
        }

        // Location
        String location = event.getLocation();
        holder.tvLocation.setText(location != null && !location.isEmpty() ? location : "No location");

        // Badge: use category or "Private"
        if (event.isPrivate()) {
            holder.tvBadge.setText("Private");
            holder.tvBadge.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF7F56D9));
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            String category = event.getCategory();
            if (category != null && !category.isEmpty()) {
                holder.tvBadge.setText(category);
                holder.tvBadge.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.primary_blue));
                holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
            } else {
                holder.tvBadge.setText("Active");
                holder.tvBadge.setBackgroundTintList(
                        ContextCompat.getColorStateList(context, R.color.badge_green_bg));
                holder.tvBadge.setTextColor(
                        ContextCompat.getColor(context, R.color.badge_green_text));
            }
        }

        holder.btnRemove.setOnClickListener(v -> showConfirmationDialog(event, holder.getAdapterPosition()));
    }

    private void showConfirmationDialog(Event event, int position) {
        String eventName = event.getName() != null ? event.getName() : "this event";
        new AlertDialog.Builder(context)
                .setTitle("Remove Event")
                .setMessage("Remove \"" + eventName + "\"?\n\n"
                        + "All associated waiting list records, QR codes, and poster images will be deleted. "
                        + "All entrants will receive a cancellation notification.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    if (removeListener != null) {
                        removeListener.onRemoveConfirmed(event, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge, tvId, tvTitle, tvDate, tvLocation;
        Button btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tv_admin_badge);
            tvId = itemView.findViewById(R.id.tv_admin_event_id);
            tvTitle = itemView.findViewById(R.id.tv_admin_event_title);
            tvDate = itemView.findViewById(R.id.tv_admin_date);
            tvLocation = itemView.findViewById(R.id.tv_admin_location);
            btnRemove = itemView.findViewById(R.id.btn_admin_remove);
        }
    }
}
