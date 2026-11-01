package com.example.skillswapproject;

import com.example.skillswapproject.util.ValidationUtils;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;

public class ValidationUtilsTest {

    @Test
    public void testEmailValidation() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertTrue(ValidationUtils.isValidEmail("alex.hunter+tag@domain.co.uk"));
        assertFalse(ValidationUtils.isValidEmail("notanemail"));
        assertFalse(ValidationUtils.isValidEmail("user@"));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail(null));
    }

    @Test
    public void testPhoneValidation() {
        assertTrue(ValidationUtils.isValidPhone("+1234567890"));
        assertTrue(ValidationUtils.isValidPhone("9876543210"));
        assertTrue(ValidationUtils.isValidPhone("+91-98765-43210"));
        assertFalse(ValidationUtils.isValidPhone("123")); // too short
        assertFalse(ValidationUtils.isValidPhone("abcdefghij"));
        assertFalse(ValidationUtils.isValidPhone(""));
        assertFalse(ValidationUtils.isValidPhone(null));
    }

    @Test
    public void testPasswordValidation() {
        // Valid password
        assertNull(ValidationUtils.validatePassword("Password123"));
        assertNull(ValidationUtils.validatePassword("Str0ngPass!"));

        // Empty
        assertEquals("Please enter your password.", ValidationUtils.validatePassword(""));
        assertEquals("Please enter your password.", ValidationUtils.validatePassword(null));

        // Leading / trailing space
        assertEquals("Password cannot begin or end with a space.", ValidationUtils.validatePassword(" Password123"));
        assertEquals("Password cannot begin or end with a space.", ValidationUtils.validatePassword("Password123 "));

        // Length < 8
        assertEquals("Password must contain at least 8 characters.", ValidationUtils.validatePassword("Pass1"));

        // Missing uppercase
        assertEquals("Password must contain at least one uppercase letter, one lowercase letter, and one number.",
                ValidationUtils.validatePassword("password123"));

        // Missing lowercase
        assertEquals("Password must contain at least one uppercase letter, one lowercase letter, and one number.",
                ValidationUtils.validatePassword("PASSWORD123"));

        // Missing digit
        assertEquals("Password must contain at least one uppercase letter, one lowercase letter, and one number.",
                ValidationUtils.validatePassword("PasswordOnly"));
    }

    @Test
    public void testNameValidation() {
        assertNull(ValidationUtils.validateName("John Doe"));
        assertNull(ValidationUtils.validateName("A"));
        assertEquals("Name cannot be empty.", ValidationUtils.validateName(""));
        assertEquals("Name cannot be empty.", ValidationUtils.validateName("   "));
        assertEquals("Name cannot be empty.", ValidationUtils.validateName(null));
        assertEquals("Name must contain letters and cannot consist only of numbers or special characters.",
                ValidationUtils.validateName("123456"));
        assertEquals("Name must contain letters and cannot consist only of numbers or special characters.",
                ValidationUtils.validateName("!@#$%^&*"));
    }

    @Test
    public void testSkillValidation() {
        List<String> currentSkills = Arrays.asList("Android", "Kotlin", "Java");

        // Valid new skill
        assertNull(ValidationUtils.validateSkillName("Python", currentSkills));

        // Blank
        assertEquals("Skill name cannot be empty.", ValidationUtils.validateSkillName("", currentSkills));
        assertEquals("Skill name cannot be empty.", ValidationUtils.validateSkillName("   ", currentSkills));
        assertEquals("Skill name cannot be empty.", ValidationUtils.validateSkillName(null, currentSkills));

        // Duplicate case-insensitive
        assertEquals("You have already added this skill.",
                ValidationUtils.validateSkillName("android", currentSkills));
        assertEquals("You have already added this skill.",
                ValidationUtils.validateSkillName("KOTLIN", currentSkills));
    }

    @Test
    public void testSessionDateTimeValidation() {
        Calendar future = Calendar.getInstance();
        future.add(Calendar.DAY_OF_YEAR, 2);

        Calendar past = Calendar.getInstance();
        past.add(Calendar.DAY_OF_YEAR, -2);

        // Date not picked
        assertEquals("Please select a date for the session.",
                ValidationUtils.validateSessionDateTime(future, false, true));

        // Time not picked
        assertEquals("Please select a time for the session.",
                ValidationUtils.validateSessionDateTime(future, true, false));

        // Past date/time
        assertEquals("Selected session date and time cannot be in the past. Please choose a future time.",
                ValidationUtils.validateSessionDateTime(past, true, true));

        // Valid future session
        assertNull(ValidationUtils.validateSessionDateTime(future, true, true));
    }

    @Test
    public void testChatMessageValidation() {
        assertNull(ValidationUtils.validateChatMessage("Hello, let's swap skills!"));
        assertEquals("Message cannot be empty.", ValidationUtils.validateChatMessage(""));
        assertEquals("Message cannot be empty.", ValidationUtils.validateChatMessage("   "));
        assertEquals("Message cannot be empty.", ValidationUtils.validateChatMessage(null));
    }

    @Test
    public void testRatingValidation() {
        assertNull(ValidationUtils.validateRating(1.0f));
        assertNull(ValidationUtils.validateRating(3.5f));
        assertNull(ValidationUtils.validateRating(5.0f));

        assertEquals("Rating must be between 1 and 5 stars.", ValidationUtils.validateRating(0.5f));
        assertEquals("Rating must be between 1 and 5 stars.", ValidationUtils.validateRating(6.0f));
    }

    @Test
    public void testFriendlyAuthErrorMessageMapping() {
        // Network exception
        Exception netEx = new FirebaseNetworkException("Network error occurred");
        assertEquals("Please check your internet connection.",
                ValidationUtils.getFriendlyAuthErrorMessage(netEx, false));

        // Specific Firebase error codes
        FirebaseAuthException userNotFound = new FirebaseAuthException("ERROR_USER_NOT_FOUND", "User not found");
        assertEquals("No account found. Please sign up first.",
                ValidationUtils.getFriendlyAuthErrorMessage(userNotFound, false));

        FirebaseAuthException wrongPassword = new FirebaseAuthException("ERROR_WRONG_PASSWORD", "Invalid password");
        assertEquals("Incorrect password. Please try again.",
                ValidationUtils.getFriendlyAuthErrorMessage(wrongPassword, false));

        FirebaseAuthException userDisabled = new FirebaseAuthException("ERROR_USER_DISABLED", "Account disabled");
        assertEquals("Your account has been disabled. Please contact support.",
                ValidationUtils.getFriendlyAuthErrorMessage(userDisabled, false));

        FirebaseAuthException emailCollision = new FirebaseAuthException("ERROR_EMAIL_ALREADY_IN_USE", "Email already in use");
        assertEquals("This email is already registered. Please log in.",
                ValidationUtils.getFriendlyAuthErrorMessage(emailCollision, false));
        assertEquals("This phone number is already registered.",
                ValidationUtils.getFriendlyAuthErrorMessage(emailCollision, true));

        FirebaseAuthException invalidEmail = new FirebaseAuthException("ERROR_INVALID_EMAIL", "Invalid email");
        assertEquals("Please enter a valid email address.",
                ValidationUtils.getFriendlyAuthErrorMessage(invalidEmail, false));

        FirebaseAuthException weakPassword = new FirebaseAuthException("ERROR_WEAK_PASSWORD", "Password too weak");
        assertEquals("Password must contain at least 8 characters with an uppercase letter, lowercase letter, and number.",
                ValidationUtils.getFriendlyAuthErrorMessage(weakPassword, false));

        FirebaseAuthException tooManyRequests = new FirebaseAuthException("ERROR_TOO_MANY_REQUESTS", "Rate limited");
        assertEquals("Too many unsuccessful attempts. Please try again later.",
                ValidationUtils.getFriendlyAuthErrorMessage(tooManyRequests, false));

        FirebaseAuthException credentialCollision = new FirebaseAuthException("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "Collision");
        assertEquals("An account already exists with this email address using a different sign-in method.",
                ValidationUtils.getFriendlyAuthErrorMessage(credentialCollision, false));
    }
}
