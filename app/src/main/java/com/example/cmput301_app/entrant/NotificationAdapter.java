package com.example.cmput301_app.entrant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Notification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for rendering notification items in the dashboard
 * notification dropdown popup.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    /**
     * Callback interface for notification item clicks.
     */
    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private final List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        // Set message
        holder.tvMessage.setText(notification.getMessage());

        // Set timestamp
        if (notification.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(notification.getTimestamp().toDate()));
        } else {
            holder.tvTime.setText("");
        }

        // Set icon tint based on notification type
        int tintColor;
        if (notification.getType() == Notification.NotificationType.LOTTERY_WIN
                || notification.getType() == Notification.NotificationType.LOTTERY_WIN_REDRAW) {
            tintColor = 0xFF4CAF50; // green for win
        } else if (notification.getType() == Notification.NotificationType.LOTTERY_LOSS) {
            tintColor = 0xFFFF5722; // red-orange for loss
        } else if (notification.getType() == Notification.NotificationType.INVITATION_CANCELLED) {
            tintColor = 0xFFE53935; // red for cancelled
        } else if (notification.getType() == Notification.NotificationType.CO_ORGANIZER_INVITATION) {
            tintColor = 0xFF7F56D9; // purple for co-organizer invitation
        } else if (notification.getType() == Notification.NotificationType.WAITING_LIST_INVITATION) {
            tintColor = 0xFF00897B; // teal for private event waiting list invitation
        } else {
            tintColor = 0xFF2196F3; // blue for broadcast / other
        }
        holder.ivIcon.setColorFilter(tintColor);

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvMessage;
        TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_notif_icon);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
        }
    }
}
