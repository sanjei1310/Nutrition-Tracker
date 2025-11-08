package com.example.nutritracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiAssistantActivity extends AppCompatActivity {

    private static final String TAG = "AiAssistantActivity";
    // --- IMPORTANT: PASTE YOUR GEMINI API KEY HERE ---
    private static final String GEMINI_API_KEY = "AIzaSyCWhWzWDNwPkkSwEcM9NuWSI32Vl22Kzdo";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent?key=" + GEMINI_API_KEY;

    private RecyclerView rvChatMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar pbLoading;

    private List<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private OkHttpClient client = new OkHttpClient();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        // Find Views
        rvChatMessages = findViewById(R.id.rv_chat_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        pbLoading = findViewById(R.id.pb_loading);

        // Setup RecyclerView
        chatAdapter = new ChatAdapter(chatMessages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(chatAdapter);

        // Setup Bottom Navigation
        setupBottomNavigation();

        // Add initial greeting
        chatMessages.add(new ChatMessage("Hi! How can I help you today? You can ask me things like:\n\n• \"Suggest a healthy breakfast\"\n• \"Analyze my lunch: 1 chicken salad and an apple\"", false));
        chatAdapter.notifyItemInserted(0);

        // Send button click listener
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Add user message to chat
        addMessageToChat(messageText, true);
        etMessage.setText("");

        // Show loading and call API
        pbLoading.setVisibility(View.VISIBLE);
        callGeminiApi(messageText);
    }

    private void addMessageToChat(String message, boolean isUser) {
        chatMessages.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        rvChatMessages.scrollToPosition(chatMessages.size() - 1);
    }

    private void callGeminiApi(String userPrompt) {
        // Build JSON request body
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String jsonBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + userPrompt + "\"}]}]}";
        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(GEMINI_API_URL)
                .post(body)
                .build();

        if (GEMINI_API_KEY.equals("PASTE_YOUR_GEMINI_API_KEY_HERE")) {
            addMessageToChat("Error: Please add your Gemini API key to AiAssistantActivity.java", false);
            pbLoading.setVisibility(View.GONE);
            return;
        }

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API Call Failed", e);
                mainHandler.post(() -> {
                    addMessageToChat("Error: Failed to connect to AI. Please check your internet connection.", false);
                    pbLoading.setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API Call Unsuccessful: " + response.code() + " Body: " + errorBody);
                    mainHandler.post(() -> {
                        addMessageToChat("Error: Received an invalid response from AI (Code: " + response.code() + "). Check API key or quotas.", false);
                        pbLoading.setVisibility(View.GONE);
                    });
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "API Response: " + responseBody); // Log the full response for debugging

                    JSONObject json = new JSONObject(responseBody);
                    // Navigate: candidates -> [0] -> content -> parts -> [0] -> text
                    String aiResponse = json.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    mainHandler.post(() -> {
                        addMessageToChat(aiResponse.trim(), false);
                        pbLoading.setVisibility(View.GONE);
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "JSON Parsing Failed", e);
                    mainHandler.post(() -> {
                        addMessageToChat("Error: Failed to parse AI response.", false);
                        pbLoading.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_ai_assistant);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
                return true;
            } else if (itemId == R.id.nav_ai_assistant) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });
    }
}

