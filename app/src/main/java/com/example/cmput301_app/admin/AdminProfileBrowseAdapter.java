/*
 * Purpose: RecyclerView adapter for the admin profile browsing screen.
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
import com.example.cmput301_app.model.Profile;
import com.example.cmput301_app.util.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Displays a tappable list of user profiles for the admin profile browsing screen.
 * Shows each user's avatar, name, email, and join date.
 */
public class AdminProfileBrowseAdapter extends RecyclerView.Adapter<AdminProfileBrowseAdapter.ProfileViewHolder> {

    public interface OnProfileClickListener {
        void onProfileClicked(Profile profile);
    }

    private final List<Profile> profiles;
    private final Context context;
    private final OnProfileClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public AdminProfileBrowseAdapter(List<Profile> profiles, Context context,
                                     OnProfileClickListener listener) {
        this.profiles = profiles;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_profile_browse, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        Profile profile = profiles.get(position);

        String name = profile.getName() != null ? profile.getName() : "Unknown User";
        holder.tvName.setText(name);

        holder.tvEmail.setText(profile.getEmail() != null ? profile.getEmail() : "—");

        // Join date
        if (profile.getJoinDate() != null) {
            holder.tvJoinDate.setText("Joined " + sdf.format(profile.getJoinDate().toDate()));
        } else {
            holder.tvJoinDate.setText("Join date unknown");
        }

        // Role badge
        String role = profile.getRole() != null ? profile.getRole() : "entrant";
        holder.tvRoleBadge.setText(capitalize(role));
        switch (role.toLowerCase()) {
            case "organizer":
                holder.tvRoleBadge.setBackgroundColor(0xFFEFF8FF);
                holder.tvRoleBadge.setTextColor(0xFF026AA2);
                break;
            case "admin":
                holder.tvRoleBadge.setBackgroundColor(0xFFF4F3FF);
                holder.tvRoleBadge.setTextColor(0xFF5925DC);
                break;
            default: // entrant
                holder.tvRoleBadge.setBackgroundColor(0xFFECFDF3);
                holder.tvRoleBadge.setTextColor(0xFF027A48);
                break;
        }

        // Avatar: profile image or initials
        String imageUrl = profile.getProfileImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            holder.tvInitial.setVisibility(View.GONE);
            ImageUtils.loadImage(context, imageUrl, holder.ivAvatar, true);
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvInitial.setVisibility(View.VISIBLE);
            holder.tvInitial.setText(name.length() > 0
                    ? String.valueOf(name.charAt(0)).toUpperCase() : "?");
        }

        holder.itemView.setOnClickListener(v -> listener.onProfileClicked(profile));
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    static class ProfileViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvInitial;
        TextView tvName;
        TextView tvEmail;
        TextView tvJoinDate;
        TextView tvRoleBadge;

        ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_browse_avatar);
            tvInitial = itemView.findViewById(R.id.tv_browse_initial);
            tvName = itemView.findViewById(R.id.tv_browse_name);
            tvEmail = itemView.findViewById(R.id.tv_browse_email);
            tvJoinDate = itemView.findViewById(R.id.tv_browse_join_date);
            tvRoleBadge = itemView.findViewById(R.id.tv_browse_role_badge);
        }
    }
}
