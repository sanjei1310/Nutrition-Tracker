# API Setup Guide for NutriTracker

This guide will help you set up all required API keys for the NutriTracker app.

## 🔑 Required API Keys

### 1. Firebase Setup (Authentication & Database)

**Time Required:** 5-10 minutes

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Click "Add project" or select existing project
   - Follow the setup wizard

2. **Add Android App**
   - Click "Add app" → Select Android icon
   - Enter package name: `com.example.nutritracker`
   - Download `google-services.json`
   - Replace the file at `app/google-services.json` with your downloaded file

3. **Enable Authentication**
   - In Firebase Console, go to "Authentication"
   - Click "Get started"
   - Go to "Sign-in method" tab
   - Enable "Email/Password" provider
   - Click "Save"

4. **Create Firestore Database**
   - In Firebase Console, go to "Firestore Database"
   - Click "Create database"
   - Select "Start in production mode"
   - Choose a location closest to your users
   - Click "Enable"

5. **Set Firestore Security Rules**
   - Go to "Firestore Database" → "Rules" tab
   - Replace with the rules from README.md
   - Click "Publish"

---

### 2. USDA FoodData Central API (Food Search)

**Time Required:** 2-3 minutes

1. **Get API Key**
   - Visit [USDA API Key Signup](https://fdc.nal.usda.gov/api-key-signup.html)
   - Fill in your name and email
   - Check your email for the API key

2. **Add to Project**
   - Open `app/src/main/java/com/example/nutritracker/AddFoodActivity.java`
   - Find line: `private static final String API_KEY = "PASTE_YOUR_USDA_API_KEY_HERE";`
   - Replace with: `private static final String API_KEY = "your_actual_api_key";`

---

### 3. Google Gemini AI API (AI Assistant)

**Time Required:** 2-3 minutes

1. **Get API Key**
   - Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
   - Sign in with your Google account
   - Click "Create API Key"
   - Copy the generated key

2. **Add to Project**
   - Open `app/src/main/java/com/example/nutritracker/AiAssistantActivity.java`
   - Find line: `private static final String GEMINI_API_KEY = "PASTE_YOUR_GEMINI_API_KEY_HERE";`
   - Replace with: `private static final String GEMINI_API_KEY = "your_actual_api_key";`

---

## ✅ Verification Checklist

Before running the app, verify:

- [ ] `app/google-services.json` contains your actual Firebase credentials
- [ ] Firebase Authentication (Email/Password) is enabled
- [ ] Firestore Database is created with security rules
- [ ] USDA API key is added to `AddFoodActivity.java`
- [ ] Gemini API key is added to `AiAssistantActivity.java`
- [ ] Gradle sync completed successfully

---

## 🔒 Security Best Practices

1. **Never commit actual API keys to public repositories**
2. **Keep your `google-services.json` file private**
3. **Use environment variables for production apps**
4. **Rotate API keys if accidentally exposed**
5. **Set up API key restrictions in Google Cloud Console**

---

## 🆘 Troubleshooting

### Firebase Issues
- **Error: "google-services.json not found"**
  - Ensure the file is in `app/` directory, not project root
  - Sync Gradle after adding the file

- **Error: "Authentication failed"**
  - Verify Email/Password is enabled in Firebase Console
  - Check package name matches: `com.example.nutritracker`

### USDA API Issues
- **Error: "API Call Failed"**
  - Verify API key is correct
  - Check internet connection
  - Ensure API key is activated (check email)

### Gemini API Issues
- **Error: "Please add your Gemini API key"**
  - Verify you replaced the placeholder text
  - Check for extra spaces or quotes
  - Ensure API key is valid

---

## 📝 Notes

- All APIs used in this project have **free tiers**
- No credit card required for any service
- Firebase free tier includes:
  - 50,000 reads/day
  - 20,000 writes/day
  - 1 GB storage
- USDA API: 1,000 requests/hour (free)
- Gemini API: 15 requests/minute (free tier)

---

## 🔗 Useful Links

- [Firebase Console](https://console.firebase.google.com/)
- [USDA API Documentation](https://fdc.nal.usda.gov/api-guide.html)
- [Gemini API Documentation](https://ai.google.dev/docs)
- [Project README](README.md)
