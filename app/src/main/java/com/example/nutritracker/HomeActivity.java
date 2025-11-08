package com.example.nutritracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.nutritracker.firebase.FirebaseService;
import com.example.nutritracker.firebase.UserProfile;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.nutritracker.firebase.NutritionEntry;

public class HomeActivity extends AppCompatActivity {

    // Dynamic goals (loaded from user profile)
    private int GOAL_KCAL = 2200;
    private int GOAL_PROTEIN = 120;
    private int GOAL_FAT = 70;
    private String username = "User";

    // Totals
    private double totalKcal = 0;
    private double totalProtein = 0;
    private double totalFat = 0;

    // Map to store calories per meal
    private Map<String, Double> mealKcalMap;

    // UI elements for totals
    private TextView tvKcalConsumed, tvKcalRemaining, tvKcalGoal;
    private TextView tvProteinProgress, tvFatProgress;
    private TextView tvGreeting;
    private ProgressBar pbKcal, pbProtein, pbFat;

    // UI elements for meals
    private View breakfastView, lunchView, snackView, dinnerView;
    private TextView tvBreakfastCals, tvLunchCals, tvSnackCals, tvDinnerCals;
    
    // Date navigation
    private TextView tvCurrentDate;
    private ImageButton btnPreviousDay, btnNextDay;
    private String currentDate;
    private SimpleDateFormat dateFormat;

    // Activity launcher
    private ActivityResultLauncher<Intent> foodLauncher;
    private ActivityResultLauncher<Intent> profileLauncher;
    
    // Firebase service
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        com.example.nutritracker.utils.ThemeManager.applyTheme(
            com.example.nutritracker.utils.ThemeManager.getTheme(this));
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        // Initialize Firebase service and check login
        firebaseService = new FirebaseService();
        
        // Check if user is logged in
        if (firebaseService.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }
        
        // Log app open activity
        firebaseService.logUserActivity("APP_OPEN", "User opened the home screen");

        // Initialize date format and current date
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        currentDate = dateFormat.format(new Date());
        


        // Initialize meal map
        mealKcalMap = new HashMap<>();
        mealKcalMap.put("Breakfast", 0.0);
        mealKcalMap.put("Lunch", 0.0);
        mealKcalMap.put("Snack", 0.0);
        mealKcalMap.put("Dinner", 0.0);

        // This is the new way to handle activity results
        foodLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                // Get the nutrients from AddFoodActivity
                                double kcal = data.getDoubleExtra("KCAL", 0);
                                double protein = data.getDoubleExtra("PROTEIN", 0);
                                double fat = data.getDoubleExtra("FAT", 0);
                                String mealType = data.getStringExtra("MEAL_TYPE");

                                // Add to totals
                                totalKcal += kcal;
                                totalProtein += protein;
                                totalFat += fat;

                                // Add to specific meal
                                if (mealType != null && mealKcalMap.containsKey(mealType)) {
                                    double currentMealKcal = mealKcalMap.get(mealType);
                                    mealKcalMap.put(mealType, currentMealKcal + kcal);
                                }
                                
                                // Save to Firebase with callback
                                String foodName = data.getStringExtra("FOOD_NAME");
                                if (foodName == null) foodName = "Unknown Food";
                                
