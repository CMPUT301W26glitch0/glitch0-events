/*
 * Purpose: Database helper class for Admin-specific queries and operations.
 * Design Pattern: Standard Android structure
 * Outstanding Issues: None
 */
package com.example.cmput301_app.database;

import com.example.cmput301_app.admin.AdminCommentEntry;
import com.example.cmput301_app.model.Comment;
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
        data.put("createdAt", Timestamp.now());

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
                        if (profile.getDeviceId() == null) {
                            profile.setDeviceId(document.getId());
                        }
                        // Populate join date from the stored createdAt field (may be null for legacy accounts)
                        profile.setJoinDate(document.getTimestamp("createdAt"));
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
     * Removes an organizer and all data they own:
     * 1. Fetches every event where organizerId == deviceId.
     * 2. For each event, runs the full removeEvent() cascade:
     *    - Sends a cancellation notification to every entrant on that event.
     *    - Deletes all notifications tied to the event.
     *    - Clears the poster (posterUrl is a base64 field, so no Storage delete needed).
     *    - Deletes the event document.
     * 3. After all events are removed, runs removeProfile() to clean up the
     *    organizer's user document and any waiting-list memberships.
     *
     * All entrants associated with the removed events receive a cancellation
     * notification before their event data is deleted.
     *
     * @param deviceId        the UID / document ID of the organizer to remove
     * @param successListener called when all operations complete successfully
     * @param failureListener called if any operation in the chain fails
     */
    public void removeOrganizer(String deviceId,
                                OnSuccessListener<Void> successListener,
                                OnFailureListener failureListener) {
        // Step 1: fetch all events owned by this organizer
        db.collection(EVENTS_COLLECTION)
                .whereEqualTo("organizerId", deviceId)
                .get()
                .addOnFailureListener(failureListener)
                .addOnSuccessListener(querySnapshot -> {
                    List<String> eventIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        eventIds.add(doc.getId());
                    }

                    if (eventIds.isEmpty()) {
                        // No events — just remove the profile
                        removeProfile(deviceId, successListener, failureListener);
                        return;
                    }

                    // Step 2: cascade-remove each event, then remove the profile
                    final int[] remaining = {eventIds.size()};
                    final boolean[] alreadyFailed = {false};

                    for (String eventId : eventIds) {
                        removeEvent(eventId,
                                aVoid -> {
                                    synchronized (remaining) {
                                        remaining[0]--;
                                        if (remaining[0] == 0 && !alreadyFailed[0]) {
                                            // All events gone — now remove the profile
                                            removeProfile(deviceId, successListener, failureListener);
                                        }
                                    }
                                },
                                e -> {
                                    synchronized (alreadyFailed) {
                                        if (!alreadyFailed[0]) {
                                            alreadyFailed[0] = true;
                                            failureListener.onFailure(e);
                                        }
                                    }
                                });
                    }
                });
    }

    /**
     * Fetches all events that have a non-empty posterUrl and returns them as a list.
     * Used to populate the admin image management screen.
     *
     * @param successListener called with the filtered list of Event objects
     * @param failureListener called if the operation fails
     */
    public void getAllImages(OnSuccessListener<List<Event>> successListener,
                             OnFailureListener failureListener) {
        db.collection(EVENTS_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(document.getId());
                            if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                                events.add(event);
                            }
                        }
                    }
                    successListener.onSuccess(events);
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Removes the poster image from an event by clearing its posterUrl field.
     * The event itself is not deleted — it continues to exist and will show
     * a placeholder image instead.
     *
     * @param eventId         the ID of the event whose poster should be removed
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void removeImage(String eventId,
                            OnSuccessListener<Void> successListener,
                            OnFailureListener failureListener) {
        db.collection(EVENTS_COLLECTION).document(eventId)
                .update("posterUrl", "")
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Comment Operations
    // -------------------------------------------------------------------------

    /**
     * Fetches all events from Firestore and extracts every comment, pairing each
     * one with its source event ID and name. The resulting list is sorted newest-first.
     *
     * @param successListener called with the full list of {@link AdminCommentEntry} objects
     * @param failureListener called if the Firestore query fails
     */
    public void getAllComments(OnSuccessListener<List<AdminCommentEntry>> successListener,
                               OnFailureListener failureListener) {
        db.collection(EVENTS_COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AdminCommentEntry> entries = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        if (event == null) continue;
                        event.setEventId(doc.getId());
                        String eventName = event.getName() != null ? event.getName() : "Unknown Event";
                        List<com.example.cmput301_app.model.Comment> comments = event.getComments();
                        if (comments == null) continue;
                        for (com.example.cmput301_app.model.Comment comment : comments) {
                            entries.add(new AdminCommentEntry(comment, event.getEventId(), eventName));
                        }
                    }
                    // Sort newest-first (nulls last)
                    entries.sort((a, b) -> {
                        com.google.firebase.Timestamp ta = a.getComment().getTimestamp();
                        com.google.firebase.Timestamp tb = b.getComment().getTimestamp();
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });
                    successListener.onSuccess(entries);
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Removes a single comment from the specified event's comments array and
     * writes an audit record to the {@code deleted_comments_log} collection.
     *
     * @param eventId         the ID of the event that contains the comment
     * @param comment         the comment to remove (must match stored fields exactly)
     * @param eventName       human-readable event name, stored in the audit log
     * @param successListener called when the removal completes successfully
     * @param failureListener called if the removal fails
     */
    public void deleteComment(String eventId,
                              Comment comment,
                              String eventName,
                              OnSuccessListener<Void> successListener,
                              OnFailureListener failureListener) {
        EventDB eventDB = new EventDB();
        eventDB.deleteComment(eventId, comment,
                unused -> {
                    // Write audit log entry (fire-and-forget)
                    java.util.Map<String, Object> logEntry = new java.util.HashMap<>();
                    logEntry.put("commentId",          comment.getId());
                    logEntry.put("eventId",            eventId);
                    logEntry.put("eventName",          eventName);
                    logEntry.put("authorName",         comment.getAuthorName());
                    logEntry.put("content",            comment.getContent());
                    logEntry.put("originalTimestamp",  comment.getTimestamp());
                    logEntry.put("organizerComment",   comment.isOrganizerComment());
                    logEntry.put("deletedAt",          Timestamp.now());
                    logEntry.put("deletedBy",          "admin");

                    db.collection("deleted_comments_log")
                            .add(logEntry)
                            .addOnSuccessListener(ref -> successListener.onSuccess(null))
                            .addOnFailureListener(e -> {
                                // Audit log failure is non-fatal — the comment is already removed
                                successListener.onSuccess(null);
                            });
                },
                failureListener);
    }

    /**
     * Fetches all notifications from Firestore and returns them sorted in
     * reverse chronological order (newest first).
     * Used for the admin notification log screen.
     *
     * @param successListener called with the sorted list of Notification objects
     * @param failureListener called if the operation fails
     */
    public void getNotificationLog(OnSuccessListener<List<Notification>> successListener,
                                   OnFailureListener failureListener) {
        notificationDB.getAllNotifications(notifications -> {
            // Sort newest first
            notifications.sort((a, b) -> {
                if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                if (a.getTimestamp() == null) return 1;
                if (b.getTimestamp() == null) return -1;
                return b.getTimestamp().compareTo(a.getTimestamp());
            });
            successListener.onSuccess(notifications);
        }, failureListener);
    }
}
