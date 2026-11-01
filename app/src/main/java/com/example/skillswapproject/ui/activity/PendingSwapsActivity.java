package com.example.skillswapproject.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.skillswapproject.data.model.FirestoreNotification;
import com.example.skillswapproject.data.model.FirestoreSwap;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.ActivityPendingSwapsBinding;
import com.example.skillswapproject.ui.adapter.SwapRequestAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PendingSwapsActivity extends AppCompatActivity implements
        SwapRequestAdapter.OnSwapActionListener,
        SwapRequestAdapter.OnNotificationClickListener {

    private ActivityPendingSwapsBinding binding;
    private SwapRequestAdapter adapter;
    private ListenerRegistration swapsRegistration;
    private ListenerRegistration notificationsRegistration;

    private final List<FirestoreSwap> receivedSwaps = new ArrayList<>();
    private final List<FirestoreSwap> sentSwaps = new ArrayList<>();
    private final List<FirestoreNotification> notificationsList = new ArrayList<>();
    private final List<Object> otherItems = new ArrayList<>();
    private List<FirestoreSwap> latestRawReceived = new ArrayList<>();
    private List<FirestoreSwap> latestRawSent = new ArrayList<>();
    private int currentTabPosition = 0; // 0: Received, 1: Sent, 2: Other

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingSwapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        loadData();
    }

    private void setupUI() {
        binding.btnBackSwaps.setOnClickListener(v -> finish());

        adapter = new SwapRequestAdapter(this);
        adapter.setOnNotificationClickListener(this);
        binding.rvSwaps.setAdapter(adapter);

        binding.tabLayoutSwaps.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                updateList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        if (binding.btnClearAllNotifications != null) {
            binding.btnClearAllNotifications.setOnClickListener(v -> showClearAllNotificationsDialog());
        }

        if (binding.bannerNotificationPermission != null) {
            com.example.skillswapproject.util.NotificationBannerHelper.setupBanner(
                    this,
                    binding.bannerNotificationPermission.getRoot(),
                    "Get swap & request notifications",
                    "Never miss incoming swap requests, acceptances, and session alerts."
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bannerNotificationPermission != null) {
            com.example.skillswapproject.util.NotificationBannerHelper.updateBannerVisibility(
                    this,
                    binding.bannerNotificationPermission.getRoot()
            );
        }
    }

    private void loadData() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in to view notifications", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.progressBarSwaps.setVisibility(View.VISIBLE);

        // 1. Listen to real-time In-App Notifications
        notificationsRegistration = FirestoreRepository.getInstance().listenToUserNotifications(currentUserId, new FirestoreRepository.OnNotificationsLoadedListener() {
            @Override
            public void onNotificationsLoaded(List<FirestoreNotification> notifications) {
                binding.progressBarSwaps.setVisibility(View.GONE);
                notificationsList.clear();
                if (notifications != null) {
                    notificationsList.addAll(notifications);
                }
                rebuildLists();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBarSwaps.setVisibility(View.GONE);
            }
        });

        // 2. Listen to real-time Swaps
        swapsRegistration = FirestoreRepository.getInstance().listenToUserSwaps(currentUserId, new FirestoreRepository.OnUserSwapsListener() {
            @Override
            public void onSwapsLoaded(List<FirestoreSwap> received, List<FirestoreSwap> sent) {
                binding.progressBarSwaps.setVisibility(View.GONE);
                latestRawReceived = received != null ? received : new ArrayList<>();
                latestRawSent = sent != null ? sent : new ArrayList<>();

                com.example.skillswapproject.util.SwapNotificationManager.markNotificationsSeen(
                        PendingSwapsActivity.this, received, sent
                );

                rebuildLists();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBarSwaps.setVisibility(View.GONE);
            }
        });
    }

    private void rebuildLists() {
        receivedSwaps.clear();
        sentSwaps.clear();
        otherItems.clear();

        // 1. Pending incoming requests -> Received tab
        for (FirestoreSwap swap : latestRawReceived) {
            if (swap == null) continue;
            String status = swap.getStatus() != null ? swap.getStatus().toUpperCase() : "PENDING";
            if ("PENDING".equals(status)) {
                receivedSwaps.add(swap);
            } else {
                // Ended, Accepted, Cancelled -> Other tab
                otherItems.add(swap);
            }
        }

        // 2. Pending outgoing requests -> Sent tab
        for (FirestoreSwap swap : latestRawSent) {
            if (swap == null) continue;
            String status = swap.getStatus() != null ? swap.getStatus().toUpperCase() : "PENDING";
            if ("PENDING".equals(status)) {
                sentSwaps.add(swap);
            } else {
                // Ended, Accepted, Cancelled -> Other tab (check duplicates)
                boolean alreadyPresent = false;
                for (Object obj : otherItems) {
                    if (obj instanceof FirestoreSwap && swap.getSwapId() != null
                            && swap.getSwapId().equals(((FirestoreSwap) obj).getSwapId())) {
                        alreadyPresent = true;
                        break;
                    }
                }
                if (!alreadyPresent) {
                    otherItems.add(swap);
                }
            }
        }

        // 3. Notifications (Swap accepted/ended, ratings, sessions) -> Other tab
        for (FirestoreNotification n : notificationsList) {
            otherItems.add(n);
        }

        // Sort each list by timestamp DESCENDING
        Collections.sort(receivedSwaps, (s1, s2) -> compareDates(s2.getTimestamp(), s1.getTimestamp()));
        Collections.sort(sentSwaps, (s1, s2) -> compareDates(s2.getTimestamp(), s1.getTimestamp()));
        Collections.sort(otherItems, (o1, o2) -> {
            Date d1 = getItemDate(o1);
            Date d2 = getItemDate(o2);
            return compareDates(d2, d1);
        });

        updateTabTitles();
        updateClearAllButtonVisibility();
        updateList();
    }

    private void updateClearAllButtonVisibility() {
        if (binding.btnClearAllNotifications != null) {
            boolean hasNotifications = !notificationsList.isEmpty();
            binding.btnClearAllNotifications.setVisibility(hasNotifications ? View.VISIBLE : View.GONE);
            binding.btnClearAllNotifications.setEnabled(hasNotifications);
        }
    }

    private void showClearAllNotificationsDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to clear all notifications?")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllNotifications())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void clearAllNotifications() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) return;

        binding.progressBarSwaps.setVisibility(View.VISIBLE);
        if (binding.btnClearAllNotifications != null) {
            binding.btnClearAllNotifications.setEnabled(false);
        }

        FirestoreRepository.getInstance().clearAllNotifications(currentUserId)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBarSwaps.setVisibility(View.GONE);
                    notificationsList.clear();
                    rebuildLists();
                    com.example.skillswapproject.util.AppNotificationHelper.cancelAllNotifications(PendingSwapsActivity.this);
                    Toast.makeText(PendingSwapsActivity.this, "All notifications cleared", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    binding.progressBarSwaps.setVisibility(View.GONE);
                    if (binding.btnClearAllNotifications != null) {
                        binding.btnClearAllNotifications.setEnabled(true);
                    }
                    Toast.makeText(PendingSwapsActivity.this, "Failed to clear notifications: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                });
    }

    private Date getItemDate(Object obj) {
        if (obj instanceof FirestoreSwap) return ((FirestoreSwap) obj).getTimestamp();
        if (obj instanceof FirestoreNotification) return ((FirestoreNotification) obj).getTimestamp();
        return null;
    }

    private int compareDates(Date d1, Date d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        return d1.compareTo(d2);
    }

    private void updateTabTitles() {
        TabLayout.Tab tab0 = binding.tabLayoutSwaps.getTabAt(0);
        if (tab0 != null) {
            tab0.setText("Received (" + receivedSwaps.size() + ")");
        }
        TabLayout.Tab tab1 = binding.tabLayoutSwaps.getTabAt(1);
        if (tab1 != null) {
            tab1.setText("Sent (" + sentSwaps.size() + ")");
        }
        TabLayout.Tab tab2 = binding.tabLayoutSwaps.getTabAt(2);
        if (tab2 != null) {
            tab2.setText("Other (" + otherItems.size() + ")");
        }
    }

    private void updateList() {
        if (currentTabPosition == 0) {
            // Received Swaps tab
            if (receivedSwaps.isEmpty()) {
                binding.layoutEmptySwaps.setVisibility(View.VISIBLE);
                binding.rvSwaps.setVisibility(View.GONE);
                binding.tvEmptySwapsTitle.setText("No Received Swaps");
                binding.tvEmptySwapsDesc.setText("When other users request a skill swap with you, their requests appear here.");
            } else {
                binding.layoutEmptySwaps.setVisibility(View.GONE);
                binding.rvSwaps.setVisibility(View.VISIBLE);
                adapter.setSwaps(receivedSwaps, true);
            }
        } else if (currentTabPosition == 1) {
            // Sent Swaps tab
            if (sentSwaps.isEmpty()) {
                binding.layoutEmptySwaps.setVisibility(View.VISIBLE);
                binding.rvSwaps.setVisibility(View.GONE);
                binding.tvEmptySwapsTitle.setText("No Sent Swaps");
                binding.tvEmptySwapsDesc.setText("You haven't requested any swaps yet. Explore profiles on the Home screen to connect!");
            } else {
                binding.layoutEmptySwaps.setVisibility(View.GONE);
                binding.rvSwaps.setVisibility(View.VISIBLE);
                adapter.setSwaps(sentSwaps, false);
            }
        } else {
            // Other Activity tab
            if (otherItems.isEmpty()) {
                binding.layoutEmptySwaps.setVisibility(View.VISIBLE);
                binding.rvSwaps.setVisibility(View.GONE);
                binding.tvEmptySwapsTitle.setText("No Other Activity");
                binding.tvEmptySwapsDesc.setText("Updates about started and ended swaps, ratings, and sessions will appear here.");
            } else {
                binding.layoutEmptySwaps.setVisibility(View.GONE);
                binding.rvSwaps.setVisibility(View.VISIBLE);
                adapter.setOtherItems(otherItems);
            }
        }
    }

    @Override
    public void onNotificationClick(FirestoreNotification notification) {
        if (notification == null) return;

        // Mark as read in Firestore
        if (!notification.isRead() && notification.getNotificationId() != null) {
            FirestoreRepository.getInstance().markNotificationAsRead(notification.getNotificationId());
        }

        String type = notification.getType() != null ? notification.getType() : "";
        Intent intent;

        if (FirestoreNotification.TYPE_NEW_MESSAGE.equalsIgnoreCase(type) && notification.getRelatedChatId() != null) {
            intent = new Intent(this, ChatDetailActivity.class);
            intent.putExtra("EXTRA_CHAT_ID", notification.getRelatedChatId());
            if (notification.getSenderUserId() != null) {
                intent.putExtra("EXTRA_FRIEND_USER_ID", notification.getSenderUserId());
            }
            intent.putExtra("EXTRA_FRIEND_NAME", notification.getSenderName() != null ? notification.getSenderName() : "User");
            startActivity(intent);
        } else if ((FirestoreNotification.TYPE_SESSION_SCHEDULED.equalsIgnoreCase(type) ||
                FirestoreNotification.TYPE_SESSION_REMINDER.equalsIgnoreCase(type)) &&
                notification.getRelatedSessionId() != null) {
            intent = new Intent(this, SessionDetailActivity.class);
            intent.putExtra(SessionDetailActivity.EXTRA_SESSION_ID, notification.getRelatedSessionId());
            startActivity(intent);
        } else if (FirestoreNotification.TYPE_NEW_FEEDBACK.equalsIgnoreCase(type) && notification.getSenderUserId() != null) {
            intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.EXTRA_TARGET_USER_ID, notification.getSenderUserId());
            startActivity(intent);
        } else if (FirestoreNotification.TYPE_SWAP_ACCEPTED.equalsIgnoreCase(type)) {
            intent = new Intent(this, ActiveSwapsActivity.class);
            startActivity(intent);
        } else {
            // Select Received tab
            currentTabPosition = 1;
            TabLayout.Tab tab1 = binding.tabLayoutSwaps.getTabAt(1);
            if (tab1 != null) tab1.select();
            updateList();
        }
    }

    @Override
    public void onAccept(FirestoreSwap swap) {
        if (swap == null || swap.getId() == null) return;

        FirestoreRepository.getInstance().updateSwapStatus(
                swap.getId(), "ACCEPTED", swap.getRequesterId(), swap.getReceiverId()
        ).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Swap accepted! Chat is now unlocked.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to accept swap: " + (e != null ? e.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDecline(FirestoreSwap swap) {
        if (swap == null || swap.getId() == null) return;

        FirestoreRepository.getInstance().updateSwapStatus(
                swap.getId(), "REJECTED", swap.getRequesterId(), swap.getReceiverId()
        ).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Swap request declined.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to decline swap: " + (e != null ? e.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDelete(FirestoreSwap swap) {
        if (swap == null || swap.getId() == null) return;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete Swap Request")
                .setMessage("Are you sure you want to delete this sent swap request?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirestoreRepository.getInstance().deleteSwap(swap.getId())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Swap request deleted.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete request: " + (e != null ? e.getMessage() : "Try again"), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (swapsRegistration != null) {
            swapsRegistration.remove();
            swapsRegistration = null;
        }
        if (notificationsRegistration != null) {
            notificationsRegistration.remove();
            notificationsRegistration = null;
        }
    }
}
