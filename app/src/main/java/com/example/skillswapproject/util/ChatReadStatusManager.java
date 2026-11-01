package com.example.skillswapproject.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.skillswapproject.data.model.FirestoreChat;

/**
 * Manages read/unread status for chats both locally (for instantaneous updates)
 * and in coordination with Firestore chat metadata.
 */
public class ChatReadStatusManager {

    private static final String PREF_NAME = "chat_read_status_prefs";
    private static final String KEY_PREFIX_READ_TIME = "last_read_";

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Marks a chat as read locally with the given timestamp (or current time if 0).
     */
    public static void markChatRead(Context context, String chatId, long timestamp) {
        if (context == null || chatId == null || chatId.isEmpty()) return;
        long timeToSave = (timestamp > 0) ? timestamp : System.currentTimeMillis();
        getPrefs(context).edit()
                .putLong(KEY_PREFIX_READ_TIME + chatId, timeToSave)
                .apply();
    }

    /**
     * Gets the last read timestamp recorded locally for the given chat.
     */
    public static long getLastReadTimestamp(Context context, String chatId) {
        if (context == null || chatId == null || chatId.isEmpty()) return 0L;
        return getPrefs(context).getLong(KEY_PREFIX_READ_TIME + chatId, 0L);
    }

    /**
     * Determines whether a chat is unread for the current user.
     */
    public static boolean isChatUnread(Context context, String currentUserId, FirestoreChat chat) {
        if (chat == null || currentUserId == null || currentUserId.isEmpty()) {
            return false;
        }

        // If current user is the sender of the last message, it's not unread for current user
        if (currentUserId.equals(chat.getLastSenderId())) {
            return false;
        }

        // If explicitly flagged as unread by the current user in Firestore
        if (currentUserId.equals(chat.getUnreadBy())) {
            return true;
        }

        // If unread flag is true and last sender is not current user
        if (chat.isUnread() && chat.getLastSenderId() != null && !currentUserId.equals(chat.getLastSenderId())) {
            return true;
        }

        // Check against local read timestamp
        if (context != null && chat.getLastMessageTimestamp() != null && chat.getChatId() != null) {
            long msgTime = chat.getLastMessageTimestamp().getTime();
            long lastRead = getLastReadTimestamp(context, chat.getChatId());
            // If a message was received after our last read time and last sender was the friend
            if (msgTime > lastRead && chat.getLastSenderId() != null && !currentUserId.equals(chat.getLastSenderId())) {
                return true;
            }
        }

        return false;
    }
}
