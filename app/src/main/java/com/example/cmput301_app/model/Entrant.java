package com.example.cmput301_app.model;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents an entrant in the application.
 *
 * Extends Profile to inherit common user identity fields such as name,
 * email, phone number, device ID, geolocation, and profile image.
 * An entrant is a user who can browse events, join waiting lists,
 * participate in lottery draws, and accept or decline invitations.
 *
 * Outstanding issues:
 * - Invitation acceptance/declination currently updates local state only;
 *   Firebase sync is handled by EntrantDB.
 * - Registration history outcome tracking depends on LotteryDB status updates.
 */
public class Entrant extends Profile {

    /**
     * List of event IDs for events the entrant is currently on the waiting list for.
     */
    private List<String> waitingListIds;

    /**
     * List of RegistrationRecord objects representing the entrant's history
     * of events they have joined, along with their lottery outcome for each.
     */
    private List<RegistrationRecord> registrationHistory;

    /**
     * Whether the entrant has opted in to receiving notifications
     * from organizers and admins. Lottery result notifications are
     * always delivered regardless of this setting.
     */
    private boolean notificationsEnabled;

    /**
     * Transient/UI property - not saved to Firestore Entrant documents.
     * Used for explicitly rendering UI status badges.
     */
    private String status;


    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Default no-argument constructor required for Firebase deserialization.
     */
    public Entrant() {
        super();
        this.waitingListIds = new ArrayList<>();
        this.registrationHistory = new ArrayList<>();
        this.notificationsEnabled = true;
    }

    /**
     * Constructs an Entrant with required profile fields.
     * Notifications are enabled by default.
     *
     * @param deviceId    the unique device identifier for this user
     * @param name        the full name of the entrant
     * @param email       the email address of the entrant
     * @param phoneNumber the optional phone number of the entrant (pass null if not provided)
     */
    public Entrant(String deviceId, String name, String email, String phoneNumber) {
        super(deviceId, name, email, phoneNumber);
        this.waitingListIds = new ArrayList<>();
        this.registrationHistory = new ArrayList<>();
        this.notificationsEnabled = true;
    }

    // -------------------------------------------------------------------------
    // Waiting List Methods
    // -------------------------------------------------------------------------

    /**
     * Adds an event to the entrant's waiting list.
     * Does nothing if the entrant is already on the waiting list for that event.
     *
     * @param eventId the ID of the event to join the waiting list for
     */
    public void joinWaitingList(String eventId) {
        if (!waitingListIds.contains(eventId)) {
            waitingListIds.add(eventId);
        }
    }

    /**
     * Removes an event from the entrant's waiting list.
     * Does nothing if the entrant is not on the waiting list for that event.
     *
     * @param eventId the ID of the event to leave the waiting list for
     */
    public void leaveWaitingList(String eventId) {
        waitingListIds.remove(eventId);
    }

    /**
     * Returns whether the entrant is currently on the waiting list for a given event.
     *
     * @param eventId the ID of the event to check
     * @return true if the entrant is on the waiting list, false otherwise
     */
    public boolean isOnWaitingList(String eventId) {
        return waitingListIds.contains(eventId);
    }

    // -------------------------------------------------------------------------
    // Invitation Methods
    // -------------------------------------------------------------------------

    /**
     * Accepts a lottery invitation for the given event.
     * Adds a RegistrationRecord with outcome ACCEPTED to the entrant's history
     * and removes the event from the active waiting list.
     *
     * @param eventId the ID of the event whose invitation is being accepted
     */
    public void acceptInvitation(String eventId) {
        waitingListIds.remove(eventId);
        registrationHistory.add(new RegistrationRecord(eventId, RegistrationRecord.Outcome.ACCEPTED));
    }

