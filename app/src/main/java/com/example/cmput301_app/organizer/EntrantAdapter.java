package com.example.cmput301_app.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Entrant;

import java.util.List;

public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    private List<Entrant> entrants;

    public EntrantAdapter(List<Entrant> entrants) {
        this.entrants = entrants;
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant_row, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        Entrant entrant = entrants.get(position);
        
        String name = entrant.getName() != null ? entrant.getName() : "Unknown";
        holder.tvName.setText(name);

        // Geolocation still holds the "Joined ..." timestamp text
        String info = entrant.getGeolocation();
        holder.tvInfo.setText(info != null && !info.isEmpty() ? info : "");

        // 1. Setup the Initials Avatar
        String initials = "?";
        if (name.length() > 0) {
            String[] parts = name.split(" ");
            if (parts.length > 1 && parts[1].length() > 0) {
                initials = parts[0].substring(0, 1).toUpperCase() + parts[1].substring(0, 1).toUpperCase();
            } else {
                initials = name.substring(0, Math.min(2, name.length())).toUpperCase();
            }
        }
        holder.tvAvatar.setText(initials);

        // Create a circular background for the avatar
        android.graphics.drawable.GradientDrawable avatarBg = new android.graphics.drawable.GradientDrawable();
        avatarBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        avatarBg.setColor(android.graphics.Color.parseColor("#F0F4F8")); // Light grayish-blue
        holder.tvAvatar.setBackground(avatarBg);

        // 2. Setup the Status Pill Badge
        String status = entrant.getStatus();
        if (status == null) status = "WAITING";
        
        holder.tvBadge.setText(status);

        android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
        badgeBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        badgeBg.setCornerRadius(50f); // High radius for pill shape

        int textColor;
        int bgColor;

        switch (status.toUpperCase()) {
            case "ACCEPTED":
                textColor = android.graphics.Color.parseColor("#4CAF50"); // Green
                bgColor = android.graphics.Color.parseColor("#E8F5E9");
                break;
            case "DECLINED":
            case "CANCELLED":
                textColor = android.graphics.Color.parseColor("#F44336"); // Red
                bgColor = android.graphics.Color.parseColor("#FFEBEE");
                break;
            case "AWAITING RESPONSE":
                textColor = android.graphics.Color.parseColor("#2196F3"); // Blue
                bgColor = android.graphics.Color.parseColor("#E3F2FD");
                break;
            default: // WAITING or NOT SELECTED
                textColor = android.graphics.Color.parseColor("#9E9E9E"); // Gray
                bgColor = android.graphics.Color.parseColor("#F5F5F5");
                break;
        }

        badgeBg.setColor(bgColor);
        holder.tvBadge.setTextColor(textColor);
        holder.tvBadge.setBackground(badgeBg);
    }

    @Override
    public int getItemCount() {
        return entrants.size();
    }

    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo, tvAvatar, tvBadge;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_entrant_name);
            tvInfo = itemView.findViewById(R.id.tv_entrant_info);
            tvAvatar = itemView.findViewById(R.id.tv_entrant_avatar);
            tvBadge = itemView.findViewById(R.id.tv_status_badge);
        }
    }
}