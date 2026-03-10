package com.example.cmput301_app.database;

import com.example.cmput301_app.model.LotteryPool;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all Firestore database operations for LotteryPool objects.
 *
 * This class is the sole point of contact between the application and
 * Firestore for lottery-related data. It follows the repository pattern,
 * meaning no other class should read or write lottery data to Firestore
 * directly — all operations go through this class.
 *
 * LotteryPool documents are stored in the "lotteryPools" collection using
 * the associated eventId as the document ID. This ensures a direct one-to-one
 * relationship between an event and its lottery pool, and allows direct
 * document lookups without requiring a query.
 *
 * Outstanding issues:
 * - updateDeclinedEntrants() and updateCancelledEntrants() move entrants
 *   between arrays. These operations are not atomic by default. A Firestore
 *   transaction should be considered here to prevent race conditions if
 *   multiple status changes occur simultaneously.
 */
public class LotteryDB {

    /** The Firestore collection name for all lottery pool documents */
    private static final String COLLECTION = "lotteryPools";

    /** The Firestore instance used for all database operations */
    private FirebaseFirestore db;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a LotteryDB and initializes the Firestore instance.
     */
    public LotteryDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    // -------------------------------------------------------------------------
    // CRUD Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new lottery pool document in the "lotteryPools" collection.
     * Uses the eventId as the document ID to maintain a direct one-to-one
     * relationship between an event and its lottery pool.
     *
     * @param eventId         the ID of the event this lottery pool belongs to
     * @param lotteryPool     the LotteryPool object to store in Firestore
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void createLotteryPool(String eventId, LotteryPool lotteryPool,
                                  OnSuccessListener<Void> successListener,
                                  OnFailureListener failureListener) {
        Map<String, Object> data = new HashMap<>();
        data.put("drawSize", lotteryPool.getDrawSize());
        data.put("selectedEntrantIds", lotteryPool.getSelectedEntrantIds());
        data.put("declinedEntrantIds", lotteryPool.getDeclinedEntrantIds());
        data.put("cancelledEntrantIds", lotteryPool.getCancelledEntrantIds());

        db.collection(COLLECTION)
                .document(eventId)
                .set(data)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Fetches a lottery pool document from Firestore by event ID and returns
     * it as a LotteryPool object via the success listener.
     *
     * @param eventId         the ID of the event whose lottery pool to fetch
     * @param successListener called with the LotteryPool object if found, or null if not found
     * @param failureListener called if the operation fails
     */
    public void getLotteryPool(String eventId,
                               OnSuccessListener<LotteryPool> successListener,
                               OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        LotteryPool lotteryPool = documentSnapshot.toObject(LotteryPool.class);
                        successListener.onSuccess(lotteryPool);
                    } else {
                        successListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(failureListener);
    }

    /**
     * Deletes a lottery pool document from Firestore by event ID.
     * Called by AdminDB when an associated event is removed.
     *
     * @param eventId         the ID of the event whose lottery pool to delete
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void deleteLotteryPool(String eventId,
                                  OnSuccessListener<Void> successListener,
                                  OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .delete()
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Draw Operations
    // -------------------------------------------------------------------------

    /**
     * Updates the selectedEntrantIds array in Firestore after a lottery draw.
     * Replaces the entire array with the newly drawn list of device IDs.
     *
     * @param eventId         the ID of the event whose lottery pool to update
     * @param selectedIds     the list of device IDs selected in the draw
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void updateSelectedEntrants(String eventId, List<String> selectedIds,
                                       OnSuccessListener<Void> successListener,
                                       OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("selectedEntrantIds", selectedIds)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Updates the draw size for the lottery pool in Firestore.
     * Called when the organizer sets or changes the number of entrants to sample.
     *
     * @param eventId         the ID of the event whose lottery pool to update
     * @param drawSize        the number of entrants to sample in the draw
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void updateDrawSize(String eventId, int drawSize,
                               OnSuccessListener<Void> successListener,
                               OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("drawSize", drawSize)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Adds a replacement entrant's device ID to the selectedEntrantIds array
     * in Firestore after a re-draw. Uses Firestore's ArrayUnion operation
     * to safely add without duplicates.
     *
     * @param eventId         the ID of the event whose lottery pool to update
     * @param deviceId        the device ID of the replacement entrant
     * @param successListener called when the operation completes successfully
     * @param failureListener called if the operation fails
     */
    public void addReplacementEntrant(String eventId, String deviceId,
                                      OnSuccessListener<Void> successListener,
                                      OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("selectedEntrantIds", FieldValue.arrayUnion(deviceId))
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // -------------------------------------------------------------------------
    // Status Update Operations
    // -------------------------------------------------------------------------

    /**
     * Moves an entrant from the selectedEntrantIds array to the declinedEntrantIds
     * array in Firestore when they decline their lottery invitation.
     *
     * Note: these two array operations are performed sequentially and are not
     * atomic. A Firestore transaction should be considered to prevent race
     * conditions if simultaneous status changes are possible.
     *
     * @param eventId         the ID of the event whose lottery pool to update
     * @param deviceId        the device ID of the entrant who declined
     * @param successListener called when both operations complete successfully
     * @param failureListener called if either operation fails
     */
    public void updateDeclinedEntrants(String eventId, String deviceId,
                                       OnSuccessListener<Void> successListener,
                                       OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("selectedEntrantIds", FieldValue.arrayRemove(deviceId))
                .addOnSuccessListener(aVoid ->
                        db.collection(COLLECTION)
                                .document(eventId)
                                .update("declinedEntrantIds", FieldValue.arrayUnion(deviceId))
                                .addOnSuccessListener(successListener)
                                .addOnFailureListener(failureListener))
                .addOnFailureListener(failureListener);
    }

    /**
     * Moves an entrant from the selectedEntrantIds array to the cancelledEntrantIds
     * array in Firestore when the organizer cancels their invitation.
     *
     * Note: these two array operations are performed sequentially and are not
     * atomic. A Firestore transaction should be considered to prevent race
     * conditions if simultaneous status changes are possible.
     *
     * @param eventId         the ID of the event whose lottery pool to update
     * @param deviceId        the device ID of the entrant to cancel
     * @param successListener called when both operations complete successfully
     * @param failureListener called if either operation fails
     */
    public void updateCancelledEntrants(String eventId, String deviceId,
                                        OnSuccessListener<Void> successListener,
                                        OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .update("selectedEntrantIds", FieldValue.arrayRemove(deviceId))
                .addOnSuccessListener(aVoid ->
                        db.collection(COLLECTION)
                                .document(eventId)
                                .update("cancelledEntrantIds", FieldValue.arrayUnion(deviceId))
                                .addOnSuccessListener(successListener)
                                .addOnFailureListener(failureListener))
                .addOnFailureListener(failureListener);
    }
}