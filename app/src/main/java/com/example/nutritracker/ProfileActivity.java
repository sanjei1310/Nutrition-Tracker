package com.example.nutritracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nutritracker.firebase.FirebaseService;
import com.example.nutritracker.firebase.UserProfile;
import com.example.nutritracker.utils.ThemeManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {
    
    private TextView tvUserEmail, tvCurrentTheme, tvCurrentPlan;
    private View layoutResetPassword, layoutThemeSelector, layoutGoalSettings, layoutSubscriptionPlans;
    private MaterialButton btnLogout, btnDeleteAccount;
    
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        ThemeManager.applyTheme(ThemeManager.getTheme(this));
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Initialize Firebase service
        firebaseService = new FirebaseService();
        
        // Check if user is logged in
        if (firebaseService.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }
        
        initViews();
        setupClickListeners();
        loadUserInfo();
    }
    
    private void initViews() {
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvCurrentTheme = findViewById(R.id.tv_current_theme);
        tvCurrentPlan = findViewById(R.id.tv_current_plan);
        layoutResetPassword = findViewById(R.id.layout_reset_password);
        layoutGoalSettings = findViewById(R.id.layout_goal_settings);
        layoutThemeSelector = findViewById(R.id.layout_theme_selector);
        layoutSubscriptionPlans = findViewById(R.id.layout_subscription_plans);
        btnLogout = findViewById(R.id.btn_logout);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);
    }
    
    private void setupClickListeners() {
        layoutResetPassword.setOnClickListener(v -> showChangePasswordDialog());
        layoutGoalSettings.setOnClickListener(v -> showGoalSettingsDialog());
        layoutThemeSelector.setOnClickListener(v -> showThemeSelector());
        layoutSubscriptionPlans.setOnClickListener(v -> showSubscriptionPlansDialog());
        btnLogout.setOnClickListener(v -> logout());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }
    
    private void loadUserInfo() {
        FirebaseUser user = firebaseService.getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail());
        }
        
        // Update theme display
        int currentTheme = ThemeManager.getTheme(this);
        tvCurrentTheme.setText(ThemeManager.getThemeName(currentTheme));
        
        // Load user profile to get subscription plan
        firebaseService.getUserProfile(new FirebaseService.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    String plan = profile.getSubscriptionPlan();
                    if (plan == null || plan.isEmpty()) {
                        plan = "Free";
                    }
                    tvCurrentPlan.setText(plan + " Plan");
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    tvCurrentPlan.setText("Free Plan");
                });
            }
        });
    }
    
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");
        
        // Create custom layout for password change
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        final EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setHint("Current Password");
        currentPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordInput);
        
        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("New Password");
        newPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordInput);
        
        final EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setHint("Confirm New Password");
        confirmPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(confirmPasswordInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("Change Password", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();
            
            if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (newPassword.length() < 6) {
                Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show();
                return;
            }
            
            firebaseService.changePassword(currentPassword, newPassword, new FirebaseService.PasswordChangeCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ProfileActivity.this, "Password changed successfully!", Toast.LENGTH_LONG).show();
                }
                
                @Override
                public void onFailure(String error) {
                    Toast.makeText(ProfileActivity.this, "Failed to change password: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    

    
    private void logout() {
        // Log logout activity
        firebaseService.logUserActivity("LOGOUT", "User logged out from ProfileActivity");
        
        // Sign out from Firebase
        firebaseService.getAuth().signOut();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }
    
    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("⚠️ WARNING: This will permanently delete:\n\n" +
                "• Your account and login credentials\n" +
                "• All nutrition tracking data\n" +
                "• All activity history\n" +
                "• All app preferences\n\n" +
                "This action CANNOT be undone!");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        
        // Add confirmation step
        builder.setPositiveButton("DELETE EVERYTHING", (dialog, which) -> {
            showFinalConfirmationDialog();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showFinalConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Final Confirmation");
        builder.setMessage("Type 'DELETE' to confirm permanent account deletion:");
        
        final EditText confirmInput = new EditText(this);
        confirmInput.setHint("Type DELETE here");
        confirmInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        builder.setView(confirmInput);
        
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String confirmation = confirmInput.getText().toString().trim();
            if ("DELETE".equals(confirmation)) {
                performAccountDeletion();
            } else {
                Toast.makeText(this, "Confirmation text doesn't match. Account not deleted.", Toast.LENGTH_LONG).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void performAccountDeletion() {
        // First, ask for password to re-authenticate
        AlertDialog.Builder authBuilder = new AlertDialog.Builder(this);
        authBuilder.setTitle("Re-authenticate");
        authBuilder.setMessage("For security, please enter your password to confirm account deletion:");
        
        final EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter your password");
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        authBuilder.setView(passwordInput);
        
        authBuilder.setPositiveButton("Confirm", (dialog, which) -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show progress dialog
            AlertDialog progressDialog = new AlertDialog.Builder(this)
                    .setTitle("Deleting Account")
                    .setMessage("Authenticating...")
                    .setCancelable(false)
                    .create();
            progressDialog.show();
            
            // Re-authenticate user before deletion
            firebaseService.reauthenticateAndDeleteAccount(password, new FirebaseService.AccountDeletionCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Account deleted successfully", Toast.LENGTH_LONG).show();
                        navigateToLogin();
                    });
                }
                
                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Failed to delete account: " + error, Toast.LENGTH_LONG).show();
                    });
                }
                
                @Override
                public void onProgress(String message) {
                    runOnUiThread(() -> {
                        progressDialog.setMessage(message);
                    });
                }
            });
        });
        
        authBuilder.setNegativeButton("Cancel", null);
        authBuilder.show();
    }
    
    private void performAccountDeletionOld() {
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Deleting Account")
                .setMessage("Please wait while we delete all your data...")
                .setCancelable(false)
                .create();
        progressDialog.show();
        
        firebaseService.deleteAccountWithAllData(new FirebaseService.AccountDeletionCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "Account and all data deleted successfully", Toast.LENGTH_LONG).show();
                    navigateToLogin();
                });
            }
            
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "Failed to delete account: " + error, Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onProgress(String message) {
                runOnUiThread(() -> {
                    progressDialog.setMessage(message);
                });
            }
        });
    }
    
    private void showThemeSelector() {
        String[] themes = {"Light", "Dark", "System Default"};
        int currentTheme = ThemeManager.getTheme(this);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Theme");
        builder.setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
            ThemeManager.setTheme(this, which);
            tvCurrentTheme.setText(ThemeManager.getThemeName(which));
            
            // Log theme change
            firebaseService.logUserActivity("THEME_CHANGED", 
                "Theme changed to: " + ThemeManager.getThemeName(which));
            
            dialog.dismiss();
            
            // Recreate activity to apply theme
            recreate();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showGoalSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Goal Settings");
        
        // Create custom layout for goal settings
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        // Calories Input
        com.google.android.material.textfield.TextInputLayout kcalLayout = 
            new com.google.android.material.textfield.TextInputLayout(this);
        kcalLayout.setHint("Daily Calories Goal");
        kcalLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        
        final com.google.android.material.textfield.TextInputEditText kcalInput = 
            new com.google.android.material.textfield.TextInputEditText(this);
        kcalInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        kcalInput.setTextColor(getResources().getColor(R.color.textDark));
        kcalInput.setHintTextColor(getResources().getColor(R.color.textSubtle));
        kcalLayout.addView(kcalInput);
        layout.addView(kcalLayout);
        
        // Add spacing
        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16));
        layout.addView(spacer1);
        
        // Protein Input
        com.google.android.material.textfield.TextInputLayout proteinLayout = 
            new com.google.android.material.textfield.TextInputLayout(this);
        proteinLayout.setHint("Daily Protein Goal (grams)");
        proteinLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        
        final com.google.android.material.textfield.TextInputEditText proteinInput = 
            new com.google.android.material.textfield.TextInputEditText(this);
        proteinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        proteinInput.setTextColor(getResources().getColor(R.color.textDark));
        proteinInput.setHintTextColor(getResources().getColor(R.color.textSubtle));
        proteinLayout.addView(proteinInput);
        layout.addView(proteinLayout);
        
        // Add spacing
        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16));
        layout.addView(spacer2);
        
        // Fat Input
        com.google.android.material.textfield.TextInputLayout fatLayout = 
            new com.google.android.material.textfield.TextInputLayout(this);
        fatLayout.setHint("Daily Fat Goal (grams)");
        fatLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        
        final com.google.android.material.textfield.TextInputEditText fatInput = 
            new com.google.android.material.textfield.TextInputEditText(this);
        fatInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        fatInput.setTextColor(getResources().getColor(R.color.textDark));
        fatInput.setHintTextColor(getResources().getColor(R.color.textSubtle));
        fatLayout.addView(fatInput);
        layout.addView(fatLayout);
        
        // Load current goals
        firebaseService.getUserProfile(new FirebaseService.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    kcalInput.setText(String.valueOf(profile.getGoalKcal()));
                    proteinInput.setText(String.valueOf(profile.getGoalProtein()));
                    fatInput.setText(String.valueOf(profile.getGoalFat()));
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                // Use default values
                runOnUiThread(() -> {
                    kcalInput.setText("2200");
                    proteinInput.setText("120");
                    fatInput.setText("70");
                });
            }
        });
        
        builder.setView(layout);
        
        builder.setPositiveButton("Save Goals", (dialog, which) -> {
            String kcalStr = kcalInput.getText().toString().trim();
            String proteinStr = proteinInput.getText().toString().trim();
            String fatStr = fatInput.getText().toString().trim();
            
            if (TextUtils.isEmpty(kcalStr) || TextUtils.isEmpty(proteinStr) || TextUtils.isEmpty(fatStr)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                int kcal = Integer.parseInt(kcalStr);
                int protein = Integer.parseInt(proteinStr);
                int fat = Integer.parseInt(fatStr);
                
                if (kcal < 1000 || kcal > 5000) {
                    Toast.makeText(this, "Calories should be between 1000-5000", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (protein < 50 || protein > 300) {
                    Toast.makeText(this, "Protein should be between 50-300g", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (fat < 20 || fat > 200) {
                    Toast.makeText(this, "Fat should be between 20-200g", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Update goals
                firebaseService.getUserProfile(new FirebaseService.UserProfileCallback() {
                    @Override
                    public void onSuccess(UserProfile profile) {
                        profile.setGoalKcal(kcal);
                        profile.setGoalProtein(protein);
                        profile.setGoalFat(fat);
                        
                        firebaseService.updateUserProfile(profile, new FirebaseService.UserProfileCallback() {
                            @Override
                            public void onSuccess(UserProfile updatedProfile) {
                                Toast.makeText(ProfileActivity.this, "Goals updated successfully!", Toast.LENGTH_SHORT).show();
                                firebaseService.logUserActivity("GOALS_UPDATED", 
                                    String.format("Updated goals: %d kcal, %d protein, %d fat", kcal, protein, fat));
                                
                                // Set result to notify HomeActivity to refresh
                                setResult(RESULT_OK);
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(ProfileActivity.this, "Failed to update goals: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(ProfileActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showSubscriptionPlansDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Subscription Plans");
        
        // Create custom layout for subscription plans
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        // Get current plan
        firebaseService.getUserProfile(new FirebaseService.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                runOnUiThread(() -> {
                    String userPlan = profile.getSubscriptionPlan();
                    if (userPlan == null || userPlan.isEmpty()) {
                        userPlan = "Free";
                    }
                    
                    // Make it effectively final for lambda usage
                    final String currentPlan = userPlan;
                    
                    // Create plan options
                    String[] plans = {"Free", "Premium", "Pro"};
                    String[] planDescriptions = {
                        "Free Plan\n• Basic nutrition tracking\n• Limited food database\n• Standard goals",
                        "Premium Plan - $4.99/month\n• Advanced nutrition tracking\n• Extended food database\n• Custom meal plans\n• Progress analytics",
                        "Pro Plan - $9.99/month\n• Everything in Premium\n• AI-powered recommendations\n• Unlimited food entries\n• Export data\n• Priority support"
                    };
                    
                    // Find current plan index
                    int currentPlanIndex = 0;
                    for (int i = 0; i < plans.length; i++) {
                        if (plans[i].equals(currentPlan)) {
                            currentPlanIndex = i;
                            break;
                        }
                    }
                    
                    // Add current plan info
                    TextView currentPlanText = new TextView(ProfileActivity.this);
                    currentPlanText.setText("Current Plan: " + currentPlan + " Plan");
                    currentPlanText.setTextSize(16);
                    currentPlanText.setTypeface(null, android.graphics.Typeface.BOLD);
                    currentPlanText.setPadding(0, 0, 0, 20);
                    layout.addView(currentPlanText);
                    
                    // Add plan descriptions
                    for (int i = 0; i < plans.length; i++) {
                        TextView planText = new TextView(ProfileActivity.this);
                        planText.setText(planDescriptions[i]);
                        planText.setTextSize(14);
                        planText.setPadding(0, 10, 0, 20);
                        
                        if (i == currentPlanIndex) {
                            planText.setTextColor(getResources().getColor(R.color.primaryGreen));
                            planText.setTypeface(null, android.graphics.Typeface.BOLD);
                        }
                        
                        layout.addView(planText);
                    }
                    
                    builder.setView(layout);
                    
                    // Add upgrade buttons for non-current plans
                    if (!currentPlan.equals("Pro")) {
                        builder.setPositiveButton("Upgrade", (dialog, which) -> {
                            // Show upgrade options
                            showUpgradeDialog(currentPlan);
                        });
                    }
                    
                    builder.setNegativeButton("Close", null);
                    builder.show();
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Failed to load subscription info", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showUpgradeDialog(String currentPlan) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upgrade Plan");
        
        String[] upgradeOptions;
        if (currentPlan.equals("Free")) {
            upgradeOptions = new String[]{"Premium - $4.99/month", "Pro - $9.99/month"};
        } else {
            upgradeOptions = new String[]{"Pro - $9.99/month"};
        }
        
        builder.setItems(upgradeOptions, (dialog, which) -> {
            String selectedPlan;
            if (currentPlan.equals("Free")) {
                selectedPlan = which == 0 ? "Premium" : "Pro";
            } else {
                selectedPlan = "Pro";
            }
            
            // Simulate upgrade process
            upgradePlan(selectedPlan);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void upgradePlan(String newPlan) {
        // Show loading dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Upgrading Plan")
                .setMessage("Processing your upgrade...")
                .setCancelable(false)
                .create();
        progressDialog.show();
        
        // Update user profile with new plan
        firebaseService.getUserProfile(new FirebaseService.UserProfileCallback() {
            @Override
            public void onSuccess(UserProfile profile) {
                profile.setSubscriptionPlan(newPlan);
                
                firebaseService.updateUserProfile(profile, new FirebaseService.UserProfileCallback() {
                    @Override
                    public void onSuccess(UserProfile updatedProfile) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            tvCurrentPlan.setText(newPlan + " Plan");
                            Toast.makeText(ProfileActivity.this, "Successfully upgraded to " + newPlan + " Plan!", Toast.LENGTH_LONG).show();
                            
                            // Log upgrade activity
                            firebaseService.logUserActivity("PLAN_UPGRADED", "Upgraded to " + newPlan + " plan");
                        });
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(ProfileActivity.this, "Failed to upgrade plan: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        // Navigate back to home activity
        Intent intent = new Intent(ProfileActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}