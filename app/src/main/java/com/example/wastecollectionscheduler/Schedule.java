package com.example.wastecollectionscheduler;

public class Schedule {
    private String id;
    private String wasteType;
    private String date;
    private String time;
    private String status;
    private String userId;

    public Schedule() {
        // Default constructor required for calls to DataSnapshot.getValue(Schedule.class)
    }

    public Schedule(String wasteType, String date, String time, String status, String userId) {
        this.wasteType = wasteType;
        this.date = date;
        this.time = time;
        this.status = status;
        this.userId = userId;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWasteType() {
        return wasteType;
    }

    public void setWasteType(String wasteType) {
        this.wasteType = wasteType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
