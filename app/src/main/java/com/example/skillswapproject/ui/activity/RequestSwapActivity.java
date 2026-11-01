package com.example.skillswapproject.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.skillswapproject.data.model.Skill;
import com.example.skillswapproject.data.model.User;
import com.example.skillswapproject.databinding.ActivityRequestSwapBinding;
import com.example.skillswapproject.ui.viewmodel.SwapViewModel;
import java.util.ArrayList;
import java.util.List;

public class RequestSwapActivity extends AppCompatActivity {

    private ActivityRequestSwapBinding binding;
    private SwapViewModel viewModel;
    private String targetUserId;
    private User targetUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRequestSwapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SwapViewModel.class);

        targetUserId = getIntent().getStringExtra("TARGET_USER_ID");
        if (targetUserId == null) targetUserId = "u_ananya";

        loadTargetUser();
        setupListeners();
    }

    private void loadTargetUser() {
        targetUser = viewModel.getTargetUser(targetUserId);
        if (targetUser != null) {
            binding.tvSwapTargetName.setText(targetUser.getName());
            binding.tvSwapTargetRating.setText(String.format(java.util.Locale.US, "%.1f (%d)", targetUser.getRating(), targetUser.getReviewsCount()));

            // Populate what target teaches
            StringBuilder teachStr = new StringBuilder("Teaches: ");
            if (targetUser.getOfferedSkills() != null && !targetUser.getOfferedSkills().isEmpty()) {
                for (int i = 0; i < targetUser.getOfferedSkills().size(); i++) {
                    teachStr.append(targetUser.getOfferedSkills().get(i).getName());
                    if (i < targetUser.getOfferedSkills().size() - 1) teachStr.append(", ");
                }
            } else {
                teachStr.append("General Skills");
            }
            binding.tvTargetTeaches.setText(teachStr.toString());

            // Populate what target wants
            StringBuilder wantStr = new StringBuilder("Wants: ");
            if (targetUser.getWantedSkills() != null && !targetUser.getWantedSkills().isEmpty()) {
                for (int i = 0; i < targetUser.getWantedSkills().size(); i++) {
                    wantStr.append(targetUser.getWantedSkills().get(i).getName());
                    if (i < targetUser.getWantedSkills().size() - 1) wantStr.append(", ");
                }
            } else {
                wantStr.append("General Skills");
            }
            binding.tvTargetWants.setText(wantStr.toString());

            // Populate Learn Spinner from target user's offered skills
            List<String> learnOptions = new ArrayList<>();
            if (targetUser.getOfferedSkills() != null) {
                for (Skill s : targetUser.getOfferedSkills()) {
                    learnOptions.add(s.getName());
                }
            }
            if (learnOptions.isEmpty()) learnOptions.add("Web Design");

            ArrayAdapter<String> learnAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_dropdown_item, learnOptions
            );
            binding.spinnerWantToLearn.setAdapter(learnAdapter);
        }

        // Populate Teach Spinner from current logged-in user's offered skills
        User currentUser = viewModel.getCurrentUser().getValue();
        List<String> teachOptions = new ArrayList<>();
        if (currentUser != null && currentUser.getOfferedSkills() != null) {
            for (Skill s : currentUser.getOfferedSkills()) {
                teachOptions.add(s.getName());
            }
        }
        if (teachOptions.isEmpty()) teachOptions.add("Python");

        ArrayAdapter<String> teachAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, teachOptions
        );
        binding.spinnerCanTeach.setAdapter(teachAdapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        View.OnClickListener profileClickListener = v -> {
            if (targetUserId != null && !targetUserId.isEmpty()) {
                Intent profileIntent = new Intent(RequestSwapActivity.this, UserProfileActivity.class);
                profileIntent.putExtra(UserProfileActivity.EXTRA_TARGET_USER_ID, targetUserId);
                startActivity(profileIntent);
            }
        };
        binding.ivSwapTargetAvatar.setOnClickListener(profileClickListener);
        binding.tvSwapTargetName.setOnClickListener(profileClickListener);

        binding.btnSendRequest.setOnClickListener(v -> {
            if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
                return;
            }

            String currentUserId = com.example.skillswapproject.data.repository.FirestoreRepository.getInstance().getCurrentUserId();
            if (targetUserId != null && targetUserId.equals(currentUserId)) {
                Toast.makeText(this, "You cannot send a swap request to yourself.", Toast.LENGTH_SHORT).show();
                return;
            }

            String learn = binding.spinnerWantToLearn.getSelectedItem() != null ? binding.spinnerWantToLearn.getSelectedItem().toString() : "";
            String teach = binding.spinnerCanTeach.getSelectedItem() != null ? binding.spinnerCanTeach.getSelectedItem().toString() : "";
            String message = binding.etMessage.getText() != null ? binding.etMessage.getText().toString().trim() : "";

            if (learn.isEmpty() || teach.isEmpty()) {
                Toast.makeText(this, "Please select both a skill to offer and a skill to learn.", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.submitSwapRequest(targetUserId, teach, learn, message);

            Intent intent = new Intent(RequestSwapActivity.this, SwapSuccessActivity.class);
            intent.putExtra("TARGET_USER_NAME", targetUser != null ? targetUser.getName() : "User");
            startActivity(intent);
            finish();
        });
    }
}
