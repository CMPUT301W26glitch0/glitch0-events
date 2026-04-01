/*
 * Purpose: RecyclerView adapter for the admin event browsing screen.
 * Design Pattern: Standard Android RecyclerView Adapter
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Event;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Displays a tappable list of events for the admin event browsing screen.
 * Shows each event's name, organizer, date, and a derived status badge.
 * Does not include a Remove button — removal is handled from the dashboard.
 */
public class AdminEventBrowseAdapter extends RecyclerView.Adapter<AdminEventBrowseAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClicked(Event event);
    }

    private final List<Event> events;
    private final Map<String, String> organizerNames;
    private final Context context;
    private final OnEventClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public AdminEventBrowseAdapter(List<Event> events, Map<String, String> organizerNames,
                                   Context context, OnEventClickListener listener) {
        this.events = events;
        this.organizerNames = organizerNames;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_event_browse, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvEventName.setText(event.getName() != null ? event.getName() : "Unnamed Event");

        // Organizer name from the UID lookup map, fall back to short UID
        String orgId = event.getOrganizerId();
        String orgDisplay = (orgId != null && organizerNames.containsKey(orgId))
                ? organizerNames.get(orgId)
                : (orgId != null ? "ID: " + orgId.substring(0, Math.min(8, orgId.length())) : "Unknown");
        holder.tvOrganizerName.setText(orgDisplay);

        // Date
        if (event.getDate() != null) {
            holder.tvEventDate.setText(sdf.format(event.getDate().toDate()));
        } else {
            holder.tvEventDate.setText("Date TBD");
        }

        // Derived status
        String status = deriveStatus(event);
        holder.tvStatus.setText(status);
        applyStatusStyle(holder.tvStatus, status);

        holder.itemView.setOnClickListener(v -> listener.onEventClicked(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Derives a human-readable status string from event fields.
     * - "Completed"  — event date is in the past
     * - "Open"       — registration is currently open
     * - "Closed"     — registration is closed but event hasn't happened yet
     * - "Unknown"    — date is null
     */
    private String deriveStatus(Event event) {
        if (event.getDate() == null) return "Unknown";
        long now = System.currentTimeMillis();
        long eventTime = event.getDate().toDate().getTime();
        if (eventTime < now) return "Completed";
        if (event.checkIsRegistrationOpen()) return "Open";
        return "Closed";
    }

    private void applyStatusStyle(TextView tv, String status) {
        switch (status) {
            case "Open":
                tv.setBackgroundColor(Color.parseColor("#ECFDF3"));
                tv.setTextColor(Color.parseColor("#027A48"));
                break;
            case "Closed":
                tv.setBackgroundColor(Color.parseColor("#FFF4ED"));
                tv.setTextColor(Color.parseColor("#B54708"));
                break;
            case "Completed":
                tv.setBackgroundColor(Color.parseColor("#F2F4F7"));
                tv.setTextColor(Color.parseColor("#344054"));
                break;
            default:
                tv.setBackgroundColor(Color.parseColor("#F2F4F7"));
                tv.setTextColor(Color.parseColor("#667085"));
                break;
        }
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName;
        TextView tvOrganizerName;
        TextView tvEventDate;
        TextView tvStatus;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tv_browse_event_name);
            tvOrganizerName = itemView.findViewById(R.id.tv_browse_organizer_name);
            tvEventDate = itemView.findViewById(R.id.tv_browse_event_date);
            tvStatus = itemView.findViewById(R.id.tv_browse_status);
        }
    }
}
