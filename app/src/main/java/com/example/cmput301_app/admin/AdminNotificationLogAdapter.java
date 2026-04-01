/*
 * Purpose: RecyclerView adapter for the admin notification log screen.
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
import com.example.cmput301_app.model.Notification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Displays a reverse-chronological list of notifications for the admin log screen.
 * Each entry shows the notification type badge, organizer name, event name,
 * message preview, and timestamp.
 */
public class AdminNotificationLogAdapter
        extends RecyclerView.Adapter<AdminNotificationLogAdapter.LogViewHolder> {

    private final List<Notification> notifications;
    private final Map<String, String> organizerNames;
    private final Map<String, String> eventNames;
    private final Context context;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault());

    public AdminNotificationLogAdapter(List<Notification> notifications,
                                       Map<String, String> organizerNames,
                                       Map<String, String> eventNames,
                                       Context context) {
        this.notifications = notifications;
        this.organizerNames = organizerNames;
        this.eventNames = eventNames;
        this.context = context;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_notification_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        Notification n = notifications.get(position);

        // Type badge — label + colour
        String typeLabel = typeLabel(n.getType());
        holder.tvTypeBadge.setText(typeLabel);
        applyTypeBadgeStyle(holder.tvTypeBadge, n.getType());

        // Organizer
        String orgId = n.getOrganizerId();
        String orgName = (orgId != null && organizerNames.containsKey(orgId))
                ? organizerNames.get(orgId)
                : (orgId != null && !orgId.isEmpty() ? "ID: " + orgId.substring(0, Math.min(8, orgId.length())) : "System");
        holder.tvOrganizerName.setText(orgName);

        // Event
        String evId = n.getEventId();
        String evName = null;
        if (evId != null && eventNames.containsKey(evId)) {
            evName = eventNames.get(evId);
        } else if (n.getEventName() != null && !n.getEventName().isEmpty()) {
            evName = n.getEventName();
        }
        if (evName != null) {
            holder.tvEventName.setVisibility(View.VISIBLE);
            holder.tvEventName.setText(evName);
        } else {
            holder.tvEventName.setVisibility(View.GONE);
        }

        // Message
        holder.tvMessage.setText(n.getMessage() != null ? n.getMessage() : "—");

        // Timestamp
        if (n.getTimestamp() != null) {
            holder.tvTimestamp.setText(sdf.format(n.getTimestamp().toDate()));
        } else {
            holder.tvTimestamp.setText("—");
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /** Returns a concise human-readable label for the notification type. */
    private String typeLabel(Notification.NotificationType type) {
        if (type == null) return "Unknown";
        switch (type) {
            case LOTTERY_WIN:            return "Selected";
            case LOTTERY_LOSS:           return "Not Selected";
            case ORGANIZER_BROADCAST:    return "Broadcast";
            case INVITATION_CANCELLED:   return "Cancelled";
            case LOTTERY_WIN_REDRAW:     return "Re-Selected";
            case NO_SPOT_AVAILABLE:      return "No Spot";
            case CO_ORGANIZER_INVITATION: return "Co-Organizer";
            case WAITING_LIST_INVITATION: return "WL Invite";
            default:                     return "Unknown";
        }
    }

    private void applyTypeBadgeStyle(TextView tv, Notification.NotificationType type) {
        if (type == null) {
            tv.setBackgroundColor(Color.parseColor("#F2F4F7"));
            tv.setTextColor(Color.parseColor("#667085"));
            return;
        }
        switch (type) {
            case LOTTERY_WIN:
            case LOTTERY_WIN_REDRAW:
                tv.setBackgroundColor(Color.parseColor("#ECFDF3"));
                tv.setTextColor(Color.parseColor("#027A48"));
                break;
            case LOTTERY_LOSS:
            case NO_SPOT_AVAILABLE:
                tv.setBackgroundColor(Color.parseColor("#FFF4ED"));
                tv.setTextColor(Color.parseColor("#B54708"));
                break;
            case INVITATION_CANCELLED:
                tv.setBackgroundColor(Color.parseColor("#FEF3F2"));
                tv.setTextColor(Color.parseColor("#B42318"));
                break;
            case ORGANIZER_BROADCAST:
                tv.setBackgroundColor(Color.parseColor("#EFF8FF"));
                tv.setTextColor(Color.parseColor("#026AA2"));
                break;
            case CO_ORGANIZER_INVITATION:
            case WAITING_LIST_INVITATION:
                tv.setBackgroundColor(Color.parseColor("#F4F3FF"));
                tv.setTextColor(Color.parseColor("#5925DC"));
                break;
            default:
                tv.setBackgroundColor(Color.parseColor("#F2F4F7"));
                tv.setTextColor(Color.parseColor("#667085"));
                break;
        }
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTypeBadge;
        TextView tvOrganizerName;
        TextView tvEventName;
        TextView tvMessage;
        TextView tvTimestamp;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTypeBadge    = itemView.findViewById(R.id.tv_log_type_badge);
            tvOrganizerName = itemView.findViewById(R.id.tv_log_organizer);
            tvEventName    = itemView.findViewById(R.id.tv_log_event);
            tvMessage      = itemView.findViewById(R.id.tv_log_message);
            tvTimestamp    = itemView.findViewById(R.id.tv_log_timestamp);
        }
    }
}
