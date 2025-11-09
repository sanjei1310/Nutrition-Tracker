# Quick Start Guide

Get NutriTracker running in 15 minutes! ⚡

## 📋 Prerequisites Checklist

- [ ] Android Studio installed
- [ ] JDK 11+ installed
- [ ] Google account (for Firebase & Gemini)
- [ ] Email address (for USDA API)

---

## 🚀 Setup Steps

### Step 1: Clone & Open (2 min)
```bash
git clone <repository-url>
cd nutritracker
# Open in Android Studio
```

### Step 2: Firebase Setup (5 min)
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create project → Add Android app
3. Package name: `com.example.nutritracker`
4. Download `google-services.json` → Place in `app/` folder
5. Enable Email/Password auth
6. Create Firestore database

### Step 3: USDA API Key (2 min)
1. Visit [USDA API Signup](https://fdc.nal.usda.gov/api-key-signup.html)
2. Get API key from email
3. Open `AddFoodActivity.java`
4. Replace: `PASTE_YOUR_USDA_API_KEY_HERE`

### Step 4: Gemini API Key (2 min)
1. Visit [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Create API key
3. Open `AiAssistantActivity.java`
4. Replace: `PASTE_YOUR_GEMINI_API_KEY_HERE`

### Step 5: Build & Run (4 min)
1. Sync Gradle
2. Connect device/emulator
3. Click Run ▶️
4. Create account & start tracking!

---

## 🎯 Quick Links

- **Detailed Setup:** [API_SETUP_GUIDE.md](API_SETUP_GUIDE.md)
- **Full README:** [README.md](README.md)
- **Security Info:** [SECURITY_CHANGES.md](SECURITY_CHANGES.md)

---

## ⚠️ Common Issues

**Build fails?**
- Ensure `google-services.json` is in `app/` folder
- Sync Gradle again

**Login doesn't work?**
- Enable Email/Password in Firebase Console
- Check internet connection

**Food search fails?**
- Verify USDA API key is correct
- Check for extra spaces

**AI assistant error?**
- Verify Gemini API key is correct
- Ensure API key is activated

---

## 📱 Test the App

1. **Register:** Create account with email/password
2. **Add Food:** Search "chicken" → Select → Enter quantity
3. **View Progress:** Check home screen for nutrition totals
4. **AI Assistant:** Ask "Suggest a healthy breakfast"
5. **Profile:** Update goals and preferences

---

## 🎉 You're Ready!

All set? Start tracking your nutrition! 🥗

Need help? Check [API_SETUP_GUIDE.md](API_SETUP_GUIDE.md) for detailed instructions.
