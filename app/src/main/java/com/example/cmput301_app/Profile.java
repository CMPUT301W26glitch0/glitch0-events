package com.example.cmput301_app;

/**
 * Represents a user profile in the application.
 *
 * This is the base class for all user roles in the system. It stores
 * common identity information shared by both Entrant and Organizer.
 * Device identification is used as the primary means of recognizing
 * a returning user without requiring a username or password.
 *
 * Outstanding issues:
 * - Geolocation field is declared but location capture logic is not yet implemented.
 * - Profile image upload logic is handled externally via EntrantDB and OrganizerDB;
 *   this class only stores the resulting download URL.
 */
public class Profile {

    /** Unique identifier tied to the user's device */
    private String deviceId;

    /** Full name of the user */
    private String name;

    /** Email address of the user */
    private String email;

    /** Optional phone number of the user */
    private String phoneNumber;

    /** Last known geolocation of the user, stored as "latitude,longitude" */
    private String geolocation;

    /** Firebase Storage download URL for the user's profile photo */
    private String profileImageUrl;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Default no-argument constructor required for Firebase deserialization.
     */
    public Profile() {}

    /**
     * Constructs a Profile with required fields.
     *
     * @param deviceId    the unique device identifier for this user
     * @param name        the full name of the user
     * @param email       the email address of the user
     * @param phoneNumber the optional phone number of the user (pass null if not provided)
     */
    public Profile(String deviceId, String name, String email, String phoneNumber) {
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Returns the unique device identifier associated with this profile.
     *
     * @return the device ID string
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Returns the full name of the user.
     *
     * @return the user's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the email address of the user.
     *
     * @return the user's email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the phone number of the user, or null if not provided.
     *
     * @return the user's phone number, or null
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Returns the last known geolocation of the user.
     *
     * @return a string in "latitude,longitude" format, or null if not set
     */
    public String getGeolocation() {
        return geolocation;
    }

    /**
     * Returns the Firebase Storage download URL for the user's profile photo.
     *
     * @return the profile image URL string, or null if no photo has been uploaded
     */
    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    /**
     * Sets the unique device identifier for this profile.
     *
     * @param deviceId the device ID to assign
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Sets the full name of the user.
     *
     * @param name the name to assign
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the email address of the user.
     *
     * @param email the email to assign
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Sets the phone number of the user.
     *
     * @param phoneNumber the phone number to assign, or null to clear it
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Sets the geolocation of the user.
     *
     * @param geolocation a string in "latitude,longitude" format
     */
    public void setGeolocation(String geolocation) {
        this.geolocation = geolocation;
    }

    /**
     * Sets the Firebase Storage download URL for the user's profile photo.
     *
     * @param profileImageUrl the URL string to assign, or null to clear the photo
     */
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}