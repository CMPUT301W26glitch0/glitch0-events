/*
 * Purpose: RecyclerView adapter for the admin image browsing grid.
 * Design Pattern: Standard Android RecyclerView Adapter
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.util.ImageUtils;

import java.util.List;
import java.util.Map;

/**
 * Displays event poster images in a grid for the admin image browsing screen.
 * Each card shows a thumbnail, event name, and organizer name.
 * Tapping a card opens the full-size image preview where removal can occur.
 */
public class AdminImageAdapter extends RecyclerView.Adapter<AdminImageAdapter.ImageViewHolder> {

    public interface OnImageClickListener {
        void onImageClicked(Event event);
    }

    private final List<Event> events;
    private final Map<String, String> organizerNames;
    private final Context context;
    private final OnImageClickListener listener;

    public AdminImageAdapter(List<Event> events, Map<String, String> organizerNames,
                             Context context, OnImageClickListener listener) {
        this.events = events;
        this.organizerNames = organizerNames;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Event event = events.get(position);

        holder.tvEventName.setText(event.getName() != null ? event.getName() : "Unnamed Event");

        String orgId = event.getOrganizerId();
        String orgName = (orgId != null && organizerNames.containsKey(orgId))
                ? organizerNames.get(orgId) : "Unknown organizer";
        holder.tvOrganizerName.setText(orgName);

        ImageUtils.loadImage(context, event.getPosterUrl(), holder.ivThumbnail, false);

        holder.itemView.setOnClickListener(v -> listener.onImageClicked(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvEventName;
        TextView tvOrganizerName;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_poster_thumbnail);
            tvEventName = itemView.findViewById(R.id.tv_image_event_name);
            tvOrganizerName = itemView.findViewById(R.id.tv_image_organizer_name);
        }
    }
}
