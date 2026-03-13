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
        holder.tvName.setText(entrant.getName());
        holder.tvEmail.setText(entrant.getEmail());
        // For distance/location info if available
        if (entrant.getGeolocation() != null) {
            holder.tvInfo.setText("Location: " + entrant.getGeolocation());
        } else {
            holder.tvInfo.setText("Joined Oct 24, 2023");
        }
    }

    @Override
    public int getItemCount() {
        return entrants.size();
    }

    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvInfo;
        ImageView ivAvatar;

        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_entrant_name);
            tvEmail = itemView.findViewById(R.id.tv_entrant_email);
            tvInfo = itemView.findViewById(R.id.tv_entrant_info);
            ivAvatar = itemView.findViewById(R.id.iv_entrant_avatar);
        }
    }
}