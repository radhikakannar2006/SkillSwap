package com.example.skillswapproject.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillswapproject.data.model.FirestoreSession;
import com.example.skillswapproject.databinding.ItemSessionCardBinding;
import com.example.skillswapproject.util.JitsiMeetHelper;

import java.util.ArrayList;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    public interface OnSessionActionListener {
        void onJoinMeetClick(FirestoreSession session);
        void onViewDetailsClick(FirestoreSession session);
    }

    private final List<FirestoreSession> sessionList = new ArrayList<>();
    private String currentUserId = "";
    private OnSessionActionListener listener;

    public SessionAdapter() {}

    public SessionAdapter(String currentUserId, OnSessionActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnSessionActionListener(OnSessionActionListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<FirestoreSession> list) {
        sessionList.clear();
        if (list != null) {
            sessionList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSessionCardBinding binding = ItemSessionCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new SessionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        holder.bind(sessionList.get(position));
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        private final ItemSessionCardBinding binding;

        public SessionViewHolder(ItemSessionCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FirestoreSession s) {
            binding.tvCardSessionSkill.setText(s.getSkillName() + " Session");

            String partnerRole = "Teacher: " + s.getTeacherName() + "  ·  Learner: " + s.getLearnerName();
            binding.tvCardSessionPartner.setText(partnerRole);

            String timeText = (s.getScheduledDate() != null ? s.getScheduledDate() : "Upcoming") +
                    (s.getStartTime() != null ? " at " + s.getStartTime() : "") +
                    " (" + (s.getDurationMinutes() == 60 ? "1 Hour" : "30 Mins") + " · " +
                    s.getCoinCost() + " " + (s.getCoinCost() == 1 ? "Coin" : "Coins") + ")";
            binding.tvCardSessionTime.setText(timeText);

            // Status chip styling
            String status = s.getStatus();
            String displayStatus = status;
            if (FirestoreSession.STATUS_WAITING_CONFIRMATION.equalsIgnoreCase(status) || FirestoreSession.STATUS_PENDING_CONFIRMATION.equalsIgnoreCase(status)) {
                displayStatus = "Waiting for Confirmation";
                binding.tvCardSessionStatus.setTextColor(Color.parseColor("#9A3412"));
                binding.tvCardSessionStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEDD5")));
            } else if (FirestoreSession.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                displayStatus = "Completed";
                binding.tvCardSessionStatus.setTextColor(Color.parseColor("#166534"));
                binding.tvCardSessionStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
            } else if (FirestoreSession.STATUS_NEEDS_REVIEW.equalsIgnoreCase(status) || FirestoreSession.STATUS_DISPUTED.equalsIgnoreCase(status)) {
                displayStatus = "Needs Review";
                binding.tvCardSessionStatus.setTextColor(Color.parseColor("#991B1B"));
                binding.tvCardSessionStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            } else if (FirestoreSession.STATUS_CANCELLED.equalsIgnoreCase(status)) {
                displayStatus = "Cancelled";
                binding.tvCardSessionStatus.setTextColor(Color.parseColor("#64748B"));
                binding.tvCardSessionStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
            } else if (FirestoreSession.STATUS_IN_PROGRESS.equalsIgnoreCase(status)) {
                displayStatus = "In Progress";
                binding.tvCardSessionStatus.setTextColor(Color.parseColor("#4338CA"));
                binding.tvCardSessionStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E7FF")));
            } else {
                displayStatus = "Scheduled";
                binding.tvCardSessionStatus.setTextColor(Color.parseColor("#1E40AF"));
                binding.tvCardSessionStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
            }
            binding.tvCardSessionStatus.setText(displayStatus);

            binding.btnCardJoinMeet.setOnClickListener(v -> {
                if (listener != null) listener.onJoinMeetClick(s);
                else JitsiMeetHelper.launchJitsiMeet(v.getContext(), s.getMeetLink());
            });

            binding.btnCardViewDetails.setOnClickListener(v -> {
                if (listener != null) listener.onViewDetailsClick(s);
            });
        }
    }
}