    /**
     * Declines a lottery invitation for the given event.
     * Adds a RegistrationRecord with outcome DECLINED to the entrant's history
     * and removes the event from the active waiting list.
     *
     * @param eventId the ID of the event whose invitation is being declined
     */
    public void declineInvitation(String eventId) {
        waitingListIds.remove(eventId);
        registrationHistory.add(new RegistrationRecord(eventId, RegistrationRecord.Outcome.DECLINED));
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    /**
     * Returns the list of event IDs the entrant is currently on the waiting list for.
     *
     * @return list of waiting list event IDs
     */
    public List<String> getWaitingListIds() {
        return waitingListIds;
    }

    /**
     * Sets the list of event IDs the entrant is currently on the waiting list for.
     *
     * @param waitingListIds the list of event IDs to assign
     */
    public void setWaitingListIds(List<String> waitingListIds) {
        this.waitingListIds = waitingListIds;
    }

    /**
     * Returns the entrant's full registration history across all events.
     *
     * @return list of RegistrationRecord objects
     */
    public List<RegistrationRecord> getRegistrationHistory() {
        return registrationHistory;
    }

    /**
     * Sets the entrant's registration history.
     *
     * @param registrationHistory the list of RegistrationRecord objects to assign
     */
    public void setRegistrationHistory(List<RegistrationRecord> registrationHistory) {
        this.registrationHistory = registrationHistory;
    }

    /**
     * Returns whether the entrant has notifications enabled for organizer and admin messages.
     *
     * @return true if notifications are enabled, false if opted out
     */
    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    /**
     * Sets whether the entrant receives notifications from organizers and admins.
     *
     * @param notificationsEnabled true to enable, false to opt out
     */
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    /**
     * Returns the transient UI status of this entrant for list rendering.
     */
    @com.google.firebase.firestore.Exclude
    public String getStatus() {
        return status;
    }

    /**
     * Sets the transient UI status of this entrant for list rendering.
     */
    @com.google.firebase.firestore.Exclude
    public void setStatus(String status) {
        this.status = status;
    }

    // -------------------------------------------------------------------------
    // Inner Class
    // -------------------------------------------------------------------------

    /**
     * Represents a single entry in an entrant's registration history.
     * Stores the event ID and the outcome of the entrant's participation.
     */
    public static class RegistrationRecord {

        /**
         * Possible outcomes for a registration entry.
         */
        public enum Outcome {
            /** Entrant is still on the waiting list, lottery has not run yet */
            WAITING,
            /** Entrant was selected by the lottery and has not yet responded */
            SELECTED,
            /** Entrant accepted the invitation and is enrolled */
            ACCEPTED,
            /** Entrant declined the invitation */
            DECLINED,
            /** Entrant was not selected in the lottery draw */
            NOT_SELECTED,
            /** Entrant was cancelled by the organizer */
            CANCELLED
        }

        /** The ID of the event this record refers to */
        private String eventId;

        /** The outcome of this registration entry */
        private Outcome outcome;

        /** Server timestamp of when this record was created/updated (nullable) */
        private com.google.firebase.Timestamp timestamp;

        /**
         * Default no-argument constructor required for Firebase deserialization.
         */
        public RegistrationRecord() {}

        /**
         * Constructs a RegistrationRecord with the given event ID and outcome.
         */
        public RegistrationRecord(String eventId, Outcome outcome) {
            this.eventId = eventId;
            this.outcome = outcome;
            this.timestamp = com.google.firebase.Timestamp.now();
        }

        /**
         * Returns the event ID associated with this registration record.
         *
         * @return the event ID string
         */
        public String getEventId() {
            return eventId;
        }

        /**
         * Sets the event ID for this registration record.
         *
         * @param eventId the event ID to assign
         */
        public void setEventId(String eventId) {
            this.eventId = eventId;
        }

        /**
         * Returns the outcome of this registration entry.
         *
         * @return the Outcome enum value
         */
        public Outcome getOutcome() {
            return outcome;
        }

        /**
         * Sets the outcome of this registration entry.
         *
         * @param outcome the Outcome enum value to assign
         */
        public void setOutcome(Outcome outcome) {
            this.outcome = outcome;
        }

        public com.google.firebase.Timestamp getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(com.google.firebase.Timestamp timestamp) {
            this.timestamp = timestamp;
        }
    }
}