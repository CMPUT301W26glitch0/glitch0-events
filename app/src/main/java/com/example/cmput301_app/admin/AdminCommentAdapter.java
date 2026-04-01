/*
 * Purpose: RecyclerView adapter for the admin comment browse screen.
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
import com.example.cmput301_app.model.Comment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Displays a list of {@link AdminCommentEntry} items for the admin comment
 * management screen. Each row shows the event name, author, timestamp,
 * comment content, and a delete button.
 */
public class AdminCommentAdapter
        extends RecyclerView.Adapter<AdminCommentAdapter.CommentViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(AdminCommentEntry entry, int position);
    }

    private final List<AdminCommentEntry> entries;
    private final Context context;
    private final OnDeleteClickListener deleteListener;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault());

    public AdminCommentAdapter(List<AdminCommentEntry> entries,
                               Context context,
                               OnDeleteClickListener deleteListener) {
        this.entries = entries;
        this.context = context;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_admin_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        AdminCommentEntry entry = entries.get(position);
        Comment comment = entry.getComment();

        holder.tvEventName.setText(entry.getEventName());
        holder.tvAuthor.setText(comment.getAuthorName() != null ? comment.getAuthorName() : "Unknown");
        holder.tvContent.setText(comment.getContent() != null ? comment.getContent() : "");

        if (comment.getTimestamp() != null) {
            holder.tvTimestamp.setText(sdf.format(comment.getTimestamp().toDate()));
        } else {
            holder.tvTimestamp.setText("—");
        }

        if (comment.isOrganizerComment()) {
            holder.tvOrganizerBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvOrganizerBadge.setVisibility(View.GONE);
        }

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID) {
                deleteListener.onDeleteClick(entry, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventName;
        TextView tvAuthor;
        TextView tvOrganizerBadge;
        TextView tvTimestamp;
        TextView tvContent;
        ImageView btnDelete;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName      = itemView.findViewById(R.id.tv_admin_comment_event);
            tvAuthor         = itemView.findViewById(R.id.tv_admin_comment_author);
            tvOrganizerBadge = itemView.findViewById(R.id.tv_admin_comment_organizer_badge);
            tvTimestamp      = itemView.findViewById(R.id.tv_admin_comment_timestamp);
            tvContent        = itemView.findViewById(R.id.tv_admin_comment_content);
            btnDelete        = itemView.findViewById(R.id.btn_admin_comment_delete);
        }
    }
}
