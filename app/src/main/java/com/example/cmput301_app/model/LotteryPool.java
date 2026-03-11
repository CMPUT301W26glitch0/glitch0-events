package com.example.cmput301_app.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the lottery pool for an event in the application.
 *
 * A LotteryPool manages the selection process for an event by randomly
 * drawing entrants from the event's waiting list. It tracks which entrants
 * have been selected, declined, or cancelled, and supports replacement
 * draws when a previously selected entrant declines or is cancelled.
 *
 * This class handles only the local state of the lottery. All persistence
 * of selection results to Firebase is handled by LotteryDB.
 *
 * Outstanding issues:
 * - Draw logic assumes the waiting list passed in has already excluded
 *   previously selected, declined, and cancelled entrants. Filtering
 *   responsibility currently falls on the caller.
 */
public class LotteryPool {

    /**
     * List of device IDs of entrants who have been selected by the lottery draw.
     */
    private List<String> selectedEntrantIds;

    /**
     * List of device IDs of entrants who declined their lottery invitation.
     */
    private List<String> declinedEntrantIds;

    /**
     * List of device IDs of entrants who were cancelled by the organizer.
     */
    private List<String> cancelledEntrantIds;

    /**
     * The number of entrants the organizer wants to sample from the waiting list.
     */
    private int drawSize;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Default no-argument constructor required for Firebase deserialization.
     */
    public LotteryPool() {
        this.selectedEntrantIds = new ArrayList<>();
        this.declinedEntrantIds = new ArrayList<>();
        this.cancelledEntrantIds = new ArrayList<>();
        this.drawSize = 0;
    }

    /**
     * Constructs a LotteryPool with a specified draw size.
     *
     * @param drawSize the number of entrants to sample from the waiting list
     */
    public LotteryPool(int drawSize) {
        this.selectedEntrantIds = new ArrayList<>();
        this.declinedEntrantIds = new ArrayList<>();
        this.cancelledEntrantIds = new ArrayList<>();
        this.drawSize = drawSize;
    }

    // -------------------------------------------------------------------------
    // Draw Methods
    // -------------------------------------------------------------------------

    /**
     * Randomly selects up to drawSize entrants from the provided waiting list.
     * If the waiting list has fewer entrants than drawSize, all entrants are selected.
     * Selected entrants are added to the selectedEntrantIds list.
     *
     * @param waitingListIds the list of device IDs currently on the waiting list
     * @return a list of device IDs that were selected in this draw
     */
    public List<String> drawEntrants(List<String> waitingListIds) {
        List<String> pool = new ArrayList<>(waitingListIds);
        Collections.shuffle(pool);

        int count = Math.min(drawSize, pool.size());
        List<String> drawn = pool.subList(0, count);

        selectedEntrantIds.addAll(drawn);
        return new ArrayList<>(drawn);
    }

    /**
     * Randomly selects one replacement entrant from the provided waiting list.
     * The selected entrant is added to the selectedEntrantIds list.
     * Returns null if the waiting list is empty.
     *
     * @param waitingListIds the list of device IDs remaining on the waiting list
     * @return the device ID of the replacement entrant, or null if none are available
     */
    public String drawReplacement(List<String> waitingListIds) {
        if (waitingListIds.isEmpty()) {
            return null;
        }

        List<String> pool = new ArrayList<>(waitingListIds);
        Collections.shuffle(pool);

        String replacement = pool.get(0);
        selectedEntrantIds.add(replacement);
        return replacement;
    }

    // -------------------------------------------------------------------------
    // Status Methods
    // -------------------------------------------------------------------------

    /**
     * Marks an entrant as selected by adding them to the selected list.
     * Does nothing if the entrant is already selected.
     *
     * @param deviceId the device ID of the entrant to select
     */
    public void selectEntrant(String deviceId) {
        if (!selectedEntrantIds.contains(deviceId)) {
            selectedEntrantIds.add(deviceId);
        }
    }

    /**
     * Moves an entrant from the selected list to the declined list.
     * Does nothing if the entrant is not in the selected list.
     *
     * @param deviceId the device ID of the entrant who declined
     */
    public void declineEntrant(String deviceId) {
        if (selectedEntrantIds.remove(deviceId)) {
            declinedEntrantIds.add(deviceId);
        }
    }

    /**
     * Moves an entrant from the selected list to the cancelled list.
     * Does nothing if the entrant is not in the selected list.
     *
     * @param deviceId the device ID of the entrant to cancel
     */
    public void cancelEntrant(String deviceId) {
        if (selectedEntrantIds.remove(deviceId)) {
            cancelledEntrantIds.add(deviceId);
        }
    }

    /**
     * Returns whether a given entrant is currently in the selected list.
     *
     * @param deviceId the device ID of the entrant to check
     * @return true if the entrant is selected, false otherwise
     */
    public boolean isSelected(String deviceId) {
        return selectedEntrantIds.contains(deviceId);
    }

    /**
     * Returns whether a given entrant has declined their invitation.
     *
     * @param deviceId the device ID of the entrant to check
     * @return true if the entrant has declined, false otherwise
     */
    public boolean hasDeclined(String deviceId) {
        return declinedEntrantIds.contains(deviceId);
    }

    /**
     * Returns whether a given entrant has been cancelled.
     *
     * @param deviceId the device ID of the entrant to check
     * @return true if the entrant has been cancelled, false otherwise
     */
    public boolean isCancelled(String deviceId) {
        return cancelledEntrantIds.contains(deviceId);
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    /**
     * Returns the list of device IDs of entrants who have been selected.
     *
     * @return list of selected entrant device IDs
     */
    public List<String> getSelectedEntrantIds() {
        return selectedEntrantIds;
    }

    /**
     * Sets the list of device IDs of entrants who have been selected.
     *
     * @param selectedEntrantIds the list of selected device IDs to assign
     */
    public void setSelectedEntrantIds(List<String> selectedEntrantIds) {
        this.selectedEntrantIds = selectedEntrantIds;
    }

    /**
     * Returns the list of device IDs of entrants who declined their invitation.
     *
     * @return list of declined entrant device IDs
     */
    public List<String> getDeclinedEntrantIds() {
        return declinedEntrantIds;
    }

    /**
     * Sets the list of device IDs of entrants who declined their invitation.
     *
     * @param declinedEntrantIds the list of declined device IDs to assign
     */
    public void setDeclinedEntrantIds(List<String> declinedEntrantIds) {
        this.declinedEntrantIds = declinedEntrantIds;
    }

    /**
     * Returns the list of device IDs of entrants who were cancelled by the organizer.
     *
     * @return list of cancelled entrant device IDs
     */
    public List<String> getCancelledEntrantIds() {
        return cancelledEntrantIds;
    }

    /**
     * Sets the list of device IDs of entrants who were cancelled by the organizer.
     *
     * @param cancelledEntrantIds the list of cancelled device IDs to assign
     */
    public void setCancelledEntrantIds(List<String> cancelledEntrantIds) {
        this.cancelledEntrantIds = cancelledEntrantIds;
    }

    /**
     * Returns the number of entrants to sample in a lottery draw.
     *
     * @return the draw size
     */
    public int getDrawSize() {
        return drawSize;
    }

    /**
     * Sets the number of entrants to sample in a lottery draw.
     *
     * @param drawSize the draw size to assign
     */
    public void setDrawSize(int drawSize) {
        this.drawSize = drawSize;
    }
}