/*
 * Purpose: RecyclerView adapter for displaying entrants who have been invited
 *          to a private event's waiting list (PENDING_INVITE status).
 *          Each row shows the entrant's name, contact info, a "Pending" badge,
 *          and a Cancel button to revoke the invitation.
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

public class InvitedEntrantAdapter extends RecyclerView.Adapter<InvitedEntrantAdapter.ViewHolder> {

    public interface OnCancelClickListener {
        void onCancelClick(InviteEntrantToWaitingListActivity.InvitedRow row);
    }

    private final List<InviteEntrantToWaitingListActivity.InvitedRow> rows;
    private final OnCancelClickListener listener;

    public InvitedEntrantAdapter(List<InviteEntrantToWaitingListActivity.InvitedRow> rows,
                                  OnCancelClickListener listener) {
        this.rows = rows;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invited_entrant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InviteEntrantToWaitingListActivity.InvitedRow row = rows.get(position);
        holder.tvName.setText(row.name);

        // Build contact line: email and/or phone
        StringBuilder contact = new StringBuilder();
        if (row.email != null && !row.email.isEmpty()) contact.append(row.email);
        if (row.phone != null && !row.phone.isEmpty()) {
            if (contact.length() > 0) contact.append("  ·  ");
            contact.append(row.phone);
        }
        holder.tvContact.setText(contact.toString());

        if (row.isPending) {
            holder.tvStatus.setText("Pending");
            holder.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF00897B)); // teal
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> {
                if (listener != null) listener.onCancelClick(row);
            });
        } else {
            holder.tvStatus.setText("Accepted");
            holder.tvStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF388E3C)); // green
            holder.btnCancel.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvContact;
        TextView tvStatus;
        Button btnCancel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_invited_name);
            tvContact = itemView.findViewById(R.id.tv_invited_contact);
            tvStatus = itemView.findViewById(R.id.tv_invited_status);
            btnCancel = itemView.findViewById(R.id.btn_cancel_invite);
        }
    }
}
