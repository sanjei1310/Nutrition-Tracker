package com.example.nutritracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nutritracker.firebase.FirebaseService;
import com.example.nutritracker.firebase.UserProfile;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    
    private TextInputEditText etUsername, etPassword, etDisplayName;
    private MaterialButton btnLogin, btnRegister;
    private ProgressBar progressBar;
    private View layoutUsername;
    private boolean isRegisterMode = false;
    
    private FirebaseService firebaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        com.example.nutritracker.utils.ThemeManager.applyTheme(
            com.example.nutritracker.utils.ThemeManager.getTheme(this));
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize Firebase service
        firebaseService = new FirebaseService();
        
        // Check if user is already logged in
        if (firebaseService.getCurrentUser() != null) {
            navigateToHome();
            return;
        }
        
        initViews();
        setupClickListeners();
    }
    
    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etDisplayName = findViewById(R.id.et_display_name);
        layoutUsername = findViewById(R.id.layout_username);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (isRegisterMode) {
                performRegister();
            } else {
                performLogin();
            }
        });
        
        btnRegister.setOnClickListener(v -> {
            toggleMode();
        });
    }
    
    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        if (isRegisterMode) {
            // Switch to register mode
            layoutUsername.setVisibility(View.VISIBLE);
            btnLogin.setText("Create Account");
            btnRegister.setText("Already have account? Login");
        } else {
            // Switch to login mode
            layoutUsername.setVisibility(View.GONE);
            btnLogin.setText("Login");
            btnRegister.setText("Create Account");
        }
    }
    
    private void performLogin() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        
        firebaseService.getAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        // Log login activity
                        firebaseService.logUserActivity("LOGIN", "User logged in from LoginActivity");
                        
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        
                        // Small delay to ensure Firebase auth is fully initialized
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            navigateToHome();
                        }, 500); // 500ms delay
                    } else {
                        Toast.makeText(this, "Authentication failed: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                            Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private void performRegister() {
        String email = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String username = etDisplayName.getText().toString().trim();
        
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (username.length() < 2) {
            Toast.makeText(this, "Username must be at least 2 characters", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        
        firebaseService.getAuth().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Create user profile in Firestore
                        firebaseService.createUserProfile(username, email, new FirebaseService.UserProfileCallback() {
                            @Override
                            public void onSuccess(UserProfile profile) {
                                showLoading(false);
                                // Log registration activity
                                firebaseService.logUserActivity("REGISTER", "User registered with email: " + email);
                                
                                Toast.makeText(LoginActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                
                                // Small delay to ensure Firebase auth is fully initialized
                                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                    navigateToHome();
                                }, 500); // 500ms delay
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                showLoading(false);
                                Toast.makeText(LoginActivity.this, "Failed to create profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Registration failed: " + 
                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"), 
                            Toast.LENGTH_LONG).show();
                    }
                });
    }
    

    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
    }
    
    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}