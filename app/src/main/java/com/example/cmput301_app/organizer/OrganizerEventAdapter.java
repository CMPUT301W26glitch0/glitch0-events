package com.example.cmput301_app.organizer;

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

public class OrganizerEventAdapter extends RecyclerView.Adapter<OrganizerEventAdapter.ViewHolder> {

    private List<Event> eventList;
    private Context context;

    public OrganizerEventAdapter(List<Event> eventList, Context context) {
        this.eventList = eventList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.tvTitle.setText(event.getName() != null ? event.getName() : "Untitled Event");
        holder.tvDescription.setText(event.getDescription() != null ? event.getDescription() : "No description.");
        
        String eventId = event.getEventId();
        String displayId = (eventId != null && eventId.length() >= 4) ? eventId.substring(0, 4) : "0000";
        holder.tvId.setText("ID: #" + displayId);
        
        holder.tvApplicants.setText(event.getWaitingListCount() + " / " + event.getCapacity() + " applicants");
        
        // Use category as badge text if available
        String category = event.getCategory();
        if (category != null && !category.isEmpty()) {
            holder.tvBadge.setText(category);
            holder.tvBadge.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary_blue));
        } else {
            holder.tvBadge.setText("Active");
            holder.tvBadge.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.badge_green_bg));
        }
        holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.white));

        holder.btnAction.setText("Manage");
        holder.btnAction.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.primary_blue));

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EntrantListActivity.class);
            intent.putExtra("eventId", event.getEventId());
            context.startActivity(intent);
        });

        holder.btnAction.setOnClickListener(v -> {
            Intent intent = new Intent(context, EntrantListActivity.class);
            intent.putExtra("eventId", event.getEventId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge, tvId, tvTitle, tvApplicants, tvDescription;
        Button btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadge = itemView.findViewById(R.id.tv_badge);
            tvId = itemView.findViewById(R.id.tv_event_id);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvApplicants = itemView.findViewById(R.id.tv_applicants_info);
            tvDescription = itemView.findViewById(R.id.tv_event_description);
            btnAction = itemView.findViewById(R.id.btn_join_list);
        }
    }
}
