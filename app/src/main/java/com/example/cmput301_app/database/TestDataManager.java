package com.example.cmput301_app.database;

import android.util.Log;
import com.example.cmput301_app.model.Entrant;
import com.example.cmput301_app.model.Event;
import com.example.cmput301_app.model.Organizer;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility class to manage realistic test data in Firestore.
 */
public class TestDataManager {
    private static final String TAG = "TestDataManager";
    private final FirebaseFirestore db;

    public TestDataManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<Void> addTestData() {
        Log.d(TAG, "Starting to add test data...");
        List<Task<?>> tasks = new ArrayList<>();

        // 1. Create Organizer
        Organizer organizer = new Organizer("test_organizer_id", "John Organizer", "john@example.com", "780-123-4567");
        tasks.add(db.collection("users").document(organizer.getDeviceId()).set(organizer)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Added Organizer")));

        // 2. Create Entrants
        String[] names = {"Alice Smith", "Bob Jones", "Charlie Brown", "Diana Prince", "Ethan Hunt"};
        for (int i = 0; i < names.length; i++) {
            String deviceId = "test_entrant_id_" + i;
            Entrant entrant = new Entrant(deviceId, names[i], names[i].toLowerCase().replace(" ", ".") + "@example.com", "555-010" + i);
            tasks.add(db.collection("users").document(entrant.getDeviceId()).set(entrant)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Added Entrant: " + deviceId)));
        }

        // 3. Create Events
        Event event1 = new Event();
        event1.setName("Community Swimming Lessons");
        event1.setDescription("Fun swimming lessons for all ages.");
        event1.setLocation("North Aquatic Center");
        event1.setOrganizerId("test_organizer_id");
        event1.setCapacity(20);
        event1.setPrice(15.0);
        event1.setDate(new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 7)));
        event1.setRegistrationOpen(new Timestamp(new Date()));
        event1.setRegistrationClose(new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 3)));
        tasks.add(db.collection("events").document("test_event_id_1").set(event1)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Added Event 1")));

        Event event2 = new Event();
        event2.setName("Charity Gala 2024");
        event2.setDescription("Annual fundraising event for local shelters.");
        event2.setLocation("Grand Ballroom");
        event2.setOrganizerId("test_organizer_id");
        event2.setCapacity(100);
        event2.setPrice(50.0);
        event2.setDate(new Timestamp(new Date(System.currentTimeMillis() + 86400000L * 14)));
        tasks.add(db.collection("events").document("test_event_id_2").set(event2)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Added Event 2")));

        return Tasks.whenAll(tasks).addOnCompleteListener(task -> {
            if (task.isSuccessful()) Log.d(TAG, "Successfully added all test data.");
            else Log.e(TAG, "Failed to add test data", task.getException());
        });
    }

    public Task<Void> deleteAllTestData() {
        Log.d(TAG, "Starting to delete test data...");
        WriteBatch batch = db.batch();

        batch.delete(db.collection("users").document("test_organizer_id"));
        for (int i = 0; i < 5; i++) {
            batch.delete(db.collection("users").document("test_entrant_id_" + i));
        }
        batch.delete(db.collection("events").document("test_event_id_1"));
        batch.delete(db.collection("events").document("test_event_id_2"));

        return batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) Log.d(TAG, "Successfully deleted all test data.");
            else Log.e(TAG, "Failed to delete test data", task.getException());
        });
    }
}
