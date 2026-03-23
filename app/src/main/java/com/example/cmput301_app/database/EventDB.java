/*
 * Purpose: Database helper class to manage Event data within Firestore.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.database;

import android.util.Log;
import com.example.cmput301_app.model.Event;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDB {
    private static final String COLLECTION = "events";
    private FirebaseFirestore db;

    public EventDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Creates a new event in Firestore using a Map for maximum reliability.
     */
    public void createEvent(Event event,
                            OnSuccessListener<Event> successListener,
                            OnFailureListener failureListener) {
        DocumentReference docRef = db.collection(COLLECTION).document();
        String generatedId = docRef.getId();
        event.setEventId(generatedId);

        // Generate the unique promotional QR code link
        String qrData = "event_details:" + generatedId;
        event.setQrCode(qrData);

        Map<String, Object> data = getEventMap(event);
        data.put("eventId", generatedId);
        data.put("qrCode", qrData);
        data.put("waitingListCount", 0);

        docRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d("EventDB", "Event successfully written to /events/" + generatedId);
                    successListener.onSuccess(event);
                })
                .addOnFailureListener(e -> {
                    Log.e("EventDB", "Failed to write event", e);
                    failureListener.onFailure(e);
                });
    }

    /**
     * Updates an existing event in Firestore.
     */
    public void updateEvent(Event event,
                            OnSuccessListener<Void> successListener,
                            OnFailureListener failureListener) {
        if (event.getEventId() == null) {
            failureListener.onFailure(new Exception("Event ID is null"));
            return;
        }

        Map<String, Object> data = getEventMap(event);

        db.collection(COLLECTION)
                .document(event.getEventId())
                .update(data)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Deletes an event from Firestore.
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

    private Map<String, Object> getEventMap(Event event) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", event.getName());
        data.put("description", event.getDescription());
        data.put("location", event.getLocation());
        data.put("category", event.getCategory());
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
        data.put("confirmedAttendeesIds", event.getConfirmedAttendeesIds());
        return data;
    }

    /**
     * Fetches all events from Firestore.
     */
    public void getAllEvents(OnSuccessListener<List<Event>> successListener,
                             OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            if (event != null) {
                                event.setEventId(document.getId());
                                events.add(event);
                            }
                        } catch (Exception e) {
                            Log.e("EventDB", "Error parsing event: " + document.getId(), e);
                        }
                    }
                    successListener.onSuccess(events);
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Sets up a real-time listener for events created by a specific organizer.
     */
    public void getEventsByOrganizer(String organizerId, EventListener<QuerySnapshot> listener) {
        db.collection(COLLECTION)
                .whereEqualTo("organizerId", organizerId)
                .addSnapshotListener(listener);
    }

    /**
     * Fetches a single event by ID.
     */
    public void getEvent(String eventId,
                         OnSuccessListener<Event> successListener,
                         OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        try {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                event.setEventId(doc.getId());
                                successListener.onSuccess(event);
                            }
                        } catch (Exception e) {
                            failureListener.onFailure(e);
                        }
                    } else {
                        successListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Adds a deviceId to the event's waitingListIds array in Firestore.
     * Also increments the waitingListCount field.
     * Uses Firestore's ArrayUnion to safely add without duplicates.
     *
     * @param eventId         the ID of the event
     * @param deviceId        the device ID of the entrant joining the waiting list
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void addToWaitingList(String eventId, String deviceId,
                                 OnSuccessListener<Void> successListener,
                                 OnFailureListener failureListener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("waitingListIds", com.google.firebase.firestore.FieldValue.arrayUnion(deviceId));
        updates.put("waitingListCount", com.google.firebase.firestore.FieldValue.increment(1));

        db.collection(COLLECTION)
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
    /**
     * Removes a deviceId from the event's waitingListIds array in Firestore.
     * Also decrements the waitingListCount field.
     * Uses Firestore's ArrayRemove to safely remove the deviceId.
     *
     * @param eventId         the ID of the event
     * @param deviceId        the device ID of the entrant leaving the waiting list
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void removeFromWaitingList(String eventId, String deviceId,
                                      OnSuccessListener<Void> successListener,
                                      OnFailureListener failureListener) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("waitingListIds", com.google.firebase.firestore.FieldValue.arrayRemove(deviceId));
        updates.put("waitingListCount", com.google.firebase.firestore.FieldValue.increment(-1));

        db.collection(COLLECTION)
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Atomically adds a given device ID to an event's confirmed attendees list in Firestore.
     *
     * @param eventId         the ID of the event
     * @param deviceId        the device ID of the entrant who accepted
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void addToConfirmedAttendees(String eventId, String deviceId,
                                        OnSuccessListener<Void> successListener,
                                        OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("confirmedAttendeesIds", FieldValue.arrayUnion(deviceId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Adds a device ID to the event's pendingCoOrganizerInvites array.
     *
     * @param eventId         the ID of the event
     * @param deviceId        the device ID of the entrant being invited
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void addPendingCoOrganizerInvite(String eventId, String deviceId,
                                             OnSuccessListener<Void> successListener,
                                             OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("pendingCoOrganizerInvites", FieldValue.arrayUnion(deviceId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Accepts a co-organizer invitation: moves deviceId from pendingCoOrganizerInvites
     * to coOrganizerIds, and removes them from the waiting list.
     *
     * @param eventId         the ID of the event
     * @param deviceId        the device ID of the entrant accepting the invitation
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void acceptCoOrganizerInvite(String eventId, String deviceId,
                                         OnSuccessListener<Void> successListener,
                                         OnFailureListener failureListener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("pendingCoOrganizerInvites", FieldValue.arrayRemove(deviceId));
        updates.put("coOrganizerIds", FieldValue.arrayUnion(deviceId));
        updates.put("waitingListIds", FieldValue.arrayRemove(deviceId));
        updates.put("waitingListCount", FieldValue.increment(-1));

        db.collection(COLLECTION)
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Stores the geographic location at which an entrant joined the waiting list.
     * The location is written under {@code waitingListLocations.<deviceId>} in the event document.
     *
     * @param eventId         the ID of the event
     * @param deviceId        the device ID of the entrant
     * @param latitude        latitude of the entrant's location
     * @param longitude       longitude of the entrant's location
     * @param joinedAt        timestamp when the entrant joined
     * @param successListener called when the write completes
     * @param failureListener called if the write fails
     */
    public void saveWaitingListLocation(String eventId, String deviceId,
                                        double latitude, double longitude,
                                        com.google.firebase.Timestamp joinedAt,
                                        OnSuccessListener<Void> successListener,
                                        OnFailureListener failureListener) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("joinedAt", joinedAt);

        db.collection(COLLECTION)
                .document(eventId)
                .update("waitingListLocations." + deviceId, locationData)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Declines a co-organizer invitation: removes deviceId from pendingCoOrganizerInvites.
     *
     * @param eventId         the ID of the event
     * @param deviceId        the device ID of the entrant declining the invitation
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void declineCoOrganizerInvite(String eventId, String deviceId,
                                          OnSuccessListener<Void> successListener,
                                          OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("pendingCoOrganizerInvites", FieldValue.arrayRemove(deviceId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }
}
