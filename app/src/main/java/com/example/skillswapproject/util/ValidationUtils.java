package com.example.skillswapproject.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import java.util.Calendar;
import java.util.List;

/**
 * Centralized utility class for all user input validation, network checking,
 * and user-friendly error message resolution throughout the Skill Swap application.
 */
public final class ValidationUtils {

    private ValidationUtils() {}

    /**
     * Checks whether the device currently has active network connectivity.
     */
    public static boolean isNetworkAvailable(@Nullable Context context) {
        if (context == null) return true;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork == null) return false;
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                );
            } else {
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        } catch (Exception ignored) {
            return true;
        }
    }

    private static final java.util.regex.Pattern EMAIL_REGEX =
            java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Validates an email address format.
     */
    public static boolean isValidEmail(@Nullable String email) {
        if (email == null) return false;
        String trimmed = email.trim();
        if (trimmed.isEmpty()) return false;
        try {
            if (Patterns.EMAIL_ADDRESS != null) {
                return Patterns.EMAIL_ADDRESS.matcher(trimmed).matches();
            }
        } catch (Exception ignored) {}
        return EMAIL_REGEX.matcher(trimmed).matches();
    }

    /**
     * Validates a phone number format (between 7 and 15 digits).
     */
    public static boolean isValidPhone(@Nullable String phone) {
        if (phone == null) return false;
        String clean = phone.replaceAll("[^0-9+]", "");
        return clean.matches("^(\\+?[0-9]{7,15})$");
    }

    /**
     * Validates password rules:
     * - Minimum 8 characters.
     * - At least one uppercase letter.
     * - At least one lowercase letter.
     * - At least one number.
     * - Does not allow spaces at beginning or end.
     *
     * @return Error message if invalid, or null if valid.
     */
    @Nullable
    public static String validatePassword(@Nullable String password) {
        if (password == null || password.isEmpty()) {
            return "Please enter your password.";
        }
        if (password.startsWith(" ") || password.endsWith(" ")) {
            return "Password cannot begin or end with a space.";
        }
        if (password.length() < 8) {
            return "Password must contain at least 8 characters.";
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUpper || !hasLower || !hasDigit) {
            return "Password must contain at least one uppercase letter, one lowercase letter, and one number.";
        }
        return null;
    }

    /**
     * Validates a user's display name:
     * - Cannot be empty.
     * - Cannot contain only numbers or special characters (must include letters).
     *
     * @return Error message if invalid, or null if valid.
     */
    @Nullable
    public static String validateName(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Name cannot be empty.";
        }
        String trimmed = name.trim();
        if (!trimmed.matches(".*[a-zA-Z].*")) {
            return "Name must contain letters and cannot consist only of numbers or special characters.";
        }
        return null;
    }

    /**
     * Validates a skill name:
     * - Cannot be empty or blank.
     * - Cannot duplicate any existing skill in the list (case-insensitive).
     *
     * @return Error message if invalid, or null if valid.
     */
    @Nullable
    public static String validateSkillName(@Nullable String skillName, @Nullable List<String> existingSkills) {
        if (skillName == null || skillName.trim().isEmpty()) {
            return "Skill name cannot be empty.";
        }
        String trimmed = skillName.trim();
        if (existingSkills != null) {
            for (String s : existingSkills) {
                if (s != null && s.trim().equalsIgnoreCase(trimmed)) {
                    return "You have already added this skill.";
                }
            }
        }
        return null;
    }

    /**
     * Validates session date and time:
     * - Date and time must both be selected.
     * - Selected date and time cannot be in the past.
     *
     * @return Error message if invalid, or null if valid.
     */
    @Nullable
    public static String validateSessionDateTime(@Nullable Calendar selectedCalendar, boolean isDatePicked, boolean isTimePicked) {
        if (!isDatePicked) {
            return "Please select a date for the session.";
        }
        if (!isTimePicked) {
            return "Please select a time for the session.";
        }
        if (selectedCalendar == null || selectedCalendar.getTimeInMillis() < (System.currentTimeMillis() - 60000)) {
            return "Selected session date and time cannot be in the past. Please choose a future time.";
        }
        return null;
    }

    /**
     * Validates chat message content:
     * - Cannot be empty.
     * - Cannot contain only spaces.
     *
     * @return Error message if invalid, or null if valid.
     */
    @Nullable
    public static String validateChatMessage(@Nullable String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Message cannot be empty.";
        }
        return null;
    }

    /**
     * Validates review rating:
     * - Must be between 1.0 and 5.0 stars.
     *
     * @return Error message if invalid, or null if valid.
     */
    @Nullable
    public static String validateRating(float rating) {
        if (rating < 1.0f || rating > 5.0f) {
            return "Rating must be between 1 and 5 stars.";
        }
        return null;
    }

    /**
     * Maps Firebase Authentication and Network exceptions to clear, user-friendly messages.
     * Never exposes technical stack traces or database errors directly to users.
     */
    @NonNull
    public static String getFriendlyAuthErrorMessage(@Nullable Exception e, boolean isPhone) {
        if (e == null) {
            return "Authentication failed. Please try again.";
        }

        // 1. Network Errors
        if (e instanceof FirebaseNetworkException) {
            return "Please check your internet connection.";
        }
        String rawMsg = e.getMessage() != null ? e.getMessage() : "";
        String lowerMsg = rawMsg.toLowerCase();
        if (lowerMsg.contains("network") || lowerMsg.contains("network_error") ||
            rawMsg.contains("NETWORK_REQUEST_FAILED") || lowerMsg.contains("unavailable")) {
            return "Please check your internet connection.";
        }

        // 2. Specific FirebaseAuthException Error Codes
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            if (code != null) {
                switch (code) {
                    case "ERROR_USER_NOT_FOUND":
                        return "No account found. Please sign up first.";
                    case "ERROR_WRONG_PASSWORD":
                    case "ERROR_INVALID_CREDENTIAL":
                        return "Incorrect password. Please try again.";
                    case "ERROR_USER_DISABLED":
                        return "Your account has been disabled. Please contact support.";
                    case "ERROR_EMAIL_ALREADY_IN_USE":
                        return isPhone ? "This phone number is already registered." : "This email is already registered. Please log in.";
                    case "ERROR_INVALID_EMAIL":
                        return "Please enter a valid email address.";
                    case "ERROR_WEAK_PASSWORD":
                        return "Password must contain at least 8 characters with an uppercase letter, lowercase letter, and number.";
                    case "ERROR_TOO_MANY_REQUESTS":
                        return "Too many unsuccessful attempts. Please try again later.";
                    case "ERROR_OPERATION_NOT_ALLOWED":
                        return "Sign-in method is currently disabled. Please contact support.";
                    case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                        return "An account already exists with this email address using a different sign-in method.";
                }
            }
        }

        // 3. Subclass Specific Checks
        if (e instanceof FirebaseAuthInvalidUserException) {
            String code = ((FirebaseAuthInvalidUserException) e).getErrorCode();
            if ("ERROR_USER_DISABLED".equals(code)) {
                return "Your account has been disabled. Please contact support.";
            }
            return "No account found. Please sign up first.";
        }

        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code) || lowerMsg.contains("badly formatted") || lowerMsg.contains("invalid email")) {
                return "Please enter a valid email address.";
            }
            return "Incorrect password. Please try again.";
        }

        if (e instanceof FirebaseAuthUserCollisionException) {
            return isPhone ? "This phone number is already registered." : "This email is already registered. Please log in.";
        }

        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Password must contain at least 8 characters with an uppercase letter, lowercase letter, and number.";
        }

        // 4. Fallback message inspection for non-standard error text
        if (lowerMsg.contains("password")) {
            return "Incorrect password. Please try again.";
        }
        if (lowerMsg.contains("no user") || lowerMsg.contains("not found")) {
            return "No account found. Please sign up first.";
        }
        if (lowerMsg.contains("already in use") || lowerMsg.contains("already exists")) {
            return isPhone ? "This phone number is already registered." : "This email is already registered. Please log in.";
        }
        if (lowerMsg.contains("disabled")) {
            return "Your account has been disabled. Please contact support.";
        }

        return "Authentication failed. Please check your details and try again.";
    }
}
