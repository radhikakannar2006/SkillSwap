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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.skillswapproject.data.model.ChatListItem;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.FragmentChatListBinding;
import com.example.skillswapproject.ui.activity.ChatDetailActivity;
import com.example.skillswapproject.ui.adapter.ChatListAdapter;
import com.example.skillswapproject.ui.viewmodel.FirestoreChatListViewModel;

public class ChatListFragment extends Fragment implements ChatListAdapter.OnChatClickListener {

    private FragmentChatListBinding binding;
    private FirestoreChatListViewModel viewModel;
    private ChatListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupViewModel();

        if (binding.bannerNotificationPermission != null) {
            com.example.skillswapproject.util.NotificationBannerHelper.setupBanner(
                    requireActivity(),
                    binding.bannerNotificationPermission.getRoot(),
                    "Get message notifications",
                    "Make sure you know when you have new messages."
            );
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatListAdapter(this);
        binding.rvChatList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChatList.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(FirestoreChatListViewModel.class);

        // Get current user ID from Firebase Auth — the only source of truth
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "You must be logged in to view chats.", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.getChatListLiveData().observe(getViewLifecycleOwner(), chatItems -> {
            if (chatItems == null || chatItems.isEmpty()) {
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
                binding.rvChatList.setVisibility(View.GONE);
            } else {
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.rvChatList.setVisibility(View.VISIBLE);
                adapter.setChatItems(chatItems);
            }
        });

        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.initialize(currentUserId);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
        if (binding != null && binding.bannerNotificationPermission != null && isAdded()) {
            com.example.skillswapproject.util.NotificationBannerHelper.updateBannerVisibility(
                    requireContext(),
                    binding.bannerNotificationPermission.getRoot()
            );
        }
    }

    @Override
    public void onChatClicked(ChatListItem chatItem) {
        if (chatItem == null) return;

        // Always get current user ID from Firebase Auth
        String currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), ChatDetailActivity.class);
        intent.putExtra("EXTRA_CHAT_ID", chatItem.getChatId());
        intent.putExtra("EXTRA_FRIEND_USER_ID", chatItem.getFriendUserId());
        intent.putExtra("EXTRA_FRIEND_NAME", chatItem.getFriendName());
        intent.putExtra("EXTRA_CURRENT_USER_ID", currentUserId);
        startActivity(intent);
    }

    @Override
    public void onProfileClicked(ChatListItem chatItem) {
        if (chatItem == null || chatItem.getFriendUserId() == null || chatItem.getFriendUserId().isEmpty()) return;

        Intent intent = new Intent(requireContext(), com.example.skillswapproject.ui.activity.UserProfileActivity.class);
        intent.putExtra(com.example.skillswapproject.ui.activity.UserProfileActivity.EXTRA_TARGET_USER_ID, chatItem.getFriendUserId());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
