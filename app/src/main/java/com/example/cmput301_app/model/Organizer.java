package com.example.cmput301_app.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an organizer in the application.
 *
 * Extends Profile to inherit common user identity fields such as name,
 * email, phone number, device ID, geolocation, and profile image.
 * An organizer is a user who can create and manage events, upload posters,
 * run lottery draws, and send notifications to entrants.
 *
 * Event-specific settings such as geolocation requirements, waiting list
 * limits, and attendee caps are stored on the Event class rather than here,
 * as these settings are per-event and not global to the organizer.
 *
 * Outstanding issues:
 * - Organizer role verification against Firebase is handled by OrganizerDB,
 *   not this class.
 */
public class Organizer extends Profile {

    /**
     * List of event IDs for events this organizer has created.
     */
    private List<String> organizedEventIds;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Default no-argument constructor required for Firebase deserialization.
     */
    public Organizer() {
        super();
        this.organizedEventIds = new ArrayList<>();
    }

    /**
     * Constructs an Organizer with required profile fields.
     *
     * @param deviceId    the unique device identifier for this user
     * @param name        the full name of the organizer
     * @param email       the email address of the organizer
     * @param phoneNumber the optional phone number of the organizer (pass null if not provided)
     */
    public Organizer(String deviceId, String name, String email, String phoneNumber) {
        super(deviceId, name, email, phoneNumber);
        this.organizedEventIds = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Event Management Methods
    // -------------------------------------------------------------------------

    /**
     * Adds an event to this organizer's list of organized events.
     * Does nothing if the event ID is already in the list.
     *
     * @param eventId the ID of the event to add
     */
    public void addEvent(String eventId) {
        if (!organizedEventIds.contains(eventId)) {
            organizedEventIds.add(eventId);
        }
    }

    /**
     * Removes an event from this organizer's list of organized events.
     * Does nothing if the event ID is not in the list.
     *
     * @param eventId the ID of the event to remove
     */
    public void removeEvent(String eventId) {
        organizedEventIds.remove(eventId);
    }

    /**
     * Returns whether this organizer owns the given event.
     *
     * @param eventId the ID of the event to check
     * @return true if the organizer owns the event, false otherwise
     */
    public boolean hasEvent(String eventId) {
        return organizedEventIds.contains(eventId);
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    /**
     * Returns the list of event IDs this organizer has created.
     *
     * @return list of organized event IDs
     */
    public List<String> getOrganizedEventIds() {
        return organizedEventIds;
    }

    /**
     * Sets the list of event IDs this organizer has created.
     *
     * @param organizedEventIds the list of event IDs to assign
     */
    public void setOrganizedEventIds(List<String> organizedEventIds) {
        this.organizedEventIds = organizedEventIds;
    }
}
