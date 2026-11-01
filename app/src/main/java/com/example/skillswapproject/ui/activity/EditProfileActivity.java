package com.example.skillswapproject.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.skillswapproject.data.model.Skill;
import com.example.skillswapproject.databinding.ActivityEditProfileBinding;
import com.example.skillswapproject.ui.viewmodel.ProfileViewModel;
import com.example.skillswapproject.util.ImageLoaderHelper;
import com.example.skillswapproject.util.SkillConstants;
import com.google.android.material.chip.Chip;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private ProfileViewModel viewModel;
    private final List<Skill> teachSkillsList = new ArrayList<>();
    private final List<Skill> learnSkillsList = new SkillList();
    private boolean isPublicProfile = true;
    private String currentPhotoUrl = "";
    private boolean isNewUser = false;

    private static class SkillList extends ArrayList<Skill> {}

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::handleImageSelected);

    private final ActivityResultLauncher<String> photoPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (Boolean.TRUE.equals(isGranted)) {
                    imagePickerLauncher.launch("image/*");
                } else {
                    Toast.makeText(this, "Permission to access photos was denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isNewUser = getIntent().getBooleanExtra("IS_NEW_USER", false);
        if (isNewUser) {
            binding.tvEditTitle.setText("Complete Your Profile");
            binding.btnSaveProfile.setText("Save & Continue");
        }

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void handleImageSelected(Uri uri) {
        if (uri == null) return;
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (original == null) {
                Toast.makeText(this, "Could not load selected image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Scale to thumbnail (max 300x300) to keep Firestore doc lightweight & super fast
            int maxDim = 300;
            int width = original.getWidth();
            int height = original.getHeight();
            float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
            int newWidth = Math.round(ratio * width);
            int newHeight = Math.round(ratio * height);

            Bitmap scaled = Bitmap.createScaledBitmap(original, Math.max(newWidth, 1), Math.max(newHeight, 1), true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            currentPhotoUrl = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            binding.ivEditAvatar.setImageBitmap(scaled);
            binding.btnRemovePhoto.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Photo updated! Click 'Save Changes' to apply.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void observeViewModel() {
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                binding.etEditName.setText(user.getName());
                binding.etEditLocation.setText(user.getLocation());
                binding.etEditAvailability.setText(user.getAvailability(), false);
                if (binding.etEditAbout != null) binding.etEditAbout.setText(user.getAboutMe());
                isPublicProfile = user.isPublic();
                binding.switchProfilePrivacy.setChecked(isPublicProfile);
                updatePrivacyUI(isPublicProfile);

                if (currentPhotoUrl.isEmpty() && user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
                    currentPhotoUrl = user.getProfilePhotoUrl();
                    ImageLoaderHelper.loadProfileImage(binding.ivEditAvatar, currentPhotoUrl);
                    binding.btnRemovePhoto.setVisibility(View.VISIBLE);
                } else if (currentPhotoUrl.isEmpty()) {
                    binding.btnRemovePhoto.setVisibility(View.GONE);
                }

                if (user.getOfferedSkills() != null && teachSkillsList.isEmpty()) {
                    teachSkillsList.clear();
                    teachSkillsList.addAll(user.getOfferedSkills());
                    renderTeachChips();
                }

                if (user.getWantedSkills() != null && learnSkillsList.isEmpty()) {
                    learnSkillsList.clear();
                    learnSkillsList.addAll(user.getWantedSkills());
                    renderLearnChips();
                }
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.btnSaveProfile.setEnabled(!isLoading);
            binding.btnSaveProfile.setText(isLoading ? "Saving..." : "Save Changes");
        });

        viewModel.getSaveSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, isNewUser ? "Welcome to Skill Swap! Profile created." : "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                if (isNewUser) {
                    Intent intent = new Intent(this, com.example.skillswapproject.MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                finish();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updatePrivacyUI(boolean isPublic) {
        if (isPublic) {
            binding.tvPrivacyTitle.setText("Public Profile");
            binding.tvPrivacyDesc.setText("Visible to everyone using the app · Switch to make private");
            binding.ivPrivacyIcon.setImageResource(com.example.skillswapproject.R.drawable.ic_visibility);
            binding.ivPrivacyIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(com.example.skillswapproject.R.color.primary_green)));
        } else {
            binding.tvPrivacyTitle.setText("Private Profile");
            binding.tvPrivacyDesc.setText("Only visible to swap partners · Switch to make public");
            binding.ivPrivacyIcon.setImageResource(com.example.skillswapproject.R.drawable.ic_lock);
            binding.ivPrivacyIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(com.example.skillswapproject.R.color.muted_text)));
        }
    }

    private void setupListeners() {
        binding.btnBackEdit.setOnClickListener(v -> {
            if (isNewUser) {
                Intent intent = new Intent(this, com.example.skillswapproject.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            finish();
        });

        binding.switchProfilePrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isPublicProfile = isChecked;
            updatePrivacyUI(isChecked);
        });

        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(this,
                com.example.skillswapproject.R.layout.dropdown_item_dark, SkillConstants.PREDEFINED_SKILLS);
        binding.etNewTeachSkill.setAdapter(skillAdapter);
        binding.etNewLearnSkill.setAdapter(skillAdapter);

        ArrayAdapter<String> availabilityAdapter = new ArrayAdapter<>(this,
                com.example.skillswapproject.R.layout.dropdown_item_dark, SkillConstants.AVAILABILITY_OPTIONS);
        binding.etEditAvailability.setAdapter(availabilityAdapter);
        binding.etEditAvailability.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.etEditAvailability.showDropDown();
        });
        binding.etEditAvailability.setOnClickListener(v -> binding.etEditAvailability.showDropDown());

        binding.etNewTeachSkill.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.etNewTeachSkill.showDropDown();
        });
        binding.etNewTeachSkill.setOnClickListener(v -> binding.etNewTeachSkill.showDropDown());

        binding.etNewLearnSkill.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.etNewLearnSkill.showDropDown();
        });
        binding.etNewLearnSkill.setOnClickListener(v -> binding.etNewLearnSkill.showDropDown());

        binding.layoutAvatarPicker.setOnClickListener(v -> handleProfilePhotoPicker());

        binding.btnRemovePhoto.setOnClickListener(v -> {
            currentPhotoUrl = "";
            binding.ivEditAvatar.setImageResource(com.example.skillswapproject.R.drawable.ic_profile);
            binding.btnRemovePhoto.setVisibility(android.view.View.GONE);
            Toast.makeText(this, "Profile picture removed. Click 'Save Changes' to apply.", Toast.LENGTH_SHORT).show();
        });

        binding.btnAddTeachSkill.setOnClickListener(v -> {
            String skillName = binding.etNewTeachSkill.getText() != null ? binding.etNewTeachSkill.getText().toString().trim() : "";
            if (skillName.isEmpty()) {
                Toast.makeText(this, "Please enter a skill name.", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Skill s : teachSkillsList) {
                if (s.getName() != null && s.getName().trim().equalsIgnoreCase(skillName)) {
                    Toast.makeText(this, "You have already added this skill.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            teachSkillsList.add(new Skill("s_" + System.currentTimeMillis(), skillName, "General"));
            binding.etNewTeachSkill.setText("");
            renderTeachChips();
            Toast.makeText(this, "Skill added.", Toast.LENGTH_SHORT).show();
        });

        binding.btnAddLearnSkill.setOnClickListener(v -> {
            String skillName = binding.etNewLearnSkill.getText() != null ? binding.etNewLearnSkill.getText().toString().trim() : "";
            if (skillName.isEmpty()) {
                Toast.makeText(this, "Please enter a skill name.", Toast.LENGTH_SHORT).show();
                return;
            }

            for (Skill s : learnSkillsList) {
                if (s.getName() != null && s.getName().trim().equalsIgnoreCase(skillName)) {
                    Toast.makeText(this, "You have already added this skill.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            learnSkillsList.add(new Skill("w_" + System.currentTimeMillis(), skillName, "General"));
            binding.etNewLearnSkill.setText("");
            renderLearnChips();
            Toast.makeText(this, "Skill added.", Toast.LENGTH_SHORT).show();
        });

        binding.btnSaveProfile.setOnClickListener(v -> {
            if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = binding.etEditName.getText() != null ? binding.etEditName.getText().toString().trim() : "";
            String location = binding.etEditLocation.getText() != null ? binding.etEditLocation.getText().toString().trim() : "";
            String availability = binding.etEditAvailability.getText() != null ? binding.etEditAvailability.getText().toString().trim() : "";
            String aboutMe = binding.etEditAbout != null && binding.etEditAbout.getText() != null
                    ? binding.etEditAbout.getText().toString().trim() : "";

            if (name.isEmpty()) {
                binding.tilEditName.setError("Name cannot be empty");
                Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
                return;
            }
            String nameErr = com.example.skillswapproject.util.ValidationUtils.validateName(name);
            if (nameErr != null) {
                binding.tilEditName.setError(nameErr);
                Toast.makeText(this, nameErr, Toast.LENGTH_SHORT).show();
                return;
            }
            binding.tilEditName.setError(null);
            binding.tilEditLocation.setError(null);

            if (teachSkillsList.isEmpty()) {
                Toast.makeText(this, "Please add at least one skill you can offer.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (learnSkillsList.isEmpty()) {
                Toast.makeText(this, "Please add at least one skill you want to learn.", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> offeredNames = new ArrayList<>();
            for (Skill s : teachSkillsList) offeredNames.add(s.getName());

            List<String> wantedNames = new ArrayList<>();
            for (Skill s : learnSkillsList) wantedNames.add(s.getName());

            viewModel.updateProfile(name, location, availability, aboutMe, currentPhotoUrl, offeredNames, wantedNames, isPublicProfile);
        });
    }

    private void renderTeachChips() {
        binding.chipGroupTeach.removeAllViews();
        for (Skill skill : teachSkillsList) {
            Chip chip = new Chip(this);
            chip.setText(skill.getName());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                teachSkillsList.remove(skill);
                renderTeachChips();
                Toast.makeText(this, "Skill removed.", Toast.LENGTH_SHORT).show();
            });
            binding.chipGroupTeach.addView(chip);
        }
    }

    private void renderLearnChips() {
        binding.chipGroupLearn.removeAllViews();
        for (Skill skill : learnSkillsList) {
            Chip chip = new Chip(this);
            chip.setText(skill.getName());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                learnSkillsList.remove(skill);
                renderLearnChips();
                Toast.makeText(this, "Skill removed.", Toast.LENGTH_SHORT).show();
            });
            binding.chipGroupLearn.addView(chip);
        }
    }

    private void handleProfilePhotoPicker() {
        String permission = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                ? android.Manifest.permission.READ_MEDIA_IMAGES
                : android.Manifest.permission.READ_EXTERNAL_STORAGE;

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*");
        } else {
            photoPermissionLauncher.launch(permission);
        }
    }
}
