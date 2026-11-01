package com.example.skillswapproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillswapproject.data.model.User;
import com.example.skillswapproject.databinding.ItemRecommendedUserBinding;
import com.example.skillswapproject.util.ImageLoaderHelper;

import java.util.ArrayList;
import java.util.List;

public class RecommendedUserAdapter extends RecyclerView.Adapter<RecommendedUserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface OnUserSwapClickListener {
        void onSwapClick(User user);
    }

    public interface OnMatchBadgeClickListener {
        void onMatchBadgeClick(User user);
    }

    private final List<User> userList = new ArrayList<>();
    private OnUserClickListener userClickListener;
    private OnUserSwapClickListener swapClickListener;
    private OnMatchBadgeClickListener matchBadgeClickListener;

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.userClickListener = listener;
    }

    public void setOnUserSwapClickListener(OnUserSwapClickListener listener) {
        this.swapClickListener = listener;
    }

    public void setOnMatchBadgeClickListener(OnMatchBadgeClickListener listener) {
        this.matchBadgeClickListener = listener;
    }

    public void submitList(List<User> users) {
        userList.clear();
        if (users != null) {
            userList.addAll(users);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecommendedUserBinding binding = ItemRecommendedUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, userClickListener, swapClickListener, matchBadgeClickListener);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecommendedUserBinding binding;

        public UserViewHolder(ItemRecommendedUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(User user, OnUserClickListener userListener, OnUserSwapClickListener swapListener, OnMatchBadgeClickListener badgeListener) {
            binding.tvUserName.setText(user.getName() != null ? user.getName() : "User");

            // Load profile picture
            ImageLoaderHelper.loadProfileImage(binding.ivUserAvatar, user.getProfilePhotoUrl());

            // AI Compatibility Match Badge
            int score = user.getCompatibilityScore();
            if (score > 0) {
                binding.tvCompatibilityBadge.setVisibility(View.VISIBLE);
                binding.tvCompatibilityBadge.setText(score + "% Match ✨");
                binding.tvCompatibilityBadge.setOnClickListener(v -> {
                    if (badgeListener != null) {
                        badgeListener.onMatchBadgeClick(user);
                    } else if (userListener != null) {
                        userListener.onUserClick(user);
                    }
                });
            } else {
                binding.tvCompatibilityBadge.setVisibility(View.GONE);
            }

            if (!user.isPublic()) {
                binding.tvSkillsSummary.setText("Private Profile · Swap to connect");
            } else {
                // Build skills summary line e.g. "Teach Web Design · Learn Python"
                StringBuilder summary = new StringBuilder();
                if (user.getOfferedSkills() != null && !user.getOfferedSkills().isEmpty()) {
                    summary.append("Teach ");
                    for (int i = 0; i < Math.min(user.getOfferedSkills().size(), 2); i++) {
                        summary.append(user.getOfferedSkills().get(i).getName());
                        if (i < Math.min(user.getOfferedSkills().size(), 2) - 1) summary.append(", ");
                    }
                }
                if (user.getWantedSkills() != null && !user.getWantedSkills().isEmpty()) {
                    if (summary.length() > 0) summary.append(" · Learn ");
                    else summary.append("Learn ");
                    for (int i = 0; i < Math.min(user.getWantedSkills().size(), 2); i++) {
                        summary.append(user.getWantedSkills().get(i).getName());
                        if (i < Math.min(user.getWantedSkills().size(), 2) - 1) summary.append(", ");
                    }
                }

                if (summary.length() == 0) {
                    summary.append("Open to Skill Swaps");
                }

                binding.tvSkillsSummary.setText(summary.toString());
            }

            View.OnClickListener profileClick = v -> {
                if (userListener != null) {
                    userListener.onUserClick(user);
                }
            };
            binding.getRoot().setOnClickListener(profileClick);
            binding.ivUserAvatar.setOnClickListener(profileClick);
            binding.tvUserName.setOnClickListener(profileClick);

            binding.btnSwapAction.setOnClickListener(v -> {
                if (swapListener != null) {
                    swapListener.onSwapClick(user);
                }
            });

            binding.cardUser.setOnClickListener(v -> {
                if (userListener != null) {
                    userListener.onUserClick(user);
                } else if (swapListener != null) {
                    swapListener.onSwapClick(user);
                }
            });
        }
    }
}
