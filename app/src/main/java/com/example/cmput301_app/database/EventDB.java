package com.example.cmput301_app.database;

import android.util.Log;
import com.example.cmput301_app.model.Event;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventDB {
    private static final String COLLECTION = "events";
    private FirebaseFirestore db;

    public EventDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void getAllEvents(OnSuccessListener<List<Event>> successListener,
                             OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = document.toObject(Event.class);
                            if (event != null) {
                                event.setEventId(document.getId());
                                events.add(event);
                            }
                        } catch (Exception e) {
                            Log.e("EventDB", "Error parsing event: " + document.getId(), e);
                            // Skip broken event records instead of crashing the whole app
                        }
                    }
                    successListener.onSuccess(events);
                })
                .addOnFailureListener(failureListener);
    }

    public void getEvent(String eventId,
                         OnSuccessListener<Event> successListener,
                         OnFailureListener failureListener) {
        db.collection(COLLECTION)
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        try {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                event.setEventId(doc.getId());
                                successListener.onSuccess(event);
                            }
                        } catch (Exception e) {
                            failureListener.onFailure(e);
                        }
                    } else {
                        successListener.onSuccess(null);
                    }
                })
                .addOnFailureListener(failureListener);
    }
}