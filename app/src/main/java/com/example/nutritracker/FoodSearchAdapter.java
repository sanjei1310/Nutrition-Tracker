package com.example.nutritracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class FoodSearchAdapter extends RecyclerView.Adapter<FoodSearchAdapter.FoodViewHolder> {

    private final List<FoodItem> foodList;
    private final OnFoodClickListener clickListener;

    /**
     * START OF FIX:
     * This is the interface that was missing.
     * It defines a "contract" that the AddFoodActivity must follow.
     */
    public interface OnFoodClickListener {
        void onFoodClick(FoodItem foodItem);
    }
    /** END OF FIX **/

    // Constructor updated to accept the listener
    public FoodSearchAdapter(List<FoodItem> foodList, OnFoodClickListener clickListener) {
        this.foodList = foodList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        FoodItem foodItem = foodList.get(position);
        holder.bind(foodItem, clickListener);
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    // --- ViewHolder Class ---
    static class FoodViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvFoodName;
        private final TextView tvBrand;
        private final TextView tvNutrients;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find views from list_item_food.xml
            tvFoodName = itemView.findViewById(R.id.tv_food_name);
            tvBrand = itemView.findViewById(R.id.tv_brand);
            tvNutrients = itemView.findViewById(R.id.tv_nutrients);
        }

        public void bind(final FoodItem foodItem, final OnFoodClickListener listener) {
            // Set text for each view
            tvFoodName.setText(foodItem.getDescription()); // Use getDescription()
            tvBrand.setText(foodItem.getBrand());

            String nutrients = String.format(Locale.getDefault(),
                    "Kcal: %.0f | P: %.1fg | F: %.1fg",
                    foodItem.getKcal(), foodItem.getProtein(), foodItem.getFat());
            tvNutrients.setText(nutrients);

            // Set the click listener for the entire row
            itemView.setOnClickListener(v -> listener.onFoodClick(foodItem));
        }
    }
}

