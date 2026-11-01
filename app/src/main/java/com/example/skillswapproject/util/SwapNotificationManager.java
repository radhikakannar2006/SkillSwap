package com.example.skillswapproject.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.skillswapproject.data.model.FirestoreSwap;

import java.util.List;

/**
 * Manages notification tracking for swap requests (received pending, accepted, and ended swaps)
 * displayed on the Home screen notification bell icon and system alerts.
 */
public class SwapNotificationManager {

    private static final String PREF_NAME = "swap_notification_prefs";
    private static final String PREFIX_SEEN_ACCEPTED = "seen_acc_";
    private static final String PREFIX_SEEN_ENDED = "seen_end_";
    private static final String PREFIX_SEEN_PENDING = "seen_pen_";
    private static final String PREFIX_NOTIFIED = "notified_";

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Marks all currently loaded swap notifications (pending, accepted, and ended) as seen.
     * Also clears any active system status bar notifications.
     */
    public static void markNotificationsSeen(Context context, List<FirestoreSwap> receivedSwaps, List<FirestoreSwap> sentSwaps) {
        if (context == null) return;
        SharedPreferences.Editor editor = getPrefs(context).edit();

        if (receivedSwaps != null) {
            for (FirestoreSwap s : receivedSwaps) {
                if (s.getId() == null) continue;
                String status = s.getStatus() != null ? s.getStatus().toUpperCase() : "";
                if ("PENDING".equals(status)) {
                    editor.putBoolean(PREFIX_SEEN_PENDING + s.getId(), true);
                } else if ("ENDED".equals(status)) {
                    editor.putBoolean(PREFIX_SEEN_ENDED + s.getId(), true);
                }
            }
        }
        if (sentSwaps != null) {
            for (FirestoreSwap s : sentSwaps) {
                if (s.getId() == null) continue;
                String status = s.getStatus() != null ? s.getStatus().toUpperCase() : "";
                if ("ACCEPTED".equals(status)) {
                    editor.putBoolean(PREFIX_SEEN_ACCEPTED + s.getId(), true);
                } else if ("ENDED".equals(status)) {
                    editor.putBoolean(PREFIX_SEEN_ENDED + s.getId(), true);
                }
            }
        }

        editor.apply();

        // Dismiss any existing alerts in system status bar
        AppNotificationHelper.cancelAllNotifications(context);
    }

    public static boolean isAcceptedSeen(Context context, String swapId) {
        if (context == null || swapId == null) return true;
        return getPrefs(context).getBoolean(PREFIX_SEEN_ACCEPTED + swapId, false);
    }

    public static boolean isEndedSeen(Context context, String swapId) {
        if (context == null || swapId == null) return true;
        return getPrefs(context).getBoolean(PREFIX_SEEN_ENDED + swapId, false);
    }

    public static boolean isPendingSeen(Context context, String swapId) {
        if (context == null || swapId == null) return true;
        return getPrefs(context).getBoolean(PREFIX_SEEN_PENDING + swapId, false);
    }

    public static boolean wasNotified(Context context, String swapId, String status) {
        if (context == null || swapId == null) return true;
        return getPrefs(context).getBoolean(PREFIX_NOTIFIED + swapId + "_" + status, false);
    }

    public static void markNotified(Context context, String swapId, String status) {
        if (context == null || swapId == null) return;
        getPrefs(context).edit().putBoolean(PREFIX_NOTIFIED + swapId + "_" + status, true).apply();
    }

    /**
     * Calculates the badge count for the Home screen notification bell icon:
     * - Any unread PENDING swap received by the current user.
     * - Any unread sent swap that was ACCEPTED.
     * - Any unread swap that was ENDED by the partner.
     */
    public static int calculateHomeNotificationCount(Context context, String currentUserId,
                                                    List<FirestoreSwap> receivedSwaps,
                                                    List<FirestoreSwap> sentSwaps) {
        if (currentUserId == null || currentUserId.isEmpty() || context == null) return 0;
        int count = 0;
        SharedPreferences prefs = getPrefs(context);

        // 1. Received swaps
        if (receivedSwaps != null) {
            for (FirestoreSwap s : receivedSwaps) {
                if (s.getId() == null) continue;
                String status = s.getStatus() != null ? s.getStatus().toUpperCase() : "";
                if ("PENDING".equals(status)) {
                    boolean seen = prefs.getBoolean(PREFIX_SEEN_PENDING + s.getId(), false);
                    if (!seen) {
                        count++;
                    }
                } else if ("ENDED".equals(status)) {
                    // Only count if ended by the other partner and not yet viewed
                    if (s.getEndedBy() != null && !currentUserId.equals(s.getEndedBy())) {
                        boolean seen = prefs.getBoolean(PREFIX_SEEN_ENDED + s.getId(), false);
                        if (!seen) {
                            count++;
                        }
                    }
                }
            }
        }

        // 2. Sent swaps
        if (sentSwaps != null) {
            for (FirestoreSwap s : sentSwaps) {
                if (s.getId() == null) continue;
                String status = s.getStatus() != null ? s.getStatus().toUpperCase() : "";
                if ("ACCEPTED".equals(status)) {
                    boolean seen = prefs.getBoolean(PREFIX_SEEN_ACCEPTED + s.getId(), false);
                    if (!seen) {
                        count++;
                    }
                } else if ("ENDED".equals(status)) {
                    // Only count if ended by partner and not yet viewed
                    if (s.getEndedBy() != null && !currentUserId.equals(s.getEndedBy())) {
                        boolean seen = prefs.getBoolean(PREFIX_SEEN_ENDED + s.getId(), false);
                        if (!seen) {
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    /**
     * Checks if there are any accepted sent swaps that haven't been seen by the user yet.
     */
    public static boolean hasUnreadAcceptedSwaps(Context context, List<FirestoreSwap> sentSwaps) {
        if (context == null || sentSwaps == null) return false;
        SharedPreferences prefs = getPrefs(context);
        for (FirestoreSwap s : sentSwaps) {
            if (s.getId() == null) continue;
            if ("ACCEPTED".equalsIgnoreCase(s.getStatus())) {
                if (!prefs.getBoolean(PREFIX_SEEN_ACCEPTED + s.getId(), false)) {
                    return true;
                }
            }
        }
        return false;
    }
}
