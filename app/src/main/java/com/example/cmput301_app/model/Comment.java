package com.example.cmput301_app.model;

import com.google.firebase.Timestamp;

public class Comment {
    private String content;
    private String authorName;
    private Timestamp timestamp;

    public Comment() {
        // REQUIRED: empty constructor for Firestore deserialization
    }

    public Comment(String content, String authorName, Timestamp timestamp) {
        this.content = content;
        this.authorName = authorName;
        this.timestamp = timestamp;
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
}
