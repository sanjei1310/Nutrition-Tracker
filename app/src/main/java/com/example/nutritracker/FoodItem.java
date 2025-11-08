package com.example.nutritracker;

public class FoodItem {
    private final String description;
    private final String brand;
    private final double kcal;
    private final double protein;
    private final double fat;

    public FoodItem(String description, String brand, double kcal, double protein, double fat) {
        this.description = description;
        this.brand = brand;
        this.kcal = kcal;
        this.protein = protein;
        this.fat = fat;
    }

    // --- Getter Methods ---
    // (This is what your other files need to get the data)

    public String getDescription() {
        return description;
    }

    public String getBrand() {
        return brand; // This is the method that was missing
    }

    public double getKcal() {
        return kcal;
    }

    public double getProtein() {
        return protein;
    }

    public double getFat() {
        return fat;
    }
}

