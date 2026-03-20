package com.example.cmput301_app.entrant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmput301_app.R;
import com.example.cmput301_app.model.Comment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments;

    public CommentAdapter(List<Comment> comments) {
        this.comments = comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        if (this.comments != null) {
            this.comments.add(comment);
            notifyItemInserted(this.comments.size() - 1);
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAuthor;
        private final TextView tvTime;
        private final TextView tvContent;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tv_comment_author);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
        }

        public void bind(Comment comment) {
            tvAuthor.setText(comment.getAuthorName() != null ? comment.getAuthorName() : "Unknown");
            tvContent.setText(comment.getContent());

            if (comment.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy \u2022 h:mm a", Locale.getDefault());
                tvTime.setText(sdf.format(comment.getTimestamp().toDate()));
            } else {
                tvTime.setText("Just now");
            }
        }
    }
}
