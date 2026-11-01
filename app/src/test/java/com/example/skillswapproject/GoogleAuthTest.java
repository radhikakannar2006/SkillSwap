package com.example.skillswapproject;

import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class GoogleAuthTest {

    private static final String TEST_CLIENT_ID = "660214780194-mhnn1eljk1089mkfhmqjqk4u319qgsq0.apps.googleusercontent.com";

    @Test
    public void testGetGoogleIdOptionAuthorizedAccounts() {
        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(TEST_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build();

        assertNotNull(option);
        assertEquals(TEST_CLIENT_ID, option.getServerClientId());
        assertTrue(option.getFilterByAuthorizedAccounts());
        assertTrue(option.getAutoSelectEnabled());
    }

    @Test
    public void testGetGoogleIdOptionAllAccountsFallback() {
        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(TEST_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build();

        assertNotNull(option);
        assertEquals(TEST_CLIENT_ID, option.getServerClientId());
        assertFalse(option.getFilterByAuthorizedAccounts());
        assertFalse(option.getAutoSelectEnabled());
    }

    @Test
    public void testGetSignInWithGoogleOptionExplicitButton() {
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(TEST_CLIENT_ID)
                .build();

        assertNotNull(option);
        assertEquals(TEST_CLIENT_ID, option.getServerClientId());
    }

    @Test
    public void testCredentialTypeConstants() {
        assertNotNull(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL);
        assertNotNull(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL);
        assertNotEquals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL);
    }

    @Test
    public void testGoogleAuthResultExistingUser() {
        FirestoreUser existingUser = new FirestoreUser();
        existingUser.setUid("uid_existing_123");
        existingUser.setName("Alex Hunter");
        existingUser.setEmail("alex@example.com");
        existingUser.setSkillCoins(15);
        existingUser.setWelcomeBonusClaimed(true);

        FirestoreRepository.GoogleAuthResult result =
                new FirestoreRepository.GoogleAuthResult(existingUser, false);

        assertNotNull(result.getUser());
        assertEquals("uid_existing_123", result.getUser().getUid());
        assertEquals("Alex Hunter", result.getUser().getName());
        assertEquals(15, result.getUser().getSkillCoins());
        assertFalse(result.isNewUser());
    }

    @Test
    public void testGoogleAuthResultNewUser() {
        FirestoreUser newUser = new FirestoreUser();
        newUser.setUid("uid_google_new_456");
        newUser.setName("Jordan Lee");
        newUser.setEmail("jordan.lee@gmail.com");
        newUser.setProfilePhotoUrl("https://lh3.googleusercontent.com/photo.jpg");
        newUser.setSkillCoins(5);
        newUser.setWelcomeBonusClaimed(true);
        newUser.setOfferedSkills(new ArrayList<>());
        newUser.setWantedSkills(new ArrayList<>());

        FirestoreRepository.GoogleAuthResult result =
                new FirestoreRepository.GoogleAuthResult(newUser, true);

        assertNotNull(result.getUser());
        assertEquals("uid_google_new_456", result.getUser().getUid());
        assertEquals("Jordan Lee", result.getUser().getName());
        assertEquals("jordan.lee@gmail.com", result.getUser().getEmail());
        assertEquals("https://lh3.googleusercontent.com/photo.jpg", result.getUser().getProfilePhotoUrl());
        assertEquals(5, result.getUser().getSkillCoins());
        assertTrue(result.isNewUser());
    }
}
