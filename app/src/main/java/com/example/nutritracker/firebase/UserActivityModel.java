package com.example.nutritracker.firebase;

import com.google.firebase.Timestamp;

public class UserActivityModel {
    private String id;
    private String userId;
    private String activityType;
    private String activityDetails;
    private Timestamp timestamp;

    // Default constructor required for Firestore
    public UserActivityModel() {}

    public UserActivityModel(String userId, String activityType, String activityDetails) {
        this.userId = userId;
        this.activityType = activityType;
        this.activityDetails = activityDetails;
        this.timestamp = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getActivityDetails() { return activityDetails; }
    public void setActivityDetails(String activityDetails) { this.activityDetails = activityDetails; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}