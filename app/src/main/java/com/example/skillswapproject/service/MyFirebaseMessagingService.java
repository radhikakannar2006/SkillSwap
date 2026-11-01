package com.example.skillswapproject.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.util.AppNotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM token: " + token);

        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            FirestoreRepository.getInstance().updateFcmToken(currentUserId, token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            if (title == null || title.isEmpty()) {
                title = data.get("title");
            }
            if (body == null || body.isEmpty()) {
                body = data.get("message");
            }
            String type = data.get("type");
            String relatedId = data.get("relatedId");
            String senderId = data.get("senderId");
            String chatId = data.get("chatId");

            if (title != null && body != null) {
                AppNotificationHelper.showGenericPushNotification(
                        getApplicationContext(), title, body, type, relatedId, senderId, chatId
                );
                return;
            }
        }

        if (title != null && body != null) {
            AppNotificationHelper.showGenericPushNotification(
                    getApplicationContext(), title, body, null, null, null, null
            );
        }
    }
}
