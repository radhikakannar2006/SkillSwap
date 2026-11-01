package com.example.skillswapproject;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class SkillSwapApplication extends Application {
    private static final String TAG = "SkillSwapApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "FirebaseApp successfully initialized.");
            }
        } catch (Exception e) {
            Log.w(TAG, "FirebaseApp initialization deferred/warning: " + e.getMessage());
        }

        // Initialize notification channels with sound and vibration early on app launch
        try {
            com.example.skillswapproject.util.AppNotificationHelper.createNotificationChannel(this);
        } catch (Exception e) {
            Log.w(TAG, "Notification channel init error: " + e.getMessage());
        }
    }
}
