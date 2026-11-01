package com.example.skillswapproject.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.skillswapproject.data.model.FirestoreSwap;
import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.BottomSheetSwapRequestBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class SwapRequestBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TARGET_ID = "TARGET_ID";
    private static final String ARG_TARGET_NAME = "TARGET_NAME";
    private static final String ARG_TARGET_PHOTO = "TARGET_PHOTO";
    private static final String ARG_TARGET_SKILLS = "TARGET_SKILLS";

    public interface OnSwapSentListener {
        void onSwapSent();
    }

    private BottomSheetSwapRequestBinding binding;
    private OnSwapSentListener listener;

    private String targetUserId;
    private String targetUserName;
    private String targetUserPhoto;
    private ArrayList<String> targetSkills;

    private String currentUserId;
    private String currentUserName = "User";
    private String currentUserPhoto = "";
    private final List<String> myOfferedSkills = new ArrayList<>();

    private String selectedOfferedSkill = "";
    private String selectedRequestedSkill = "";

    public static SwapRequestBottomSheet newInstance(String targetId, String targetName, String targetPhoto, ArrayList<String> targetSkills) {
        SwapRequestBottomSheet fragment = new SwapRequestBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TARGET_ID, targetId);
        args.putString(ARG_TARGET_NAME, targetName);
        args.putString(ARG_TARGET_PHOTO, targetPhoto);
        args.putStringArrayList(ARG_TARGET_SKILLS, targetSkills);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnSwapSentListener(OnSwapSentListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUserId = getArguments().getString(ARG_TARGET_ID);
            targetUserName = getArguments().getString(ARG_TARGET_NAME, "User");
            targetUserPhoto = getArguments().getString(ARG_TARGET_PHOTO, "");
            targetSkills = getArguments().getStringArrayList(ARG_TARGET_SKILLS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSwapRequestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private String selectedSkillLevel = "Beginner";
    private int selectedCoinCost = 1;
    private int myAvailableCoins = 5;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tvSwapTitle.setText("Swap Skills with " + targetUserName);

        setupTargetSkillsChips();
        setupSkillLevelChips();
        loadCurrentUserData();
        setupSubmitButton();
    }

    private void setupSkillLevelChips() {
        binding.chipGroupSkillLevel.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(binding.chipLevelAdvanced.getId())) {
                selectedSkillLevel = "Advanced";
                selectedCoinCost = 3;
            } else if (checkedIds.contains(binding.chipLevelIntermediate.getId())) {
                selectedSkillLevel = "Intermediate";
                selectedCoinCost = 2;
            } else {
                selectedSkillLevel = "Beginner";
                selectedCoinCost = 1;
            }
            updateCoinPricingUi();
        });
    }

    private void updateCoinPricingUi() {
        if (binding == null) return;
        binding.tvWalletBalanceStatus.setText(
                "Your Balance: " + myAvailableCoins + " " + (myAvailableCoins == 1 ? "Coin" : "Coins") +
                " (Cost: " + selectedCoinCost + " " + (selectedCoinCost == 1 ? "Coin" : "Coins") + ")"
        );

        if (myAvailableCoins < selectedCoinCost) {
            binding.tvInsufficientCoinsWarning.setVisibility(View.VISIBLE);
            binding.tvInsufficientCoinsWarning.setText("You don't have enough Skill Coins. Teach a skill to earn more coins.");
            binding.layoutWalletBalanceStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEE2E2"))
            );
            binding.btnSendSwapRequest.setEnabled(false);
            binding.btnSendSwapRequest.setAlpha(0.6f);
        } else {
            binding.tvInsufficientCoinsWarning.setVisibility(View.GONE);
            binding.layoutWalletBalanceStatus.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F0FDF4"))
            );
            binding.btnSendSwapRequest.setEnabled(true);
            binding.btnSendSwapRequest.setAlpha(1.0f);
        }
    }

    private void setupTargetSkillsChips() {
        binding.chipGroupTargetSkills.removeAllViews();
        if (targetSkills == null || targetSkills.isEmpty()) {
            binding.tvNoTargetSkills.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvNoTargetSkills.setVisibility(View.GONE);
        for (String skill : targetSkills) {
            Chip chip = new Chip(requireContext());
            chip.setText(skill);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedRequestedSkill = skill;
                } else if (skill.equals(selectedRequestedSkill)) {
                    selectedRequestedSkill = "";
                }
            });
            binding.chipGroupTargetSkills.addView(chip);
        }
    }

    private void loadCurrentUserData() {
        currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null) return;

        FirestoreRepository.getInstance().getUser(currentUserId).addOnSuccessListener(fUser -> {
            if (fUser != null && getContext() != null && binding != null) {
                currentUserName = fUser.getName() != null ? fUser.getName() : "User";
                currentUserPhoto = fUser.getProfilePhotoUrl() != null ? fUser.getProfilePhotoUrl() : "";
                myAvailableCoins = fUser.getSkillCoins();

                myOfferedSkills.clear();
                if (fUser.getOfferedSkills() != null) {
                    myOfferedSkills.addAll(fUser.getOfferedSkills());
                }

                populateMyOfferedChips();
                updateCoinPricingUi();
            }
        });
    }

    private void populateMyOfferedChips() {
        binding.chipGroupMyOffered.removeAllViews();
        if (myOfferedSkills.isEmpty()) {
            binding.tvNoOfferedSkills.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvNoOfferedSkills.setVisibility(View.GONE);
        for (String skill : myOfferedSkills) {
            Chip chip = new Chip(requireContext());
            chip.setText(skill);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedOfferedSkill = skill;
                } else if (skill.equals(selectedOfferedSkill)) {
                    selectedOfferedSkill = "";
                }
            });
            binding.chipGroupMyOffered.addView(chip);
        }
    }

    private void setupSubmitButton() {
        binding.btnSendSwapRequest.setOnClickListener(v -> {
            if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(getContext())) {
                Toast.makeText(requireContext(), "Please check your internet connection.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUserId == null) {
                Toast.makeText(requireContext(), "You must be logged in to send a swap request.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (targetUserId != null && targetUserId.equals(currentUserId)) {
                Toast.makeText(requireContext(), "You cannot send a swap request to yourself.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedOfferedSkill.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a skill you will offer.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedRequestedSkill.isEmpty()) {
                if (targetSkills != null && !targetSkills.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select a skill you want to learn.", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    selectedRequestedSkill = "General Knowledge";
                }
            }

            if (myAvailableCoins < selectedCoinCost) {
                Toast.makeText(requireContext(), "You don't have enough Skill Coins. Teach a skill to earn more coins.", Toast.LENGTH_LONG).show();
                return;
            }

            binding.btnSendSwapRequest.setEnabled(false);
            binding.btnSendSwapRequest.setText("Sending Request...");

            FirestoreSwap swap = new FirestoreSwap();
            swap.setRequesterId(currentUserId);
            swap.setRequesterName(currentUserName);
            swap.setRequesterPhoto(currentUserPhoto);
            swap.setReceiverId(targetUserId);
            swap.setReceiverName(targetUserName);
            swap.setReceiverPhoto(targetUserPhoto);
            swap.setOfferedSkillId(selectedOfferedSkill);
            swap.setOfferedSkillName(selectedOfferedSkill);
            swap.setRequestedSkillId(selectedRequestedSkill);
            swap.setRequestedSkillName(selectedRequestedSkill);
            swap.setSkillLevel(selectedSkillLevel);
            swap.setCoinCost(selectedCoinCost);
            swap.setStatus("PENDING");

            FirestoreRepository.getInstance().createSwap(swap)
                    .addOnSuccessListener(ref -> {
                        if (getContext() != null) {
                            android.content.Intent intent = new android.content.Intent(requireContext(), com.example.skillswapproject.ui.activity.SwapSuccessActivity.class);
                            intent.putExtra("TARGET_USER_NAME", targetUserName);
                            startActivity(intent);
                        }
                        if (listener != null) listener.onSwapSent();
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        if (binding != null) {
                            binding.btnSendSwapRequest.setEnabled(true);
                            binding.btnSendSwapRequest.setText("Send Swap Request");
                        }
                        String msg = (e != null && e.getMessage() != null && !e.getMessage().isEmpty())
                                ? e.getMessage()
                                : "Failed to send swap request. Please try again.";
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
