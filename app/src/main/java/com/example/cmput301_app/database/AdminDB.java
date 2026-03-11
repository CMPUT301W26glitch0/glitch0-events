package com.example.cmput301_app.database;

import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.model.Notification;
import com.example.cmput301_app.model.Profile;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs an AdminDB and initializes the Firestore instance.
     */
    public AdminDB() {
        this.db = FirebaseFirestore.getInstance();
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
     * TODO: Not required for the halfway checkpoint.
     * Removes an event and all associated data including lottery pool, poster,
     * and notifications. Coordinates across EventDB, LotteryDB, PosterDB,
     * and NotificationDB to clean up all related documents.
     * To be implemented in a future sprint.
     *
     * @param eventId         the ID of the event to remove
     * @param successListener called when all operations complete successfully
     * @param failureListener called if any operation in the chain fails
     */
    public void removeEvent(String eventId,
                            OnSuccessListener<Void> successListener,
                            OnFailureListener failureListener) {
        // Not yet implemented
    }

    /**
     * TODO: Not required for the halfway checkpoint.
     * Removes a user profile from Firestore. Determines whether the profile
     * belongs to an entrant or organizer and delegates to the appropriate
     * DB class. If the profile belongs to an organizer, all associated events
     * are also removed.
     * To be implemented in a future sprint.
     *
     * @param deviceId        the device ID of the profile to remove
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void removeProfile(String deviceId,
                              OnSuccessListener<Void> successListener,
                              OnFailureListener failureListener) {
        // Not yet implemented
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
