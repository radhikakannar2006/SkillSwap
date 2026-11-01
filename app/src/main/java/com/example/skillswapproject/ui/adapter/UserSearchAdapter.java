package com.example.skillswapproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillswapproject.R;
import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.util.ImageLoaderHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying search results — real Firestore users.
 * Each item shows avatar photo/initial, name, skills summary.
 * Clicking opens their Profile screen.
 */
public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClicked(FirestoreUser user);
    }

    public interface OnMatchBadgeClickListener {
        void onMatchBadgeClick(FirestoreUser user);
    }

    private final List<FirestoreUser> users = new ArrayList<>();
    private OnUserClickListener listener;
    private OnMatchBadgeClickListener matchBadgeListener;

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void setOnMatchBadgeClickListener(OnMatchBadgeClickListener matchBadgeListener) {
        this.matchBadgeListener = matchBadgeListener;
    }

    public void submitList(List<FirestoreUser> newUsers) {
        users.clear();
        if (newUsers != null) {
            users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search_result, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(users.get(position), listener, matchBadgeListener);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatarInitial;
        private final ImageView ivSearchAvatar;
        private final TextView tvUserName;
        private final TextView tvSkillsSummary;
        private final TextView tvMatchBadge;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitial = itemView.findViewById(R.id.tvSearchAvatarInitial);
            ivSearchAvatar = itemView.findViewById(R.id.ivSearchAvatar);
            tvUserName = itemView.findViewById(R.id.tvSearchUserName);
            tvSkillsSummary = itemView.findViewById(R.id.tvSearchSkillsSummary);
            tvMatchBadge = itemView.findViewById(R.id.tvSearchMatchBadge);
        }

        public void bind(FirestoreUser user, OnUserClickListener listener, OnMatchBadgeClickListener matchBadgeListener) {
            String name = user.getName() != null ? user.getName() : "User";
            tvUserName.setText(name);

            if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
                ivSearchAvatar.setVisibility(View.VISIBLE);
                tvAvatarInitial.setVisibility(View.GONE);
                ImageLoaderHelper.loadProfileImage(ivSearchAvatar, user.getProfilePhotoUrl());
            } else {
                ivSearchAvatar.setVisibility(View.GONE);
                tvAvatarInitial.setVisibility(View.VISIBLE);
                tvAvatarInitial.setText(name.isEmpty() ? "U" : String.valueOf(name.charAt(0)).toUpperCase());
            }

            // AI Compatibility Match Badge
            int score = user.getCompatibilityScore();
            if (score > 0) {
                tvMatchBadge.setVisibility(View.VISIBLE);
                tvMatchBadge.setText(score + "% Match ✨");
                tvMatchBadge.setOnClickListener(v -> {
                    if (matchBadgeListener != null) {
                        matchBadgeListener.onMatchBadgeClick(user);
                    } else if (listener != null) {
                        listener.onUserClicked(user);
                    }
                });
            } else {
                tvMatchBadge.setVisibility(View.GONE);
            }

            if (!user.isPublic()) {
                tvSkillsSummary.setText("Private Profile · Swap to connect");
            } else {
                // Build skills summary
                StringBuilder sb = new StringBuilder();
                if (user.getOfferedSkills() != null && !user.getOfferedSkills().isEmpty()) {
                    sb.append("Teaches: ");
                    sb.append(String.join(", ", user.getOfferedSkills().subList(
                            0, Math.min(2, user.getOfferedSkills().size()))));
                }
                if (user.getWantedSkills() != null && !user.getWantedSkills().isEmpty()) {
                    if (sb.length() > 0) sb.append("  ·  ");
                    sb.append("Learns: ");
                    sb.append(String.join(", ", user.getWantedSkills().subList(
                            0, Math.min(2, user.getWantedSkills().size()))));
                }
                if (sb.length() == 0) sb.append("Skill Swap Member");
                tvSkillsSummary.setText(sb.toString());
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onUserClicked(user);
            });
        }
    }
}
