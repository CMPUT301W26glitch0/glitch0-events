package com.example.cmput301_app.model;

import com.google.firebase.Timestamp;

public class Comment {
    private String id;
    private String content;
    private String authorName;
    private Timestamp timestamp;
    private boolean organizerComment;

    public Comment() {
        // REQUIRED: empty constructor for Firestore deserialization
    }

    public Comment(String id, String content, String authorName, Timestamp timestamp, boolean organizerComment) {
        this.id = id;
        this.content = content;
        this.authorName = authorName;
        this.timestamp = timestamp;
        this.organizerComment = organizerComment;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isOrganizerComment() {
        return organizerComment;
    }

    public void setOrganizerComment(boolean organizerComment) {
        this.organizerComment = organizerComment;
    }
}
