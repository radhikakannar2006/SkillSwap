package com.example.skillswapproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillswapproject.data.model.FirestoreReview;
import com.example.skillswapproject.databinding.ItemUserReviewBinding;
import com.example.skillswapproject.util.ImageLoaderHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserReviewAdapter extends RecyclerView.Adapter<UserReviewAdapter.ReviewViewHolder> {

    public interface OnReviewActionListener {
        void onEditReview(FirestoreReview review);
        void onDeleteReview(FirestoreReview review);
    }

    private final List<FirestoreReview> reviewList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private String currentUserId;
    private OnReviewActionListener actionListener;

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        notifyDataSetChanged();
    }

    public void setOnReviewActionListener(OnReviewActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void submitList(List<FirestoreReview> reviews) {
        reviewList.clear();
        if (reviews != null) {
            reviewList.addAll(reviews);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserReviewBinding binding = ItemUserReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ReviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(reviewList.get(position));
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserReviewBinding binding;

        public ReviewViewHolder(@NonNull ItemUserReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FirestoreReview review) {
            if (review == null) return;

            binding.tvReviewerName.setText(review.getReviewerName());
            binding.tvReviewRating.setText(String.format(Locale.US, "%.1f", review.getRating()));
            binding.tvReviewFeedback.setText(review.getFeedback());

            if (review.getTimestamp() != null) {
                binding.tvReviewDate.setText(dateFormat.format(review.getTimestamp()));
            } else {
                binding.tvReviewDate.setText("Recent");
            }

            ImageLoaderHelper.loadProfileImage(binding.ivReviewerAvatar, review.getReviewerPhotoUrl());

            // Only author of review can edit or delete
            boolean isOwner = currentUserId != null && currentUserId.equals(review.getReviewerId());
            if (isOwner) {
                binding.layoutReviewActions.setVisibility(android.view.View.VISIBLE);
                binding.btnEditReview.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onEditReview(review);
                    }
                });
                binding.btnDeleteReview.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onDeleteReview(review);
                    }
                });
            } else {
                binding.layoutReviewActions.setVisibility(android.view.View.GONE);
                binding.btnEditReview.setOnClickListener(null);
                binding.btnDeleteReview.setOnClickListener(null);
            }
        }
    }
}
