/*
 * Purpose: RecyclerView adapter for displaying a list of events to entrants.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
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

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;
    private Context context;

    public EventAdapter(List<Event> eventList, Context context) {
        this.eventList = eventList;
        this.context = context;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.tvTitle.setText(event.getName() != null ? event.getName() : "Untitled Event");
        holder.tvDescription.setText(event.getDescription() != null ? event.getDescription() : "No description available.");
        
        // Safer ID handling to prevent crashes
        String eventId = event.getEventId();
        String displayId = (eventId != null && eventId.length() >= 4) ? eventId.substring(0, 4) : "0000";
        holder.tvId.setText("ID: #" + displayId);
        
        holder.tvApplicants.setText(event.getWaitingListCount() + " / " + event.getCapacity() + " applicants");
        
        String status = "Open"; // Default status
        holder.tvBadge.setText(status);
        holder.tvBadge.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.badge_green_bg));
        holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_green_text));

        holder.btnJoin.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsActivity.class);
            intent.putExtra("eventId", event.getEventId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge, tvId, tvTitle, tvApplicants, tvDescription;
        Button btnJoin;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tv_badge);
            tvId = itemView.findViewById(R.id.tv_event_id);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvApplicants = itemView.findViewById(R.id.tv_applicants_info);
            tvDescription = itemView.findViewById(R.id.tv_event_description);
            btnJoin = itemView.findViewById(R.id.btn_join_list);
        }
    }
}