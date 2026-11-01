# Skill Swap — Native Android Application

Skill Swap is a native Android application built in Java that connects learners and mentors to exchange knowledge and skills. Users can offer skills they excel at, discover skills they want to learn, propose swaps, schedule sessions with real-time video meetings, chat in real-time, earn Skill Coins, and build community reputation through verified ratings and reviews.

---

## 🌟 Main Features

- **Authentication & Onboarding:**
  - Email/Password sign up and sign in via Firebase Authentication.
  - One-tap Google Sign-In integration.
  - Daily persistent reminder prompting new users to complete their Offered and Wanted skills.

- **Profile & Skill Management:**
  - Customizable profile with profile picture, bio, location, and availability schedule.
  - Categorized skill selection (30+ skills across Tech, Design, Languages, Music, Fitness, etc.).
  - Public/Private profile visibility controls.

- **AI Compatibility Matcher:**
  - Algorithmic compatibility scoring (0%–100%) between users based on 2-way skill reciprocity, mentor ranking, geographical proximity, and community trust.

- **Skill Swap Requests & Workflow:**
  - Send, accept, decline, and end skill swap requests.
  - Real-time in-app notification bell badge for received swap requests and status updates.

- **1-on-1 Real-time Chat:**
  - Direct messaging backed by Cloud Firestore.
  - Displays conversations only for active chat partners.
  - Smart conversation starter suggestions for new swaps.
  - Message read status indicators.

- **Session Scheduling & Video Meetings:**
  - Schedule learning and teaching sessions with date and time pickers.
  - Integrated video conferencing support via **Jitsi Meet** (instant room links) and **Google Meet**.
  - Two-party session completion confirmation flow with dispute handling.

- **Skill Coins Wallet & Gamification:**
  - **5 Skill Coins** Welcome Starter Bonus on account creation (idempotent, single claim).
  - **+1 Daily Active Reward** coin for daily engagement.
  - Coin earning for teaching sessions and 5-star reviews; coin deductions for booking sessions.
  - Dynamic Mentor Ranks: **Beginner** (0–5), **Contributor** (6–20), **Mentor** (21–50), and **Master** (51+).

- **Reviews & Ratings:**
  - Post-session 5-star ratings with constructive written feedback and bonus coin incentives.

---

## 🛠️ Technologies & Architecture

- **Language:** Java (JDK 17)
- **Minimum SDK:** Android 7.0 (API 24)
- **Target SDK:** Android 15 / 16 Preview (API 36)
- **Architecture:** MVVM (Model-View-ViewModel) with Android Architecture Components & ViewBinding
- **Database & Backend:**
  - Firebase Authentication (Email/Password & Google Sign-In)
  - Cloud Firestore (Real-time NoSQL database)
  - Firebase Cloud Messaging (FCM push notifications)
- **Video & Calendar:**
  - Jitsi Meet Intent integration
  - Google Calendar API v3
- **Design System:** Material Design 3 (Material Components for Android)

---

## 📋 Prerequisites & Requirements

Before opening the project, ensure you have the following installed:
1. **Android Studio** (Ladybug | 2024.2.1 or newer recommended).
2. **Java Development Kit (JDK):** Version 17 (bundled automatically in Android Studio as JBR).
3. **Android SDK:** Platform 34 or higher installed via Android Studio SDK Manager.
4. A physical Android device with USB Debugging enabled, or an Android Virtual Device (AVD) running API 26+.
5. A **Firebase Account** (free Spark plan is sufficient).

---

## ⚙️ Setup & Firebase Configuration

Because this is a public repository, the Firebase configuration file (`app/google-services.json`) is **excluded** for security. Anyone running this application must connect it to their own Firebase project.

### Step 1: Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add Project** and give it a name (e.g., `MySkillSwapApp`).
3. (Optional) Disable Google Analytics for faster setup, then click **Create Project**.

### Step 2: Register the Android Application
1. On the project overview page, click the **Android** icon to add an Android app.
2. In **Android package name**, enter:
   ```text
   com.example.skillswapproject
   ```
3. (Optional) Enter an App nickname (e.g., `Skill Swap Android`).
4. (Recommended for Google Sign-In): Generate your computer's debug SHA-1 signing certificate fingerprint:
   - On Windows: Run `keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android`
   - Paste the SHA-1 fingerprint into the **Debug signing certificate SHA-1** field.
5. Click **Register app**.

### Step 3: Download and Place `google-services.json`
1. Download the generated `google-services.json` file from Firebase.
2. Place the file inside the `app/` folder of this project:
   ```text
   SkillSwapProject/
   ├── app/
   │   ├── google-services.json  <-- Place your downloaded file here
   │   ├── google-services.json.example
   │   └── build.gradle
   ```
   > ℹ️ A template file [`app/google-services.json.example`](app/google-services.json.example) is provided to demonstrate the required JSON structure.

### Step 4: Configure Firebase Authentication
1. In the Firebase Console sidebar, navigate to **Build > Authentication**.
2. Click **Get Started**.
3. Under the **Sign-in method** tab, enable:
   - **Email/Password**
   - **Google** (Select your project support email and save)

### Step 5: Configure Cloud Firestore & Security Rules
1. In the Firebase Console sidebar, navigate to **Build > Firestore Database**.
2. Click **Create Database**, select a server location near you, and choose **Start in production mode** (or test mode for local evaluation).
3. Under the **Rules** tab, ensure authorized users can only read and write appropriate user and swap documents:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       // Users collection
       match /users/{userId} {
         allow read: if request.auth != null;
         allow write: if request.auth != null && request.auth.uid == userId;
         
         match /coinTransactions/{txId} {
           allow read: if request.auth != null;
           allow write: if request.auth != null && request.auth.uid == userId;
         }
       }
       
       // Swaps collection
       match /swaps/{swapId} {
         allow read, write: if request.auth != null;
       }
       
       // Chats & Messages
       match /chats/{chatId} {
         allow read, write: if request.auth != null;
         
         match /messages/{messageId} {
           allow read, write: if request.auth != null;
         }
       }
       
       // Sessions collection
       match /sessions/{sessionId} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

---

## 🚀 Building & Running the App

1. **Clone or Download the Repository:**
   ```bash
   git clone https://github.com/radhikakannar2006/SkillSwap.git
   ```
2. **Open in Android Studio:**
   - Launch Android Studio.
   - Click **Open** and select the cloned `SkillSwapProject` directory.
   - Allow Gradle to sync dependencies automatically.
3. **Run Unit Tests:**
   - In Android Studio, right-click `app/src/test/java` and select **Run 'Tests in 'com.example.skillswapproject''**.
   - Or run from terminal:
     ```bash
     ./gradlew testDebugUnitTest
     ```
4. **Deploy to Device:**
   - Connect your Android phone or boot an emulator.
   - Click the green **Run (▶)** button in the top toolbar or press `Shift + F10`.

---

## 🔒 Security Advisory & Best Practices

> [!WARNING]
> **Never commit personal credentials, keystores, or API secrets.**
> - `.gitignore` is pre-configured to ignore `app/google-services.json`, `local.properties`, release keystores (`*.jks`, `*.keystore`), and `.env` files.
> - **Important Security Note:** Merely excluding `google-services.json` from Git does not automatically secure a backend. Firebase security relies fundamentally on **Cloud Firestore Security Rules** and Firebase Authentication server-side checks. Always configure robust security rules before deploying to production.

---

## 📄 License

This project is licensed for educational and portfolio demonstration purposes. All rights reserved.
