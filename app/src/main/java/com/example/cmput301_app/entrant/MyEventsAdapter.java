package com.example.cmput301_app.entrant;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Entrant;
import com.example.cmput301_app.model.Event;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MyEventsAdapter extends RecyclerView.Adapter<MyEventsAdapter.MyEventViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(MyEventItem item);
    }

    private List<MyEventItem> items;
    private OnItemClickListener listener;

    public MyEventsAdapter(List<MyEventItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_event, parent, false);
        return new MyEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyEventViewHolder holder, int position) {
        MyEventItem item = items.get(position);
        Context context = holder.itemView.getContext();

        // set event name
        holder.tvTitle.setText(item.event.getName() != null ? item.event.getName() : "Untitled Event");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });

        // set event date
        if (item.event.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
            holder.tvDate.setText(sdf.format(item.event.getDate().toDate()));
        } else {
            holder.tvDate.setText("Date unavailable");
        }

        // set status badge text and colors based on outcome
        switch (item.outcome) {
            case WAITING:
                setBadgeStyle(context, holder.tvStatus, "Waitlist", R.color.badge_gray_bg, R.color.badge_gray_text);
                break;
            case SELECTED:
                setBadgeStyle(context, holder.tvStatus, "Pending", R.color.badge_orange_bg, R.color.badge_orange_text);
                break;
            case ACCEPTED:
                setBadgeStyle(context, holder.tvStatus, "Won", R.color.light_blue_bg, R.color.primary_blue);
                break;
            case DECLINED:
                setBadgeStyle(context, holder.tvStatus, "Declined", R.color.badge_orange_bg, R.color.badge_orange_text);
                break;
            case NOT_SELECTED:
                setBadgeStyle(context, holder.tvStatus, "Not Selected", R.color.badge_gray_bg, R.color.badge_gray_text);
                break;
            case CANCELLED:
                setBadgeStyle(context, holder.tvStatus, "Cancelled", R.color.badge_gray_bg, R.color.badge_gray_text);
                break;
            default:
                setBadgeStyle(context, holder.tvStatus, "Unknown", R.color.badge_gray_bg, R.color.badge_gray_text);
                break;
        }
    }

    private void setBadgeStyle(Context context, TextView tvStatus, String text, int bgColorRes, int textColorRes) {
        tvStatus.setText(text);
        tvStatus.setTextColor(ContextCompat.getColor(context, textColorRes));

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(40f);
        background.setColor(ContextCompat.getColor(context, bgColorRes));
        tvStatus.setBackground(background);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // -------------------------------------------------------------------------
    // ViewHolder
    // -------------------------------------------------------------------------

    public static class MyEventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvStatus;

        public MyEventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_my_event_title);
            tvDate = itemView.findViewById(R.id.tv_my_event_date);
            tvStatus = itemView.findViewById(R.id.tv_my_event_status);
        }
    }

    // -------------------------------------------------------------------------
    // Data Model
    // -------------------------------------------------------------------------

    /**
     * Pairs an Event with its corresponding lottery outcome
     * for display in the My Events list.
     */
    public static class MyEventItem {
        public Event event;
        public Entrant.RegistrationRecord.Outcome outcome;

        public MyEventItem(Event event, Entrant.RegistrationRecord.Outcome outcome) {
            this.event = event;
            this.outcome = outcome;
        }
    }
}
