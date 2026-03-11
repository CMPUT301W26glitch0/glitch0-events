package com.example.cmput301_app.database;

import com.example.cmput301_app.model.Event;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all Firestore database operations for Event objects.
 *
 * This class is the sole point of contact between the application and
 * Firestore for event-related data. It follows the repository pattern,
 * meaning no other class should read or write event data to Firestore
 * directly — all operations go through this class.
 *
 * Event documents are stored in the "events" collection using a Firestore
 * auto-generated document ID as the event ID.
 *
 * This class also provides updatePosterUrl() which is called by PosterDB
 * to keep the posterUrl field on the Event document in sync whenever a
 * poster is uploaded or removed.
 *
 * Outstanding issues:
 * - deleteEvent() removes the event document but does not yet remove
 *   associated lottery pool, poster, and notification documents. This
 *   cleanup should be coordinated through AdminDB once all DB classes
 *   are in place.
 * - getAllEvents() currently returns all events regardless of registration
 *   status. Filtering by open registration window should be added once
 *   the browse events screen is implemented.
 */
public class EventDB {

    /** The Firestore collection name for all event documents */
    private static final String COLLECTION = "events";

    /** The Firestore instance used for all database operations */
    private FirebaseFirestore db;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an EventDB and initializes the Firestore instance.
     */
    public EventDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    // -------------------------------------------------------------------------
    // CRUD Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new event document in the "events" collection.
     * Firestore auto-generates the document ID, which is then set back
     * on the Event object and passed to the success listener.
     *
     * @param event           the Event object to store in Firestore
     * @param successListener called with the updated Event object containing the generated ID
     * @param failureListener called if the operation fails
     */
    public void createEvent(Event event,
                            OnSuccessListener<Event> successListener,
                            OnFailureListener failureListener) {
        Map<String, Object> data = eventToMap(event);

        db.collection(COLLECTION)
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    event.setEventId(documentReference.getId());
                    documentReference.update("eventId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> successListener.onSuccess(event))
                            .addOnFailureListener(failureListener);
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Fetches an event document from Firestore by event ID and returns
     * it as an Event object via the success listener.
     *
     * @param eventId         the ID of the event to fetch
     * @param successListener called with the Event object if found, or null if not found
     * @param failureListener called if the operation fails
     */
    public void getEvent(String eventId,
                         OnSuccessListener<Event> successListener,
                         OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        successListener.onSuccess(event);
                    } else {
                        successListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Updates an existing event document in Firestore with the current
     * state of the provided Event object. The document is identified by
     * the event's eventId.
     *
     * @param event           the Event object containing updated data
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void updateEvent(Event event,
                            OnSuccessListener<Void> successListener,
                            OnFailureListener failureListener) {
        Map<String, Object> data = eventToMap(event);

        db.collection(COLLECTION)
                .document(event.getEventId())
                .update(data)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Deletes an event document from Firestore by event ID.
     * This method is also used by AdminDB when removing an event.
     *
     * Note: this method removes the event document only. Removal of
     * associated lottery pool, poster, and notification documents should
     * be coordinated through AdminDB once all DB classes are in place.
     *
     * @param eventId         the ID of the event to delete
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void deleteEvent(String eventId,
                            OnSuccessListener<Void> successListener,
                            OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Query Operations
    // -------------------------------------------------------------------------

    /**
     * Fetches all event documents from Firestore and returns them as a
     * list of Event objects via the success listener.
     * Used to populate the browse events screen (US 01.01.03).
     *
     * @param successListener called with the list of Event objects
     * @param failureListener called if the operation fails
     */
    public void getAllEvents(OnSuccessListener<List<Event>> successListener,
                             OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        events.add(event);
                    }
                    successListener.onSuccess(events);
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Fetches all events belonging to a specific organizer and returns them
     * as a list of Event objects via the success listener.
     * Used to populate the organizer's event management screen.
     *
     * @param organizerId     the device ID of the organizer
     * @param successListener called with the list of Event objects
     * @param failureListener called if the operation fails
     */
    public void getEventsByOrganizer(String organizerId,
                                     OnSuccessListener<List<Event>> successListener,
                                     OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        events.add(event);
                    }
                    successListener.onSuccess(events);
                })
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Waiting List Operations
    // -------------------------------------------------------------------------

    /**
     * Adds an entrant's device ID to the event's waitingListIds array in Firestore.
     * Uses Firestore's ArrayUnion operation to safely add without duplicates.
     *
     * @param eventId         the ID of the event
     * @param deviceId        the device ID of the entrant to add
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void addToWaitingList(String eventId, String deviceId,
                                 OnSuccessListener<Void> successListener,
                                 OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("waitingListIds", FieldValue.arrayUnion(deviceId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Removes an entrant's device ID from the event's waitingListIds array in Firestore.
     * Uses Firestore's ArrayRemove operation to safely remove the entry.
     *
     * @param eventId         the ID of the event
     * @param deviceId        the device ID of the entrant to remove
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void removeFromWaitingList(String eventId, String deviceId,
                                      OnSuccessListener<Void> successListener,
                                      OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("waitingListIds", FieldValue.arrayRemove(deviceId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Poster and QR Code Operations
    // -------------------------------------------------------------------------

    /**
     * Updates the posterUrl field on an event document in Firestore.
     * This method is called by PosterDB to keep the Event document in sync
     * whenever a poster is uploaded or removed.
     *
     * @param eventId         the ID of the event to update
     * @param posterUrl       the new poster URL to assign, or null to clear it
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void updatePosterUrl(String eventId, String posterUrl,
                                OnSuccessListener<Void> successListener,
                                OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("posterUrl", posterUrl)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Updates the qrCode field on an event document in Firestore.
     * Called after QR code generation to store the result on the event.
     *
     * @param eventId         the ID of the event to update
     * @param qrCode          the generated QR code string to assign
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void updateQrCode(String eventId, String qrCode,
                             OnSuccessListener<Void> successListener,
                             OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("qrCode", qrCode)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Converts an Event object into a Map for Firestore storage.
     * Used internally by createEvent() and updateEvent().
     *
     * @param event the Event object to convert
     * @return a Map containing all event fields
     */
    private Map<String, Object> eventToMap(Event event) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", event.getEventId());
        data.put("name", event.getName());
        data.put("description", event.getDescription());
        data.put("location", event.getLocation());
        data.put("date", event.getDate());
        data.put("price", event.getPrice());
        data.put("capacity", event.getCapacity());
        data.put("registrationOpen", event.getRegistrationOpen());
        data.put("registrationClose", event.getRegistrationClose());
        data.put("organizerId", event.getOrganizerId());
        data.put("posterUrl", event.getPosterUrl());
        data.put("qrCode", event.getQrCode());
        data.put("geolocationEnabled", event.isGeolocationEnabled());
        data.put("waitingListLimit", event.getWaitingListLimit());
        data.put("waitingListIds", event.getWaitingListIds());
        return data;
    }
}