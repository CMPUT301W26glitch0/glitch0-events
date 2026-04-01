/*
 * Purpose: Database helper class for Admin-specific queries and operations.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.database;

import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.model.Notification;
import com.example.cmput301_app.model.Profile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles all Firestore database operations for Admin actions.
 *
 * This class is the sole point of contact between the application and
 * Firestore for admin-related actions. It follows the repository pattern
 * and acts as a coordinator, delegating to other DB classes such as
 * EventDB, EntrantDB, OrganizerDB, PosterDB, and NotificationDB to carry
 * out admin actions that span multiple collections.
 *
 * Unlike other DB classes, AdminDB does not manage a dedicated Firestore
 * collection. Admin accounts are created directly in Firebase Authentication
 * rather than through the app's registration flow.
 *
 * Outstanding issues:
 * - removeEvent(), removeProfile(), removeOrganizer(), removeImage(), and
 *   getNotificationLog() are not yet implemented as they are not required
 *   for the halfway checkpoint. These methods require coordination across
 *   multiple DB classes and should be implemented in a future sprint.
 */
public class AdminDB {

    /** The Firestore collection name for all user documents */
    private static final String USERS_COLLECTION = "users";

    /** The Firestore collection name for all event documents */
    private static final String EVENTS_COLLECTION = "events";

    /** The Firestore instance used for all database operations */
    private FirebaseFirestore db;

    /** NotificationDB used for creating and deleting notifications */
    private NotificationDB notificationDB;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an AdminDB and initializes the Firestore instance.
     */
    public AdminDB() {
        this.db = FirebaseFirestore.getInstance();
        this.notificationDB = new NotificationDB();
    }

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new admin document in the "users" collection.
     * Uses the Firebase Auth UID as the document ID so that checkUserAndNavigate()
     * in MainActivity can look it up and route to AdminDashboardActivity.
     *
     * @param uid             the Firebase Auth UID for the new admin account
     * @param name            the admin's display name
     * @param email           the admin's email address
     * @param successListener called when the write completes successfully
     * @param failureListener called if the write fails
     */
    public void createAdmin(String uid, String name, String email,
                            OnSuccessListener<Void> successListener,
                            OnFailureListener failureListener) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("role", "admin");
        data.put("name", name);
        data.put("email", email);
        data.put("adminId", uid);

        db.collection(USERS_COLLECTION)
                .document(uid)
                .set(data)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Browse Operations
    // -------------------------------------------------------------------------

