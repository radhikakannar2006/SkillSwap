package com.example.skillswapproject.ui.activity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.skillswapproject.R;
import com.example.skillswapproject.data.model.FirestoreReview;
import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.model.Skill;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.ActivityUserProfileBinding;
import com.example.skillswapproject.databinding.DialogAddReviewBinding;
import com.example.skillswapproject.ui.adapter.SkillChipAdapter;
import com.example.skillswapproject.ui.adapter.UserReviewAdapter;
import com.example.skillswapproject.ui.dialog.SwapRequestBottomSheet;
import com.example.skillswapproject.util.ImageLoaderHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_USER_ID = "EXTRA_TARGET_USER_ID";

    private ActivityUserProfileBinding binding;
    private String targetUserId;
    private FirestoreUser targetUser;
    private SkillChipAdapter teachSkillsAdapter;
    private SkillChipAdapter learnSkillsAdapter;
    private UserReviewAdapter reviewAdapter;
    private ListenerRegistration reviewsRegistration;
    private boolean isSwapAccepted = false;
    private boolean hasEverSwapped = false;
    private FirestoreReview myExistingReview = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        if (targetUserId == null || targetUserId.isEmpty()) {
            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupAdapters();
        setupListeners();
        loadUserProfile();
    }

    private void setupAdapters() {
        teachSkillsAdapter = new SkillChipAdapter();
        binding.rvOtherSkillsTeach.setAdapter(teachSkillsAdapter);

        learnSkillsAdapter = new SkillChipAdapter();
        binding.rvOtherSkillsLearn.setAdapter(learnSkillsAdapter);

        reviewAdapter = new UserReviewAdapter();
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId != null) {
            reviewAdapter.setCurrentUserId(currentUserId);
        }
        reviewAdapter.setOnReviewActionListener(new UserReviewAdapter.OnReviewActionListener() {
            @Override
            public void onEditReview(FirestoreReview review) {
                showRateDialog(review);
            }

            @Override
            public void onDeleteReview(FirestoreReview review) {
                confirmDeleteReview(review);
            }
        });
        binding.rvUserReviews.setAdapter(reviewAdapter);
    }

    private void setupListeners() {
        binding.btnBackProfile.setOnClickListener(v -> finish());

        binding.btnRequestSwapAction.setOnClickListener(v -> openSwapPopup());

        binding.btnMessageAction.setOnClickListener(v -> {
            String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
            if (currentUserId == null || targetUser == null) return;

            String chatId = FirestoreRepository.getChatId(currentUserId, targetUserId);
            Intent intent = new Intent(UserProfileActivity.this, ChatDetailActivity.class);
            intent.putExtra("EXTRA_CHAT_ID", chatId);
            intent.putExtra("EXTRA_FRIEND_USER_ID", targetUserId);
            intent.putExtra("EXTRA_FRIEND_NAME", targetUser.getName() != null ? targetUser.getName() : "User");
            startActivity(intent);
        });

        binding.btnEndSwapAction.setOnClickListener(v -> confirmEndSwap());

        binding.btnRateUserAction.setOnClickListener(v -> showRateDialog(myExistingReview));
    }

    private void confirmEndSwap() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || targetUserId == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("End Skill Swap")
                .setMessage("Are you sure you want to end this skill swap? You will no longer be able to message each other until a new swap is accepted.")
                .setPositiveButton("End Swap", (dialog, which) -> {
                    binding.btnEndSwapAction.setEnabled(false);
                    FirestoreRepository.getInstance().endSwap(currentUserId, targetUserId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Skill swap ended.", Toast.LENGTH_SHORT).show();
                                checkSwapStatus();
                            })
                            .addOnFailureListener(e -> {
                                binding.btnEndSwapAction.setEnabled(true);
                                Toast.makeText(this, "Failed to end swap: " + (e != null ? e.getMessage() : "Try again"), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadUserProfile() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();

        FirestoreRepository.getInstance().getUser(targetUserId).addOnSuccessListener(user -> {
            if (user == null) {
                Toast.makeText(this, "Profile does not exist", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            targetUser = user;

            // Check profile visibility: public vs private
            boolean isPrivate = !user.isPublic();
            android.util.Log.d("SWAP_DEBUG", "loadUserProfile: currentUserId=" + currentUserId + ", targetUserId=" + targetUserId 
                    + ", name=" + user.getName() + ", isPublic=" + user.isPublic() + ", isPrivate=" + isPrivate);
            if (isPrivate && currentUserId != null && !currentUserId.equals(targetUserId)) {
                // Immediately render private state so no info flashes or leaks
                showPrivateProfileState();

                FirestoreRepository.getInstance().checkCanViewPrivateProfile(currentUserId, targetUserId, new FirestoreRepository.OnSwapCheckListener() {
                    @Override
                    public void onResult(boolean canView) {
                        android.util.Log.d("SWAP_DEBUG", "checkCanViewPrivateProfile onResult: canView=" + canView);
                        if (isFinishing() || isDestroyed()) return;
                        if (canView) {
                            // Swapped partner (ACCEPTED, ENDED, or COMPLETED) can see full profile & rate
                            showFullProfile(user);
                            checkSwapStatus();
                            listenToReviews();
                        } else {
                            showPrivateProfileState();
                            checkPendingSwapStatus(currentUserId);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        android.util.Log.e("SWAP_DEBUG", "checkCanViewPrivateProfile onError", e);
                        if (isFinishing() || isDestroyed()) return;
                        showPrivateProfileState();
                        checkPendingSwapStatus(currentUserId);
                    }
                });
            } else {
                android.util.Log.d("SWAP_DEBUG", "Showing full profile directly: isPrivate=" + isPrivate + ", currentUserId=" + currentUserId + ", targetUserId=" + targetUserId);
                showFullProfile(user);
                checkSwapStatus();
                listenToReviews();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load user profile: " + (e != null ? e.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
        });
    }

    private void checkPendingSwapStatus(String currentUserId) {
        if (currentUserId == null || targetUserId == null) return;
        FirestoreRepository.getInstance().checkPendingSwap(currentUserId, targetUserId, new FirestoreRepository.OnSwapCheckListener() {
            @Override
            public void onResult(boolean isPending) {
                if (isFinishing() || isDestroyed()) return;
                if (isPending) {
                    binding.btnRequestSwapAction.setText("Request Sent");
                    binding.btnRequestSwapAction.setEnabled(false);
                } else {
                    binding.btnRequestSwapAction.setText("Request Swap");
                    binding.btnRequestSwapAction.setEnabled(true);
                }
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                binding.btnRequestSwapAction.setText("Request Swap");
                binding.btnRequestSwapAction.setEnabled(true);
            }
        });
    }

    private void showPrivateProfileState() {
        if (reviewsRegistration != null) {
            reviewsRegistration.remove();
            reviewsRegistration = null;
        }
        myExistingReview = null;
        hasEverSwapped = false;
        isSwapAccepted = false;

        binding.cardPrivateProfileNotice.setVisibility(View.VISIBLE);

        // Show user's name and profile picture
        binding.tvOtherUserName.setText(targetUser != null && targetUser.getName() != null ? targetUser.getName() : "Skill Swap Member");
        binding.ivOtherUserAvatar.setPadding(0, 0, 0, 0);
        binding.ivOtherUserAvatar.setImageTintList(null);
        ImageLoaderHelper.loadProfileImage(binding.ivOtherUserAvatar, targetUser != null ? targetUser.getProfilePhotoUrl() : null);

        // Hide headline, location, badge, and ratings
        binding.tvOtherUserHeadline.setVisibility(View.GONE);
        binding.tvSwapStatusBadge.setVisibility(View.GONE);
        binding.tvAiMatchBadge.setVisibility(View.GONE);
        binding.tvUserCoinBalance.setVisibility(View.GONE);
        binding.layoutUserOverallRating.setVisibility(View.GONE);

        // Allow requesting a swap to connect
        binding.layoutActionButtons.setVisibility(View.VISIBLE);
        binding.btnRequestSwapAction.setVisibility(View.VISIBLE);
        binding.btnMessageAction.setVisibility(View.GONE);
        binding.btnEndSwapAction.setVisibility(View.GONE);
        binding.btnRateUserAction.setVisibility(View.GONE);

        // Teach Skills hidden
        binding.tvOtherTeachLabel.setVisibility(View.GONE);
        binding.rvOtherSkillsTeach.setVisibility(View.GONE);
        binding.tvEmptyOtherTeach.setVisibility(View.GONE);

        // Learn Skills hidden
        binding.tvOtherLearnLabel.setVisibility(View.GONE);
        binding.rvOtherSkillsLearn.setVisibility(View.GONE);
        binding.tvEmptyOtherLearn.setVisibility(View.GONE);

        // About hidden
        binding.tvOtherAboutLabel.setVisibility(View.GONE);
        binding.cardOtherAbout.setVisibility(View.GONE);

        // Reviews & Feedback hidden
        binding.tvOtherFeedbackLabel.setVisibility(View.GONE);
        binding.rvUserReviews.setVisibility(View.GONE);
    }

    private void showFullProfile(FirestoreUser user) {
        binding.cardPrivateProfileNotice.setVisibility(View.GONE);
        binding.ivOtherUserAvatar.setPadding(0, 0, 0, 0);
        binding.ivOtherUserAvatar.setImageTintList(null);

        bindUserData(user);
    }

    private void bindUserData(FirestoreUser user) {
        binding.tvOtherUserName.setText(user.getName() != null ? user.getName() : "User");
        binding.tvOtherUserHeadline.setVisibility(View.VISIBLE);

        String location = user.getLocation();
        String availability = user.getAvailability() != null ? user.getAvailability() : "Open to Swaps";
        String headline;
        if (location != null && !location.trim().isEmpty() && !location.equalsIgnoreCase("Earth")) {
            headline = location.trim() + " · " + availability;
        } else {
            headline = availability;
        }
        binding.tvOtherUserHeadline.setText(headline);

        int coins = user.getSkillCoins();
        binding.tvUserCoinBalance.setText("💰 " + coins + " SkillCoins");
        binding.tvUserCoinBalance.setVisibility(View.VISIBLE);

        // Bind Mentor Rank
        String rankTitle = com.example.skillswapproject.util.MentorRankHelper.getRankTitle(coins);
        int badgeColor = com.example.skillswapproject.util.MentorRankHelper.getRankBadgeColor(coins);
        int bgColor = com.example.skillswapproject.util.MentorRankHelper.getRankBgColor(coins);
        binding.tvUserMentorRank.setText("Rank: " + rankTitle);
        binding.tvUserMentorRank.setTextColor(badgeColor);
        binding.tvUserMentorRank.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
        binding.tvUserMentorRank.setVisibility(View.VISIBLE);

        // Bind AI Match Compatibility Badge
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId != null && !currentUserId.equals(user.getUid())) {
            FirestoreRepository.getInstance().getUserProfile(currentUserId).addOnSuccessListener(me -> {
                if (isFinishing() || isDestroyed() || me == null) return;
                com.example.skillswapproject.util.CompatibilityMatcher.MatchResult res =
                        com.example.skillswapproject.util.CompatibilityMatcher.calculateMatch(me, user);

                binding.tvAiMatchBadge.setText(res.getOverallScore() + "% AI Match ✨ · View Breakdown");
                binding.tvAiMatchBadge.setVisibility(View.VISIBLE);
                binding.tvAiMatchBadge.setOnClickListener(v -> {
                    com.example.skillswapproject.ui.dialog.MatchBreakdownBottomSheet sheet =
                            com.example.skillswapproject.ui.dialog.MatchBreakdownBottomSheet.newInstance(
                                    res,
                                    user.getName() != null ? user.getName() : "User"
                            );
                    sheet.show(getSupportFragmentManager(), "MatchBreakdownBottomSheet");
                });
            });
        } else {
            binding.tvAiMatchBadge.setVisibility(View.GONE);
        }

        binding.tvOtherAboutLabel.setVisibility(View.VISIBLE);
        binding.cardOtherAbout.setVisibility(View.VISIBLE);
        binding.tvOtherAboutText.setText(user.getAboutMe() != null && !user.getAboutMe().isEmpty()
                ? user.getAboutMe()
                : "No bio available.");

        ImageLoaderHelper.loadProfileImage(binding.ivOtherUserAvatar, user.getProfilePhotoUrl());

        // Bind Teach Skills
        binding.tvOtherTeachLabel.setVisibility(View.VISIBLE);
        if (user.getOfferedSkills() != null && !user.getOfferedSkills().isEmpty()) {
            binding.tvEmptyOtherTeach.setVisibility(View.GONE);
            binding.rvOtherSkillsTeach.setVisibility(View.VISIBLE);
            List<Skill> list = new ArrayList<>();
            for (String s : user.getOfferedSkills()) {
                list.add(new Skill("s_" + s.hashCode(), s, "General"));
            }
            teachSkillsAdapter.submitList(list);
        } else {
            binding.tvEmptyOtherTeach.setVisibility(View.VISIBLE);
            binding.rvOtherSkillsTeach.setVisibility(View.GONE);
        }

        // Bind Learn Skills
        binding.tvOtherLearnLabel.setVisibility(View.VISIBLE);
        if (user.getWantedSkills() != null && !user.getWantedSkills().isEmpty()) {
            binding.tvEmptyOtherLearn.setVisibility(View.GONE);
            binding.rvOtherSkillsLearn.setVisibility(View.VISIBLE);
            List<Skill> list = new ArrayList<>();
            for (String s : user.getWantedSkills()) {
                list.add(new Skill("w_" + s.hashCode(), s, "General"));
            }
            learnSkillsAdapter.submitList(list);
        } else {
            binding.tvEmptyOtherLearn.setVisibility(View.VISIBLE);
            binding.rvOtherSkillsLearn.setVisibility(View.GONE);
        }
    }

    private void checkSwapStatus() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || targetUserId == null) return;

        // Do not allow swapping with oneself
        if (currentUserId.equals(targetUserId)) {
            binding.layoutActionButtons.setVisibility(View.GONE);
            binding.btnRateUserAction.setVisibility(View.GONE);
            return;
        }

        binding.layoutActionButtons.setVisibility(View.VISIBLE);

        FirestoreRepository.getInstance().checkAcceptedSwap(currentUserId, targetUserId, new FirestoreRepository.OnSwapCheckListener() {
            @Override
            public void onResult(boolean isAccepted) {
                if (isFinishing() || isDestroyed()) return;
                isSwapAccepted = isAccepted;
                if (isAccepted) {
                    binding.tvSwapStatusBadge.setVisibility(View.VISIBLE);
                    binding.tvSwapStatusBadge.setText("Matched Swap Partner");
                    binding.btnMessageAction.setVisibility(View.VISIBLE);
                    binding.btnEndSwapAction.setVisibility(View.VISIBLE);
                    binding.btnEndSwapAction.setEnabled(true);
                    binding.btnRequestSwapAction.setVisibility(View.GONE);
                    updateRateButtonState();
                } else {
                    binding.tvSwapStatusBadge.setVisibility(View.GONE);
                    binding.btnMessageAction.setVisibility(View.GONE);
                    binding.btnEndSwapAction.setVisibility(View.GONE);
                    binding.btnRequestSwapAction.setVisibility(View.VISIBLE);
                    checkPendingSwapStatus(currentUserId);

                    // If not actively accepted, check if they have completed a swap in the past
                    checkPastSwapForRating(currentUserId);
                }
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                checkPastSwapForRating(currentUserId);
            }
        });
    }

    private void checkPastSwapForRating(String currentUserId) {
        FirestoreRepository.getInstance().checkHasCompletedSessionWith(currentUserId, targetUserId, new FirestoreRepository.OnSwapCheckListener() {
            @Override
            public void onResult(boolean hasCompletedSession) {
                if (isFinishing() || isDestroyed()) return;
                hasEverSwapped = hasCompletedSession;
                updateRateButtonState();
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                hasEverSwapped = false;
                updateRateButtonState();
            }
        });
    }

    private void updateRateButtonState() {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.equals(targetUserId)) {
            binding.btnRateUserAction.setVisibility(View.GONE);
            return;
        }

        // Common swap eligibility check for BOTH public and private profiles:
        // View Profile -> Request Swap -> Swap Accepted -> Enable Rating & Feedback
        if (isSwapAccepted || hasEverSwapped) {
            binding.btnRateUserAction.setVisibility(View.VISIBLE);
            if (myExistingReview != null) {
                binding.btnRateUserAction.setText("Edit Rating & Feedback");
                binding.btnRateUserAction.setIconResource(R.drawable.ic_edit);
            } else {
                binding.btnRateUserAction.setText("Rate & Give Feedback");
                binding.btnRateUserAction.setIconResource(R.drawable.ic_star);
            }
        } else {
            binding.btnRateUserAction.setVisibility(View.GONE);
        }
    }

    private void listenToReviews() {
        if (reviewsRegistration != null) {
            reviewsRegistration.remove();
        }

        // If this profile is private and viewer hasn't swapped, do NOT listen or display reviews
        boolean isPrivate = targetUser != null && !targetUser.isPublic();
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (isPrivate && currentUserId != null && !currentUserId.equals(targetUserId) && !isSwapAccepted) {
            binding.layoutUserOverallRating.setVisibility(View.GONE);
            binding.tvOtherFeedbackLabel.setVisibility(View.GONE);
            binding.rvUserReviews.setVisibility(View.GONE);
            binding.btnRateUserAction.setVisibility(View.GONE);
            return;
        }

        reviewsRegistration = FirestoreRepository.getInstance().listenToUserReviews(targetUserId, new FirestoreRepository.OnReviewsLoadedListener() {
            @Override
            public void onReviewsLoaded(List<FirestoreReview> reviews) {
                if (isFinishing() || isDestroyed()) return;

                // Double check privacy guard inside callback
                if (targetUser != null && !targetUser.isPublic() && currentUserId != null && !currentUserId.equals(targetUserId) && !isSwapAccepted) {
                    binding.layoutUserOverallRating.setVisibility(View.GONE);
                    binding.tvOtherFeedbackLabel.setVisibility(View.GONE);
                    binding.rvUserReviews.setVisibility(View.GONE);
                    binding.btnRateUserAction.setVisibility(View.GONE);
                    return;
                }

                myExistingReview = null;

                if (reviews == null || reviews.isEmpty()) {
                    // Do not show 0 rating or 0 feedback!
                    binding.layoutUserOverallRating.setVisibility(View.GONE);
                    binding.tvOtherFeedbackLabel.setVisibility(View.GONE);
                    binding.rvUserReviews.setVisibility(View.GONE);
                    updateRateButtonState();
                    return;
                }

                // Check if current user already reviewed this partner
                if (currentUserId != null) {
                    for (FirestoreReview r : reviews) {
                        if (currentUserId.equals(r.getReviewerId())) {
                            myExistingReview = r;
                            break;
                        }
                    }
                }
                updateRateButtonState();

                // Calculate overall average rating
                float totalRating = 0;
                for (FirestoreReview r : reviews) {
                    totalRating += (float) r.getRating();
                }
                float avgRating = totalRating / reviews.size();

                binding.tvUserOverallRating.setText(String.format(Locale.US, "%.1f", avgRating));
                binding.tvUserReviewsCount.setText("(" + reviews.size() + (reviews.size() == 1 ? " review)" : " reviews)"));
                binding.layoutUserOverallRating.setVisibility(View.VISIBLE);

                // Show feedback section with list
                binding.tvOtherFeedbackLabel.setVisibility(View.VISIBLE);
                binding.rvUserReviews.setVisibility(View.VISIBLE);
                reviewAdapter.submitList(reviews);
            }

            @Override
            public void onError(Exception e) {
                // Keep current state
            }
        });
    }

    private void showRateDialog(FirestoreReview existingReview) {
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || targetUserId == null) return;

        // Prevent self-rating
        if (currentUserId.equals(targetUserId)) {
            Toast.makeText(this, "You cannot rate your own profile", Toast.LENGTH_SHORT).show();
            return;
        }

        // Strict swap partner check: Only users who are swapping or have swapped can rate & give feedback
        if (!isSwapAccepted && !hasEverSwapped) {
            Toast.makeText(this, "You can only rate and give feedback to users you have swapped with.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        DialogAddReviewBinding dialogBinding = DialogAddReviewBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        String partnerName = (targetUser != null && targetUser.getName() != null) ? targetUser.getName() : "Swap Partner";
        boolean isEditing = (existingReview != null);

        if (isEditing) {
            dialogBinding.tvDialogReviewTitle.setText("Edit Rating & Feedback");
            dialogBinding.tvDialogReviewSubtitle.setText("Update your experience swapping skills with " + partnerName);
            dialogBinding.btnSubmitReview.setText("Update");
            dialogBinding.btnDeleteReviewDialog.setVisibility(View.VISIBLE);

            float existingRating = (float) existingReview.getRating();
            if (existingRating < 1) existingRating = 1;
            dialogBinding.ratingBarReview.setRating(existingRating);
            setRatingLabel(dialogBinding, existingRating);

            if (existingReview.getFeedback() != null) {
                dialogBinding.etReviewFeedback.setText(existingReview.getFeedback());
            }
        } else {
            dialogBinding.tvDialogReviewTitle.setText("Rate & Give Feedback");
            dialogBinding.tvDialogReviewSubtitle.setText("Share how your skill swap went with " + partnerName);
            dialogBinding.btnSubmitReview.setText("Submit");
            dialogBinding.btnDeleteReviewDialog.setVisibility(View.GONE);
            dialogBinding.ratingBarReview.setRating(5);
            setRatingLabel(dialogBinding, 5);
        }

        dialogBinding.ratingBarReview.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (rating < 1) {
                ratingBar.setRating(1);
                rating = 1;
            }
            setRatingLabel(dialogBinding, rating);
        });

        dialogBinding.btnCancelReview.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnDeleteReviewDialog.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Rating & Feedback")
                    .setMessage("Are you sure you want to delete your rating and feedback? This cannot be undone.")
                    .setPositiveButton("Delete", (d, which) -> {
                        dialog.dismiss();
                        if (existingReview != null && existingReview.getReviewId() != null) {
                            executeDeleteReview(existingReview.getReviewId(), currentUserId, targetUserId);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialogBinding.btnSubmitReview.setOnClickListener(v -> {
            if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
                return;
            }

            float rating = dialogBinding.ratingBarReview.getRating();
            if (rating < 1.0f || rating > 5.0f) {
                Toast.makeText(this, "Rating must be between 1 and 5 stars.", Toast.LENGTH_SHORT).show();
                return;
            }

            String feedback = dialogBinding.etReviewFeedback.getText() != null
                    ? dialogBinding.etReviewFeedback.getText().toString().trim()
                    : "";

            // Feedback text is optional
            dialogBinding.tilReviewFeedback.setError(null);

            dialogBinding.btnSubmitReview.setEnabled(false);
            dialogBinding.btnSubmitReview.setText(isEditing ? "Updating..." : "Submitting...");

            // Fetch current user details or fallback to FirebaseAuth
            FirestoreRepository.getInstance().getUser(currentUserId).addOnSuccessListener(currUser -> {
                submitReviewInternal(dialog, dialogBinding, currentUserId, currUser, rating, feedback, existingReview);
            }).addOnFailureListener(e -> {
                // Fallback: Proceed even if getUser document fetch failed
                submitReviewInternal(dialog, dialogBinding, currentUserId, null, rating, feedback, existingReview);
            });
        });

        dialog.show();
    }

    private void setRatingLabel(DialogAddReviewBinding dialogBinding, float rating) {
        if (rating >= 5) dialogBinding.tvRatingValueLabel.setText("5.0 - Excellent");
        else if (rating >= 4) dialogBinding.tvRatingValueLabel.setText("4.0 - Very Good");
        else if (rating >= 3) dialogBinding.tvRatingValueLabel.setText("3.0 - Good");
        else if (rating >= 2) dialogBinding.tvRatingValueLabel.setText("2.0 - Fair");
        else dialogBinding.tvRatingValueLabel.setText("1.0 - Needs Improvement");
    }

    private void submitReviewInternal(Dialog dialog, DialogAddReviewBinding dialogBinding,
                                      String currentUserId, FirestoreUser currUser,
                                      float rating, String feedback, FirestoreReview existingReview) {
        String reviewerName = "Swap Partner";
        String reviewerPhoto = "";

        if (currUser != null) {
            if (currUser.getName() != null && !currUser.getName().trim().isEmpty()) {
                reviewerName = currUser.getName().trim();
            }
            if (currUser.getProfilePhotoUrl() != null) {
                reviewerPhoto = currUser.getProfilePhotoUrl();
            }
        } else {
            com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (fbUser != null) {
                if (fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()) {
                    reviewerName = fbUser.getDisplayName().trim();
                }
                if (fbUser.getPhotoUrl() != null) {
                    reviewerPhoto = fbUser.getPhotoUrl().toString();
                }
            }
        }

        String reviewId = (existingReview != null && existingReview.getReviewId() != null)
                ? existingReview.getReviewId()
                : (currentUserId + "_" + targetUserId);

        FirestoreReview review = new FirestoreReview(targetUserId, currentUserId, reviewerName, reviewerPhoto, (double) rating, feedback);
        review.setReviewId(reviewId);

        FirestoreRepository.getInstance().addReview(review).addOnSuccessListener(aVoid -> {
            Toast.makeText(UserProfileActivity.this,
                    existingReview != null ? "Rating & feedback updated!" : "Thank you for your rating & feedback!",
                    Toast.LENGTH_SHORT).show();
            myExistingReview = review;
            updateRateButtonState();
            dialog.dismiss();
        }).addOnFailureListener(e -> {
            dialogBinding.btnSubmitReview.setEnabled(true);
            dialogBinding.btnSubmitReview.setText(existingReview != null ? "Update" : "Submit");
            Toast.makeText(UserProfileActivity.this, "Failed to submit: " + (e != null ? e.getMessage() : "Try again"), Toast.LENGTH_SHORT).show();
        });
    }

    private void confirmDeleteReview(FirestoreReview review) {
        if (review == null || review.getReviewId() == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Rating & Feedback")
                .setMessage("Are you sure you want to delete your rating and feedback? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> executeDeleteReview(review.getReviewId(), review.getReviewerId(), review.getTargetUserId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeDeleteReview(String reviewId, String reviewerId, String targetUserId) {
        FirestoreRepository.getInstance().deleteReview(reviewId, reviewerId, targetUserId).addOnSuccessListener(aVoid -> {
            Toast.makeText(UserProfileActivity.this, "Rating & feedback deleted.", Toast.LENGTH_SHORT).show();
            myExistingReview = null;
            updateRateButtonState();
        }).addOnFailureListener(e -> {
            Toast.makeText(UserProfileActivity.this, "Failed to delete: " + (e != null ? e.getMessage() : "Try again"), Toast.LENGTH_SHORT).show();
        });
    }

    private void openSwapPopup() {
        if (targetUser == null) return;
        ArrayList<String> skills = targetUser.getOfferedSkills() != null
                ? new ArrayList<>(targetUser.getOfferedSkills())
                : new ArrayList<>();

        SwapRequestBottomSheet sheet = SwapRequestBottomSheet.newInstance(
                targetUserId,
                targetUser.getName() != null ? targetUser.getName() : "User",
                targetUser.getProfilePhotoUrl() != null ? targetUser.getProfilePhotoUrl() : "",
                skills
        );

        sheet.setOnSwapSentListener(() -> {
            binding.btnRequestSwapAction.setText("Request Sent");
            binding.btnRequestSwapAction.setEnabled(false);
        });

        sheet.show(getSupportFragmentManager(), "SwapRequestBottomSheet");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reviewsRegistration != null) {
            reviewsRegistration.remove();
            reviewsRegistration = null;
        }
    }
}
