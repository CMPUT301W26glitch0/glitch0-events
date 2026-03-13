package com.example.cmput301_app.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for the application's data models.
 */
public class ModelTest {

    @Test
    public void testEventRegistrationOpen() {
        Event event = new Event();
        
        // Past registration window
        event.setRegistrationOpen(new Timestamp(new Date(System.currentTimeMillis() - 2000000)));
        event.setRegistrationClose(new Timestamp(new Date(System.currentTimeMillis() - 1000000)));
        assertFalse("Registration should be closed if window is in the past", event.checkIsRegistrationOpen());

        // Current registration window
        event.setRegistrationOpen(new Timestamp(new Date(System.currentTimeMillis() - 1000000)));
        event.setRegistrationClose(new Timestamp(new Date(System.currentTimeMillis() + 1000000)));
        assertTrue("Registration should be open if window is current", event.checkIsRegistrationOpen());
    }

    @Test
    public void testEntrantWaitingList() {
        Entrant entrant = new Entrant("dev123", "Test User", "test@example.com", null);
        String eventId = "event456";

        assertFalse(entrant.isOnWaitingList(eventId));
        
        entrant.joinWaitingList(eventId);
        assertTrue(entrant.isOnWaitingList(eventId));
        
        entrant.leaveWaitingList(eventId);
        assertFalse(entrant.isOnWaitingList(eventId));
    }

    @Test
    public void testEntrantInvitation() {
        Entrant entrant = new Entrant("dev123", "Test User", "test@example.com", null);
        String eventId = "event456";

        entrant.joinWaitingList(eventId);
        entrant.acceptInvitation(eventId);

        assertFalse("Should be removed from waiting list after accepting", entrant.isOnWaitingList(eventId));
        assertEquals(1, entrant.getRegistrationHistory().size());
        assertEquals(Entrant.RegistrationRecord.Outcome.ACCEPTED, entrant.getRegistrationHistory().get(0).getOutcome());
    }

    @Test
    public void testLotteryPoolDeclineEntrant() {
        LotteryPool pool = new LotteryPool(2);

        // Pre-populate a selected entrant
        pool.selectEntrant("entrant_A");
        assertTrue("Entrant A should be in selected list", pool.isSelected("entrant_A"));

        // Decline the entrant
        pool.declineEntrant("entrant_A");

        assertFalse("Entrant A should no longer be in selected list after declining",
                pool.isSelected("entrant_A"));
        assertTrue("Entrant A should appear in declined list after declining",
                pool.hasDeclined("entrant_A"));
    }

    @Test
    public void testLotteryPoolDrawReplacement() {
        LotteryPool pool = new LotteryPool(1);

        // Non-empty waiting list should yield a winner
        java.util.List<String> candidates = new java.util.ArrayList<>();
        candidates.add("entrant_B");
        candidates.add("entrant_C");

        String chosen = pool.drawReplacement(candidates);
        assertNotNull("drawReplacement should pick someone from a non-empty list", chosen);
        assertTrue("Chosen entrant should be added to selectedEntrantIds",
                pool.isSelected(chosen));

        // Empty waiting list should yield null
        LotteryPool emptyPool = new LotteryPool(1);
        String result = emptyPool.drawReplacement(new java.util.ArrayList<>());
        assertNull("drawReplacement should return null when waiting list is empty", result);
    }
}
