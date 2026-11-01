package com.example.skillswapproject.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends AndroidViewModel {

    private final FirestoreRepository firestoreRepository;

    private final MutableLiveData<Boolean> authSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<FirestoreRepository.GoogleAuthResult> googleAuthSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.firestoreRepository = FirestoreRepository.getInstance();
    }

    public LiveData<Boolean> getAuthSuccess() {
        return authSuccessLiveData;
    }

    public LiveData<FirestoreRepository.GoogleAuthResult> getGoogleAuthSuccess() {
        return googleAuthSuccessLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return errorLiveData;
    }

    /**
     * Returns true ONLY if Firebase Auth has a valid current user session.
     * No local fallback.
     */
    public boolean isUserLoggedIn() {
        return firestoreRepository.isUserLoggedIn();
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return firestoreRepository.getCurrentFirebaseUser();
    }

    /**
     * Log in using Firebase Authentication (Email or Phone).
     */
    public void login(String emailOrPhone, String password) {
        if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(getApplication())) {
            errorLiveData.setValue("Please check your internet connection.");
            return;
        }

        if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) {
            errorLiveData.setValue("Please enter your email or phone number.");
            return;
        }
        if (password == null || password.isEmpty()) {
            errorLiveData.setValue("Please enter your password.");
            return;
        }

        boolean isPhone = !emailOrPhone.contains("@");
        if (!isPhone && !com.example.skillswapproject.util.ValidationUtils.isValidEmail(emailOrPhone)) {
            errorLiveData.setValue("Please enter a valid email address.");
            return;
        }

        isLoadingLiveData.setValue(true);

        firestoreRepository.loginWithEmailOrPhone(emailOrPhone.trim(), password)
                .addOnSuccessListener(authResult -> {
                    isLoadingLiveData.postValue(false);
                    authSuccessLiveData.postValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoadingLiveData.postValue(false);
                    errorLiveData.postValue(com.example.skillswapproject.util.ValidationUtils.getFriendlyAuthErrorMessage(e, isPhone));
                });
    }

    /**
     * Sign up using Firebase Authentication + Firestore Users collection.
     */
    public void signUp(String name, String email, String password) {
        signUp("", "", email, "", password, "Morning");
    }

    public void signUp(String firstName, String lastName, String email, String phone, String password, String availability) {
        if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(getApplication())) {
            errorLiveData.setValue("Please check your internet connection.");
            return;
        }

        if (firstName == null || firstName.trim().isEmpty() || lastName == null || lastName.trim().isEmpty()) {
            errorLiveData.setValue("Please fill in all required fields.");
            return;
        }

        String fNameErr = com.example.skillswapproject.util.ValidationUtils.validateName(firstName);
        if (fNameErr != null) {
            errorLiveData.setValue(fNameErr);
            return;
        }

        String lNameErr = com.example.skillswapproject.util.ValidationUtils.validateName(lastName);
        if (lNameErr != null) {
            errorLiveData.setValue(lNameErr);
            return;
        }

        boolean hasEmail = email != null && !email.trim().isEmpty();
        boolean hasPhone = phone != null && !phone.trim().isEmpty();
        if (!hasEmail && !hasPhone) {
            errorLiveData.setValue("Please fill in all required fields.");
            return;
        }

        if (hasEmail && !com.example.skillswapproject.util.ValidationUtils.isValidEmail(email)) {
            errorLiveData.setValue("Please enter a valid email address.");
            return;
        }

        if (hasPhone && !com.example.skillswapproject.util.ValidationUtils.isValidPhone(phone)) {
            errorLiveData.setValue("Please enter a valid phone number.");
            return;
        }

        String pwdErr = com.example.skillswapproject.util.ValidationUtils.validatePassword(password);
        if (pwdErr != null) {
            errorLiveData.setValue(pwdErr);
            return;
        }

        isLoadingLiveData.setValue(true);

        boolean isPhoneOnly = !hasEmail && hasPhone;

        firestoreRepository.signUpWithEmail(firstName.trim(), lastName.trim(),
                hasEmail ? email.trim() : "",
                hasPhone ? phone.trim() : "",
                password,
                availability)
                .addOnSuccessListener(authResult -> {
                    isLoadingLiveData.postValue(false);
                    authSuccessLiveData.postValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoadingLiveData.postValue(false);
                    errorLiveData.postValue(com.example.skillswapproject.util.ValidationUtils.getFriendlyAuthErrorMessage(e, isPhoneOnly));
                });
    }

    /**
     * Sign in / Sign up with Google OAuth credential via Android Credential Manager.
     */
    public void loginWithGoogle(com.google.firebase.auth.AuthCredential credential, String displayName, String email, String photoUrl) {
        if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(getApplication())) {
            errorLiveData.setValue("Please check your internet connection.");
            return;
        }

        if (credential == null) {
            errorLiveData.setValue("Google credential was empty. Please try again.");
            return;
        }

        isLoadingLiveData.setValue(true);

        firestoreRepository.signInWithGoogle(credential, displayName, email, photoUrl)
                .addOnSuccessListener(result -> {
                    isLoadingLiveData.postValue(false);
                    googleAuthSuccessLiveData.postValue(result);
                })
                .addOnFailureListener(e -> {
                    isLoadingLiveData.postValue(false);
                    errorLiveData.postValue(com.example.skillswapproject.util.ValidationUtils.getFriendlyAuthErrorMessage(e, false));
                });
    }

    public void loginWithGoogle(com.google.firebase.auth.AuthCredential credential, com.google.android.gms.auth.api.signin.GoogleSignInAccount account) {
        String name = account != null ? account.getDisplayName() : null;
        String email = account != null ? account.getEmail() : null;
        String photo = (account != null && account.getPhotoUrl() != null) ? account.getPhotoUrl().toString() : null;
        loginWithGoogle(credential, name, email, photo);
    }

    public void signOut() {
        firestoreRepository.signOut();
    }
}
