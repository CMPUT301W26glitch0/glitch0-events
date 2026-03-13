/*
 * Purpose: Database helper class for Entrant-related Firestore operations.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.database;

import com.example.cmput301_app.model.Entrant;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles all Firestore database operations for Entrant objects.
 *
 * This class is the sole point of contact between the application and
 * Firestore for entrant-related data. It follows the repository pattern,
 * meaning no other class should read or write entrant data to Firestore
 * directly — all operations go through this class.
 *
 * Entrant documents are stored in the "users" collection using the
 * entrant's deviceId as the document ID. This allows direct document
 * lookups without requiring a query.
 *
 * Outstanding issues:
 * - deleteEntrant() removes the entrant's document but does not yet
 *   remove their deviceId from the waitingListIds array on Event documents.
 *   This cleanup should be implemented once EventDB is in place.
 */
public class EntrantDB {

    /** The Firestore collection name for all user documents */
    private static final String COLLECTION = "users";

    /** The Firestore instance used for all database operations */
    private FirebaseFirestore db;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an EntrantDB and initializes the Firestore instance.
     */
    public EntrantDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    // -------------------------------------------------------------------------
    // CRUD Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new entrant document in the "users" collection.
     * Uses the entrant's deviceId as the document ID.
     * If a document with the same deviceId already exists, it will be overwritten.
     *
     * @param entrant         the Entrant object to store in Firestore
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void createEntrant(Entrant entrant,
                              OnSuccessListener<Void> successListener,
                              OnFailureListener failureListener) {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", entrant.getDeviceId());
        data.put("name", entrant.getName());
        data.put("email", entrant.getEmail());
        data.put("phoneNumber", entrant.getPhoneNumber());
        data.put("geolocation", entrant.getGeolocation());
        data.put("profileImageUrl", entrant.getProfileImageUrl());
        data.put("role", "entrant");
        data.put("waitingListIds", entrant.getWaitingListIds());
        data.put("registrationHistory", entrant.getRegistrationHistory());
        data.put("notificationsEnabled", entrant.isNotificationsEnabled());

        db.collection(COLLECTION)
                .document(entrant.getDeviceId())
                .set(data)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Fetches an entrant document from Firestore by device ID and returns
     * it as an Entrant object via the success listener.
     *
     * @param deviceId        the device ID of the entrant to fetch
     * @param successListener called with the Entrant object if found, or null if not found
     * @param failureListener called if the operation fails
     */
    public void getEntrant(String deviceId,
                           OnSuccessListener<Entrant> successListener,
                           OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(deviceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Entrant entrant = documentSnapshot.toObject(Entrant.class);
                        successListener.onSuccess(entrant);
                    } else {
                        successListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Updates an existing entrant document in Firestore with the current
     * state of the provided Entrant object. The document is identified by
     * the entrant's deviceId.
     *
     * @param entrant         the Entrant object containing updated data
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void updateEntrant(Entrant entrant,
                              OnSuccessListener<Void> successListener,
                              OnFailureListener failureListener) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", entrant.getName());
        data.put("email", entrant.getEmail());
        data.put("phoneNumber", entrant.getPhoneNumber());
        data.put("geolocation", entrant.getGeolocation());
        data.put("profileImageUrl", entrant.getProfileImageUrl());
        data.put("notificationsEnabled", entrant.isNotificationsEnabled());

        db.collection(COLLECTION)
                .document(entrant.getDeviceId())
                .update(data)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Deletes an entrant document from Firestore by device ID.
     *
     * Note: this method removes the entrant's profile document only.
     * Removal of the entrant's deviceId from Event waiting lists should
     * be handled by EventDB once it is implemented.
     *
     * @param deviceId        the device ID of the entrant to delete
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void deleteEntrant(String deviceId,
                              OnSuccessListener<Void> successListener,
                              OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(deviceId)
                .delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Waiting List Operations
    // -------------------------------------------------------------------------

    /**
     * Adds an event ID to the entrant's waitingListIds array in Firestore.
     * Uses Firestore's ArrayUnion operation to safely add without duplicates.
     *
     * @param deviceId        the device ID of the entrant
     * @param eventId         the ID of the event to add to the waiting list
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void addToWaitingList(String deviceId, String eventId,
                                 OnSuccessListener<Void> successListener,
                                 OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(deviceId)
                .update("waitingListIds", FieldValue.arrayUnion(eventId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Removes an event ID from the entrant's waitingListIds array in Firestore.
     * Uses Firestore's ArrayRemove operation to safely remove the entry.
     *
     * @param deviceId        the device ID of the entrant
     * @param eventId         the ID of the event to remove from the waiting list
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void removeFromWaitingList(String deviceId, String eventId,
                                      OnSuccessListener<Void> successListener,
                                      OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(deviceId)
                .update("waitingListIds", FieldValue.arrayRemove(eventId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Notification Preference Operations
    // -------------------------------------------------------------------------

    /**
     * Updates the entrant's notification preference in Firestore.
     *
     * @param deviceId        the device ID of the entrant
     * @param enabled         true to enable notifications, false to opt out
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void updateNotificationPreference(String deviceId, boolean enabled,
                                             OnSuccessListener<Void> successListener,
                                             OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(deviceId)
                .update("notificationsEnabled", enabled)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Registration History Operations
    // -------------------------------------------------------------------------

    /**
     * Adds a registration history record to the entrant's document in Firestore.
     * Uses Firestore's ArrayUnion operation to append the record to the
     * registrationHistory array.
     *
     * @param deviceId        the device ID of the entrant
     * @param record          the RegistrationRecord to add to the history
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void addRegistrationRecord(String deviceId,
                                      Entrant.RegistrationRecord record,
                                      OnSuccessListener<Void> successListener,
                                      OnFailureListener failureListener) {
        Map<String, Object> recordData = new HashMap<>();
        recordData.put("eventId", record.getEventId());
        recordData.put("outcome", record.getOutcome().name());

        db.collection(COLLECTION)
                .document(deviceId)
                .update("registrationHistory", FieldValue.arrayUnion(recordData))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }
}