                                firebaseService.saveNutritionEntry(currentDate, mealType, foodName, kcal, protein, fat, 1.0, 
                                    new FirebaseService.SaveCallback() {
                                        @Override
                                        public void onSuccess() {
                                            android.util.Log.d("HomeActivity", "✅ Food data saved to Firebase successfully!");
                                        }
                                        
                                        @Override
                                        public void onFailure(Exception e) {
                                            android.util.Log.e("HomeActivity", "❌ Failed to save food data to Firebase", e);
                                            runOnUiThread(() -> {
                                                Toast.makeText(HomeActivity.this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                        }
                                    });
                                
                                // Log food addition activity
                                String foodDetails = String.format("Added %s to %s: %.1f kcal, %.1f protein, %.1f fat", 
                                    foodName, mealType, kcal, protein, fat);
                                firebaseService.logUserActivity("ADD_FOOD", foodDetails);

                                // Update UI immediately
                                updateUI();
                            }
                        }
                    }
                });

        // Profile launcher to handle goal updates
        profileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // Goals were updated, refresh user profile and UI
                            android.util.Log.d("HomeActivity", "🔄 Goals updated, refreshing profile and UI");
                            loadUserProfile();
                        }
                    }
                });

        // Find all UI views
        findViews();
        // Set up click listeners for the "add" buttons
        setupClickListeners();
        
        // Load user profile first, then nutrition data
        loadUserProfile();
        
        // Show initial UI with zeros while loading
        updateUI();
        
        // Load nutrition data for current date (this will update UI when data is loaded)
        loadNutritionDataForDate(currentDate);

        // --- Bottom Navigation ---
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_ai_assistant) {
                startActivity(new Intent(getApplicationContext(), AiAssistantActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent profileIntent = new Intent(getApplicationContext(), ProfileActivity.class);
                profileLauncher.launch(profileIntent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
    }

    private void findViews() {
        // Find greeting view
        tvGreeting = findViewById(R.id.tv_greeting);
        
        // Find date navigation views
        tvCurrentDate = findViewById(R.id.tv_current_date);
        btnPreviousDay = findViewById(R.id.btn_previous_day);
        btnNextDay = findViewById(R.id.btn_next_day);
        
        // Find total summary views
        tvKcalConsumed = findViewById(R.id.tv_kcal_consumed);
        tvKcalRemaining = findViewById(R.id.tv_kcal_remaining);
        tvKcalGoal = findViewById(R.id.tv_kcal_goal);
        pbKcal = findViewById(R.id.pb_kcal_main);

        tvProteinProgress = findViewById(R.id.tv_protein_progress);
        pbProtein = findViewById(R.id.pb_protein);

        tvFatProgress = findViewById(R.id.tv_fat_progress);
        pbFat = findViewById(R.id.pb_fat);

        // Find meal <include> layouts
        breakfastView = findViewById(R.id.layout_breakfast);
        lunchView = findViewById(R.id.layout_lunch);
        snackView = findViewById(R.id.layout_snack);
        dinnerView = findViewById(R.id.layout_dinner);

        // Find views *inside* each meal layout
        tvBreakfastCals = breakfastView.findViewById(R.id.tv_meal_kcal);
        ((TextView) breakfastView.findViewById(R.id.tv_meal_name)).setText("Breakfast");
        ((ImageView) breakfastView.findViewById(R.id.iv_meal_icon)).setImageResource(R.drawable.ic_breakfast_placeholder); // Set icon
        ((ImageView) breakfastView.findViewById(R.id.iv_meal_icon)).setColorFilter(ContextCompat.getColor(this, R.color.breakfastColor)); // Example color

        tvLunchCals = lunchView.findViewById(R.id.tv_meal_kcal);
        ((TextView) lunchView.findViewById(R.id.tv_meal_name)).setText("Lunch");
        ((ImageView) lunchView.findViewById(R.id.iv_meal_icon)).setImageResource(R.drawable.ic_lunch_placeholder); // Set icon
        ((ImageView) lunchView.findViewById(R.id.iv_meal_icon)).setColorFilter(ContextCompat.getColor(this, R.color.lunchColor)); // Example color

        tvSnackCals = snackView.findViewById(R.id.tv_meal_kcal);
        ((TextView) snackView.findViewById(R.id.tv_meal_name)).setText("Snack");
        ((ImageView) snackView.findViewById(R.id.iv_meal_icon)).setImageResource(R.drawable.ic_snack_placeholder); // Set icon
        ((ImageView) snackView.findViewById(R.id.iv_meal_icon)).setColorFilter(ContextCompat.getColor(this, R.color.snackColor)); // Example color

        tvDinnerCals = dinnerView.findViewById(R.id.tv_meal_kcal);
        ((TextView) dinnerView.findViewById(R.id.tv_meal_name)).setText("Dinner");
        ((ImageView) dinnerView.findViewById(R.id.iv_meal_icon)).setImageResource(R.drawable.ic_dinner_placeholder); // Set icon
        ((ImageView) dinnerView.findViewById(R.id.iv_meal_icon)).setColorFilter(ContextCompat.getColor(this, R.color.dinnerColor)); // Example color
    }

    private void setupClickListeners() {
        // Date navigation listeners
        btnPreviousDay.setOnClickListener(v -> navigateDate(-1));
        btnNextDay.setOnClickListener(v -> navigateDate(1));
        
        // Find the "add" button inside each meal layout
        ImageButton btnAddBreakfast = breakfastView.findViewById(R.id.btn_add_meal);
        ImageButton btnAddLunch = lunchView.findViewById(R.id.btn_add_meal);
        ImageButton btnAddSnack = snackView.findViewById(R.id.btn_add_meal);
        ImageButton btnAddDinner = dinnerView.findViewById(R.id.btn_add_meal);

        // Set click listeners
        btnAddBreakfast.setOnClickListener(v -> openAddFood("Breakfast"));
        btnAddLunch.setOnClickListener(v -> openAddFood("Lunch"));
        btnAddSnack.setOnClickListener(v -> openAddFood("Snack"));
        btnAddDinner.setOnClickListener(v -> openAddFood("Dinner"));
    }

    private void openAddFood(String mealType) {
        Intent intent = new Intent(HomeActivity.this, AddFoodActivity.class);
        intent.putExtra("MEAL_TYPE", mealType);
        foodLauncher.launch(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void updateUI() {
        // --- Update Total Calories ---
        double remainingKcal = GOAL_KCAL - totalKcal;
        tvKcalConsumed.setText(String.format(Locale.getDefault(), "%.0f", totalKcal));
        tvKcalRemaining.setText(String.format(Locale.getDefault(), "%.0f", remainingKcal));
        tvKcalGoal.setText(String.format(Locale.getDefault(), "Goal: %d kcal", GOAL_KCAL));
        pbKcal.setMax(GOAL_KCAL);
        pbKcal.setProgress((int) totalKcal);

        // --- Update Protein ---
        tvProteinProgress.setText(String.format(Locale.getDefault(), "%.0f / %dg", totalProtein, GOAL_PROTEIN));
        pbProtein.setMax(GOAL_PROTEIN);
        pbProtein.setProgress((int) totalProtein);

        // --- Update Fat ---
        tvFatProgress.setText(String.format(Locale.getDefault(), "%.0f / %dg", totalFat, GOAL_FAT));
        pbFat.setMax(GOAL_FAT);
        pbFat.setProgress((int) totalFat);

        // --- Update Individual Meal Calories ---
        tvBreakfastCals.setText(String.format(Locale.getDefault(), "%.0f kcal", mealKcalMap.get("Breakfast")));
        tvLunchCals.setText(String.format(Locale.getDefault(), "%.0f kcal", mealKcalMap.get("Lunch")));
        tvSnackCals.setText(String.format(Locale.getDefault(), "%.0f kcal", mealKcalMap.get("Snack")));
        tvDinnerCals.setText(String.format(Locale.getDefault(), "%.0f kcal", mealKcalMap.get("Dinner")));
    }
    
    // Removed menu - all options moved to Profile screen
    
    private void navigateDate(int dayOffset) {
        try {
            Date date = dateFormat.parse(currentDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH, dayOffset);
            
            currentDate = dateFormat.format(calendar.getTime());
            updateDateDisplay();
            loadNutritionDataForDate(currentDate);
            
            // Log date navigation
            firebaseService.logUserActivity("DATE_NAVIGATION", 
                "Navigated to date: " + currentDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void updateDateDisplay() {
        try {
            Date date = dateFormat.parse(currentDate);
            SimpleDateFormat displayFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
            
            // Check if it's today
            String today = dateFormat.format(new Date());
            if (currentDate.equals(today)) {
                tvCurrentDate.setText("Today, " + displayFormat.format(date));
            } else {
                tvCurrentDate.setText(displayFormat.format(date));
            }
        } catch (Exception e) {
            tvCurrentDate.setText(currentDate);
        }
    }
    
    private void loadNutritionDataForDate(String date) {
        // Show loading state but don't reset data yet
        updateDateDisplay();
        
        if (firebaseService == null) {
            // Reinitialize Firebase service if needed
            firebaseService = new FirebaseService();
        }
        
        // Check if user is still logged in
        if (firebaseService.getCurrentUser() == null) {
            android.util.Log.w("HomeActivity", "User not logged in, cannot load nutrition data");
            navigateToLogin();
            return;
        }
        
        android.util.Log.d("HomeActivity", "Loading nutrition data for date: " + date);
        
        // Debug: Check all entries in database
        firebaseService.debugAllNutritionEntries();
        
        firebaseService.getNutritionEntriesForDate(date, new FirebaseService.NutritionEntriesCallback() {
            @Override
            public void onSuccess(List<NutritionEntry> entries) {
                runOnUiThread(() -> {
                    android.util.Log.d("HomeActivity", "🎯 Processing " + entries.size() + " entries for date: " + date);
                    
                    // Reset totals ONLY after successful Firebase response
                    totalKcal = 0;
                    totalProtein = 0;
                    totalFat = 0;
                    mealKcalMap.put("Breakfast", 0.0);
                    mealKcalMap.put("Lunch", 0.0);
                    mealKcalMap.put("Snack", 0.0);
                    mealKcalMap.put("Dinner", 0.0);
                    
                    // Calculate totals from Firebase data
                    for (NutritionEntry entry : entries) {
                        android.util.Log.d("HomeActivity", "📊 Processing entry: " + entry.getFoodName() + 
                            " - " + entry.getKcal() + " kcal (" + entry.getMealType() + ")");
                        
                        totalKcal += entry.getKcal();
                        totalProtein += entry.getProtein();
                        totalFat += entry.getFat();
                        
                        // Add to specific meal
                        String mealType = entry.getMealType();
                        if (mealKcalMap.containsKey(mealType)) {
                            double currentMealKcal = mealKcalMap.get(mealType);
                            mealKcalMap.put(mealType, currentMealKcal + entry.getKcal());
                            android.util.Log.d("HomeActivity", "🍽️ Updated " + mealType + " to " + (currentMealKcal + entry.getKcal()) + " kcal");
                        }
                    }
                    
                    android.util.Log.d("HomeActivity", "📈 Final totals for " + date + ": " + 
                        totalKcal + " kcal, " + totalProtein + "g protein, " + totalFat + "g fat");
                    
                    updateUI();
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    // Log the error for debugging
                    android.util.Log.e("HomeActivity", "Failed to load nutrition data for " + date, e);
                    
                    // Only reset to zeros if we're loading a different date
                    // Keep current data to prevent accidental data loss
                    String today = dateFormat.format(new Date());
                    if (!date.equals(today)) {
                        // Loading a different date that failed, show zeros
                        totalKcal = 0;
                        totalProtein = 0;
                        totalFat = 0;
                        mealKcalMap.put("Breakfast", 0.0);
                        mealKcalMap.put("Lunch", 0.0);
                        mealKcalMap.put("Snack", 0.0);
                        mealKcalMap.put("Dinner", 0.0);
                        updateUI();
                    }
                    // If loading today's data failed, keep existing data
                });
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Ensure home icon is highlighted when returning from profile
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
        
        // Refresh data when activity resumes
        if (firebaseService != null && firebaseService.getCurrentUser() != null) {
            android.util.Log.d("HomeActivity", "🔄 onResume: Reloading data for date: " + currentDate);
            loadUserProfile();
            loadNutritionDataForDate(currentDate);
        }
    }
    
    private void refreshCurrentDateData() {
        // Refresh data for current date without changing the date
        android.util.Log.d("HomeActivity", "Manually refreshing data for current date: " + currentDate);
        loadNutritionDataForDate(currentDate);
    }
    
    public void forceDataRefresh() {
        // Public method to force refresh data (can be called from outside)
        refreshCurrentDateData();
    }
    
    private void loadUserProfile() {
        firebaseService.getUserProfile(new FirebaseService.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    username = profile.getUsername();
                    GOAL_KCAL = profile.getGoalKcal();
                    GOAL_PROTEIN = profile.getGoalProtein();
                    GOAL_FAT = profile.getGoalFat();
                    
                    // Update greeting
                    tvGreeting.setText("Hi " + username + "!");
                    
                    // Update UI with new goals
                    updateUI();
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.w("HomeActivity", "Failed to load user profile: " + e.getMessage());
                // Use default values and show default greeting
                runOnUiThread(() -> {
                    tvGreeting.setText("Hi there!");
                });
            }
        });
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void logout() {
        // Log logout activity
        firebaseService.logUserActivity("LOGOUT", "User logged out from HomeActivity");
        
        // Sign out from Firebase
        firebaseService.getAuth().signOut();
        
        // Navigate to login
        navigateToLogin();
    }
}

