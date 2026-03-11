package com.example.cmput301_app.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

public class Event {
    private String eventId;
    private String name;
    private String description;
    private String location;
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

    public Event() {
        this.waitingListIds = new ArrayList<>();
        this.waitingListLimit = -1;
    }

    @PropertyName("name")
    public String getName() { return name; }
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    @PropertyName("title")
    public String getTitle() { return name; }
    @PropertyName("title")
    public void setTitle(String title) { this.name = title; }

    @PropertyName("registrationOpen")
    public Timestamp getRegistrationOpen() { return registrationOpen; }
    @PropertyName("registrationOpen")
    public void setRegistrationOpen(Timestamp registrationOpen) { this.registrationOpen = registrationOpen; }

    @PropertyName("registrationStart")
    public Timestamp getRegistrationStart() { return registrationOpen; }
    @PropertyName("registrationStart")
    public void setRegistrationStart(Timestamp start) { this.registrationOpen = start; }

    @PropertyName("registrationClose")
    public Timestamp getRegistrationClose() { return registrationClose; }
    @PropertyName("registrationClose")
    public void setRegistrationClose(Timestamp registrationClose) { this.registrationClose = registrationClose; }

    @PropertyName("registrationEnd")
    public Timestamp getRegistrationEnd() { return registrationClose; }
    @PropertyName("registrationEnd")
    public void setRegistrationEnd(Timestamp end) { this.registrationClose = end; }

    public boolean isRegistrationOpen() {
        if (registrationOpen == null || registrationClose == null) return false;
        Timestamp now = Timestamp.now();
        return now.compareTo(registrationOpen) > 0 && now.compareTo(registrationClose) < 0;
    }

    public int getWaitingListCount() {
        return waitingListIds != null ? waitingListIds.size() : 0;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public long getCapacity() { return capacity; }
    public void setCapacity(long capacity) { this.capacity = capacity; }
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
}