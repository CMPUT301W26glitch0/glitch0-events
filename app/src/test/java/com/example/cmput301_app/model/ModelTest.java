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
        java.util.List<String> candidates = new java.util.ArrayList<>();
        candidates.add("entrant_B");
        candidates.add("entrant_C");
        String chosen = pool.drawReplacement(candidates);
        assertNotNull("drawReplacement should pick someone from a non-empty list", chosen);
        assertTrue("Chosen entrant should be added to selectedEntrantIds", pool.isSelected(chosen));

        LotteryPool emptyPool = new LotteryPool(1);
        String result = emptyPool.drawReplacement(new java.util.ArrayList<>());
        assertNull("drawReplacement should return null when waiting list is empty", result);
    }

    // ─── Filter Helper Tests (US 01.01.04) ─────────────────────────────

    @Test
    public void testEventGetDayOfWeek() {
        Event event = new Event();
        assertEquals("Day should be -1 when date is null", -1, event.getDayOfWeek());

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(2026, java.util.Calendar.MARCH, 16, 10, 0, 0);
        event.setDate(new Timestamp(cal.getTime()));
        assertEquals("March 16 2026 should be Monday",
                java.util.Calendar.MONDAY, event.getDayOfWeek());
    }

    @Test
    public void testEventGetHourOfDay() {
        Event event = new Event();
        assertEquals("Hour should be -1 when date is null", -1, event.getHourOfDay());

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(2026, java.util.Calendar.MARCH, 16, 14, 30, 0);
        event.setDate(new Timestamp(cal.getTime()));
        assertEquals("Hour should be 14 for 2:30pm", 14, event.getHourOfDay());
    }

    @Test
    public void testEventHasWaitlistSpace() {
        Event event = new Event();
        assertTrue("Unlimited waitlist should have space", event.hasWaitlistSpace());

        event.setWaitingListLimit(5);
        event.setWaitingListCount(3);
        assertTrue("Waitlist with 3/5 should have space", event.hasWaitlistSpace());

        event.setWaitingListCount(5);
        assertFalse("Waitlist with 5/5 should be full", event.hasWaitlistSpace());
    }

    @Test
    public void testEventIsFull() {
        Event event = new Event();
        assertFalse("Event with 0 capacity should not be full", event.isFull());

        event.setCapacity(3);
        java.util.List<String> confirmed = new java.util.ArrayList<>();
        confirmed.add("user1");
        confirmed.add("user2");
        event.setConfirmedAttendeesIds(confirmed);
        assertFalse("Event with 2/3 confirmed should not be full", event.isFull());

        confirmed.add("user3");
        event.setConfirmedAttendeesIds(confirmed);
        assertTrue("Event with 3/3 confirmed should be full", event.isFull());
    }

    // ─── Search Helper Tests (US: Entrant Search by Keyword) ────────────

    @Test
    public void testMatchesKeywordByName() {
        Event event = new Event();
        event.setName("Senior Yoga - Session A");
        assertTrue("Should match keyword in name", event.matchesKeyword("yoga"));
        assertTrue("Should match case-insensitively", event.matchesKeyword("YOGA"));
    }

    @Test
    public void testMatchesKeywordByDescription() {
        Event event = new Event();
        event.setDescription("A gentle flow yoga class suitable for all levels");
        assertTrue("Should match keyword in description", event.matchesKeyword("gentle"));
    }

    @Test
    public void testMatchesKeywordByCategory() {
        Event event = new Event();
        event.setCategory("Fitness");
        assertTrue("Should match keyword in category", event.matchesKeyword("fitness"));
    }

    @Test
    public void testMatchesKeywordByLocation() {
        Event event = new Event();
        event.setLocation("Edmonton Community Centre");
        assertTrue("Should match keyword in location", event.matchesKeyword("edmonton"));
    }

    @Test
    public void testMatchesKeywordNoMatch() {
        Event event = new Event();
        event.setName("Morning Run");
        event.setDescription("A brisk morning jog");
        event.setCategory("Sports");
        assertFalse("Should not match unrelated keyword", event.matchesKeyword("swimming"));
    }

    @Test
    public void testMatchesKeywordNullFields() {
        Event event = new Event();
        // All fields are null by default
        assertFalse("Should return false when all fields are null", event.matchesKeyword("test"));
    }

    @Test
    public void testMatchesKeywordEmptyString() {
        Event event = new Event();
        event.setName("Cycling");
        assertTrue("Empty keyword should match everything", event.matchesKeyword(""));
        assertTrue("Null keyword should match everything", event.matchesKeyword(null));
    }
}

