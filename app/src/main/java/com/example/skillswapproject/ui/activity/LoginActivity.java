package com.example.skillswapproject.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.skillswapproject.MainActivity;
import com.example.skillswapproject.databinding.ActivityLoginBinding;
import com.example.skillswapproject.ui.viewmodel.AuthViewModel;
import com.example.skillswapproject.util.GoogleAuthHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.AuthCredential;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;
    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupTabs();
        setupAvailabilityDropdown();
        setupListeners();
        observeViewModel();
    }

    private void setupAvailabilityDropdown() {
        android.widget.ArrayAdapter<String> availabilityAdapter = new android.widget.ArrayAdapter<>(this,
                com.example.skillswapproject.R.layout.dropdown_item_dark, com.example.skillswapproject.util.SkillConstants.AVAILABILITY_OPTIONS);
        binding.etSignupAvailability.setAdapter(availabilityAdapter);
        binding.etSignupAvailability.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.etSignupAvailability.showDropDown();
        });
        binding.etSignupAvailability.setOnClickListener(v -> binding.etSignupAvailability.showDropDown());
    }

    private void setupTabs() {
        binding.tabLayoutAuth.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isSignUpMode = (tab.getPosition() == 1);
                updateUiForMode();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateUiForMode() {
        if (isSignUpMode) {
            binding.tilFirstName.setVisibility(View.VISIBLE);
            binding.tilLastName.setVisibility(View.VISIBLE);
            binding.tilPhone.setVisibility(View.VISIBLE);
            binding.tilConfirmPassword.setVisibility(View.VISIBLE);
            binding.tilSignupAvailability.setVisibility(View.VISIBLE);
            binding.tilEmail.setHint("Email Address");
            binding.tilPhone.setHint("Phone Number (Optional)");
            binding.btnAuthSubmit.setText("Create Account");
            binding.tvToggleMode.setText("Already have an account? Log In");
        } else {
            binding.tilFirstName.setVisibility(View.GONE);
            binding.tilLastName.setVisibility(View.GONE);
            binding.tilPhone.setVisibility(View.GONE);
            binding.tilConfirmPassword.setVisibility(View.GONE);
            binding.tilSignupAvailability.setVisibility(View.GONE);
            binding.tilEmail.setHint("Email Address or Phone Number");
            binding.btnAuthSubmit.setText("Log In");
            binding.tvToggleMode.setText("Don't have an account? Sign Up");
        }
    }

    private void setupListeners() {
        // Toggle text link
        binding.tvToggleMode.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            TabLayout.Tab tab = binding.tabLayoutAuth.getTabAt(isSignUpMode ? 1 : 0);
            if (tab != null) tab.select();
        });

        // Main submit button
        binding.btnAuthSubmit.setOnClickListener(v -> handleAuthSubmission());

        // Google OAuth Sign-In / Sign-Up
        if (binding.btnGoogleOauth != null) {
            binding.btnGoogleOauth.setOnClickListener(v -> launchGoogleSignIn());
        }
        if (binding.btnAppleOauth != null) {
            binding.btnAppleOauth.setOnClickListener(v ->
                Toast.makeText(this, "Apple Sign-In coming soon!", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void handleAuthSubmission() {
        // Clear previous inline errors
        binding.tilFirstName.setError(null);
        binding.tilLastName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPhone.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSignUpMode) {
            String firstName = binding.etFirstName.getText() != null ? binding.etFirstName.getText().toString().trim() : "";
            String lastName = binding.etLastName.getText() != null ? binding.etLastName.getText().toString().trim() : "";
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
            String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";
            // Do NOT trim the password before authentication
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
            String confirmPassword = binding.etConfirmPassword.getText() != null ? binding.etConfirmPassword.getText().toString() : "";
            String availability = binding.etSignupAvailability.getText() != null ? binding.etSignupAvailability.getText().toString().trim() : "";

            if (firstName.isEmpty()) {
                binding.tilFirstName.setError("Please enter your first name.");
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            String fNameErr = com.example.skillswapproject.util.ValidationUtils.validateName(firstName);
            if (fNameErr != null) {
                binding.tilFirstName.setError(fNameErr);
                Toast.makeText(this, fNameErr, Toast.LENGTH_SHORT).show();
                return;
            }

            if (lastName.isEmpty()) {
                binding.tilLastName.setError("Please enter your last name.");
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            String lNameErr = com.example.skillswapproject.util.ValidationUtils.validateName(lastName);
            if (lNameErr != null) {
                binding.tilLastName.setError(lNameErr);
                Toast.makeText(this, lNameErr, Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.isEmpty()) {
                binding.tilEmail.setError("Please enter your email address.");
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!com.example.skillswapproject.util.ValidationUtils.isValidEmail(email)) {
                binding.tilEmail.setError("Please enter a valid email address.");
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!phone.isEmpty() && !com.example.skillswapproject.util.ValidationUtils.isValidPhone(phone)) {
                binding.tilPhone.setError("Please enter a valid phone number.");
                Toast.makeText(this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show();
                return;
            }

            String pwdErr = com.example.skillswapproject.util.ValidationUtils.validatePassword(password);
            if (pwdErr != null) {
                binding.tilPassword.setError(pwdErr);
                Toast.makeText(this, pwdErr, Toast.LENGTH_SHORT).show();
                return;
            }

            if (confirmPassword.isEmpty()) {
                binding.tilConfirmPassword.setError("Please confirm your password.");
                Toast.makeText(this, "Please confirm your password.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                binding.tilConfirmPassword.setError("Passwords do not match.");
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (availability.isEmpty()) {
                availability = "Morning";
            }

            viewModel.signUp(firstName, lastName, email, phone, password, availability);
        } else {
            // Log In mode
            String emailOrPhone = binding.etEmail.getText() != null
                    ? binding.etEmail.getText().toString().trim() : "";
            // Do NOT trim the password before authentication
            String password = binding.etPassword.getText() != null
                    ? binding.etPassword.getText().toString() : "";

            if (emailOrPhone.isEmpty()) {
                binding.tilEmail.setError("Please enter your email or phone number.");
                Toast.makeText(this, "Please enter your email or phone number.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (emailOrPhone.contains("@") && !com.example.skillswapproject.util.ValidationUtils.isValidEmail(emailOrPhone)) {
                binding.tilEmail.setError("Please enter a valid email address.");
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                binding.tilPassword.setError("Please enter your password.");
                Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.login(emailOrPhone, password);
        }
    }

    private void observeViewModel() {
        viewModel.getAuthSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, isSignUpMode ? "Account created successfully!" : "Login successful!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        });

        viewModel.getGoogleAuthSuccess().observe(this, result -> {
            if (result != null) {
                if (result.isNewUser()) {
                    Toast.makeText(this, "Welcome to Skill Swap! Please complete your profile.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, EditProfileActivity.class);
                    intent.putExtra("IS_NEW_USER", true);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                }
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.btnAuthSubmit.setEnabled(!isLoading);
            binding.tvToggleMode.setEnabled(!isLoading);
            if (binding.btnGoogleOauth != null) {
                binding.btnGoogleOauth.setEnabled(!isLoading);
            }
            binding.btnAuthSubmit.setText(isLoading ? "Please wait..." : (isSignUpMode ? "Create Account" : "Log In"));
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void launchGoogleSignIn() {
        binding.btnAuthSubmit.setEnabled(false);
        if (binding.btnGoogleOauth != null) {
            binding.btnGoogleOauth.setEnabled(false);
        }

        GoogleAuthHelper.signIn(this, new GoogleAuthHelper.GoogleAuthCallback() {
            @Override
            public void onSuccess(@NonNull AuthCredential credential,
                                  @Nullable String displayName,
                                  @Nullable String email,
                                  @Nullable String photoUrl) {
                viewModel.loginWithGoogle(credential, displayName, email, photoUrl);
            }

            @Override
            public void onCancelled() {
                binding.btnAuthSubmit.setEnabled(true);
                if (binding.btnGoogleOauth != null) {
                    binding.btnGoogleOauth.setEnabled(true);
                }
                Toast.makeText(LoginActivity.this, "Google Sign-In was cancelled.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                binding.btnAuthSubmit.setEnabled(true);
                if (binding.btnGoogleOauth != null) {
                    binding.btnGoogleOauth.setEnabled(true);
                }
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
