package com.example.nutritracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nutritracker.firebase.FirebaseService;
import com.example.nutritracker.firebase.UserActivityModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserActivityViewActivity extends AppCompatActivity {
    
    private TextView tvActivities;
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simple layout with just a TextView
        tvActivities = new TextView(this);
        tvActivities.setPadding(32, 32, 32, 32);
        tvActivities.setTextSize(14);
        setContentView(tvActivities);
        
        // Initialize Firebase service
        firebaseService = new FirebaseService();
        
        loadUserActivities();
    }
    
    private void loadUserActivities() {
        if (firebaseService.getCurrentUser() == null) {
            tvActivities.setText("No user logged in");
            return;
        }
        
        tvActivities.setText("Loading activities...");
        
        firebaseService.getUserActivities(new FirebaseService.UserActivitiesCallback() {
            @Override
            public void onSuccess(List<UserActivityModel> activities) {
                runOnUiThread(() -> {
                    if (activities.isEmpty()) {
                        tvActivities.setText("No activities found");
                        return;
                    }
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("User Activities:\n\n");
                    
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
                    
                    for (UserActivityModel activity : activities) {
                        Date date = activity.getTimestamp().toDate();
                        sb.append(String.format("%s\n%s: %s\n\n", 
                            dateFormat.format(date),
                            activity.getActivityType(),
                            activity.getActivityDetails()));
                    }
                    
                    tvActivities.setText(sb.toString());
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    tvActivities.setText("Error loading activities: " + e.getMessage());
                    Toast.makeText(UserActivityViewActivity.this, "Failed to load activities", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}