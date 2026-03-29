/*
 * Purpose: Database helper class handling Organizer-specific Firestore queries.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.database;

import com.example.cmput301_app.model.Organizer;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all Firestore database operations for Organizer objects.
 *
 * This class is the sole point of contact between the application and
 * Firestore for organizer-related data. It follows the repository pattern,
 * meaning no other class should read or write organizer data to Firestore
 * directly — all operations go through this class.
 *
 * Organizer documents are stored in the "users" collection using the
 * organizer's deviceId as the document ID, alongside entrant documents.
 * The "role" field is set to "organizer" to distinguish them from entrants.
 *
 * Outstanding issues:
 * - deleteOrganizer() removes the organizer's document but does not yet
 *   remove their associated events, lottery pools, posters, and notifications
 *   from Firestore. This cleanup should be coordinated through AdminDB
 *   when removing an organizer who violates app policy.
 */
public class OrganizerDB {

    /** The Firestore collection name for all user documents */
    private static final String COLLECTION = "users";

    /** The Firestore instance used for all database operations */
    private FirebaseFirestore db;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an OrganizerDB and initializes the Firestore instance.
     */
    public OrganizerDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    // -------------------------------------------------------------------------
    // CRUD Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new organizer document in the "users" collection.
     * Uses the organizer's deviceId as the document ID.
     * If a document with the same deviceId already exists, it will be overwritten.
     *
     * @param organizer       the Organizer object to store in Firestore
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void createOrganizer(Organizer organizer,
                                OnSuccessListener<Void> successListener,
                                OnFailureListener failureListener) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", organizer.getDeviceId());
        data.put("name", organizer.getName());
        data.put("email", organizer.getEmail());
        data.put("phoneNumber", organizer.getPhoneNumber());
        data.put("geolocation", organizer.getGeolocation());
        data.put("profileImageUrl", organizer.getProfileImageUrl() != null ? organizer.getProfileImageUrl() : "");
        data.put("role", "organizer");
        data.put("organizedEventIds", organizer.getOrganizedEventIds());

        db.collection(COLLECTION)
                .document(organizer.getDeviceId())
                .set(data)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Fetches an organizer document from Firestore by device ID and returns
     * it as an Organizer object via the success listener.
     *
     * @param deviceId        the device ID of the organizer to fetch
     * @param successListener called with the Organizer object if found, or null if not found
     * @param failureListener called if the operation fails
     */
    public void getOrganizer(String deviceId,
                             OnSuccessListener<Organizer> successListener,
                             OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Organizer organizer = documentSnapshot.toObject(Organizer.class);
                        successListener.onSuccess(organizer);
                    } else {
                        successListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Updates an existing organizer document in Firestore with the current
     * state of the provided Organizer object. The document is identified by
     * the organizer's deviceId.
     *
     * @param organizer       the Organizer object containing updated data
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void updateOrganizer(Organizer organizer,
                                OnSuccessListener<Void> successListener,
                                OnFailureListener failureListener) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", organizer.getName());
        data.put("email", organizer.getEmail());
        data.put("phoneNumber", organizer.getPhoneNumber());
        data.put("geolocation", organizer.getGeolocation());
        data.put("profileImageUrl", organizer.getProfileImageUrl());

        db.collection(COLLECTION)
                .document(organizer.getDeviceId())
                .update(data)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Deletes an organizer document from Firestore by device ID.
     * This method is also used by AdminDB when removing an organizer
     * who violates app policy.
     *
     * Note: this method removes the organizer's profile document only.
     * Removal of the organizer's associated events, lottery pools, posters,
     * and notifications should be coordinated through AdminDB.
     *
     * @param deviceId        the device ID of the organizer to delete
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void deleteOrganizer(String deviceId,
                                OnSuccessListener<Void> successListener,
                                OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(deviceId)
                .delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Organized Event Operations
    // -------------------------------------------------------------------------

    /**
     * Adds an event ID to the organizer's organizedEventIds array in Firestore.
     * Uses Firestore's ArrayUnion operation to safely add without duplicates.
     *
     * @param deviceId        the device ID of the organizer
     * @param eventId         the ID of the event to add
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void addOrganizedEvent(String deviceId, String eventId,
                                  OnSuccessListener<Void> successListener,
                                  OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(deviceId)
                .update("organizedEventIds", FieldValue.arrayUnion(eventId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Removes an event ID from the organizer's organizedEventIds array in Firestore.
     * Uses Firestore's ArrayRemove operation to safely remove the entry.
     *
     * @param deviceId        the device ID of the organizer
     * @param eventId         the ID of the event to remove
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void removeOrganizedEvent(String deviceId, String eventId,
                                     OnSuccessListener<Void> successListener,
                                     OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(deviceId)
                .update("organizedEventIds", FieldValue.arrayRemove(eventId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }
}