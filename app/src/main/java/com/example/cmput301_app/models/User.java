package com.example.cmput301_app.models;

import java.util.ArrayList;
import java.util.List;

/**
 * User model representing entrants, organizers and admins
 */
public class User {
    private String userId;
    private String deviceId;
    private String name;
    private String email;
    private String phoneNumber;
    private String role;
    private String profileImageUrl;
    private boolean notificationsEnabled;
    private List<String> joinedEvents;
    private List<String> selectedEvents;
    private List<String> enrolledEvents;
    private long createdAt;
    private long updatedAt;

    /**
     * Empty constructor required for Firebase Firestore
     */
    public User() {
        this.joinedEvents = new ArrayList<>();
        this.selectedEvents = new ArrayList<>();
        this.enrolledEvents = new ArrayList<>();
    }

    /**
     * Constructor for creating new user with required fields
     *
     * @param deviceId Unique device identifier
     * @param name User's full name
     * @param email User's email address
     */
    public User(String deviceId, String name, String email) {
        this();
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.role = "entrant"; // default role
        this.notificationsEnabled = true; // default enabled
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters

    /**
     * Gets the Firestore document ID
     * @return User ID
     */
    public String getUserId() { return userId; }

    /**
     * Sets the Firestore document ID
     * @param userId User ID to set
     */
    public void setUserId(String userId) { this.userId = userId; }

    /**
     * Gets the device identifier
     * @return Device ID
     */
    public String getDeviceId() { return deviceId; }

    /**
     * Sets the device identifier
     * @param deviceId Device ID to set
     */
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    /**
     * Gets the user's name
     * @return User name
     */
    public String getName() { return name; }

    /**
     * Sets the user's name
     * @param name Name to set
     */
    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Gets the user's email
     * @return Email address
     */
    public String getEmail() { return email; }

    /**
     * Sets the user's email
     * @param email Email to set
     */
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Gets the user's phone number
     * @return Phone number or null if not set
     */
    public String getPhoneNumber() { return phoneNumber; }

    /**
     * Sets the user's phone number
     * @param phoneNumber Phone number to set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Gets the user's role
     * @return Role ("entrant", "organizer", "admin")
     */
    public String getRole() { return role; }

    /**
     * Sets the user's role
     * @param role Role to set
     */
    public void setRole(String role) { this.role = role; }

    /**
     * Gets the profile image URL
     * @return Image URL or null
     */
    public String getProfileImageUrl() { return profileImageUrl; }

    /**
     * Sets the profile image URL
     * @param profileImageUrl URL to set
     */
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Checks if notifications are enabled
     * @return true if notifications enabled
     */
    public boolean isNotificationsEnabled() { return notificationsEnabled; }

    /**
     * Sets notification preference
     * @param notificationsEnabled true to enable notifications
     */
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    /**
     * Gets list of event IDs user joined waiting list for
     * @return List of event IDs
     */
    public List<String> getJoinedEvents() { return joinedEvents; }

    /**
     * Sets joined events list
     * @param joinedEvents List of event IDs
     */
    public void setJoinedEvents(List<String> joinedEvents) { this.joinedEvents = joinedEvents; }

    /**
     * Gets list of event IDs user was selected for
     * @return List of event IDs
     */
    public List<String> getSelectedEvents() { return selectedEvents; }

    /**
     * Sets selected events list
     * @param selectedEvents List of event IDs
     */
    public void setSelectedEvents(List<String> selectedEvents) { this.selectedEvents = selectedEvents; }

    /**
     * Gets list of event IDs user enrolled in
     * @return List of event IDs
     */
    public List<String> getEnrolledEvents() { return enrolledEvents; }

    /**
     * Sets enrolled events list
     * @param enrolledEvents List of event IDs
     */
    public void setEnrolledEvents(List<String> enrolledEvents) { this.enrolledEvents = enrolledEvents; }

    /**
     * Gets creation timestamp
     * @return Creation time in milliseconds
     */
    public long getCreatedAt() { return createdAt; }

    /**
     * Sets creation timestamp
     * @param createdAt Time in milliseconds
     */
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /**
     * Gets last update timestamp
     * @return Update time in milliseconds
     */
    public long getUpdatedAt() { return updatedAt; }

    /**
     * Sets update timestamp
     * @param updatedAt Time in milliseconds
     */
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Checks if user is an organizer
     * @return true if role is organizer
     */
    public boolean isOrganizer() {
        return "organizer".equals(role);
    }

    /**
     * Checks if user is an admin
     * @return true if role is admin
     */
    public boolean isAdmin() {
        return "admin".equals(role);
    }
}

