package com.example.skillswapproject.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.skillswapproject.data.model.FirestoreSwap;
import com.example.skillswapproject.data.model.User;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.FragmentProfileBinding;
import com.example.skillswapproject.ui.activity.EditProfileActivity;
import com.example.skillswapproject.ui.activity.LoginActivity;
import com.example.skillswapproject.ui.activity.PendingSwapsActivity;
import com.example.skillswapproject.ui.adapter.SkillChipAdapter;
import com.example.skillswapproject.ui.viewmodel.ProfileViewModel;
import com.example.skillswapproject.util.ImageLoaderHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private SkillChipAdapter teachSkillsAdapter;
    private SkillChipAdapter learnSkillsAdapter;
    private com.example.skillswapproject.ui.adapter.UserReviewAdapter reviewAdapter;
    private ListenerRegistration swapsRegistration;
    private ListenerRegistration reviewsRegistration;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupAdapters();
        setupListeners();
        observeCurrentUserState();
        listenToSwapsCount();
        listenToReviews();
    }

    private void setupAdapters() {
        teachSkillsAdapter = new SkillChipAdapter();
        binding.rvSkillsTeach.setAdapter(teachSkillsAdapter);

        learnSkillsAdapter = new SkillChipAdapter();
        binding.rvSkillsLearn.setAdapter(learnSkillsAdapter);

        reviewAdapter = new com.example.skillswapproject.ui.adapter.UserReviewAdapter();
        binding.rvMyReviews.setAdapter(reviewAdapter);
    }

    private void setupListeners() {
        binding.btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        View.OnClickListener openPendingSwaps = v -> {
            Intent intent = new Intent(requireContext(), PendingSwapsActivity.class);
            startActivity(intent);
        };

        View.OnClickListener openActiveSwaps = v -> {
            Intent intent = new Intent(requireContext(), com.example.skillswapproject.ui.activity.ActiveSwapsActivity.class);
            startActivity(intent);
        };

        binding.btnPendingSwaps.setOnClickListener(openPendingSwaps);
        binding.btnPendingSwapsTop.setOnClickListener(openPendingSwaps);
        binding.layoutStatPendingSwaps.setOnClickListener(openPendingSwaps);
        binding.layoutStatActiveSwaps.setOnClickListener(openActiveSwaps);

        binding.cardSkillCoins.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.skillswapproject.ui.activity.WalletActivity.class);
            startActivity(intent);
        });

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            try {
                com.example.skillswapproject.util.GoogleAuthHelper.clearCredentialState(requireContext(), null);
            } catch (Exception ignored) {}
            try {
                com.google.android.gms.auth.api.signin.GoogleSignInOptions gso =
                        new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build();
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireContext(), gso).signOut();
            } catch (Exception ignored) {}
            com.example.skillswapproject.data.repository.SkillSwapRepository.getInstance().setCurrentUser(null);
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), com.example.skillswapproject.ui.activity.OnboardingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    private void observeCurrentUserState() {
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                bindUserData(user);
            }
        });

        viewModel.getFirestoreUser().observe(getViewLifecycleOwner(), fUser -> {
            if (fUser != null && binding != null) {
                int coins = fUser.getSkillCoins();
                binding.tvProfileSkillCoins.setText(coins + (coins == 1 ? " Coin" : " Coins"));

                String rank = com.example.skillswapproject.util.MentorRankHelper.getRankTitle(coins);
                int badgeColor = com.example.skillswapproject.util.MentorRankHelper.getRankBadgeColor(coins);
                int bgColor = com.example.skillswapproject.util.MentorRankHelper.getRankBgColor(coins);

                binding.tvProfileMentorRankChip.setText(rank);
                binding.tvProfileMentorRankChip.setTextColor(badgeColor);
                binding.tvProfileMentorRankChip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
            }
        });
    }

    private void bindUserData(User user) {
        binding.tvProfileName.setText(user.getName() != null && !user.getName().isEmpty() ? user.getName() : "User");
        
        String location = user.getLocation();
        String availability = user.getAvailability() != null ? user.getAvailability() : "Open to Swaps";
        String headline;
        if (location != null && !location.trim().isEmpty() && !location.equalsIgnoreCase("Earth")) {
            headline = location.trim() + " • " + availability;
        } else {
            headline = availability;
        }
        binding.tvProfileHeadline.setText(headline);

        binding.tvAboutMeText.setText(user.getAboutMe() != null && !user.getAboutMe().isEmpty()
                ? user.getAboutMe()
                : "No bio added yet. Tell others about your skills and interests!");

        ImageLoaderHelper.loadProfileImage(binding.ivProfilePic, user.getProfilePhotoUrl());

        if (user.getOfferedSkills() != null && !user.getOfferedSkills().isEmpty()) {
            binding.tvEmptyTeach.setVisibility(View.GONE);
            binding.rvSkillsTeach.setVisibility(View.VISIBLE);
            teachSkillsAdapter.submitList(user.getOfferedSkills());
        } else {
            binding.tvEmptyTeach.setVisibility(View.VISIBLE);
            binding.rvSkillsTeach.setVisibility(View.GONE);
        }

        if (user.getWantedSkills() != null && !user.getWantedSkills().isEmpty()) {
            binding.tvEmptyLearn.setVisibility(View.GONE);
            binding.rvSkillsLearn.setVisibility(View.VISIBLE);
            learnSkillsAdapter.submitList(user.getWantedSkills());
        } else {
            binding.tvEmptyLearn.setVisibility(View.VISIBLE);
            binding.rvSkillsLearn.setVisibility(View.GONE);
        }
    }

    private void listenToSwapsCount() {
        String currentUid = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUid == null) return;

        swapsRegistration = FirestoreRepository.getInstance().listenToUserSwaps(currentUid, new FirestoreRepository.OnUserSwapsListener() {
            @Override
            public void onSwapsLoaded(List<FirestoreSwap> receivedSwaps, List<FirestoreSwap> sentSwaps) {
                int activeCount = 0;
                int pendingCount = 0;

                if (receivedSwaps != null) {
                    for (FirestoreSwap s : receivedSwaps) {
                        if ("ACCEPTED".equalsIgnoreCase(s.getStatus())) activeCount++;
                        else if ("PENDING".equalsIgnoreCase(s.getStatus())) pendingCount++;
                    }
                }
                if (sentSwaps != null) {
                    for (FirestoreSwap s : sentSwaps) {
                        if ("ACCEPTED".equalsIgnoreCase(s.getStatus())) activeCount++;
                        else if ("PENDING".equalsIgnoreCase(s.getStatus())) pendingCount++;
                    }
                }

                if (binding != null) {
                    binding.tvStatSwapsCount.setText(String.valueOf(activeCount));
                    binding.tvStatPendingCount.setText(String.valueOf(pendingCount));
                }
            }

            @Override
            public void onError(Exception e) {
                // Silently keep default 0
            }
        });
    }

    private void listenToReviews() {
        String currentUid = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUid == null) return;

        reviewsRegistration = FirestoreRepository.getInstance().listenToUserReviews(currentUid, new FirestoreRepository.OnReviewsLoadedListener() {
            @Override
            public void onReviewsLoaded(List<com.example.skillswapproject.data.model.FirestoreReview> reviews) {
                if (binding == null) return;

                if (reviews == null || reviews.isEmpty()) {
                    // Do not show 0 rating or 0 feedback!
                    binding.layoutMyOverallRating.setVisibility(View.GONE);
                    binding.tvMyFeedbackHeader.setVisibility(View.GONE);
                    binding.rvMyReviews.setVisibility(View.GONE);
                    return;
                }

                // Compute overall average rating
                float total = 0;
                for (com.example.skillswapproject.data.model.FirestoreReview r : reviews) {
                    total += (float) r.getRating();
                }
                float avg = total / reviews.size();

                binding.tvMyOverallRating.setText(String.format(java.util.Locale.US, "%.1f", avg));
                binding.tvMyReviewsCount.setText("(" + reviews.size() + (reviews.size() == 1 ? " review)" : " reviews)"));
                binding.layoutMyOverallRating.setVisibility(View.VISIBLE);

                binding.tvMyFeedbackHeader.setVisibility(View.VISIBLE);
                binding.rvMyReviews.setVisibility(View.VISIBLE);
                reviewAdapter.submitList(reviews);
            }

            @Override
            public void onError(Exception e) {
                // Silently keep default
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (swapsRegistration != null) {
            swapsRegistration.remove();
            swapsRegistration = null;
        }
        if (reviewsRegistration != null) {
            reviewsRegistration.remove();
            reviewsRegistration = null;
        }
        binding = null;
    }
}
