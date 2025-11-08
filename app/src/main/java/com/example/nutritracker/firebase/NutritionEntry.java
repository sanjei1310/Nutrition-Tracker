package com.example.nutritracker.firebase;

import com.google.firebase.Timestamp;

public class NutritionEntry {
    private String id;
    private String userId;
    private String date; // Format: "yyyy-MM-dd"
    private String mealType; // "Breakfast", "Lunch", "Snack", "Dinner"
    private String foodName;
    private double kcal;
    private double protein;
    private double fat;
    private double quantity;
    private Timestamp timestamp;

    // Default constructor required for Firestore
    public NutritionEntry() {}

    public NutritionEntry(String userId, String date, String mealType, String foodName, 
                         double kcal, double protein, double fat, double quantity) {
        this.userId = userId;
        this.date = date;
        this.mealType = mealType;
        this.foodName = foodName;
        this.kcal = kcal;
        this.protein = protein;
        this.fat = fat;
        this.quantity = quantity;
        this.timestamp = Timestamp.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public double getKcal() { return kcal; }
    public void setKcal(double kcal) { this.kcal = kcal; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}