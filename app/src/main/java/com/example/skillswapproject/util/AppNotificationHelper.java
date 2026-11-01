package com.example.skillswapproject.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.skillswapproject.MainActivity;
import com.example.skillswapproject.R;
import com.example.skillswapproject.ui.activity.ChatDetailActivity;

public class AppNotificationHelper {

    private static final String CHANNEL_ID = "skill_swap_channel";
    private static final String CHANNEL_NAME = "Skill Swap Alerts";
    private static final String CHANNEL_DESC = "Notifications for accepted and ended skill swaps";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 200, 250});
            android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            android.net.Uri defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            channel.setSound(defaultSoundUri, audioAttributes);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showSwapAcceptedNotification(Context context, String partnerName, String chatId, String partnerId) {
        if (context == null) return;
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String displayName = (partnerName != null && !partnerName.isEmpty()) ? partnerName : "A member";

        Intent intent = new Intent(context, com.example.skillswapproject.ui.activity.PendingSwapsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                201,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_swap_success)
                .setContentTitle("Skill Swap Accepted! 🎉")
                .setContentText(displayName + " accepted your skill swap! Check your notifications.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(201, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void showSwapEndedNotification(Context context, String partnerName) {
        if (context == null) return;
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String displayName = (partnerName != null && !partnerName.isEmpty()) ? partnerName : "Your partner";

        Intent intent = new Intent(context, com.example.skillswapproject.ui.activity.PendingSwapsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                301,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_swap_success)
                .setContentTitle("Skill Swap Ended")
                .setContentText(displayName + " has ended the skill swap.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(202, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void showSwapRequestNotification(Context context, String requesterName, String offeredSkill, String requestedSkill) {
        if (context == null) return;
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String displayName = (requesterName != null && !requesterName.isEmpty()) ? requesterName : "A member";
        Intent intent = new Intent(context, com.example.skillswapproject.ui.activity.PendingSwapsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String message = displayName + " wants to swap " +
                (offeredSkill != null ? offeredSkill : "a skill") + " for " +
                (requestedSkill != null ? requestedSkill : "your skill");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_swap_success)
                .setContentTitle("New Skill Swap Request 🔄")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(101, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void showMessageReceivedNotification(Context context, String senderName, String messageText, String chatId, String senderId) {
        if (context == null) return;
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        String displayName = (senderName != null && !senderName.isEmpty()) ? senderName : "A member";
        String content = (messageText != null && !messageText.isEmpty()) ? messageText : "Sent you a message";

        Intent intent = new Intent(context, ChatDetailActivity.class);
        intent.putExtra("EXTRA_CHAT_ID", chatId);
        intent.putExtra("EXTRA_FRIEND_USER_ID", senderId);
        intent.putExtra("EXTRA_FRIEND_NAME", displayName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int reqCode = (chatId != null ? Math.abs(chatId.hashCode() % 100000) + 400 : 401);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle("Message from " + displayName)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(reqCode, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void showGenericPushNotification(Context context, String title, String message,
                                                   String type, String relatedId, String senderId, String chatId) {
        if (context == null) return;
        createNotificationChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent;
        if ("NEW_MESSAGE".equalsIgnoreCase(type) && chatId != null) {
            intent = new Intent(context, ChatDetailActivity.class);
            intent.putExtra("EXTRA_CHAT_ID", chatId);
            if (senderId != null) intent.putExtra("EXTRA_FRIEND_USER_ID", senderId);
        } else if (("SESSION_SCHEDULED".equalsIgnoreCase(type) || "SESSION_REMINDER".equalsIgnoreCase(type)) && relatedId != null) {
            intent = new Intent(context, com.example.skillswapproject.ui.activity.SessionDetailActivity.class);
            intent.putExtra(com.example.skillswapproject.ui.activity.SessionDetailActivity.EXTRA_SESSION_ID, relatedId);
        } else if ("NEW_FEEDBACK".equalsIgnoreCase(type) && senderId != null) {
            intent = new Intent(context, com.example.skillswapproject.ui.activity.UserProfileActivity.class);
            intent.putExtra(com.example.skillswapproject.ui.activity.UserProfileActivity.EXTRA_TARGET_USER_ID, senderId);
        } else if ("SWAP_ACCEPTED".equalsIgnoreCase(type)) {
            intent = new Intent(context, com.example.skillswapproject.ui.activity.ActiveSwapsActivity.class);
        } else {
            intent = new Intent(context, com.example.skillswapproject.ui.activity.PendingSwapsActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int reqCode = Math.abs((title + message).hashCode() % 100000) + 500;

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        int icon = R.drawable.ic_swap_success;
        if ("NEW_MESSAGE".equalsIgnoreCase(type)) icon = R.drawable.ic_chat;
        else if ("NEW_FEEDBACK".equalsIgnoreCase(type)) icon = R.drawable.ic_star;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title != null ? title : "SkillSwap Alert")
                .setContentText(message != null ? message : "You have a new update.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(reqCode, builder.build());
        } catch (SecurityException ignored) {
        }
    }

    public static void showSessionStartingSoonNotification(Context context, String sessionId, String skillName, String partnerName) {
        String skill = (skillName != null && !skillName.isEmpty()) ? skillName : "Skill Swap";
        String partner = (partnerName != null && !partnerName.isEmpty()) ? partnerName : "your partner";
        showGenericPushNotification(
                context,
                "Session Reminder ⏰",
                "⏰ Your " + skill + " session with " + partner + " starts in 30 minutes.",
                "SESSION_REMINDER",
                sessionId,
                null,
                null
        );
    }

    public static void showSessionStartingNowNotification(Context context, String sessionId, String skillName, String partnerName) {
        String skill = (skillName != null && !skillName.isEmpty()) ? skillName : "Skill Swap";
        showGenericPushNotification(
                context,
                "Session Starting Now 🟢",
                "🟢 Your " + skill + " session is starting now.",
                "SESSION_REMINDER",
                sessionId,
                null,
                null
        );
    }

    public static void showSessionCompletionPromptNotification(Context context, String sessionId, String skillName, String partnerName) {
        showGenericPushNotification(
                context,
                "Confirm Session Completion",
                "Did you complete your session? Please confirm.",
                "SESSION_REMINDER",
                sessionId,
                null,
                null
        );
    }

    public static void cancelAllNotifications(Context context) {
        if (context == null) return;
        try {
            NotificationManagerCompat.from(context).cancelAll();
        } catch (Exception ignored) {
        }
    }
}
