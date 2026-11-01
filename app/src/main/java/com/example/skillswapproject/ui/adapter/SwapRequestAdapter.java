package com.example.skillswapproject.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillswapproject.R;
import com.example.skillswapproject.data.model.FirestoreNotification;
import com.example.skillswapproject.data.model.FirestoreSwap;
import com.example.skillswapproject.databinding.ItemSwapRequestBinding;
import com.example.skillswapproject.util.ImageLoaderHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SwapRequestAdapter extends RecyclerView.Adapter<SwapRequestAdapter.SwapViewHolder> {

    public interface OnSwapActionListener {
        void onAccept(FirestoreSwap swap);
        void onDecline(FirestoreSwap swap);
        void onDelete(FirestoreSwap swap);
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(FirestoreNotification notification);
    }

    private final List<Object> itemsList = new ArrayList<>();
    private boolean isReceivedMode = true;
    private boolean isNotificationMode = false;
    private final OnSwapActionListener swapListener;
    private OnNotificationClickListener notificationListener;

    public SwapRequestAdapter(OnSwapActionListener swapListener) {
        this.swapListener = swapListener;
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.notificationListener = listener;
    }

    public void setSwaps(List<FirestoreSwap> newSwaps, boolean isReceivedMode) {
        this.isNotificationMode = false;
        this.isReceivedMode = isReceivedMode;
        this.itemsList.clear();
        if (newSwaps != null) {
            this.itemsList.addAll(newSwaps);
        }
        notifyDataSetChanged();
    }

    public void setNotifications(List<FirestoreNotification> notifications) {
        this.isNotificationMode = true;
        this.itemsList.clear();
        if (notifications != null) {
            this.itemsList.addAll(notifications);
        }
        notifyDataSetChanged();
    }

    public void setOtherItems(List<Object> otherItems) {
        this.isNotificationMode = true;
        this.itemsList.clear();
        if (otherItems != null) {
            this.itemsList.addAll(otherItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SwapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSwapRequestBinding binding = ItemSwapRequestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SwapViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SwapViewHolder holder, int position) {
        Object item = itemsList.get(position);
        if (item instanceof FirestoreNotification) {
            holder.bindNotification((FirestoreNotification) item, notificationListener);
        } else if (item instanceof FirestoreSwap) {
            holder.bindSwap((FirestoreSwap) item, isReceivedMode, swapListener);
        }
    }

    @Override
    public int getItemCount() {
        return itemsList.size();
    }

    public static String getRelativeTimeString(Date date) {
        if (date == null) return "Just now";
        long now = System.currentTimeMillis();
        long diff = now - date.getTime();
        if (diff < 60 * 1000) return "Just now";
        if (diff < 60 * 60 * 1000) return (diff / (60 * 1000)) + " min ago";
        if (diff < 24 * 60 * 60 * 1000) return (diff / (60 * 60 * 1000)) + " hr ago";
        if (diff < 48 * 60 * 60 * 1000) return "Yesterday";
        return (diff / (24 * 60 * 60 * 1000)) + " days ago";
    }

    static class SwapViewHolder extends RecyclerView.ViewHolder {
        private final ItemSwapRequestBinding binding;

        public SwapViewHolder(ItemSwapRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bindNotification(FirestoreNotification notification, OnNotificationClickListener listener) {
            Context context = itemView.getContext();

            binding.tvSwapUserName.setText(notification.getTitle() != null ? notification.getTitle() : "Notification");
            binding.tvSwapSkillsDetails.setText(notification.getMessage() != null ? notification.getMessage() : "");

            ImageLoaderHelper.loadProfileImage(binding.ivSwapAvatar, notification.getSenderAvatarUrl());

            // Relative Timestamp
            binding.tvNotificationTimestamp.setVisibility(View.VISIBLE);
            binding.tvNotificationTimestamp.setText(getRelativeTimeString(notification.getTimestamp()));

            // Unread Dot & Card Tint
            if (!notification.isRead()) {
                binding.viewUnreadDot.setVisibility(View.VISIBLE);
                binding.getRoot().setCardBackgroundColor(ColorStateList.valueOf(0xFFF4FBF7));
            } else {
                binding.viewUnreadDot.setVisibility(View.GONE);
                binding.getRoot().setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));
            }

            // Chip styling by type
            String type = notification.getType() != null ? notification.getType() : "";
            binding.tvSwapStatusChip.setVisibility(View.VISIBLE);

            if (FirestoreNotification.TYPE_SWAP_ACCEPTED.equalsIgnoreCase(type)) {
                binding.tvSwapStatusChip.setText("ACCEPTED");
                binding.tvSwapStatusChip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_green_accent)));
                binding.tvSwapStatusChip.setTextColor(ContextCompat.getColor(context, R.color.primary_green));
            } else if (FirestoreNotification.TYPE_NEW_MESSAGE.equalsIgnoreCase(type)) {
                binding.tvSwapStatusChip.setText("MESSAGE");
                binding.tvSwapStatusChip.setBackgroundTintList(ColorStateList.valueOf(0xFFE0F2FE));
                binding.tvSwapStatusChip.setTextColor(0xFF0369A1);
            } else if (FirestoreNotification.TYPE_NEW_FEEDBACK.equalsIgnoreCase(type)) {
                binding.tvSwapStatusChip.setText("FEEDBACK");
                binding.tvSwapStatusChip.setBackgroundTintList(ColorStateList.valueOf(0xFFFEF3C7));
                binding.tvSwapStatusChip.setTextColor(0xFFB45309);
            } else if (FirestoreNotification.TYPE_SESSION_SCHEDULED.equalsIgnoreCase(type) ||
                    FirestoreNotification.TYPE_SESSION_REMINDER.equalsIgnoreCase(type)) {
                binding.tvSwapStatusChip.setText("SESSION");
                binding.tvSwapStatusChip.setBackgroundTintList(ColorStateList.valueOf(0xFFEDE9FE));
                binding.tvSwapStatusChip.setTextColor(0xFF6D28D9);
            } else {
                binding.tvSwapStatusChip.setText("REQUEST");
                binding.tvSwapStatusChip.setBackgroundTintList(ColorStateList.valueOf(0xFFFFF3CD));
                binding.tvSwapStatusChip.setTextColor(ContextCompat.getColor(context, R.color.rating_gold));
            }

            // Hide action button rows for notifications (clicking row navigates directly)
            binding.layoutReceivedActions.setVisibility(View.GONE);
            binding.layoutSentActions.setVisibility(View.GONE);
            binding.layoutAcceptedActions.setVisibility(View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }

        public void bindSwap(FirestoreSwap swap, boolean isReceived, OnSwapActionListener listener) {
            Context context = itemView.getContext();

            binding.viewUnreadDot.setVisibility(View.GONE);
            binding.tvNotificationTimestamp.setVisibility(View.VISIBLE);
            binding.tvNotificationTimestamp.setText(getRelativeTimeString(swap.getTimestamp()));
            binding.getRoot().setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));

            String displayName;
            String photo;
            String skillsDesc;

            if (isReceived) {
                displayName = swap.getRequesterName() != null ? swap.getRequesterName() : "User";
                photo = swap.getRequesterPhoto();
                skillsDesc = "Offers: " + swap.getOfferedSkillName() + " ➔ Wants: " + swap.getRequestedSkillName();
            } else {
                displayName = swap.getReceiverName() != null ? swap.getReceiverName() : "User";
                photo = swap.getReceiverPhoto();
                skillsDesc = "Offered: " + swap.getOfferedSkillName() + " ➔ For: " + swap.getRequestedSkillName();
            }

            binding.tvSwapUserName.setText(displayName);
            binding.tvSwapSkillsDetails.setText(skillsDesc);
            ImageLoaderHelper.loadProfileImage(binding.ivSwapAvatar, photo);

            // Clicking avatar or name opens user profile
            final String targetPartnerId = isReceived ? swap.getRequesterId() : swap.getReceiverId();
            View.OnClickListener profileClickListener = v -> {
                if (targetPartnerId != null && !targetPartnerId.isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(context, com.example.skillswapproject.ui.activity.UserProfileActivity.class);
                    intent.putExtra(com.example.skillswapproject.ui.activity.UserProfileActivity.EXTRA_TARGET_USER_ID, targetPartnerId);
                    context.startActivity(intent);
                }
            };
            binding.ivSwapAvatar.setOnClickListener(profileClickListener);
            binding.tvSwapUserName.setOnClickListener(profileClickListener);

            String status = swap.getStatus() != null ? swap.getStatus().toUpperCase() : "PENDING";
            binding.tvSwapStatusChip.setText(status);

            if ("ACCEPTED".equals(status)) {
                binding.tvSwapStatusChip.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.light_green_accent)));
                binding.tvSwapStatusChip.setTextColor(ContextCompat.getColor(context, R.color.primary_green));
                binding.layoutReceivedActions.setVisibility(View.GONE);
                binding.layoutSentActions.setVisibility(View.GONE);
                binding.layoutAcceptedActions.setVisibility(View.VISIBLE);
                binding.btnMessageSwap.setOnClickListener(v -> {
                    if (targetPartnerId != null && !targetPartnerId.isEmpty()) {
                        String currentUserId = com.example.skillswapproject.data.repository.FirestoreRepository.getInstance().getCurrentUserId();
                        String chatId = com.example.skillswapproject.data.repository.FirestoreRepository.getChatId(currentUserId, targetPartnerId);
                        android.content.Intent chatIntent = new android.content.Intent(context, com.example.skillswapproject.ui.activity.ChatDetailActivity.class);
                        chatIntent.putExtra("EXTRA_CHAT_ID", chatId);
                        chatIntent.putExtra("EXTRA_FRIEND_USER_ID", targetPartnerId);
                        chatIntent.putExtra("EXTRA_FRIEND_NAME", displayName);
                        context.startActivity(chatIntent);
                    }
                });
            } else if ("ENDED".equals(status)) {
                binding.tvSwapStatusChip.setText("SWAP ENDED");
                binding.tvSwapStatusChip.setBackgroundTintList(ColorStateList.valueOf(0xFFECEFF1));
                binding.tvSwapStatusChip.setTextColor(ContextCompat.getColor(context, R.color.muted_text));
                binding.layoutReceivedActions.setVisibility(View.GONE);
                binding.layoutSentActions.setVisibility(View.GONE);
                binding.layoutAcceptedActions.setVisibility(View.GONE);
            } else if ("REJECTED".equals(status)) {
                binding.tvSwapStatusChip.setBackgroundTintList(ColorStateList.valueOf(0xFFFFE4E4));
                binding.tvSwapStatusChip.setTextColor(ContextCompat.getColor(context, R.color.error_warning));
                binding.layoutReceivedActions.setVisibility(View.GONE);
                binding.layoutAcceptedActions.setVisibility(View.GONE);
                if (!isReceived) {
                    binding.layoutSentActions.setVisibility(View.VISIBLE);
                } else {
                    binding.layoutSentActions.setVisibility(View.GONE);
                }
            } else {
                // PENDING
                binding.tvSwapStatusChip.setBackgroundTintList(ColorStateList.valueOf(0xFFFFF3CD));
                binding.tvSwapStatusChip.setTextColor(ContextCompat.getColor(context, R.color.rating_gold));
                binding.layoutAcceptedActions.setVisibility(View.GONE);

                if (isReceived) {
                    binding.layoutReceivedActions.setVisibility(View.VISIBLE);
                    binding.layoutSentActions.setVisibility(View.GONE);
                } else {
                    binding.layoutReceivedActions.setVisibility(View.GONE);
                    binding.layoutSentActions.setVisibility(View.VISIBLE);
                }
            }

            binding.btnAcceptSwap.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(swap);
            });

            binding.btnDeclineSwap.setOnClickListener(v -> {
                if (listener != null) listener.onDecline(swap);
            });

            binding.btnDeleteSwap.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(swap);
            });
        }
    }
}
