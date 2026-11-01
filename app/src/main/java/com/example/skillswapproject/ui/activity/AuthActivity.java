package com.example.skillswapproject.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.skillswapproject.MainActivity;
import com.example.skillswapproject.databinding.ActivityAuthBinding;
import com.example.skillswapproject.util.GoogleAuthHelper;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Authentication and Cloud Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is already authenticated
        if (mAuth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        setupAvailabilityDropdown();
        setupListeners();
        updateUiMode();
    }

    private void setupAvailabilityDropdown() {
        android.widget.ArrayAdapter<String> availabilityAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, com.example.skillswapproject.util.SkillConstants.AVAILABILITY_OPTIONS);
        binding.etAuthAvailability.setAdapter(availabilityAdapter);
        binding.etAuthAvailability.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.etAuthAvailability.showDropDown();
        });
        binding.etAuthAvailability.setOnClickListener(v -> binding.etAuthAvailability.showDropDown());
    }

    private void setupListeners() {
        binding.tvAuthToggleMode.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            updateUiMode();
        });

        binding.btnAuthSubmit.setOnClickListener(v -> handleAuthSubmission());

        if (binding.btnGoogleAuth != null) {
            binding.btnGoogleAuth.setOnClickListener(v -> launchGoogleSignIn());
        }
    }

    private void updateUiMode() {
        if (isSignUpMode) {
            binding.tilAuthFirstName.setVisibility(View.VISIBLE);
            binding.tilAuthLastName.setVisibility(View.VISIBLE);
            binding.tilAuthPhone.setVisibility(View.VISIBLE);
            binding.tilAuthConfirmPassword.setVisibility(View.VISIBLE);
            binding.tilAuthAvailability.setVisibility(View.VISIBLE);
            binding.tilAuthEmail.setHint("Email Address");
            binding.btnAuthSubmit.setText("Create Account");
            binding.tvAuthToggleMode.setText("Already have an account? Log In");
        } else {
            binding.tilAuthFirstName.setVisibility(View.GONE);
            binding.tilAuthLastName.setVisibility(View.GONE);
            binding.tilAuthPhone.setVisibility(View.GONE);
            binding.tilAuthConfirmPassword.setVisibility(View.GONE);
            binding.tilAuthAvailability.setVisibility(View.GONE);
            binding.tilAuthEmail.setHint("Email Address or Phone Number");
            binding.btnAuthSubmit.setText("Log In");
            binding.tvAuthToggleMode.setText("Need an account? Sign Up");
        }
    }

    private void handleAuthSubmission() {
        binding.tilAuthFirstName.setError(null);
        binding.tilAuthLastName.setError(null);
        binding.tilAuthEmail.setError(null);
        binding.tilAuthPhone.setError(null);
        binding.tilAuthPassword.setError(null);
        binding.tilAuthConfirmPassword.setError(null);

        if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSignUpMode) {
            String firstName = binding.etAuthFirstName.getText() != null ? binding.etAuthFirstName.getText().toString().trim() : "";
            String lastName = binding.etAuthLastName.getText() != null ? binding.etAuthLastName.getText().toString().trim() : "";
            String email = binding.etAuthEmail.getText() != null ? binding.etAuthEmail.getText().toString().trim() : "";
            String phone = binding.etAuthPhone.getText() != null ? binding.etAuthPhone.getText().toString().trim() : "";
            // Do NOT trim password
            String password = binding.etAuthPassword.getText() != null ? binding.etAuthPassword.getText().toString() : "";
            String confirmPassword = binding.etAuthConfirmPassword.getText() != null ? binding.etAuthConfirmPassword.getText().toString() : "";
            String availability = binding.etAuthAvailability.getText() != null ? binding.etAuthAvailability.getText().toString().trim() : "";

            if (firstName.isEmpty()) {
                binding.tilAuthFirstName.setError("Please enter your first name.");
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            String fNameErr = com.example.skillswapproject.util.ValidationUtils.validateName(firstName);
            if (fNameErr != null) {
                binding.tilAuthFirstName.setError(fNameErr);
                Toast.makeText(this, fNameErr, Toast.LENGTH_SHORT).show();
                return;
            }

            if (lastName.isEmpty()) {
                binding.tilAuthLastName.setError("Please enter your last name.");
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            String lNameErr = com.example.skillswapproject.util.ValidationUtils.validateName(lastName);
            if (lNameErr != null) {
                binding.tilAuthLastName.setError(lNameErr);
                Toast.makeText(this, lNameErr, Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.isEmpty()) {
                binding.tilAuthEmail.setError("Please enter your email address.");
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!com.example.skillswapproject.util.ValidationUtils.isValidEmail(email)) {
                binding.tilAuthEmail.setError("Please enter a valid email address.");
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!phone.isEmpty() && !com.example.skillswapproject.util.ValidationUtils.isValidPhone(phone)) {
                binding.tilAuthPhone.setError("Please enter a valid phone number.");
                Toast.makeText(this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show();
                return;
            }

            String pwdErr = com.example.skillswapproject.util.ValidationUtils.validatePassword(password);
            if (pwdErr != null) {
                binding.tilAuthPassword.setError(pwdErr);
                Toast.makeText(this, pwdErr, Toast.LENGTH_SHORT).show();
                return;
            }

            if (confirmPassword.isEmpty()) {
                binding.tilAuthConfirmPassword.setError("Please confirm your password.");
                Toast.makeText(this, "Please confirm your password.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                binding.tilAuthConfirmPassword.setError("Passwords do not match.");
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (availability.isEmpty()) {
                availability = "Morning";
            }

            performSignUp(firstName, lastName, email, phone, password, availability);
        } else {
            // Log In mode
            String emailOrPhone = binding.etAuthEmail.getText() != null ? binding.etAuthEmail.getText().toString().trim() : "";
            // Do NOT trim password
            String password = binding.etAuthPassword.getText() != null ? binding.etAuthPassword.getText().toString() : "";

            if (emailOrPhone.isEmpty()) {
                binding.tilAuthEmail.setError("Please enter your email or phone number.");
                Toast.makeText(this, "Please enter your email or phone number.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (emailOrPhone.contains("@") && !com.example.skillswapproject.util.ValidationUtils.isValidEmail(emailOrPhone)) {
                binding.tilAuthEmail.setError("Please enter a valid email address.");
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                binding.tilAuthPassword.setError("Please enter your password.");
                Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(emailOrPhone, password);
        }
    }

    private void performSignUp(String firstName, String lastName, String email, String phone, String password, String availability) {
        setLoading(true);

        boolean isPhoneOnly = (email == null || email.isEmpty()) && (phone != null && !phone.isEmpty());

        com.example.skillswapproject.data.repository.FirestoreRepository.getInstance()
                .signUpWithEmail(firstName, lastName, email, phone, password, availability)
                .addOnSuccessListener(authResult -> {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, com.example.skillswapproject.util.ValidationUtils.getFriendlyAuthErrorMessage(e, isPhoneOnly), Toast.LENGTH_LONG).show();
                });
    }

    private void performLogin(String emailOrPhone, String password) {
        setLoading(true);

        boolean isPhone = !emailOrPhone.contains("@");

        com.example.skillswapproject.data.repository.FirestoreRepository.getInstance()
                .loginWithEmailOrPhone(emailOrPhone, password)
                .addOnSuccessListener(authResult -> {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, com.example.skillswapproject.util.ValidationUtils.getFriendlyAuthErrorMessage(e, isPhone), Toast.LENGTH_LONG).show();
                });
    }

    private void launchGoogleSignIn() {
        setLoading(true);

        GoogleAuthHelper.signIn(this, new GoogleAuthHelper.GoogleAuthCallback() {
            @Override
            public void onSuccess(@NonNull AuthCredential credential,
                                  @Nullable String displayName,
                                  @Nullable String email,
                                  @Nullable String photoUrl) {
                com.example.skillswapproject.data.repository.FirestoreRepository.getInstance()
                        .signInWithGoogle(credential, displayName, email, photoUrl)
                        .addOnSuccessListener(result -> {
                            setLoading(false);
                            if (result != null && result.isNewUser()) {
                                Toast.makeText(AuthActivity.this, "Welcome to Skill Swap! Please complete your profile.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(AuthActivity.this, EditProfileActivity.class);
                                intent.putExtra("IS_NEW_USER", true);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(AuthActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                navigateToMain();
                            }
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            if (e instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                Toast.makeText(AuthActivity.this, "An account already exists with this email address. Please log in with your email and password.", Toast.LENGTH_LONG).show();
                            } else {
                                String msg = e != null && e.getMessage() != null ? e.getMessage() : "Google sign-in failed.";
                                Toast.makeText(AuthActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onCancelled() {
                setLoading(false);
                Toast.makeText(AuthActivity.this, "Google Sign-In was cancelled.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                setLoading(false);
                Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.pbAuthLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnAuthSubmit.setEnabled(!isLoading);
        binding.tvAuthToggleMode.setEnabled(!isLoading);
        if (binding.btnGoogleAuth != null) {
            binding.btnGoogleAuth.setEnabled(!isLoading);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
