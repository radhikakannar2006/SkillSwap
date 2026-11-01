package com.example.skillswapproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillswapproject.R;
import com.example.skillswapproject.data.model.ChatListItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    public interface OnChatClickListener {
        void onChatClicked(ChatListItem chatItem);
        void onProfileClicked(ChatListItem chatItem);
    }

    private final List<ChatListItem> chatItems = new ArrayList<>();
    private final OnChatClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    public ChatListAdapter(OnChatClickListener listener) {
        this.listener = listener;
    }

    public void setChatItems(List<ChatListItem> newItems) {
        chatItems.clear();
        if (newItems != null) {
            chatItems.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatListItem item = chatItems.get(position);
        holder.bind(item, listener, timeFormat, dateFormat);
    }

    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final View avatarCard;
        private final TextView tvAvatarInitial;
        private final android.widget.ImageView ivFriendAvatar;
        private final View viewProfileUnreadDot;
        private final TextView tvFriendName;
        private final TextView tvLastMessage;
        private final TextView tvMessageTime;
        private final android.widget.ImageView ivUnreadIndicatorRight;
        private final TextView swapBadge;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarCard = itemView.findViewById(R.id.avatarCard);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            ivFriendAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            viewProfileUnreadDot = itemView.findViewById(R.id.viewProfileUnreadDot);
            tvFriendName = itemView.findViewById(R.id.tvFriendName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
            ivUnreadIndicatorRight = itemView.findViewById(R.id.ivUnreadIndicatorRight);
            swapBadge = itemView.findViewById(R.id.swapBadge);
        }

        public void bind(ChatListItem item, OnChatClickListener listener, SimpleDateFormat timeFormat, SimpleDateFormat dateFormat) {
            tvFriendName.setText(item.getFriendName() != null ? item.getFriendName() : "User");
            tvLastMessage.setText(item.getLastMessageText() != null ? item.getLastMessageText() : "");

            if (item.getFriendProfilePhoto() != null && !item.getFriendProfilePhoto().isEmpty()) {
                if (ivFriendAvatar != null) {
                    ivFriendAvatar.setVisibility(View.VISIBLE);
                    com.example.skillswapproject.util.ImageLoaderHelper.loadProfileImage(ivFriendAvatar, item.getFriendProfilePhoto());
                }
                tvAvatarInitial.setVisibility(View.GONE);
            } else {
                if (ivFriendAvatar != null) ivFriendAvatar.setVisibility(View.GONE);
                tvAvatarInitial.setVisibility(View.VISIBLE);
                if (item.getFriendName() != null && !item.getFriendName().isEmpty()) {
                    tvAvatarInitial.setText(String.valueOf(item.getFriendName().charAt(0)).toUpperCase());
                } else {
                    tvAvatarInitial.setText("U");
                }
            }

            if (item.getLastMessageTime() != null) {
                long now = System.currentTimeMillis();
                long msgTime = item.getLastMessageTime().getTime();
                if (now - msgTime < 24 * 60 * 60 * 1000) {
                    tvMessageTime.setText(timeFormat.format(item.getLastMessageTime()));
                } else {
                    tvMessageTime.setText(dateFormat.format(item.getLastMessageTime()));
                }
            } else {
                tvMessageTime.setText("");
            }

            // Unread indicators (badge on avatar and chat icon on right)
            if (item.isUnread()) {
                if (viewProfileUnreadDot != null) viewProfileUnreadDot.setVisibility(View.VISIBLE);
                if (ivUnreadIndicatorRight != null) ivUnreadIndicatorRight.setVisibility(View.VISIBLE);
                tvFriendName.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLastMessage.setTextColor(itemView.getContext().getColor(R.color.dark_text));
            } else {
                if (viewProfileUnreadDot != null) viewProfileUnreadDot.setVisibility(View.GONE);
                if (ivUnreadIndicatorRight != null) ivUnreadIndicatorRight.setVisibility(View.GONE);
                tvFriendName.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
                tvLastMessage.setTextColor(itemView.getContext().getColor(R.color.muted_text));
            }

            // Clicking avatar or name opens user profile
            View.OnClickListener profileClickListener = v -> {
                if (listener != null) {
                    listener.onProfileClicked(item);
                }
            };
            if (avatarCard != null) avatarCard.setOnClickListener(profileClickListener);
            tvFriendName.setOnClickListener(profileClickListener);

            // Clicking message / row opens chat
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClicked(item);
                }
            });
        }
    }
}
