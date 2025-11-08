package com.example.nutritracker.firebase;

import com.google.firebase.Timestamp;

public class UserProfile {
    private String id;
    private String userId;
    private String username;
    private String email;
    private int goalKcal;
    private int goalProtein;
    private int goalFat;
    private String subscriptionPlan;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Default constructor required for Firestore
    public UserProfile() {}

    public UserProfile(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.goalKcal = 2200; // Default goals
        this.goalProtein = 120;
        this.goalFat = 70;
        this.subscriptionPlan = "Free"; // Default plan
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getGoalKcal() { return goalKcal; }
    public void setGoalKcal(int goalKcal) { this.goalKcal = goalKcal; }

    public int getGoalProtein() { return goalProtein; }
    public void setGoalProtein(int goalProtein) { this.goalProtein = goalProtein; }

    public int getGoalFat() { return goalFat; }
    public void setGoalFat(int goalFat) { this.goalFat = goalFat; }

    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}