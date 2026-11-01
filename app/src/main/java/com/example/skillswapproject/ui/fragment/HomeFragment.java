package com.example.skillswapproject.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.skillswapproject.R;
import com.example.skillswapproject.data.model.Skill;
import com.example.skillswapproject.data.model.User;
import com.example.skillswapproject.databinding.FragmentHomeBinding;
import com.example.skillswapproject.ui.activity.PendingSwapsActivity;
import com.example.skillswapproject.ui.activity.UserProfileActivity;
import com.example.skillswapproject.ui.adapter.RecommendedUserAdapter;
import com.example.skillswapproject.ui.adapter.SkillChipAdapter;
import com.example.skillswapproject.ui.dialog.SwapRequestBottomSheet;
import com.example.skillswapproject.ui.viewmodel.HomeViewModel;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private SkillChipAdapter popularSkillsAdapter;
    private RecommendedUserAdapter recommendedUserAdapter;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private com.google.firebase.firestore.ListenerRegistration profileRegistration;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupAdapters();
        setupSearchBarClick();
        setupSeeAllClick();
        setupNotificationsClick();
        setupWalletClick();
        observeViewModel();
        setupWalletBalanceListener();
    }

    private void setupWalletClick() {
        binding.btnHomeWallet.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.skillswapproject.ui.activity.WalletActivity.class);
            startActivity(intent);
        });
    }

    private void setupWalletBalanceListener() {
        String uid = com.example.skillswapproject.data.repository.FirestoreRepository.getInstance().getCurrentUserId();
        if (uid == null) return;

        profileRegistration = com.example.skillswapproject.data.repository.FirestoreRepository.getInstance()
                .listenToUserProfile(uid, new com.example.skillswapproject.data.repository.FirestoreRepository.OnUserProfileListener() {
                    @Override
                    public void onUserProfileLoaded(com.example.skillswapproject.data.model.FirestoreUser user) {
                        if (user != null && binding != null) {
                            binding.tvHomeCoinCount.setText(String.valueOf(user.getSkillCoins()));
                        }
                    }

                    @Override
                    public void onError(Exception e) {}
                });
    }

    private void setupAdapters() {
        popularSkillsAdapter = new SkillChipAdapter();
        popularSkillsAdapter.setOnSkillClickListener(skill -> {
            String skillName = skill.getName() != null ? skill.getName() : "All";
            popularSkillsAdapter.setSelectedSkill(skillName);
            viewModel.filterBySkill(skillName);
        });
        binding.rvPopularSkills.setAdapter(popularSkillsAdapter);

        recommendedUserAdapter = new RecommendedUserAdapter();

        // Tapping card opens User Profile screen (not chat directly)
        recommendedUserAdapter.setOnUserClickListener(user -> openUserProfile(user));

        // Tapping Swap button opens Swap Request Bottom Sheet popup
        recommendedUserAdapter.setOnUserSwapClickListener(user -> openSwapPopup(user));

        // Tapping AI Match Badge opens AI Match Breakdown bottom sheet
        recommendedUserAdapter.setOnMatchBadgeClickListener(user -> openMatchBreakdown(user));

        binding.rvRecommendedUsers.setAdapter(recommendedUserAdapter);
    }

    private void setupSearchBarClick() {
        binding.cardSearchBox.setOnClickListener(v -> navigateToSearch(v));
    }

    private void setupSeeAllClick() {
        binding.tvSeeAllSkills.setOnClickListener(v -> openAllSkillsSheet());
    }

    private void openAllSkillsSheet() {
        String selected = viewModel.getSelectedSkill();
        com.example.skillswapproject.ui.dialog.AllSkillsBottomSheet sheet =
                com.example.skillswapproject.ui.dialog.AllSkillsBottomSheet.newInstance(selected);
        sheet.setOnSkillSelectedListener(skillName -> {
            popularSkillsAdapter.setSelectedSkill(skillName);
            viewModel.filterBySkill(skillName);
            binding.rvPopularSkills.smoothScrollToPosition(0);
        });
        sheet.show(getChildFragmentManager(), "AllSkillsBottomSheet");
    }

    private void navigateToSearch(View v) {
        com.example.skillswapproject.ui.fragment.SearchFragment.sAutoFocusKeyboard = true;
        com.google.android.material.bottomnavigation.BottomNavigationView bnv =
                requireActivity().findViewById(R.id.bottom_navigation);
        if (bnv != null) {
            bnv.setSelectedItemId(R.id.searchFragment);
        } else {
            Navigation.findNavController(v).navigate(R.id.searchFragment);
        }
    }

    private void setupNotificationsClick() {
        View.OnClickListener openNotifications = v -> {
            if (viewModel != null) {
                viewModel.markNotificationsSeen();
            }
            binding.tvNotificationBadge.setVisibility(View.GONE);
            Intent intent = new Intent(requireContext(), PendingSwapsActivity.class);
            startActivity(intent);
        };
        binding.ivNotifications.setOnClickListener(openNotifications);
        binding.layoutNotificationsBell.setOnClickListener(openNotifications);
        binding.tvNotificationBadge.setOnClickListener(openNotifications);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshNotificationBadge();
        }
    }

    private void observeViewModel() {
        viewModel.getPopularSkills().observe(getViewLifecycleOwner(), skills ->
            popularSkillsAdapter.submitList(skills)
        );

        viewModel.getRecommendedUsers().observe(getViewLifecycleOwner(), users ->
            recommendedUserAdapter.submitList(users)
        );

        viewModel.getPendingSwapsCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                binding.tvNotificationBadge.setVisibility(View.VISIBLE);
                binding.tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                binding.tvNotificationBadge.setVisibility(View.GONE);
            }
        });
    }

    private void openUserProfile(User user) {
        if (user == null || user.getId() == null) return;
        Intent intent = new Intent(requireContext(), UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_TARGET_USER_ID, user.getId());
        startActivity(intent);
    }

    private void openSwapPopup(User user) {
        if (user == null || user.getId() == null) return;

        ArrayList<String> targetTeachSkills = new ArrayList<>();
        if (user.getOfferedSkills() != null) {
            for (Skill s : user.getOfferedSkills()) {
                targetTeachSkills.add(s.getName());
            }
        }

        SwapRequestBottomSheet sheet = SwapRequestBottomSheet.newInstance(
                user.getId(),
                user.getName() != null ? user.getName() : "User",
                user.getProfilePhotoUrl() != null ? user.getProfilePhotoUrl() : "",
                targetTeachSkills
        );

        sheet.show(getParentFragmentManager(), "SwapRequestBottomSheet");
    }

    private void openMatchBreakdown(User user) {
        if (user == null || user.getId() == null) return;

        String currentUserId = com.example.skillswapproject.data.repository.FirestoreRepository.getInstance().getCurrentUserId();
        com.example.skillswapproject.data.repository.FirestoreRepository.getInstance().getUserProfile(currentUserId).addOnSuccessListener(me -> {
            com.example.skillswapproject.data.repository.FirestoreRepository.getInstance().getUserProfile(user.getId()).addOnSuccessListener(target -> {
                if (getContext() != null && me != null && target != null) {
                    com.example.skillswapproject.util.CompatibilityMatcher.MatchResult res =
                            com.example.skillswapproject.util.CompatibilityMatcher.calculateMatch(me, target);

                    com.example.skillswapproject.ui.dialog.MatchBreakdownBottomSheet sheet =
                            com.example.skillswapproject.ui.dialog.MatchBreakdownBottomSheet.newInstance(res, target.getName() != null ? target.getName() : "User");
                    sheet.show(getChildFragmentManager(), "MatchBreakdownBottomSheet");
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (profileRegistration != null) {
            profileRegistration.remove();
            profileRegistration = null;
        }
        binding = null;
    }
}
