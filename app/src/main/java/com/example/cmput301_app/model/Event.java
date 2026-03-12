package com.example.cmput301_app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class Event {
    private String eventId;
    private String name;
    private String description;
    private String location;
    private String category;
    private Timestamp date;
    private double price;
    private long capacity;
    private Timestamp registrationOpen;
    private Timestamp registrationClose;
    private String organizerId;
    private String posterUrl;
    private String qrCode;
    private boolean geolocationEnabled;
    private long waitingListLimit;
    private List<String> waitingListIds;
    private long waitingListCount;

    public Event() {
        this.waitingListIds = new ArrayList<>();
        this.waitingListLimit = -1;
        this.waitingListCount = 0;
    }

    // Standard Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public long getCapacity() { return capacity; }
    public void setCapacity(long capacity) { this.capacity = capacity; }
    public Timestamp getRegistrationOpen() { return registrationOpen; }
    public void setRegistrationOpen(Timestamp registrationOpen) { this.registrationOpen = registrationOpen; }
    public Timestamp getRegistrationClose() { return registrationClose; }
    public void setRegistrationClose(Timestamp registrationClose) { this.registrationClose = registrationClose; }
    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public boolean isGeolocationEnabled() { return geolocationEnabled; }
    public void setGeolocationEnabled(boolean geolocationEnabled) { this.geolocationEnabled = geolocationEnabled; }
    public long getWaitingListLimit() { return waitingListLimit; }
    public void setWaitingListLimit(long limit) { this.waitingListLimit = limit; }
    public List<String> getWaitingListIds() { return waitingListIds; }
    public void setWaitingListIds(List<String> ids) { this.waitingListIds = ids != null ? ids : new ArrayList<>(); }
    public long getWaitingListCount() { return waitingListCount; }
    public void setWaitingListCount(long count) { this.waitingListCount = count; }

    @Exclude
    public boolean checkIsRegistrationOpen() {
        if (registrationOpen == null || registrationClose == null) return false;
        long now = System.currentTimeMillis();
        return now >= registrationOpen.toDate().getTime() && now <= registrationClose.toDate().getTime();
    }
}
