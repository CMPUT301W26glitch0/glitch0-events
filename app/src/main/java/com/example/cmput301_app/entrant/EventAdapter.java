/**
 * RecyclerView adapter for displaying a browsable list of available events to an entrant.
 *
 * Each item is rendered using {@code item_event} and shows:
 *  - Event name, status badge (Open / Upcoming / Closed)
 *  - Event date, location, registration close date
 *  - Description (truncated to 2 lines)
 *
 * Tapping the card or the "View" button opens {@link EventDetailsActivity}.
 */
package com.example.cmput301_app.entrant;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Event;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final Context context;
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public EventAdapter(List<Event> eventList, Context context) {
        this.eventList = eventList;
        this.context = context;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);

        // Title
        holder.tvTitle.setText(event.getName() != null ? event.getName() : "Untitled Event");

        // Short ID pill
        String eventId = event.getEventId();
        String displayId = (eventId != null && eventId.length() >= 4)
                ? eventId.substring(0, 4) : "0000";
        holder.tvId.setText("ID: #" + displayId);

        // Description
        holder.tvDescription.setText(
                event.getDescription() != null ? event.getDescription() : "No description available.");

        // Event date
        if (event.getDate() != null) {
            holder.tvDate.setText("📅 " + DATE_FMT.format(event.getDate().toDate()));
        } else {
            holder.tvDate.setText("📅 Date TBD");
        }

        // Location
        holder.tvLocation.setText(
                (event.getLocation() != null && !event.getLocation().isEmpty())
                        ? "📍 " + event.getLocation() : "📍 Location TBD");

        // Registration close date
        if (event.getRegistrationClose() != null) {
            holder.tvRegClose.setText("⏰ Reg. closes: "
                    + DATE_FMT.format(event.getRegistrationClose().toDate()));
            holder.tvRegClose.setVisibility(View.VISIBLE);
        } else {
            holder.tvRegClose.setVisibility(View.GONE);
        }

        // Status badge
        long now = System.currentTimeMillis();
        String status;
        int bgColor;
        int textColor;

        boolean hasOpen = event.getRegistrationOpen() != null;
        boolean hasClose = event.getRegistrationClose() != null;

        if (hasClose && event.getRegistrationClose().toDate().getTime() < now) {
            status = "Closed";
            bgColor = 0x1AEF4444;   // light red bg
            textColor = 0xFFDC2626; // red text
        } else if (hasOpen && event.getRegistrationOpen().toDate().getTime() > now) {
            status = "Upcoming";
            bgColor = 0x1AF59E0B;   // light amber bg
            textColor = 0xFFB45309; // amber text
        } else {
            status = "Open";
            bgColor = ContextCompat.getColor(context, R.color.badge_green_bg);
            textColor = ContextCompat.getColor(context, R.color.badge_green_text);
        }
        holder.tvBadge.setText(status);
        holder.tvBadge.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(bgColor));
        holder.tvBadge.setTextColor(textColor);

        // Navigate to details on card or button tap
        View.OnClickListener openDetails = v -> {
            Intent intent = new Intent(context, EventDetailsActivity.class);
            intent.putExtra("eventId", event.getEventId());
            context.startActivity(intent);
        };
        holder.itemView.setOnClickListener(openDetails);
        holder.btnJoin.setOnClickListener(openDetails);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge, tvId, tvTitle, tvDate, tvLocation, tvRegClose, tvDescription;
        Button btnJoin;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge       = itemView.findViewById(R.id.tv_badge);
            tvId          = itemView.findViewById(R.id.tv_event_id);
            tvTitle       = itemView.findViewById(R.id.tv_event_title);
            tvDate        = itemView.findViewById(R.id.tv_draws_info);
            tvLocation    = itemView.findViewById(R.id.tv_applicants_info);
            tvRegClose    = itemView.findViewById(R.id.tv_reg_close_info);
            tvDescription = itemView.findViewById(R.id.tv_event_description);
            btnJoin       = itemView.findViewById(R.id.btn_join_list);
        }
    }
}