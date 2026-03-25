package com.example.cmput301_app.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Comment;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrganizerCommentAdapter extends RecyclerView.Adapter<OrganizerCommentAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(Comment comment);
    }

    private List<Comment> comments;
    private final OnDeleteClickListener deleteListener;

    public OrganizerCommentAdapter(List<Comment> comments, OnDeleteClickListener deleteListener) {
        this.comments = comments;
        this.deleteListener = deleteListener;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.tvAuthor.setText(comment.getAuthorName() != null ? comment.getAuthorName() : "Unknown");
        holder.tvContent.setText(comment.getContent());

        if (comment.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(comment.getTimestamp().toDate()));
        } else {
            holder.tvTime.setText("");
        }

        if (comment.isOrganizerComment()) {
            holder.tvOrganizerBadge.setVisibility(View.VISIBLE);
            holder.cardView.setCardBackgroundColor(0xFFF4F3FF); // light purple
        } else {
            holder.tvOrganizerBadge.setVisibility(View.GONE);
            holder.cardView.setCardBackgroundColor(0xFFF9FAFB); // light gray
        }

        holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(comment));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvAuthor, tvTime, tvContent, tvOrganizerBadge;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cv_comment_card);
            tvAuthor = itemView.findViewById(R.id.tv_org_comment_author);
            tvTime = itemView.findViewById(R.id.tv_org_comment_time);
            tvContent = itemView.findViewById(R.id.tv_org_comment_content);
            tvOrganizerBadge = itemView.findViewById(R.id.tv_organizer_badge);
            btnDelete = itemView.findViewById(R.id.btn_delete_comment);
        }
    }
}