    /**
     * Fetches all event documents from Firestore and returns them as a list
     * of Event objects via the success listener.
     * Used to populate the admin event browsing screen (US 03.04.01).
     *
     * @param successListener called with the list of Event objects
     * @param failureListener called if the operation fails
     */
    public void getAllEvents(OnSuccessListener<List<Event>> successListener,
                             OnFailureListener failureListener) {
        db.collection(EVENTS_COLLECTION)
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
     * Fetches all user profile documents from Firestore and returns them as a
     * list of Profile objects via the success listener.
     * Used to populate the admin profile browsing screen (US 03.05.01).
     *
     * @param successListener called with the list of Profile objects
     * @param failureListener called if the operation fails
     */
    public void getAllProfiles(OnSuccessListener<List<Profile>> successListener,
                               OnFailureListener failureListener) {
        db.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Profile> profiles = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Profile profile = document.toObject(Profile.class);
                        profiles.add(profile);
                    }
                    successListener.onSuccess(profiles);
                })
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Remove Operations
    // -------------------------------------------------------------------------

    /**
     * Removes an event and all associated data:
     * 1. Sends a cancellation notification to all entrants (waiting list + confirmed attendees).
     * 2. Deletes all notification documents associated with this event.
     * 3. Deletes the poster image from Firebase Storage (if a posterUrl is set).
     * 4. Deletes the event document from Firestore.
     *
     * @param eventId         the ID of the event to remove
     * @param successListener called when all operations complete successfully
     * @param failureListener called if any operation in the chain fails
     */
    public void removeEvent(String eventId,
                            OnSuccessListener<Void> successListener,
                            OnFailureListener failureListener) {
        // Step 1: fetch the event so we have entrant lists and posterUrl
        db.collection(EVENTS_COLLECTION).document(eventId).get()
                .addOnFailureListener(failureListener)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // Already gone — treat as success
                        successListener.onSuccess(null);
                        return;
                    }
                    Event event = doc.toObject(Event.class);
                    if (event != null) {
                        event.setEventId(doc.getId());
                    }

                    // Collect all entrant IDs who should be notified
                    Set<String> entrantIds = new HashSet<>();
                    if (event != null && event.getWaitingListIds() != null) {
                        entrantIds.addAll(event.getWaitingListIds());
                    }
                    if (event != null && event.getConfirmedAttendeesIds() != null) {
                        entrantIds.addAll(event.getConfirmedAttendeesIds());
                    }

                    String posterUrl = (event != null) ? event.getPosterUrl() : null;
                    String eventName = (event != null && event.getName() != null)
                            ? event.getName() : "an event";

                    // Step 2: send cancellation notification (fire-and-forget)
                    if (!entrantIds.isEmpty()) {
                        Notification cancellation = new Notification(
                                null, eventId, "admin",
                                "The event \"" + eventName + "\" has been cancelled by an administrator.",
                                Notification.NotificationType.INVITATION_CANCELLED,
                                Timestamp.now());
                        cancellation.setRecipientIds(new ArrayList<>(entrantIds));
                        notificationDB.createNotification(cancellation, n -> {}, e -> {});
                    }

                    // Step 3: delete all notifications for this event (fire-and-forget)
                    notificationDB.deleteNotificationsByEvent(eventId, v -> {}, e -> {});

                    // Step 4: delete poster from Firebase Storage if present
                    if (posterUrl != null && !posterUrl.isEmpty()) {
                        try {
                            StorageReference posterRef = FirebaseStorage.getInstance()
                                    .getReferenceFromUrl(posterUrl);
                            posterRef.delete().addOnCompleteListener(task -> {
                                // Proceed regardless of storage deletion result
                                deleteEventDocument(eventId, successListener, failureListener);
                            });
                        } catch (IllegalArgumentException e) {
                            // URL not a valid Firebase Storage URL — skip storage deletion
                            deleteEventDocument(eventId, successListener, failureListener);
                        }
                    } else {
                        deleteEventDocument(eventId, successListener, failureListener);
                    }
                });
    }

    /** Deletes the event Firestore document as the final step of removeEvent(). */
    private void deleteEventDocument(String eventId,
                                     OnSuccessListener<Void> successListener,
                                     OnFailureListener failureListener) {
        db.collection(EVENTS_COLLECTION).document(eventId)
                .delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Removes a user profile and all associated waiting list records:
     * 1. Fetches the user document to read their waitingListIds and profileImageUrl.
     * 2. For each event the user is waiting on, removes their deviceId from
     *    that event's waitingListIds array and decrements waitingListCount.
     * 3. Deletes the profile image from Firebase Storage (if a Storage URL is set).
     * 4. Deletes the user document from Firestore.
     *
     * Note: active devices will be signed out on their next app resume because
     * DashboardActivity and OrganizerDashboardActivity check whether the user's
     * Firestore document still exists and call FirebaseAuth.signOut() if not.
     *
     * @param deviceId        the UID / document ID of the profile to remove
     * @param successListener called when all operations complete successfully
     * @param failureListener called if any operation in the chain fails
     */
    public void removeProfile(String deviceId,
                              OnSuccessListener<Void> successListener,
                              OnFailureListener failureListener) {
        db.collection(USERS_COLLECTION).document(deviceId).get()
                .addOnFailureListener(failureListener)
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        successListener.onSuccess(null);
                        return;
                    }

                    // Collect waiting list event IDs to clean up
                    @SuppressWarnings("unchecked")
                    List<String> waitingListIds = (List<String>) doc.get("waitingListIds");
                    String profileImageUrl = doc.getString("profileImageUrl");

                    // Step 1: remove user from each event's waitingListIds (fire-and-forget)
                    if (waitingListIds != null && !waitingListIds.isEmpty()) {
                        EventDB eventDB = new EventDB();
                        for (String eventId : waitingListIds) {
                            eventDB.removeFromWaitingList(eventId, deviceId, v -> {}, e -> {});
                        }
                    }

                    // Step 2: delete profile image from Firebase Storage if present
                    if (profileImageUrl != null && profileImageUrl.startsWith("https://")) {
                        try {
                            StorageReference ref = FirebaseStorage.getInstance()
                                    .getReferenceFromUrl(profileImageUrl);
                            ref.delete().addOnCompleteListener(task ->
                                    deleteUserDocument(deviceId, successListener, failureListener));
                        } catch (IllegalArgumentException e) {
                            deleteUserDocument(deviceId, successListener, failureListener);
                        }
                    } else {
                        deleteUserDocument(deviceId, successListener, failureListener);
                    }
                });
    }

    /** Deletes the user Firestore document as the final step of removeProfile(). */
    private void deleteUserDocument(String deviceId,
                                    OnSuccessListener<Void> successListener,
                                    OnFailureListener failureListener) {
        db.collection(USERS_COLLECTION).document(deviceId)
                .delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * TODO: Not required for the halfway checkpoint.
     * Removes an organizer who violates app policy along with all their
     * associated events, lottery pools, posters, and notifications.
     * Coordinates across OrganizerDB, EventDB, LotteryDB, PosterDB,
     * and NotificationDB.
     * To be implemented in a future sprint.
     *
     * @param deviceId        the device ID of the organizer to remove
     * @param successListener called when all operations complete successfully
     * @param failureListener called if any operation in the chain fails
     */
    public void removeOrganizer(String deviceId,
                                OnSuccessListener<Void> successListener,
                                OnFailureListener failureListener) {
        // Not yet implemented
    }

    /**
     * TODO: Not required for the halfway checkpoint.
     * Removes an inappropriate poster image by delegating to
     * PosterDB.deletePoster(), which handles Firebase Storage deletion,
     * Firestore document removal, and Event posterUrl sync.
     * To be implemented in a future sprint.
     *
     * @param posterId        the ID of the poster to remove
     * @param eventId         the ID of the event this poster belongs to
     * @param successListener called when all operations complete successfully
     * @param failureListener called if any operation in the chain fails
     */
    public void removeImage(String posterId, String eventId,
                            OnSuccessListener<Void> successListener,
                            OnFailureListener failureListener) {
        // Not yet implemented
    }

    /**
     * TODO: Not required for the halfway checkpoint.
     * Fetches the full notification log by delegating to
     * NotificationDB.getAllNotifications().
     * Used for the admin notification log screen (US 03.08.01).
     * To be implemented in a future sprint.
     *
     * @param successListener called with the list of Notification objects
     * @param failureListener called if the operation fails
     */
    public void getNotificationLog(OnSuccessListener<List<Notification>> successListener,
                                   OnFailureListener failureListener) {
        // Not yet implemented
    }
}
