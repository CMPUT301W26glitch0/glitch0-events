package com.example.cmput301_app.models;

import java.util.Date;

public class Event {
    private String eventId;
    private String title;
    private String description;
    private Date registrationStart;
    private Date registrationEnd;
    private long waitingListCount;
    private long maxSpots;
    private String idNumber;
    private String status;

    public Event() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getRegistrationStart() { return registrationStart; }
    public void setRegistrationStart(Date registrationStart) { this.registrationStart = registrationStart; }
    public Date getRegistrationEnd() { return registrationEnd; }
    public void setRegistrationEnd(Date registrationEnd) { this.registrationEnd = registrationEnd; }
    public long getWaitingListCount() { return waitingListCount; }
    public void setWaitingListCount(long waitingListCount) { this.waitingListCount = waitingListCount; }
    public long getMaxSpots() { return maxSpots; }
    public void setMaxSpots(long maxSpots) { this.maxSpots = maxSpots; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isRegistrationOpen() {
        if (registrationStart == null || registrationEnd == null) return false;
        Date now = new Date();
        return now.after(registrationStart) && now.before(registrationEnd);
    }
}