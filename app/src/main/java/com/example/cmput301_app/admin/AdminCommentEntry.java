/*
 * Purpose: Wrapper model that pairs a Comment with its source event context for admin display.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.admin;

import com.example.cmput301_app.model.Comment;

/**
 * Pairs a {@link Comment} with the event it belongs to, so the admin comment
 * browse screen can display both the comment content and its event context.
 */
public class AdminCommentEntry {
    private final Comment comment;
    private final String eventId;
    private final String eventName;

    public AdminCommentEntry(Comment comment, String eventId, String eventName) {
        this.comment = comment;
        this.eventId = eventId;
        this.eventName = eventName;
    }

    public Comment getComment() { return comment; }
    public String getEventId()  { return eventId; }
    public String getEventName() { return eventName != null ? eventName : "Unknown Event"; }
}
