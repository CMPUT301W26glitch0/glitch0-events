/*
 * Purpose: RecyclerView adapter for the Admin profile management screen.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Profile;

import java.util.List;

/**
 * Adapter for displaying all user profiles in the admin profile management screen.
 * Each item shows the user's name, email, role badge, and a "Remove" button
 * that shows a confirmation dialog before deletion.
 */
public class AdminProfileAdapter extends RecyclerView.Adapter<AdminProfileAdapter.ViewHolder> {

    /** Callback invoked when the admin confirms removal of a profile. */
    public interface OnRemoveClickListener {
        void onRemoveConfirmed(Profile profile, int position);
    }

    private final List<Profile> profileList;
    private final Context context;
    private final OnRemoveClickListener removeListener;

    public AdminProfileAdapter(List<Profile> profileList, Context context,
                               OnRemoveClickListener removeListener) {
        this.profileList = profileList;
        this.context = context;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Profile profile = profileList.get(position);

        holder.tvName.setText(profile.getName() != null ? profile.getName() : "Unknown User");

        String email = profile.getEmail();
        holder.tvEmail.setText(email != null && !email.isEmpty() ? email : "No email");

        String deviceId = profile.getDeviceId();
        String shortId = (deviceId != null && deviceId.length() >= 4)
                ? deviceId.substring(0, 4) : "????";
        holder.tvId.setText("ID: #" + shortId);

        // Role badge styling
        String role = profile.getRole();
        if ("organizer".equalsIgnoreCase(role)) {
            holder.tvRoleBadge.setText("Organizer");
            holder.tvRoleBadge.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.primary_blue));
            holder.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else if ("admin".equalsIgnoreCase(role)) {
            holder.tvRoleBadge.setText("Admin");
            holder.tvRoleBadge.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF7F56D9));
            holder.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.tvRoleBadge.setText("Entrant");
            holder.tvRoleBadge.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, R.color.badge_green_bg));
            holder.tvRoleBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.badge_green_text));
        }

        holder.btnRemove.setOnClickListener(v ->
                showConfirmationDialog(profile, holder.getAdapterPosition()));
    }

    private void showConfirmationDialog(Profile profile, int position) {
        String name = profile.getName() != null ? profile.getName() : "this user";
        new AlertDialog.Builder(context)
                .setTitle("Remove Profile")
                .setMessage("Remove profile for \"" + name + "\"?\n\n"
                        + "All associated waiting list records will be deleted. "
                        + "The user will be signed out on their next app launch.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    if (removeListener != null) {
                        removeListener.onRemoveConfirmed(profile, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoleBadge, tvName, tvEmail, tvId;
        Button btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoleBadge = itemView.findViewById(R.id.tv_profile_role_badge);
            tvName = itemView.findViewById(R.id.tv_profile_name);
            tvEmail = itemView.findViewById(R.id.tv_profile_email);
            tvId = itemView.findViewById(R.id.tv_profile_id);
            btnRemove = itemView.findViewById(R.id.btn_profile_remove);
        }
    }
}
