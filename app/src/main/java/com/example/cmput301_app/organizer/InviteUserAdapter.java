/*
 * Purpose: RecyclerView adapter for displaying a searchable list of users
 *          that can be invited to a private event's waiting list.
 * Design Pattern: Standard Android adapter pattern
 * Outstanding Issues: None
 */
package com.example.cmput301_app.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;

import java.util.List;

public class InviteUserAdapter extends RecyclerView.Adapter<InviteUserAdapter.ViewHolder> {

    public interface OnInviteClickListener {
        void onInviteClick(InviteEntrantToWaitingListActivity.UserRow user);
    }

    private final List<InviteEntrantToWaitingListActivity.UserRow> users;
    private final OnInviteClickListener listener;
    private final String buttonLabel;

    public InviteUserAdapter(List<InviteEntrantToWaitingListActivity.UserRow> users,
                             OnInviteClickListener listener) {
        this(users, listener, null);
    }

    public InviteUserAdapter(List<InviteEntrantToWaitingListActivity.UserRow> users,
                             OnInviteClickListener listener, String buttonLabel) {
        this.users = users;
        this.listener = listener;
        this.buttonLabel = buttonLabel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invite_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InviteEntrantToWaitingListActivity.UserRow user = users.get(position);
        holder.tvName.setText(user.name);
        holder.tvEmail.setText(user.email);
        if (user.phone != null && !user.phone.isEmpty()) {
            holder.tvPhone.setText(user.phone);
            holder.tvPhone.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.tvPhone.setVisibility(android.view.View.GONE);
        }
        if (buttonLabel != null) holder.btnInvite.setText(buttonLabel);
        holder.btnInvite.setOnClickListener(v -> {
            if (listener != null) listener.onInviteClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvEmail;
        TextView tvPhone;
        Button btnInvite;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_invite_user_name);
            tvEmail = itemView.findViewById(R.id.tv_invite_user_email);
            tvPhone = itemView.findViewById(R.id.tv_invite_user_phone);
            btnInvite = itemView.findViewById(R.id.btn_invite_user);
        }
    }
}
