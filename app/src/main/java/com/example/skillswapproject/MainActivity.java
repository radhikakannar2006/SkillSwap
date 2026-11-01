package com.example.skillswapproject;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.skillswapproject.data.model.FirestoreChat;
import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.util.ChatReadStatusManager;
import com.example.skillswapproject.ui.activity.OnboardingActivity;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ListenerRegistration chatsRegistration;
    private List<FirestoreChat> cachedChats = new ArrayList<>();

    private final androidx.activity.result.ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                // Notifications permitted
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Require active Firebase Authentication session to access Home
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            android.content.Intent intent = new android.content.Intent(this, OnboardingActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        requestNotificationPermission();
        setupFcmToken();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            bottomNavigationView.setOnItemReselectedListener(item -> {
                if (item.getItemId() == R.id.homeFragment) {
                    navController.popBackStack(R.id.homeFragment, false);
                }
            });

            setupChatBadgeListener();
            setupSwapNotificationsListener();
            handleIntent(getIntent());
            checkDailyRewardAndReminders();
        }
    }

    private boolean isDailyRewardDialogShowing = false;
    private String lastDailyRewardCheckedDate = "";
    private static final String PREF_SKILL_REMINDER = "skill_completion_reminder_prefs";
    private static final String KEY_LAST_REMINDER_DATE_PREFIX = "last_reminder_date_";

    private void checkDailyRewardAndReminders() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) return;

        String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

        FirestoreRepository.getInstance().getUserProfile(currentUserId).addOnSuccessListener(user -> {
            if (user == null || isFinishing() || isDestroyed()) return;

            String lastRewardDate = user.getLastActiveRewardDate();
            boolean rewardAvailable = !todayStr.equals(lastRewardDate);

            if (rewardAvailable && !isDailyRewardDialogShowing && !todayStr.equals(lastDailyRewardCheckedDate)) {
                lastDailyRewardCheckedDate = todayStr;
                int nextDay = user.getActiveDaysCount() + 1;
                showDailyRewardClaimDialog(user, nextDay);
            } else {
                checkSkillCompletionReminder(user);
            }
        }).addOnFailureListener(e -> {
            // Gracefully handle profile fetch error
        });
    }

    private void showDailyRewardClaimDialog(FirestoreUser user, int nextDay) {
        if (isFinishing() || isDestroyed()) return;
        isDailyRewardDialogShowing = true;

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_daily_reward);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.TextView tvDay = dialog.findViewById(R.id.tv_reward_day);
        android.widget.TextView tvDesc = dialog.findViewById(R.id.tv_reward_desc);
        com.google.android.material.button.MaterialButton btnClaim = dialog.findViewById(R.id.btn_reward_continue);

        if (tvDay != null) {
            tvDay.setText("Day " + nextDay);
        }
        if (tvDesc != null) {
            tvDesc.setText("Claim your daily +1 Skill Coin\nfor using Skill Swap today!");
        }
        if (btnClaim != null) {
            btnClaim.setText("Claim Reward");
            btnClaim.setEnabled(true);
            btnClaim.setOnClickListener(v -> {
                btnClaim.setEnabled(false);
                btnClaim.setText("Claiming...");

                String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
                FirestoreRepository.getInstance().claimDailyActiveReward(currentUserId, new FirestoreRepository.OnDailyRewardListener() {
                    @Override
                    public void onRewardGranted(int dayCount) {
                        if (isFinishing() || isDestroyed()) return;
                        if (tvDay != null) tvDay.setText("Day " + dayCount);
                        if (tvDesc != null) tvDesc.setText("Congratulations! You received +1 Skill Coin\nfor being active today!");
                        btnClaim.setText("Claimed ✓");
                        btnClaim.setEnabled(false);

                        Toast.makeText(MainActivity.this, "🎉 Daily reward claimed! +1 Skill Coin added to your wallet.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onAlreadyClaimedOrError() {
                        if (isFinishing() || isDestroyed()) return;
                        btnClaim.setText("Already Claimed");
                        btnClaim.setEnabled(false);
                        Toast.makeText(MainActivity.this, "Daily reward already claimed today.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        dialog.setOnDismissListener(d -> {
            isDailyRewardDialogShowing = false;
            checkSkillCompletionReminder(user);
        });

        dialog.show();
    }

    private void checkSkillCompletionReminder(FirestoreUser user) {
        if (user == null || isFinishing() || isDestroyed()) return;

        com.google.firebase.auth.FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser == null) return;

        List<String> offered = user.getOfferedSkills();
        List<String> wanted = user.getWantedSkills();
        boolean hasOffered = offered != null && !offered.isEmpty();
        boolean hasWanted = wanted != null && !wanted.isEmpty();

        // If user completes both sections, stop all future reminders immediately
        if (hasOffered && hasWanted) {
            return;
        }

        long createdAt = 0L;
        if (authUser.getMetadata() != null) {
            createdAt = authUser.getMetadata().getCreationTimestamp();
        }

        int dayNumber = 1;
        if (createdAt > 0L) {
            long now = System.currentTimeMillis();
            long diffMs = Math.max(0L, now - createdAt);
            long dayIndex = diffMs / (24L * 60 * 60 * 1000L);
            dayNumber = (int) (dayIndex + 1);
        }

        // Show no more than once per day, tracked persistently per user
        String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        android.content.SharedPreferences prefs = getSharedPreferences(PREF_SKILL_REMINDER, MODE_PRIVATE);
        String userKey = KEY_LAST_REMINDER_DATE_PREFIX + authUser.getUid();
        String lastShownDate = prefs.getString(userKey, "");

        if (todayStr.equals(lastShownDate)) {
            return;
        }

        prefs.edit().putString(userKey, todayStr).apply();
        showSkillReminderDialog(dayNumber, hasOffered, hasWanted);
    }

    private void showSkillReminderDialog(int dayNumber, boolean hasOffered, boolean hasWanted) {
        if (isFinishing() || isDestroyed()) return;

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_skill_reminder);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.TextView tvDayBadge = dialog.findViewById(R.id.tv_reminder_day_badge);
        android.widget.TextView tvTitle = dialog.findViewById(R.id.tv_reminder_title);
        android.widget.TextView tvMessage = dialog.findViewById(R.id.tv_reminder_message);
        android.view.View btnComplete = dialog.findViewById(R.id.btn_complete_my_skills);
        android.view.View btnDismiss = dialog.findViewById(R.id.btn_reminder_dismiss);

        if (tvDayBadge != null) {
            tvDayBadge.setText("Day " + dayNumber + " Reminder");
        }

        if (!hasOffered && !hasWanted) {
            if (tvTitle != null) tvTitle.setText("Complete Your Skills Profile");
            if (tvMessage != null) {
                tvMessage.setText("You haven't added Skills Offered (what you can teach) or Skills Wanted (what you want to learn). Add both to start finding swap partners!");
            }
        } else if (!hasOffered) {
            if (tvTitle != null) tvTitle.setText("Add Skills You Can Teach");
            if (tvMessage != null) {
                tvMessage.setText("You haven't added your Skills Offered (skills you can teach) yet. Add them to let others know what you can share!");
            }
        } else {
            if (tvTitle != null) tvTitle.setText("Add Skills You Want to Learn");
            if (tvMessage != null) {
                tvMessage.setText("You haven't added your Skills Wanted (skills you want to learn) yet. Add them to discover mentors who can teach you!");
            }
        }

        if (btnComplete != null) {
            btnComplete.setOnClickListener(v -> {
                dialog.dismiss();
                android.content.Intent intent = new android.content.Intent(MainActivity.this, com.example.skillswapproject.ui.activity.EditProfileActivity.class);
                startActivity(intent);
            });
        }

        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private static final String PREF_NOTIFICATION_PERM = "notification_perm_prefs";
    private static final String KEY_PROMPTED_PREFIX = "has_prompted_perm_";

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupFcmToken() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) return;

        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null && !token.isEmpty()) {
                        FirestoreRepository.getInstance().updateFcmToken(currentUserId, token);
                    }
                });
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(android.content.Intent intent) {
        if (intent != null && intent.hasExtra("EXTRA_NAVIGATE_TO") && bottomNavigationView != null) {
            int targetId = intent.getIntExtra("EXTRA_NAVIGATE_TO", R.id.homeFragment);
            bottomNavigationView.setSelectedItemId(targetId);
        }
    }

    private final java.util.Map<String, Long> knownChatTimestamps = new java.util.HashMap<>();
    private boolean isFirstChatsLoad = true;

    private void setupChatBadgeListener() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) return;

        chatsRegistration = FirestoreRepository.getInstance().listenToUserChats(currentUserId, new FirestoreRepository.OnChatsLoadedListener() {
            @Override
            public void onChatsLoaded(List<FirestoreChat> chats) {
                cachedChats = (chats != null) ? chats : new ArrayList<>();
                updateChatBadge();

                if (isFirstChatsLoad) {
                    if (chats != null) {
                        for (FirestoreChat c : chats) {
                            if (c.getChatId() != null) {
                                long time = c.getLastMessageTimestamp() != null ? c.getLastMessageTimestamp().getTime() : 0L;
                                knownChatTimestamps.put(c.getChatId(), time);
                            }
                        }
                    }
                    isFirstChatsLoad = false;
                    return;
                }

                if (chats != null) {
                    for (FirestoreChat c : chats) {
                        if (c.getChatId() == null) continue;
                        long newTime = c.getLastMessageTimestamp() != null ? c.getLastMessageTimestamp().getTime() : 0L;
                        Long prevTime = knownChatTimestamps.get(c.getChatId());
                        if (prevTime == null) prevTime = 0L;

                        // Check if a new message has arrived from another user
                        if (newTime > prevTime && c.getLastSenderId() != null && !currentUserId.equals(c.getLastSenderId())) {
                            // Don't show system notification if user is currently inside this chat
                            if (com.example.skillswapproject.ui.activity.ChatDetailActivity.sActiveChatId == null ||
                                    !c.getChatId().equals(com.example.skillswapproject.ui.activity.ChatDetailActivity.sActiveChatId)) {

                                String senderId = c.getLastSenderId();
                                String messageText = c.getLastMessageText();

                                FirestoreRepository.getInstance().getUser(senderId).addOnSuccessListener(senderUser -> {
                                    String senderName = (senderUser != null && senderUser.getName() != null) ? senderUser.getName() : "Swap Partner";
                                    com.example.skillswapproject.util.AppNotificationHelper.showMessageReceivedNotification(
                                            MainActivity.this, senderName, messageText, c.getChatId(), senderId
                                    );
                                }).addOnFailureListener(err -> {
                                    com.example.skillswapproject.util.AppNotificationHelper.showMessageReceivedNotification(
                                            MainActivity.this, "Swap Partner", messageText, c.getChatId(), senderId
                                    );
                                });
                            }
                        }
                        knownChatTimestamps.put(c.getChatId(), newTime);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                // Keep current state on error
            }
        });
    }

    private List<com.example.skillswapproject.data.model.FirestoreSwap> cachedReceivedSwaps = new ArrayList<>();
    private List<com.example.skillswapproject.data.model.FirestoreSwap> cachedSentSwaps = new ArrayList<>();
    private com.google.firebase.firestore.ListenerRegistration swapsRegistration;
    private final java.util.Map<String, String> knownSwapStatuses = new java.util.HashMap<>();
    private boolean isFirstSwapsLoad = true;

    private void setupSwapNotificationsListener() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) return;

        swapsRegistration = FirestoreRepository.getInstance().listenToUserSwaps(currentUserId, new FirestoreRepository.OnUserSwapsListener() {
            @Override
            public void onSwapsLoaded(List<com.example.skillswapproject.data.model.FirestoreSwap> receivedSwaps,
                                      List<com.example.skillswapproject.data.model.FirestoreSwap> sentSwaps) {
                cachedReceivedSwaps = (receivedSwaps != null) ? receivedSwaps : new ArrayList<>();
                cachedSentSwaps = (sentSwaps != null) ? sentSwaps : new ArrayList<>();
                updateHomeBadge();

                if (isFirstSwapsLoad) {
                    if (sentSwaps != null) {
                        for (com.example.skillswapproject.data.model.FirestoreSwap s : sentSwaps) {
                            if (s.getId() != null && s.getStatus() != null) {
                                knownSwapStatuses.put(s.getId(), s.getStatus());
                            }
                        }
                    }
                    if (receivedSwaps != null) {
                        for (com.example.skillswapproject.data.model.FirestoreSwap s : receivedSwaps) {
                            if (s.getId() != null && s.getStatus() != null) {
                                knownSwapStatuses.put(s.getId(), s.getStatus());
                            }
                        }
                    }
                    isFirstSwapsLoad = false;
                    return;
                }

                // Check for newly accepted or ended sent swaps in real-time
                if (sentSwaps != null) {
                    for (com.example.skillswapproject.data.model.FirestoreSwap s : sentSwaps) {
                        if (s.getId() == null || s.getStatus() == null) continue;
                        String prevStatus = knownSwapStatuses.get(s.getId());
                        if ("ACCEPTED".equalsIgnoreCase(s.getStatus()) && !"ACCEPTED".equalsIgnoreCase(prevStatus)) {
                            if (!com.example.skillswapproject.util.SwapNotificationManager.isAcceptedSeen(MainActivity.this, s.getId())
                                    && !com.example.skillswapproject.util.SwapNotificationManager.wasNotified(MainActivity.this, s.getId(), "ACCEPTED")) {
                                com.example.skillswapproject.util.SwapNotificationManager.markNotified(MainActivity.this, s.getId(), "ACCEPTED");
                                String chatId = FirestoreRepository.getChatId(currentUserId, s.getReceiverId());
                                com.example.skillswapproject.util.AppNotificationHelper.showSwapAcceptedNotification(
                                        MainActivity.this, s.getReceiverName(), chatId, s.getReceiverId()
                                );
                            }
                        } else if ("ENDED".equalsIgnoreCase(s.getStatus()) && !"ENDED".equalsIgnoreCase(prevStatus)) {
                            if (s.getEndedBy() != null && !currentUserId.equals(s.getEndedBy())) {
                                if (!com.example.skillswapproject.util.SwapNotificationManager.isEndedSeen(MainActivity.this, s.getId())
                                        && !com.example.skillswapproject.util.SwapNotificationManager.wasNotified(MainActivity.this, s.getId(), "ENDED")) {
                                    com.example.skillswapproject.util.SwapNotificationManager.markNotified(MainActivity.this, s.getId(), "ENDED");
                                    com.example.skillswapproject.util.AppNotificationHelper.showSwapEndedNotification(
                                            MainActivity.this, s.getReceiverName()
                                    );
                                }
                            }
                        }
                        knownSwapStatuses.put(s.getId(), s.getStatus());
                    }
                }

                // Track received swaps and check for new incoming or ended swaps in real-time
                if (receivedSwaps != null) {
                    for (com.example.skillswapproject.data.model.FirestoreSwap s : receivedSwaps) {
                        if (s.getId() == null || s.getStatus() == null) continue;
                        String prevStatus = knownSwapStatuses.get(s.getId());
                        if (prevStatus == null && "PENDING".equalsIgnoreCase(s.getStatus())) {
                            if (!com.example.skillswapproject.util.SwapNotificationManager.isPendingSeen(MainActivity.this, s.getId())
                                    && !com.example.skillswapproject.util.SwapNotificationManager.wasNotified(MainActivity.this, s.getId(), "PENDING")) {
                                com.example.skillswapproject.util.SwapNotificationManager.markNotified(MainActivity.this, s.getId(), "PENDING");
                                com.example.skillswapproject.util.AppNotificationHelper.showSwapRequestNotification(
                                        MainActivity.this, s.getRequesterName(), s.getOfferedSkillName(), s.getRequestedSkillName()
                                );
                            }
                        } else if ("ENDED".equalsIgnoreCase(s.getStatus()) && !"ENDED".equalsIgnoreCase(prevStatus)) {
                            if (s.getEndedBy() != null && !currentUserId.equals(s.getEndedBy())) {
                                if (!com.example.skillswapproject.util.SwapNotificationManager.isEndedSeen(MainActivity.this, s.getId())
                                        && !com.example.skillswapproject.util.SwapNotificationManager.wasNotified(MainActivity.this, s.getId(), "ENDED")) {
                                    com.example.skillswapproject.util.SwapNotificationManager.markNotified(MainActivity.this, s.getId(), "ENDED");
                                    com.example.skillswapproject.util.AppNotificationHelper.showSwapEndedNotification(
                                            MainActivity.this, s.getRequesterName()
                                    );
                                }
                            }
                        }
                        knownSwapStatuses.put(s.getId(), s.getStatus());
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                // Keep current state on error
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateChatBadge();
        updateHomeBadge();
        checkDailyRewardAndReminders();
    }

    private void updateHomeBadge() {
        if (bottomNavigationView == null) return;
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            bottomNavigationView.removeBadge(R.id.homeFragment);
            return;
        }

        int count = com.example.skillswapproject.util.SwapNotificationManager.calculateHomeNotificationCount(
                this, currentUserId, cachedReceivedSwaps, cachedSentSwaps
        );

        if (count > 0) {
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.homeFragment);
            badge.setVisible(true);
            badge.setNumber(count);
            badge.setBackgroundColor(getColor(R.color.error_warning));
            badge.setBadgeTextColor(getColor(R.color.white));
        } else {
            bottomNavigationView.removeBadge(R.id.homeFragment);
        }
    }

    private void updateChatBadge() {
        if (bottomNavigationView == null) return;

        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            bottomNavigationView.removeBadge(R.id.chatFragment);
            return;
        }

        int unreadCount = 0;
        for (FirestoreChat chat : cachedChats) {
            if (ChatReadStatusManager.isChatUnread(this, currentUserId, chat)) {
                unreadCount++;
            }
        }

        if (unreadCount > 0) {
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.chatFragment);
            badge.setVisible(true);
            badge.setNumber(unreadCount);
            badge.setBackgroundColor(getColor(R.color.primary_green));
            badge.setBadgeTextColor(getColor(R.color.white));
        } else {
            bottomNavigationView.removeBadge(R.id.chatFragment);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatsRegistration != null) {
            chatsRegistration.remove();
            chatsRegistration = null;
        }
        if (swapsRegistration != null) {
            swapsRegistration.remove();
            swapsRegistration = null;
        }
    }
}