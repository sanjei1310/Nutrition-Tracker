 package com.example.nutritracker.firebase;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirebaseService {
    private static final String TAG = "FirebaseService";
    private static final String COLLECTION_USER_ACTIVITIES = "user_activities";
    private static final String COLLECTION_NUTRITION_ENTRIES = "nutrition_entries";
    private static final String COLLECTION_USER_PROFILES = "user_profiles";
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    public FirebaseService() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }
    
    public FirebaseAuth getAuth() {
        return mAuth;
    }
    
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
    
    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    public void logUserActivity(String activityType, String activityDetails) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "Cannot log activity: No user logged in");
            return;
        }
        
        UserActivityModel activity = new UserActivityModel(userId, activityType, activityDetails);
        
        db.collection(COLLECTION_USER_ACTIVITIES)
                .add(activity)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Activity logged with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error logging activity", e);
                });
    }
    
    public interface UserActivitiesCallback {
        void onSuccess(List<UserActivityModel> activities);
        void onFailure(Exception e);
    }
    
    public void getUserActivities(UserActivitiesCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("No user logged in"));
            return;
        }
        
        db.collection(COLLECTION_USER_ACTIVITIES)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50) // Limit to last 50 activities
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<UserActivityModel> activities = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UserActivityModel activity = document.toObject(UserActivityModel.class);
                            activity.setId(document.getId());
                            activities.add(activity);
                        }
                        callback.onSuccess(activities);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
    
    public void getUserActivitiesByType(String activityType, UserActivitiesCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("No user logged in"));
            return;
        }
        
        db.collection(COLLECTION_USER_ACTIVITIES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("activityType", activityType)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<UserActivityModel> activities = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            UserActivityModel activity = document.toObject(UserActivityModel.class);
                            activity.setId(document.getId());
                            activities.add(activity);
                        }
                        callback.onSuccess(activities);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }
    
    // Nutrition Entry Methods
    public interface SaveCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    public void saveNutritionEntry(String date, String mealType, String foodName, 
                                  double kcal, double protein, double fat, double quantity, SaveCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "Cannot save nutrition entry: No user logged in");
            if (callback != null) callback.onFailure(new Exception("No user logged in"));
            return;
        }
        
        NutritionEntry entry = new NutritionEntry(userId, date, mealType, foodName, kcal, protein, fat, quantity);
        
        Log.d(TAG, "Saving nutrition entry: " + foodName + " for " + date + " (" + mealType + ") - " + kcal + " kcal");
        
        db.collection(COLLECTION_NUTRITION_ENTRIES)
                .add(entry)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✅ Nutrition entry saved successfully with ID: " + documentReference.getId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error saving nutrition entry", e);
                    if (callback != null) callback.onFailure(e);
                });
    }
    
    // Overloaded method for backward compatibility
    public void saveNutritionEntry(String date, String mealType, String foodName, 
                                  double kcal, double protein, double fat, double quantity) {
        saveNutritionEntry(date, mealType, foodName, kcal, protein, fat, quantity, null);
    }
    
    public interface NutritionEntriesCallback {
        void onSuccess(List<NutritionEntry> entries);
        void onFailure(Exception e);
    }
    
    public void getNutritionEntriesForDate(String date, NutritionEntriesCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "Cannot get nutrition entries: No user logged in");
            callback.onFailure(new Exception("No user logged in"));
            return;
        }
        
        Log.d(TAG, "🔍 QUERY: Loading entries for userId=" + userId + ", date=" + date);
        
        db.collection(COLLECTION_NUTRITION_ENTRIES)
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int totalDocs = task.getResult().size();
                        Log.d(TAG, "📊 QUERY RESULT: Found " + totalDocs + " documents");
                        
                        List<NutritionEntry> entries = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Log raw document data
                            Log.d(TAG, "📄 RAW DOC: " + document.getData());
                            
                            NutritionEntry entry = document.toObject(NutritionEntry.class);
                            entry.setId(document.getId());
                            entries.add(entry);
                            
                            Log.d(TAG, "✅ PARSED: " + entry.getFoodName() + " | " + entry.getKcal() + " kcal | " + entry.getMealType() + " | date=" + entry.getDate());
                        }
                        
                        Log.d(TAG, "🎯 FINAL: Returning " + entries.size() + " entries to callback");
                        callback.onSuccess(entries);
                    } else {
                        Log.e(TAG, "❌ QUERY FAILED for date: " + date, task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }
    
    public void deleteNutritionEntry(String entryId) {
        db.collection(COLLECTION_NUTRITION_ENTRIES)
                .document(entryId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Nutrition entry deleted successfully");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error deleting nutrition entry", e);
                });
    }
    
    // Debug method to check all nutrition entries for current user
    public void debugAllNutritionEntries() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "Cannot debug entries: No user logged in");
            return;
        }
        
        Log.d(TAG, "🔍 DEBUG: Checking all nutrition entries for user: " + userId);
        
        db.collection(COLLECTION_NUTRITION_ENTRIES)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "📊 DEBUG: Total entries in database: " + task.getResult().size());
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            NutritionEntry entry = document.toObject(NutritionEntry.class);
                            Log.d(TAG, "📝 DEBUG Entry: Date=" + entry.getDate() + 
                                ", Food=" + entry.getFoodName() + 
                                ", Meal=" + entry.getMealType() + 
                                ", Kcal=" + entry.getKcal());
                        }
                    } else {
                        Log.e(TAG, "❌ DEBUG: Error loading all entries", task.getException());
                    }
                });
    }
    
    // Account deletion callback
    public interface AccountDeletionCallback {
        void onSuccess();
        void onFailure(String error);
        void onProgress(String message);
    }
    
    // Re-authenticate and delete account
    public void reauthenticateAndDeleteAccount(String password, AccountDeletionCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null || user.getEmail() == null) {
            callback.onFailure("No user logged in");
            return;
        }
        
        // Re-authenticate user
        com.google.firebase.auth.AuthCredential credential = 
            com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), password);
        
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Re-authentication successful");
                        // Now delete account with all data
                        deleteAccountWithAllData(callback);
                    } else {
                        String error = task.getException() != null ? 
                            task.getException().getMessage() : "Re-authentication failed";
                        Log.e(TAG, "❌ Re-authentication failed", task.getException());
                        callback.onFailure(error);
                    }
                });
    }
    
    // Delete account with all user data
    public void deleteAccountWithAllData(AccountDeletionCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onFailure("No user logged in");
            return;
        }
        
        String userId = user.getUid();
        Log.d(TAG, "🗑️ Starting account deletion for user: " + userId);
        
        callback.onProgress("Deleting nutrition entries...");
        
        // Step 1: Delete all nutrition entries
        db.collection(COLLECTION_NUTRITION_ENTRIES)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task1.getResult()) {
                            document.getReference().delete();
                        }
                        Log.d(TAG, "✅ Deleted " + task1.getResult().size() + " nutrition entries");
                        
                        callback.onProgress("Deleting user activities...");
                        
                        // Step 2: Delete all user activities
                        db.collection(COLLECTION_USER_ACTIVITIES)
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        for (QueryDocumentSnapshot document : task2.getResult()) {
                                            document.getReference().delete();
                                        }
                                        Log.d(TAG, "✅ Deleted " + task2.getResult().size() + " user activities");
                                        
                                        callback.onProgress("Deleting user profile...");
                                        
                                        // Step 3: Delete user profile
                                        db.collection(COLLECTION_USER_PROFILES)
                                                .whereEqualTo("userId", userId)
                                                .get()
                                                .addOnCompleteListener(task3 -> {
                                                    if (task3.isSuccessful()) {
                                                        for (QueryDocumentSnapshot document : task3.getResult()) {
                                                            document.getReference().delete();
                                                        }
                                                        Log.d(TAG, "✅ Deleted user profile");
                                                        
                                                        callback.onProgress("Deleting Firebase account...");
                                                        
                                                        // Step 4: Delete Firebase Auth account
                                                        user.delete()
                                                                .addOnCompleteListener(task4 -> {
                                                                    if (task4.isSuccessful()) {
                                                                        Log.d(TAG, "✅ Account deleted successfully");
                                                                        callback.onSuccess();
                                                                    } else {
                                                                        String error = task4.getException() != null ? 
                                                                            task4.getException().getMessage() : "Failed to delete account";
                                                                        Log.e(TAG, "❌ Failed to delete account", task4.getException());
                                                                        callback.onFailure(error);
                                                                    }
                                                                });
                                                    } else {
                                                        callback.onFailure("Failed to delete user profile");
                                                    }
                                                });
                                    } else {
                                        callback.onFailure("Failed to delete user activities");
                                    }
                                });
                    } else {
                        callback.onFailure("Failed to delete nutrition entries");
                    }
                });
    }
    
    // Method to fix date issues in existing entries (change 2025 to 2024)
    public void fixDateIssuesInEntries(SaveCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            Log.w(TAG, "Cannot fix entries: No user logged in");
            if (callback != null) callback.onFailure(new Exception("No user logged in"));
            return;
        }
        
        Log.d(TAG, "🔧 Fixing date issues in nutrition entries for user: " + userId);
        
        db.collection(COLLECTION_NUTRITION_ENTRIES)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int fixedCount = 0;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            NutritionEntry entry = document.toObject(NutritionEntry.class);
                            String oldDate = entry.getDate();
                            
                            // Check if date starts with 2025 and fix it to 2024
                            if (oldDate != null && oldDate.startsWith("2025")) {
                                String newDate = oldDate.replace("2025", "2024");
                                
                                // Update the document
                                document.getReference().update("date", newDate)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✅ Fixed date: " + oldDate + " → " + newDate + " for " + entry.getFoodName());
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ Failed to fix date for " + entry.getFoodName(), e);
                                        });
                                fixedCount++;
                            }
                        }
                        
                        Log.d(TAG, "🔧 Fixed " + fixedCount + " entries");
                        if (callback != null) callback.onSuccess();
                    } else {
                        Log.e(TAG, "❌ Error loading entries to fix", task.getException());
                        if (callback != null) callback.onFailure(task.getException());
                    }
                });
    }
    

    
    // User Profile Methods
    public interface UserProfileCallback {
        void onSuccess(UserProfile profile);
        void onFailure(Exception e);
    }
    
    public void createUserProfile(String username, String email, UserProfileCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("No user logged in"));
            return;
        }
        
        UserProfile profile = new UserProfile(userId, username, email);
        
        db.collection(COLLECTION_USER_PROFILES)
                .document(userId)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created successfully");
                    callback.onSuccess(profile);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user profile", e);
                    callback.onFailure(e);
                });
    }
    
    public void getUserProfile(UserProfileCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("No user logged in"));
            return;
        }
        
        db.collection(COLLECTION_USER_PROFILES)
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        UserProfile profile = task.getResult().toObject(UserProfile.class);
                        if (profile != null) {
                            profile.setId(task.getResult().getId());
                            callback.onSuccess(profile);
                        } else {
                            callback.onFailure(new Exception("Failed to parse user profile"));
                        }
                    } else {
                        callback.onFailure(new Exception("User profile not found"));
                    }
                });
    }
    
    public void updateUserProfile(UserProfile profile, UserProfileCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure(new Exception("No user logged in"));
            return;
        }
        
        profile.setUpdatedAt(com.google.firebase.Timestamp.now());
        
        db.collection(COLLECTION_USER_PROFILES)
                .document(userId)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile updated successfully");
                    callback.onSuccess(profile);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile", e);
                    callback.onFailure(e);
                });
    }
    
    // Password change method
    public interface PasswordChangeCallback {
        void onSuccess();
        void onFailure(String error);
    }
    
    public void changePassword(String currentPassword, String newPassword, PasswordChangeCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            Log.w(TAG, "Cannot change password: No user logged in");
            callback.onFailure("No user logged in");
            return;
        }
        
        String email = user.getEmail();
        if (email == null) {
            Log.w(TAG, "Cannot change password: User email not found");
            callback.onFailure("User email not found");
            return;
        }
        
        Log.d(TAG, "Attempting to change password for user: " + email);
        
        // Re-authenticate user with current password
        com.google.firebase.auth.AuthCredential credential = 
            com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword);
            
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Re-authentication successful, updating password");
                        // Update password
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Log.d(TAG, "Password updated successfully");
                                        logUserActivity("PASSWORD_CHANGED", "User changed password successfully");
                                        callback.onSuccess();
                                    } else {
                                        String error = updateTask.getException() != null ? 
                                            updateTask.getException().getMessage() : "Failed to update password";
                                        Log.e(TAG, "Failed to update password: " + error);
                                        callback.onFailure(error);
                                    }
                                });
                    } else {
                        String error = task.getException() != null ? 
                            task.getException().getMessage() : "Current password is incorrect";
                        Log.e(TAG, "Re-authentication failed: " + error);
                        callback.onFailure(error);
                    }
                });
    }
}