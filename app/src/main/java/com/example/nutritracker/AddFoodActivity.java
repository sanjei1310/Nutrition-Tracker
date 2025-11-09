package com.example.nutritracker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nutritracker.firebase.FirebaseService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddFoodActivity extends AppCompatActivity {

    // Your API key for FoodData Central
    // Get your free API key from: https://fdc.nal.usda.gov/api-key-signup.html
    private static final String API_KEY = "PASTE_YOUR_USDA_API_KEY_HERE";
    private static final String TAG = "AddFoodActivity";

    private String mealType = "Food"; // Member variable to store meal type

    // Standard UI views
    private EditText etSearchFood;
    private ImageButton btnSearch, btnBack;
    private Button btnAddFood;
    private RecyclerView rvFoodResults;
    
    // Firebase service
    private FirebaseService firebaseService;
    private ProgressBar pbLoading;
    private TextView tvMealTitle;

    // New views for the "Quantity" screen
    private FoodItem selectedFood = null;
    private LinearLayout layoutQuantity;
    private TextView tvSelectedFoodName;
    private EditText etQuantity;

    // RecyclerView components
    private FoodSearchAdapter adapter;
    private final List<FoodItem> foodList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);
        
        // Initialize Firebase service
        firebaseService = new FirebaseService();

        // Find standard views
        tvMealTitle = findViewById(R.id.tv_meal_title);
        etSearchFood = findViewById(R.id.et_search_food);
        btnSearch = findViewById(R.id.btn_search);
        btnBack = findViewById(R.id.btn_back);
        btnAddFood = findViewById(R.id.btn_add_food);
        rvFoodResults = findViewById(R.id.rv_food_results);
        pbLoading = findViewById(R.id.pb_loading);

        // Find new quantity views
        layoutQuantity = findViewById(R.id.layout_quantity);
        tvSelectedFoodName = findViewById(R.id.tv_selected_food_name);
        etQuantity = findViewById(R.id.et_quantity);

        // Set meal title from the intent and store it
        mealType = getIntent().getStringExtra("MEAL_TYPE");
        if (mealType == null || mealType.isEmpty()) {
            mealType = "Food"; // Default fallback
        }
        tvMealTitle.setText("Add " + mealType);

        setupRecyclerView();
        setupButtonListeners();
    }

    private void setupRecyclerView() {
        // Initialize the adapter with a click listener
        adapter = new FoodSearchAdapter(foodList, foodItem -> {
            // This code runs when a user clicks a food in the search results
            this.selectedFood = foodItem;

            // Update UI to show quantity entry
            tvSelectedFoodName.setText(foodItem.getDescription());
            layoutQuantity.setVisibility(View.VISIBLE);
            etQuantity.requestFocus(); // Focus the quantity field
            rvFoodResults.setVisibility(View.GONE); // Hide search results

            // Disable search to lock in choice
            etSearchFood.setEnabled(false);
            btnSearch.setEnabled(false);

            // Show keyboard for quantity
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etQuantity, InputMethodManager.SHOW_IMPLICIT);
        });
        rvFoodResults.setLayoutManager(new LinearLayoutManager(this));
        rvFoodResults.setAdapter(adapter);
    }

    private void setupButtonListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnSearch.setOnClickListener(v -> searchFood());

        // Handle 'Enter' key on keyboard to search
        etSearchFood.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchFood();
                return true;
            }
            return false;
        });

        // Updated "Add Food" button logic
        btnAddFood.setOnClickListener(v -> {
            // Check if a food has been selected
            if (selectedFood == null) {
                Toast.makeText(this, "Please select a food from the list first", Toast.LENGTH_SHORT).show();
                return;
            }

            String quantityStr = etQuantity.getText().toString();
            if (quantityStr.isEmpty()) {
                etQuantity.setError("Enter quantity");
                etQuantity.requestFocus();
                return;
            }

            try {
                // Calculate nutrients based on quantity
                // API data is per 100g, so we divide by 100 to get per-gram values
                double quantityGrams = Double.parseDouble(quantityStr);
                double multiplier = quantityGrams / 100.0;

                double finalKcal = selectedFood.getKcal() * multiplier;
                double finalProtein = selectedFood.getProtein() * multiplier;
                double finalFat = selectedFood.getFat() * multiplier;

                // Send calculated data AND meal type back to HomeActivity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("MEAL_TYPE", mealType); // Add this line
                resultIntent.putExtra("FOOD_NAME", selectedFood.getDescription()); // Add food name
                resultIntent.putExtra("KCAL", finalKcal);
                resultIntent.putExtra("PROTEIN", finalProtein);
                resultIntent.putExtra("FAT", finalFat);
                setResult(RESULT_OK, resultIntent);
                finish(); // Close this activity and go back to Home
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

            } catch (NumberFormatException e) {
                etQuantity.setError("Invalid number");
                etQuantity.requestFocus();
                Log.e(TAG, "NumberFormatException parsing quantity", e);
            }
        });
    }

    // Smart back button logic
    @Override
    public void onBackPressed() {
        // If user is on quantity screen, back button should return to search results
        if (layoutQuantity.getVisibility() == View.VISIBLE) {
            layoutQuantity.setVisibility(View.GONE);
            rvFoodResults.setVisibility(View.VISIBLE);
            etSearchFood.setEnabled(true);
            btnSearch.setEnabled(true);
            this.selectedFood = null; // Clear selection
            hideKeyboard();
        } else {
            // Otherwise, perform the default back action (finish activity)
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }


    private void searchFood() {
        String query = etSearchFood.getText().toString().trim();
        if (query.isEmpty()) {
            etSearchFood.setError("Please enter a food");
            return;
        }
        
        // Log food search activity
        firebaseService.logUserActivity("FOOD_SEARCH", "Searched for food: " + query);
        
        hideKeyboard();

        // Show loading spinner and hide old results
        runOnUiThread(() -> {
            pbLoading.setVisibility(View.VISIBLE);
            rvFoodResults.setVisibility(View.GONE);
            foodList.clear();
            adapter.notifyDataSetChanged();
        });

        // Build the API URL
        String url = "https://api.nal.usda.gov/fdc/v1/foods/search?api_key=" + API_KEY + "&query=" + query + "&pageSize=20";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "API Call Failed", e);
                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(AddFoodActivity.this, "Search failed. Check connection.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "API Call Unsuccessful: " + response.code());
                    runOnUiThread(() -> pbLoading.setVisibility(View.GONE));
                    return;
                }

                // Parse the JSON response
                try (var responseBody = response.body()) {
                    String jsonString = responseBody.string();
                    JSONObject json = new JSONObject(jsonString);
                    JSONArray foods = json.getJSONArray("foods");
                    foodList.clear();
                    foodList.addAll(parseFoodData(foods));
                } catch (Exception e) {
                    Log.e(TAG, "JSON Parsing Error", e);
                }

                // Update the UI on the main thread
                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    if (foodList.isEmpty()) {
                        Toast.makeText(AddFoodActivity.this, "No results found.", Toast.LENGTH_SHORT).show();
                    } else {
                        rvFoodResults.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private List<FoodItem> parseFoodData(JSONArray foods) {
        List<FoodItem> items = new ArrayList<>();
        try {
            for (int i = 0; i < foods.length(); i++) {
                JSONObject food = foods.getJSONObject(i);
                String desc = food.getString("description");
                String brand = food.optString("brandOwner", "Generic"); // Use "Generic" if no brand

                double kcal = 0, protein = 0, fat = 0;
                JSONArray nutrients = food.getJSONArray("foodNutrients");
                for (int j = 0; j < nutrients.length(); j++) {
                    JSONObject nutrient = nutrients.getJSONObject(j);
                    String nutrientName = nutrient.getString("nutrientName");

                    // API data is per 100g.
                    if (nutrientName.equals("Energy") && nutrient.optString("unitName", "").equalsIgnoreCase("KCAL")) {
                        kcal = nutrient.getDouble("value");
                    } else if (nutrientName.equals("Protein")) {
                        protein = nutrient.getDouble("value");
                    } else if (nutrientName.equals("Total lipid (fat)") || nutrientName.equals("Fat")) {
                        fat = nutrient.getDouble("value");
                    }
                }
                // Only add if it has calorie data
                if (kcal > 0) {
                    items.add(new FoodItem(desc, brand, kcal, protein, fat));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing individual food", e);
        }
        return items;
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    

}

