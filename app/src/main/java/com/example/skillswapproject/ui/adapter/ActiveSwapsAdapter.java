package com.example.skillswapproject.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillswapproject.data.model.FirestoreSwap;
import com.example.skillswapproject.databinding.ItemActiveSwapBinding;
import com.example.skillswapproject.util.ImageLoaderHelper;

import java.util.ArrayList;
import java.util.List;

public class ActiveSwapsAdapter extends RecyclerView.Adapter<ActiveSwapsAdapter.ActiveSwapViewHolder> {

    public interface OnActiveSwapActionListener {
        void onProfileClick(String partnerId);
        void onMessageClick(String partnerId, String partnerName, String partnerPhoto);
        void onScheduleClick(FirestoreSwap swap, String partnerName, String partnerId);
        void onEndSwapClick(FirestoreSwap swap, String partnerName, String partnerId);
    }

    private final String currentUserId;
    private final OnActiveSwapActionListener listener;
    private final List<FirestoreSwap> fullList = new ArrayList<>();
    private final List<FirestoreSwap> displayList = new ArrayList<>();
    private String currentQuery = "";

    public ActiveSwapsAdapter(String currentUserId, OnActiveSwapActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setSwaps(List<FirestoreSwap> swaps) {
        fullList.clear();
        if (swaps != null) {
            fullList.addAll(swaps);
        }
        applyFilter();
    }

    public void filter(String query) {
        this.currentQuery = (query != null) ? query.trim().toLowerCase() : "";
        applyFilter();
    }

    private void applyFilter() {
        displayList.clear();
        if (currentQuery.isEmpty()) {
            displayList.addAll(fullList);
        } else {
            for (FirestoreSwap s : fullList) {
                String partnerName = getPartnerName(s);
                String offered = s.getOfferedSkillName() != null ? s.getOfferedSkillName() : "";
                String requested = s.getRequestedSkillName() != null ? s.getRequestedSkillName() : "";

                if (partnerName.toLowerCase().contains(currentQuery)
                        || offered.toLowerCase().contains(currentQuery)
                        || requested.toLowerCase().contains(currentQuery)) {
                    displayList.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return displayList.size();
    }

    public int getTotalCount() {
        return fullList.size();
    }

    private String getPartnerId(FirestoreSwap swap) {
        if (swap == null) return "";
        return currentUserId.equals(swap.getRequesterId()) ? swap.getReceiverId() : swap.getRequesterId();
    }

    private String getPartnerName(FirestoreSwap swap) {
        if (swap == null) return "User";
        String name = currentUserId.equals(swap.getRequesterId()) ? swap.getReceiverName() : swap.getRequesterName();
        return (name != null && !name.isEmpty()) ? name : "User";
    }

    private String getPartnerPhoto(FirestoreSwap swap) {
        if (swap == null) return null;
        return currentUserId.equals(swap.getRequesterId()) ? swap.getReceiverPhoto() : swap.getRequesterPhoto();
    }

    @NonNull
    @Override
    public ActiveSwapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemActiveSwapBinding binding = ItemActiveSwapBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ActiveSwapViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveSwapViewHolder holder, int position) {
        FirestoreSwap swap = displayList.get(position);
        holder.bind(swap);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    class ActiveSwapViewHolder extends RecyclerView.ViewHolder {
        private final ItemActiveSwapBinding binding;

        public ActiveSwapViewHolder(ItemActiveSwapBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FirestoreSwap swap) {
            String partnerId = getPartnerId(swap);
            String partnerName = getPartnerName(swap);
            String partnerPhoto = getPartnerPhoto(swap);

            binding.tvActiveSwapName.setText(partnerName);

            // Construct skills summary
            String skillsDesc;
            if (currentUserId.equals(swap.getRequesterId())) {
                skillsDesc = "Trading: " + swap.getOfferedSkillName() + " ➔ " + swap.getRequestedSkillName();
            } else {
                skillsDesc = "Trading: " + swap.getRequestedSkillName() + " ➔ " + swap.getOfferedSkillName();
            }
            binding.tvActiveSwapSkills.setText(skillsDesc);

            // Load partner avatar
            ImageLoaderHelper.loadProfileImage(binding.ivActiveSwapAvatar, partnerPhoto);

            // Clicking avatar or name opens user profile
            View.OnClickListener profileClick = v -> {
                if (listener != null && partnerId != null && !partnerId.isEmpty()) {
                    listener.onProfileClick(partnerId);
                }
            };
            binding.ivActiveSwapAvatar.setOnClickListener(profileClick);
            binding.layoutPartnerNameInfo.setOnClickListener(profileClick);
            binding.tvActiveSwapName.setOnClickListener(profileClick);

            // Card click opens session details / schedule
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScheduleClick(swap, partnerName, partnerId);
                }
            });

            // Schedule Session button
            binding.btnScheduleSessionCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onScheduleClick(swap, partnerName, partnerId);
                }
            });

            // Message button
            binding.btnMessagePartner.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMessageClick(partnerId, partnerName, partnerPhoto);
                }
            });

            // End Swap button
            binding.btnEndActiveSwap.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEndSwapClick(swap, partnerName, partnerId);
                }
            });
        }
    }
}
