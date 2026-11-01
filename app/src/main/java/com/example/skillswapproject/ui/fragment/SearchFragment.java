package com.example.skillswapproject.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.skillswapproject.R;
import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.FragmentSearchBinding;
import com.example.skillswapproject.ui.activity.ChatDetailActivity;
import com.example.skillswapproject.ui.adapter.UserSearchAdapter;
import com.example.skillswapproject.ui.viewmodel.SearchViewModel;

import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private UserSearchAdapter adapter;

    public SearchFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public static boolean sAutoFocusKeyboard = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        setupRecyclerView();
        setupSearchBar();
        observeViewModel();

        checkAutoFocusKeyboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAutoFocusKeyboard();
    }

    private void checkAutoFocusKeyboard() {
        if (sAutoFocusKeyboard) {
            sAutoFocusKeyboard = false;
            focusAndShowKeyboard();
        }
    }

    public void focusAndShowKeyboard() {
        if (binding == null || binding.etSearchQuery == null) return;
        binding.etSearchQuery.requestFocus();
        binding.etSearchQuery.postDelayed(() -> {
            if (isAdded() && getActivity() != null && binding != null && binding.etSearchQuery != null) {
                binding.etSearchQuery.requestFocus();
                if (binding.etSearchQuery.getText() != null) {
                    binding.etSearchQuery.setSelection(binding.etSearchQuery.getText().length());
                }
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager) requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(binding.etSearchQuery, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 150);
    }

    private void setupRecyclerView() {
        adapter = new UserSearchAdapter();
        adapter.setOnUserClickListener(this::openUserProfile);
        adapter.setOnMatchBadgeClickListener(this::openMatchBreakdown);
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSearchResults.setAdapter(adapter);
    }

    private void setupSearchBar() {
        binding.etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                viewModel.search(query);

                // Show/hide clear button
                binding.ivClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear button resets search
        binding.ivClearSearch.setOnClickListener(v -> {
            binding.etSearchQuery.setText("");
            binding.etSearchQuery.requestFocus();
        });

        binding.btnBackSearch.setOnClickListener(v -> {
            com.google.android.material.bottomnavigation.BottomNavigationView bnv =
                    requireActivity().findViewById(R.id.bottom_navigation);
            if (bnv != null) {
                bnv.setSelectedItemId(R.id.homeFragment);
            } else {
                androidx.navigation.Navigation.findNavController(v).popBackStack();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressSearch.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), users -> {
            updateUI(users);
        });
    }

    private void updateUI(List<FirestoreUser> users) {
        String query = binding.etSearchQuery.getText() != null
                ? binding.etSearchQuery.getText().toString().trim() : "";

        if (query.isEmpty()) {
            // Show empty state with prompt
            binding.rvSearchResults.setVisibility(View.GONE);
            binding.tvResultsLabel.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyStateMessage.setText("Search for people\nby name, skill, or location");
            return;
        }

        if (users == null || users.isEmpty()) {
            // No results for this query
            binding.rvSearchResults.setVisibility(View.GONE);
            binding.tvResultsLabel.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.tvEmptyStateMessage.setText("No users found for\n\"" + query + "\"");
        } else {
            // Show results
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.rvSearchResults.setVisibility(View.VISIBLE);
            binding.tvResultsLabel.setVisibility(View.VISIBLE);
            binding.tvResultsLabel.setText(users.size() + " member" + (users.size() == 1 ? "" : "s") + " found");
            adapter.submitList(users);
        }
    }

    private void openUserProfile(FirestoreUser targetUser) {
        if (targetUser == null || targetUser.getUid() == null) return;
        Intent intent = new Intent(requireContext(), com.example.skillswapproject.ui.activity.UserProfileActivity.class);
        intent.putExtra(com.example.skillswapproject.ui.activity.UserProfileActivity.EXTRA_TARGET_USER_ID, targetUser.getUid());
        startActivity(intent);
    }

    private void openMatchBreakdown(FirestoreUser targetUser) {
        if (targetUser == null || targetUser.getUid() == null) return;

        String currentUserId = com.example.skillswapproject.data.repository.FirestoreRepository.getInstance().getCurrentUserId();
        com.example.skillswapproject.data.repository.FirestoreRepository.getInstance().getUserProfile(currentUserId).addOnSuccessListener(me -> {
            if (getContext() != null && me != null) {
                com.example.skillswapproject.util.CompatibilityMatcher.MatchResult res =
                        com.example.skillswapproject.util.CompatibilityMatcher.calculateMatch(me, targetUser);

                com.example.skillswapproject.ui.dialog.MatchBreakdownBottomSheet sheet =
                        com.example.skillswapproject.ui.dialog.MatchBreakdownBottomSheet.newInstance(res, targetUser.getName() != null ? targetUser.getName() : "User");
                sheet.show(getChildFragmentManager(), "MatchBreakdownBottomSheet");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
