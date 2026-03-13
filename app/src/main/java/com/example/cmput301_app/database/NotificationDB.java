/*
 * Purpose: Database helper class for pushing and retrieving user notifications.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.database;

import com.example.cmput301_app.model.Notification;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all Firestore database operations for Notification objects.
 *
 * This class is the sole point of contact between the application and
 * Firestore for notification-related data. It follows the repository pattern,
 * meaning no other class should read or write notification data to Firestore
 * directly — all operations go through this class.
 *
 * Notification documents are stored in the "notifications" collection using
 * a Firestore auto-generated document ID as the notification ID.
 *
 * Outstanding issues:
 * - Actual push notification delivery via Firebase Cloud Messaging (FCM)
 *   is not yet implemented. This class handles only the persistence of
 *   notification data to Firestore.
 * - deleteNotification() and getNotificationsByOrganizer() are not yet
 *   implemented as they are not required for the halfway checkpoint.
 */
public class NotificationDB {

    /** The Firestore collection name for all notification documents */
    private static final String COLLECTION = "notifications";

    /** The Firestore instance used for all database operations */
    private FirebaseFirestore db;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a NotificationDB and initializes the Firestore instance.
     */
    public NotificationDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new notification document in the "notifications" collection.
     * Firestore auto-generates the document ID, which is then set back on
     * the Notification object and passed to the success listener.
     *
     * @param notification    the Notification object to store in Firestore
     * @param successListener called with the updated Notification object containing the generated ID
     * @param failureListener called if the operation fails
     */
    public void createNotification(Notification notification,
                                   OnSuccessListener<Notification> successListener,
                                   OnFailureListener failureListener) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", notification.getEventId());
        data.put("organizerId", notification.getOrganizerId());
        data.put("recipientIds", notification.getRecipientIds());
        data.put("message", notification.getMessage());
        data.put("type", notification.getType().name());
        data.put("timestamp", notification.getTimestamp());

        db.collection(COLLECTION)
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    notification.setNotificationId(documentReference.getId());
                    documentReference.update("notificationId", documentReference.getId())
                            .addOnSuccessListener(aVoid -> successListener.onSuccess(notification))
                            .addOnFailureListener(failureListener);
                })
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Read Operations
    // -------------------------------------------------------------------------

    /**
     * Fetches a notification document from Firestore by notification ID and
     * returns it as a Notification object via the success listener.
     *
     * @param notificationId  the ID of the notification to fetch
     * @param successListener called with the Notification object if found, or null if not found
     * @param failureListener called if the operation fails
     */
    public void getNotification(String notificationId,
                                OnSuccessListener<Notification> successListener,
                                OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(notificationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Notification notification = documentSnapshot.toObject(Notification.class);
                        successListener.onSuccess(notification);
                    } else {
                        successListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Fetches all notification documents associated with a specific event and
     * returns them as a list of Notification objects via the success listener.
     * Used to display the notification history for an event.
     *
     * @param eventId         the ID of the event whose notifications to fetch
     * @param successListener called with the list of Notification objects
     * @param failureListener called if the operation fails
     */
    public void getNotificationsByEvent(String eventId,
                                        OnSuccessListener<List<Notification>> successListener,
                                        OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notifications.add(notification);
                    }
                    successListener.onSuccess(notifications);
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Fetches all notification documents from Firestore and returns them as a
     * list of Notification objects via the success listener.
     * Used to populate the admin notification log screen (US 03.08.01).
     *
     * @param successListener called with the list of Notification objects
     * @param failureListener called if the operation fails
     */
    public void getAllNotifications(OnSuccessListener<List<Notification>> successListener,
                                    OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Notification notification = document.toObject(Notification.class);
                        notifications.add(notification);
                    }
                    successListener.onSuccess(notifications);
                })
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Delete Operations
    // -------------------------------------------------------------------------

    /**
     * TODO: Not required for the halfway checkpoint.
     * Deletes a notification document from Firestore by notification ID.
     * To be implemented in a future sprint.
     *
     * @param notificationId  the ID of the notification to delete
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void deleteNotification(String notificationId,
                                   OnSuccessListener<Void> successListener,
                                   OnFailureListener failureListener) {
        // Not yet implemented
    }

    /**
     * TODO: Not required for the halfway checkpoint.
     * Fetches all notification documents sent by a specific organizer and returns
     * them as a list of Notification objects via the success listener.
     * Used for admin notification log filtering (US 03.08.01).
     * To be implemented in a future sprint.
     *
     * @param organizerId     the device ID of the organizer whose notifications to fetch
     * @param successListener called with the list of Notification objects
     * @param failureListener called if the operation fails
     */
    public void getNotificationsByOrganizer(String organizerId,
                                            OnSuccessListener<List<Notification>> successListener,
                                            OnFailureListener failureListener) {
        // Not yet implemented
    }
}
