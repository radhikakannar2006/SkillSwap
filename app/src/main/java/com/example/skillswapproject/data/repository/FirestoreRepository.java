package com.example.skillswapproject.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.skillswapproject.data.model.CoinTransaction;
import com.example.skillswapproject.data.model.FirestoreChat;
import com.example.skillswapproject.data.model.FirestoreMessage;
import com.example.skillswapproject.data.model.FirestoreReview;
import com.example.skillswapproject.data.model.FirestoreSession;
import com.example.skillswapproject.data.model.FirestoreSwap;
import com.example.skillswapproject.data.model.FirestoreUser;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.skillswapproject.data.model.FirestoreNotification;
// Removed GoogleCalendarMeetHelper

/**
 * Singleton repository coordinating Firebase Authentication and Cloud Firestore.
 * Handles the NoSQL collections: Users, Swaps, Chats, Messages, and Sessions.
 */
public class FirestoreRepository {
    private static final String TAG = "FirestoreRepository";

    public static final String COLLECTION_USERS = "Users";
    public static final String COLLECTION_SWAPS = "Swaps";
    public static final String COLLECTION_CHATS = "Chats";
    public static final String COLLECTION_REVIEWS = "Reviews";
    public static final String COLLECTION_SESSIONS = "Sessions";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String SUBCOLLECTION_MESSAGES = "Messages";
    public static final String SUBCOLLECTION_COIN_TRANSACTIONS = "CoinTransactions";

    public static final int WELCOME_BONUS_COINS = 5;

    public static final String SWAP_STATUS_PENDING = "PENDING";
    public static final String SWAP_STATUS_ACCEPTED = "ACCEPTED";
    public static final String SWAP_STATUS_REJECTED = "REJECTED";

    private static volatile FirestoreRepository instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    // Callbacks & Listener Interfaces
    public interface OnTransactionsLoadedListener {
        void onTransactionsLoaded(List<CoinTransaction> transactions);
        void onError(Exception e);
    }

    public interface OnCoinBalanceListener {
        void onBalanceChanged(int balance);
        void onError(Exception e);
    }

    public interface OnUserProfileListener {
        void onUserProfileLoaded(FirestoreUser user);
        void onError(Exception e);
    }

    public interface OnReviewsLoadedListener {
        void onReviewsLoaded(List<FirestoreReview> reviews);
        void onError(Exception e);
    }

    public interface OnPublicUsersListener {
        void onUsersLoaded(List<FirestoreUser> users);
        void onError(Exception e);
    }

    public interface OnSwapsLoadedListener {
        void onSwapsLoaded(List<FirestoreSwap> acceptedSwaps, List<String> friendUserIds);
        void onError(Exception e);
    }

    public interface OnChatsLoadedListener {
        void onChatsLoaded(List<FirestoreChat> chats);
        void onError(Exception e);
    }

    public interface OnMessagesLoadedListener {
        void onMessagesLoaded(List<FirestoreMessage> messages);
        void onError(Exception e);
    }

    public interface OnSwapStatusListener {
        void onStatusChanged(boolean isAccepted, String currentStatus);
        void onError(Exception e);
    }

    public interface OnUserSwapsListener {
        void onSwapsLoaded(List<FirestoreSwap> receivedSwaps, List<FirestoreSwap> sentSwaps);
        void onError(Exception e);
    }

    public interface OnSwapCheckListener {
        void onResult(boolean isAccepted);
        void onError(Exception e);
    }

    public interface OnSessionsLoadedListener {
        void onSessionsLoaded(List<FirestoreSession> sessions);
        void onError(Exception e);
    }

    public interface OnSingleSessionListener {
        void onSessionLoaded(FirestoreSession session);
        void onError(Exception e);
    }

    private FirestoreRepository() {
        FirebaseAuth tempAuth = null;
        FirebaseFirestore tempDb = null;
        try {
            tempAuth = FirebaseAuth.getInstance();
            tempDb = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.w(TAG, "Firebase services initialization warning: " + e.getMessage());
        }
        this.auth = tempAuth;
        this.db = tempDb;
    }

    public static FirestoreRepository getInstance() {
        if (instance == null) {
            synchronized (FirestoreRepository.class) {
                if (instance == null) {
                    instance = new FirestoreRepository();
                }
            }
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return auth != null ? auth.getCurrentUser() : null;
    }

    public String getCurrentUserId() {
        return auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public boolean isUserLoggedIn() {
        return auth != null && auth.getCurrentUser() != null;
    }

    public void signOut() {
        if (auth != null) {
            auth.signOut();
        }
    }

    /**
     * Creates a deterministic chatId from two UIDs sorted alphabetically (e.g. uidA_uidB).
     */
    public static String getChatId(String uid1, String uid2) {
        if (uid1 == null || uid2 == null) return "";
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    // =========================================================================
    // 1. FIREBASE AUTHENTICATION & USER MANAGEMENT
    // =========================================================================

    /**
     * Signs up a new user with Firebase Auth and creates their user document in Users/{userId}.
     */
    public Task<AuthResult> signUpWithEmail(String name, String email, String password) {
        return signUpWithEmail(name, "", email, "", password, "Morning");
    }

    public Task<AuthResult> signUpWithEmail(String firstName, String lastName, String email, String phone, String password, String availability) {
        if (auth == null || db == null) {
            return Tasks.forException(new IllegalStateException("Firebase services are unavailable. Check google-services configuration."));
        }
        String fName = firstName != null ? firstName.trim() : "";
        String lName = lastName != null ? lastName.trim() : "";
        String userEmail = email != null ? email.trim() : "";
        String userPhone = phone != null ? phone.trim() : "";

        String combinedName = (fName + " " + lName).trim();
        if (combinedName.isEmpty()) combinedName = !userEmail.isEmpty() ? userEmail : userPhone;
        String finalName = combinedName;

        // Determine auth email: if user left email empty, synthesize a phone-based email for Firebase Auth
        String authEmail = userEmail;
        if (authEmail.isEmpty() && !userPhone.isEmpty()) {
            String cleanPhone = userPhone.replaceAll("[^0-9]", "");
            if (cleanPhone.isEmpty()) cleanPhone = "user_" + System.currentTimeMillis();
            authEmail = cleanPhone + "@phone.skillswap.com";
        }

        if (authEmail.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Please enter an email address or phone number."));
        }

        final String finalAuthEmail = authEmail;

        // 1. Perform Firebase Auth user creation FIRST (avoids unauthenticated Firestore permission issues)
        return auth.createUserWithEmailAndPassword(finalAuthEmail, password)
                .continueWithTask(authTask -> {
                    if (!authTask.isSuccessful() || authTask.getResult() == null) {
                        return Tasks.forException(authTask.getException() != null ? authTask.getException() : new Exception("Sign-up failed."));
                    }
                    AuthResult result = authTask.getResult();
                    String uid = result.getUser().getUid();

                    // Construct new FirestoreUser profile
                    FirestoreUser newUser = new FirestoreUser();
                    newUser.setUid(uid);
                    newUser.setFirstName(fName);
                    newUser.setLastName(lName);
                    newUser.setName(finalName);
                    newUser.setEmail(userEmail);
                    newUser.setPhone(userPhone);
                    newUser.setLocation("");
                    newUser.setProfilePhotoUrl("");
                    newUser.setPublic(true);
                    newUser.setOfferedSkills(new ArrayList<>());
                    newUser.setWantedSkills(new ArrayList<>());
                    newUser.setAvailability(availability != null && !availability.trim().isEmpty() ? availability.trim() : "Morning");
                    newUser.setSkillCoins(WELCOME_BONUS_COINS);
                    newUser.setTotalCoinsEarned(WELCOME_BONUS_COINS);
                    newUser.setWelcomeBonusClaimed(true);

                    return createUserProfile(newUser).continueWithTask(saveTask -> {
                        if (!saveTask.isSuccessful()) {
                            Log.e(TAG, "User document save failed: ", saveTask.getException());
                            return Tasks.forException(saveTask.getException());
                        }
                        return Tasks.forResult(result);
                    });
                });
    }

    /**
     * Logs in an existing user with Firebase Auth by email or phone.
     */
    public Task<AuthResult> loginWithEmail(String email, String password) {
        return loginWithEmailOrPhone(email, password);
    }

    public Task<AuthResult> loginWithEmailOrPhone(String emailOrPhone, String password) {
        if (auth == null || db == null) {
            return Tasks.forException(new IllegalStateException("Firebase services are unavailable."));
        }
        if (emailOrPhone == null || emailOrPhone.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Please enter an email or phone number."));
        }

        String input = emailOrPhone.trim();

        // Determine target auth email directly to avoid unauthenticated Firestore permission issues
        String authEmail = input;
        if (!input.contains("@")) {
            String cleanPhone = input.replaceAll("[^0-9]", "");
            if (cleanPhone.isEmpty()) cleanPhone = input;
            authEmail = cleanPhone + "@phone.skillswap.com";
        }

        final String targetAuthEmail = authEmail;

        return auth.signInWithEmailAndPassword(targetAuthEmail, password)
                .continueWithTask(authTask -> {
                    if (authTask.isSuccessful() && authTask.getResult() != null) {
                        AuthResult result = authTask.getResult();
                        FirebaseUser fbUser = result.getUser();
                        return ensureUserDocExists(fbUser, input).continueWith(t -> result);
                    }

                    // Secondary attempt if input was formatted differently (e.g. phone vs email)
                    String altAuthEmail;
                    if (input.contains("@")) {
                        String cleanPhone = input.replaceAll("[^0-9]", "");
                        altAuthEmail = cleanPhone + "@phone.skillswap.com";
                    } else {
                        altAuthEmail = input;
                    }

                    if (!altAuthEmail.equals(targetAuthEmail) && altAuthEmail.contains("@")) {
                        return auth.signInWithEmailAndPassword(altAuthEmail, password)
                                .continueWithTask(altTask -> {
                                    if (altTask.isSuccessful() && altTask.getResult() != null) {
                                        AuthResult altResult = altTask.getResult();
                                        return ensureUserDocExists(altResult.getUser(), input).continueWith(t -> altResult);
                                    }
                                    return Tasks.forException(altTask.getException() != null ? altTask.getException() : authTask.getException());
                                });
                    }
                    return Tasks.forException(authTask.getException() != null ? authTask.getException() : new Exception("Authentication failed."));
                });
    }

    /**
     * Container holding the authenticated user profile and whether this is a newly registered profile.
     */
    public static class GoogleAuthResult {
        private final FirestoreUser user;
        private final boolean isNewUser;

        public GoogleAuthResult(FirestoreUser user, boolean isNewUser) {
            this.user = user;
            this.isNewUser = isNewUser;
        }

        public FirestoreUser getUser() {
            return user;
        }

        public boolean isNewUser() {
            return isNewUser;
        }
    }

    /**
     * Authenticates with Firebase using a Google AuthCredential, checks Firestore for an existing profile,
     * creates an initial profile with 5 welcome coins if new, or preserves existing data if returning.
     */
    public Task<GoogleAuthResult> signInWithGoogle(AuthCredential credential, String fallbackDisplayName, String fallbackEmail, String fallbackPhotoUrl) {
        if (auth == null || db == null) {
            return Tasks.forException(new IllegalStateException("Firebase services are unavailable."));
        }
        if (credential == null) {
            return Tasks.forException(new IllegalArgumentException("Google AuthCredential cannot be null."));
        }

        return auth.signInWithCredential(credential)
                .continueWithTask(authTask -> {
                    if (!authTask.isSuccessful() || authTask.getResult() == null) {
                        return Tasks.forException(authTask.getException());
                    }
                    AuthResult result = authTask.getResult();
                    FirebaseUser fbUser = result.getUser();
                    if (fbUser == null) {
                        return Tasks.forException(new IllegalStateException("Google authentication succeeded but no user was returned."));
                    }
                    String uid = fbUser.getUid();
                    DocumentReference userDoc = db.collection(COLLECTION_USERS).document(uid);

                    return userDoc.get().continueWithTask(getTask -> {
                        if (!getTask.isSuccessful()) {
                            return Tasks.forException(getTask.getException());
                        }
                        DocumentSnapshot doc = getTask.getResult();
                        if (doc != null && doc.exists()) {
                            FirestoreUser existingUser = doc.toObject(FirestoreUser.class);
                            if (existingUser != null) {
                                existingUser.setUid(uid);
                                existingUser.setPublic(parseUserPrivacy(doc));

                                // Safe non-destructive update of Google profile fields if previously missing
                                Map<String, Object> updates = new HashMap<>();
                                String gName = fbUser.getDisplayName() != null ? fbUser.getDisplayName() : fallbackDisplayName;
                                String gEmail = fbUser.getEmail() != null ? fbUser.getEmail() : fallbackEmail;
                                String gPhoto = fbUser.getPhotoUrl() != null ? fbUser.getPhotoUrl().toString() : fallbackPhotoUrl;

                                if ((existingUser.getName() == null || existingUser.getName().trim().isEmpty() || "User".equalsIgnoreCase(existingUser.getName().trim()))
                                        && gName != null && !gName.trim().isEmpty()) {
                                    updates.put("name", gName.trim());
                                    existingUser.setName(gName.trim());
                                }
                                if ((existingUser.getEmail() == null || existingUser.getEmail().trim().isEmpty())
                                        && gEmail != null && !gEmail.trim().isEmpty()) {
                                    updates.put("email", gEmail.trim());
                                    existingUser.setEmail(gEmail.trim());
                                }
                                if ((existingUser.getProfilePhotoUrl() == null || existingUser.getProfilePhotoUrl().trim().isEmpty())
                                        && gPhoto != null && !gPhoto.trim().isEmpty()) {
                                    updates.put("profilePhotoUrl", gPhoto.trim());
                                    existingUser.setProfilePhotoUrl(gPhoto.trim());
                                }
                                if (!updates.isEmpty()) {
                                    userDoc.update(updates);
                                }
                                return Tasks.forResult(new GoogleAuthResult(existingUser, false));
                            }
                        }

                        // No profile exists in Firestore: create initial profile with Google details & welcome bonus
                        String displayName = fbUser.getDisplayName();
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = fallbackDisplayName;
                        }
                        if (displayName == null || displayName.trim().isEmpty()) {
                            String email = fbUser.getEmail() != null ? fbUser.getEmail()
                                    : (fallbackEmail != null ? fallbackEmail : "");
                            displayName = !email.isEmpty() ? email.split("@")[0] : "Skill Swap Member";
                        }
                        String fName = displayName;
                        String lName = "";
                        if (displayName.contains(" ")) {
                            int idx = displayName.indexOf(" ");
                            fName = displayName.substring(0, idx).trim();
                            lName = displayName.substring(idx + 1).trim();
                        }
                        String photoUrl = fbUser.getPhotoUrl() != null ? fbUser.getPhotoUrl().toString()
                                : (fallbackPhotoUrl != null ? fallbackPhotoUrl : "");
                        String email = fbUser.getEmail() != null ? fbUser.getEmail()
                                : (fallbackEmail != null ? fallbackEmail : "");

                        FirestoreUser newUser = new FirestoreUser();
                        newUser.setUid(uid);
                        newUser.setName(displayName);
                        newUser.setFirstName(fName);
                        newUser.setLastName(lName);
                        newUser.setEmail(email);
                        newUser.setPhone("");
                        newUser.setLocation("");
                        newUser.setProfilePhotoUrl(photoUrl);
                        newUser.setPublic(true);
                        newUser.setOfferedSkills(new ArrayList<>());
                        newUser.setWantedSkills(new ArrayList<>());
                        newUser.setAvailability("Morning");
                        newUser.setSkillCoins(WELCOME_BONUS_COINS);
                        newUser.setTotalCoinsEarned(WELCOME_BONUS_COINS);
                        newUser.setWelcomeBonusClaimed(true);

                        return createUserProfile(newUser).continueWith(saveTask -> {
                            if (!saveTask.isSuccessful()) {
                                Log.e(TAG, "Failed to create base user profile for Google user: ", saveTask.getException());
                            }
                            return new GoogleAuthResult(newUser, true);
                        });
                    });
                });
    }

    public Task<GoogleAuthResult> signInWithGoogleCredential(AuthCredential credential, GoogleSignInAccount account) {
        String name = account != null ? account.getDisplayName() : null;
        String email = account != null ? account.getEmail() : null;
        String photo = (account != null && account.getPhotoUrl() != null) ? account.getPhotoUrl().toString() : null;
        return signInWithGoogle(credential, name, email, photo);
    }

    /**
     * Completely wipes the current user's profile, swaps, sessions, reviews, notifications,
     * chats/messages, coin transactions, and Firebase Auth account.
     */
    public Task<Void> cleanCurrentUserDataAndAccount() {
        if (auth == null || db == null) {
            return Tasks.forResult(null);
        }
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forResult(null);
        }
        String uid = currentUser.getUid();
        return deleteUserEntireDataAndAccount(uid, currentUser);
    }

    public Task<Void> deleteUserEntireDataAndAccount(String uid, FirebaseUser userAccount) {
        if (db == null || uid == null || uid.trim().isEmpty()) {
            if (userAccount != null) userAccount.delete();
            if (auth != null) auth.signOut();
            return Tasks.forResult(null);
        }

        List<Task<?>> cleanupTasks = new ArrayList<>();

        // 1. Users collection & CoinTransactions subcollection
        DocumentReference userDocRef = db.collection(COLLECTION_USERS).document(uid);
        cleanupTasks.add(userDocRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                WriteBatch b = db.batch();
                for (DocumentSnapshot doc : t.getResult().getDocuments()) b.delete(doc.getReference());
                return b.commit();
            }
            return Tasks.forResult(null);
        }).continueWithTask(t -> userDocRef.delete()));

        // 2. Swaps
        cleanupTasks.add(db.collection(COLLECTION_SWAPS).whereEqualTo("fromUserId", uid).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                WriteBatch b = db.batch();
                for (DocumentSnapshot doc : t.getResult().getDocuments()) b.delete(doc.getReference());
                return b.commit();
            }
            return Tasks.forResult(null);
        }));
        cleanupTasks.add(db.collection(COLLECTION_SWAPS).whereEqualTo("toUserId", uid).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                WriteBatch b = db.batch();
                for (DocumentSnapshot doc : t.getResult().getDocuments()) b.delete(doc.getReference());
                return b.commit();
            }
            return Tasks.forResult(null);
        }));

        // 3. Sessions
        cleanupTasks.add(db.collection(COLLECTION_SESSIONS).whereEqualTo("learnerId", uid).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                WriteBatch b = db.batch();
                for (DocumentSnapshot doc : t.getResult().getDocuments()) b.delete(doc.getReference());
                return b.commit();
            }
            return Tasks.forResult(null);
        }));
        cleanupTasks.add(db.collection(COLLECTION_SESSIONS).whereEqualTo("teacherId", uid).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                WriteBatch b = db.batch();
                for (DocumentSnapshot doc : t.getResult().getDocuments()) b.delete(doc.getReference());
                return b.commit();
            }
            return Tasks.forResult(null);
        }));

        // 4. Reviews
        cleanupTasks.add(db.collection(COLLECTION_REVIEWS).whereEqualTo("reviewerId", uid).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                WriteBatch b = db.batch();
                for (DocumentSnapshot doc : t.getResult().getDocuments()) b.delete(doc.getReference());
                return b.commit();
            }
            return Tasks.forResult(null);
        }));
        cleanupTasks.add(db.collection(COLLECTION_REVIEWS).whereEqualTo("targetUserId", uid).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                WriteBatch b = db.batch();
                for (DocumentSnapshot doc : t.getResult().getDocuments()) b.delete(doc.getReference());
                return b.commit();
            }
            return Tasks.forResult(null);
        }));

        // 5. Notifications
        cleanupTasks.add(db.collection(COLLECTION_NOTIFICATIONS).whereEqualTo("recipientUserId", uid).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                WriteBatch b = db.batch();
                for (DocumentSnapshot doc : t.getResult().getDocuments()) b.delete(doc.getReference());
                return b.commit();
            }
            return Tasks.forResult(null);
        }));
        cleanupTasks.add(db.collection(COLLECTION_NOTIFICATIONS).whereEqualTo("senderUserId", uid).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                WriteBatch b = db.batch();
                for (DocumentSnapshot doc : t.getResult().getDocuments()) b.delete(doc.getReference());
                return b.commit();
            }
            return Tasks.forResult(null);
        }));

        // 6. Chats and Messages subcollection
        cleanupTasks.add(db.collection(COLLECTION_CHATS).get().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                List<Task<?>> chatTasks = new ArrayList<>();
                for (DocumentSnapshot chatDoc : t.getResult().getDocuments()) {
                    String chatId = chatDoc.getId();
                    List<?> participants = (List<?>) chatDoc.get("participants");
                    boolean matches = chatId.contains(uid);
                    if (participants != null) {
                        for (Object p : participants) {
                            if (uid.equals(String.valueOf(p))) {
                                matches = true;
                                break;
                            }
                        }
                    }
                    if (matches) {
                        Task<Void> subTask = chatDoc.getReference().collection(SUBCOLLECTION_MESSAGES).get().continueWithTask(msgSnap -> {
                            if (msgSnap.isSuccessful() && msgSnap.getResult() != null) {
                                WriteBatch mb = db.batch();
                                for (DocumentSnapshot msgDoc : msgSnap.getResult().getDocuments()) {
                                    mb.delete(msgDoc.getReference());
                                }
                                return mb.commit();
                            }
                            return Tasks.forResult(null);
                        }).continueWithTask(v -> chatDoc.getReference().delete());
                        chatTasks.add(subTask);
                    }
                }
                return Tasks.whenAllComplete(chatTasks);
            }
            return Tasks.forResult(null);
        }));

        return Tasks.whenAllComplete(cleanupTasks).continueWithTask(t -> {
            if (userAccount != null) {
                return userAccount.delete().continueWith(delTask -> {
                    if (auth != null) auth.signOut();
                    return null;
                });
            } else {
                if (auth != null) auth.signOut();
                return Tasks.forResult(null);
            }
        });
    }

    // =========================================================================
    // 2. USERS COLLECTION (Users/{userId})
    // =========================================================================

    public Task<Void> createUserProfile(FirestoreUser user) {
        if (user == null || user.getUid() == null) {
            return Tasks.forException(new IllegalArgumentException("User or UID cannot be null"));
        }
        if (db == null) {
            return Tasks.forException(new IllegalStateException("Cloud Firestore is unavailable. Check google-services configuration."));
        }

        String fName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lName = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = user.getName();
        if (fullName == null || fullName.trim().isEmpty()) {
            fullName = (fName + " " + lName).trim();
        }
        if (fullName.isEmpty()) {
            fullName = user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail()
                    : (user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "User");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("uid", user.getUid());
        map.put("name", fullName);
        map.put("firstName", fName);
        map.put("lastName", lName);
        map.put("email", user.getEmail() != null ? user.getEmail() : "");
        map.put("phone", user.getPhone() != null ? user.getPhone() : "");
        map.put("location", user.getLocation() != null ? user.getLocation() : "");
        map.put("profilePhotoUrl", user.getProfilePhotoUrl() != null ? user.getProfilePhotoUrl() : "");
        map.put("isPublic", user.isPublic());
        map.put("public", user.isPublic());
        map.put("offeredSkills", user.getOfferedSkills() != null ? user.getOfferedSkills() : new ArrayList<>());
        map.put("wantedSkills", user.getWantedSkills() != null ? user.getWantedSkills() : new ArrayList<>());
        map.put("availability", user.getAvailability() != null ? user.getAvailability() : "Morning");
        map.put("aboutMe", user.getAboutMe() != null ? user.getAboutMe() : "");
        map.put("skillCoins", user.getSkillCoins() > 0 ? user.getSkillCoins() : WELCOME_BONUS_COINS);
        map.put("totalCoinsEarned", user.getTotalCoinsEarned() > 0 ? user.getTotalCoinsEarned() : WELCOME_BONUS_COINS);
        map.put("welcomeBonusClaimed", true);

        DocumentReference userDoc = db.collection(COLLECTION_USERS).document(user.getUid());
        return userDoc.set(map, SetOptions.merge()).continueWithTask(t -> {
            if (!t.isSuccessful()) {
                Log.e(TAG, "Failed writing user profile document", t.getException());
                return Tasks.forException(t.getException());
            }
            // Save welcome transaction
            DocumentReference welcomeTxDoc = userDoc.collection(SUBCOLLECTION_COIN_TRANSACTIONS)
                    .document("tx_welcome_" + user.getUid());
            CoinTransaction welcomeTx = new CoinTransaction(
                    "tx_welcome_" + user.getUid(),
                    user.getUid(),
                    WELCOME_BONUS_COINS,
                    CoinTransaction.TYPE_WELCOME_BONUS,
                    "Welcome Starter Bonus",
                    null,
                    0,
                    user.getSkillCoins() > 0 ? user.getSkillCoins() : WELCOME_BONUS_COINS
            );
            welcomeTxDoc.set(welcomeTx, SetOptions.merge());
            return Tasks.forResult(null);
        });
    }

    public Task<FirestoreUser> ensureUserDocExists(FirebaseUser fbUser, String originalInput) {
        if (fbUser == null || db == null) {
            return Tasks.forException(new IllegalStateException("Auth or Firestore unavailable"));
        }
        String uid = fbUser.getUid();
        DocumentReference userDoc = db.collection(COLLECTION_USERS).document(uid);
        return userDoc.get().continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                FirestoreUser existing = task.getResult().toObject(FirestoreUser.class);
                if (existing != null) {
                    existing.setUid(uid);
                    return Tasks.forResult(existing);
                }
            }
            // If doc doesn't exist, create it from Firebase Auth & original input
            String email = fbUser.getEmail() != null ? fbUser.getEmail() : "";
            String phone = "";
            if (email.endsWith("@phone.skillswap.com")) {
                phone = email.replace("@phone.skillswap.com", "");
                email = "";
            }
            if (email.isEmpty() && originalInput != null && originalInput.contains("@")) {
                email = originalInput;
            }
            String name = fbUser.getDisplayName();
            if (name == null || name.trim().isEmpty()) {
                name = !email.isEmpty() ? email.split("@")[0] : (!phone.isEmpty() ? phone : "User");
            }

            FirestoreUser newUser = new FirestoreUser();
            newUser.setUid(uid);
            newUser.setName(name);
            newUser.setFirstName(name);
            newUser.setLastName("");
            newUser.setEmail(email);
            newUser.setPhone(phone);
            newUser.setPublic(true);
            newUser.setSkillCoins(WELCOME_BONUS_COINS);
            newUser.setTotalCoinsEarned(WELCOME_BONUS_COINS);
            newUser.setWelcomeBonusClaimed(true);
            newUser.setAvailability("Morning");

            return createUserProfile(newUser).continueWith(saveTask -> newUser);
        });
    }

    /**
     * Parses user privacy from a document snapshot.
     * If either 'isPublic' or 'public' is explicitly false (or "false"/"private"), the user is PRIVATE.
     */
    public static boolean parseUserPrivacy(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return true;
        Boolean p1 = doc.getBoolean("isPublic");
        Boolean p2 = doc.getBoolean("public");
        Object o1 = doc.get("isPublic");
        Object o2 = doc.get("public");
        boolean isPriv = (Boolean.FALSE.equals(p1) || Boolean.FALSE.equals(p2)
                || (o1 != null && ("false".equalsIgnoreCase(o1.toString()) || "private".equalsIgnoreCase(o1.toString())))
                || (o2 != null && ("false".equalsIgnoreCase(o2.toString()) || "private".equalsIgnoreCase(o2.toString()))));
        Log.d("SWAP_DEBUG", "parseUserPrivacy for " + doc.getId() + ": isPublic=" + o1 + ", public=" + o2 + " => isPrivate=" + isPriv + ", returning=" + (!isPriv));
        return !isPriv;
    }

    public Task<FirestoreUser> getUserProfile(String uid) {
        if (uid == null) return Tasks.forResult(null);
        if (db == null) return Tasks.forResult(null);
        return db.collection(COLLECTION_USERS).document(uid).get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                FirestoreUser user = task.getResult().toObject(FirestoreUser.class);
                if (user != null) {
                    user.setUid(task.getResult().getId());
                    user.setPublic(parseUserPrivacy(task.getResult()));
                    return user;
                }
            }
            return null;
        });
    }

    // Alias for compatibility
    public Task<FirestoreUser> getUser(String uid) {
        return getUserProfile(uid);
    }

    /**
     * Real-time SnapshotListener on Users/{currentUserId}.
     */
    public ListenerRegistration listenToUserProfile(String uid, OnUserProfileListener listener) {
        if (uid == null || uid.isEmpty() || db == null) {
            if (listener != null) listener.onError(new IllegalArgumentException("Firestore or UID is unavailable"));
            return () -> {};
        }
        return db.collection(COLLECTION_USERS).document(uid).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                if (listener != null) listener.onError(error);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                FirestoreUser user = snapshot.toObject(FirestoreUser.class);
                if (user != null) {
                    user.setUid(snapshot.getId());
                    user.setPublic(parseUserPrivacy(snapshot));
                    if (listener != null) listener.onUserProfileLoaded(user);
                }
            } else if (listener != null) {
                listener.onUserProfileLoaded(null);
            }
        });
    }

    /**
     * Updates an existing user document in Users/{userId}.
     */
    public Task<Void> updateUserProfile(FirestoreUser user) {
        if (user == null || user.getUid() == null) {
            return Tasks.forException(new IllegalArgumentException("User or UID cannot be null"));
        }
        if (db == null) {
            return Tasks.forException(new IllegalStateException("Cloud Firestore is unavailable. Check google-services configuration."));
        }
        Map<String, Object> map = new HashMap<>();
        map.put("uid", user.getUid());
        if (user.getName() != null) map.put("name", user.getName());
        if (user.getEmail() != null) map.put("email", user.getEmail());
        if (user.getLocation() != null) map.put("location", user.getLocation());
        if (user.getAvailability() != null) map.put("availability", user.getAvailability());
        if (user.getAboutMe() != null) map.put("aboutMe", user.getAboutMe());
        if (user.getProfilePhotoUrl() != null) map.put("profilePhotoUrl", user.getProfilePhotoUrl());
        if (user.getOfferedSkills() != null) map.put("offeredSkills", user.getOfferedSkills());
        if (user.getWantedSkills() != null) map.put("wantedSkills", user.getWantedSkills());
        map.put("isPublic", user.isPublic());
        map.put("public", user.isPublic());

        return db.collection(COLLECTION_USERS)
                .document(user.getUid())
                .set(map, SetOptions.merge());
    }

    /**
     * Seeds initial Indian demo profiles into Cloud Firestore Users collection if they do not exist.
     * This ensures test users with varied Indian locations, teaching skills, and learning skills
     * are available for testing search and AI Compatibility Matching.
     */
    public Task<Void> seedDemoProfilesIfNotExist() {
        if (db == null) return Tasks.forResult(null);

        List<FirestoreUser> demoUsers = new ArrayList<>();

        // Demo User 1: Ananya Sharma
        FirestoreUser u1 = new FirestoreUser();
        u1.setUid("demo_ananya_sharma");
        u1.setFirstName("Ananya");
        u1.setLastName("Sharma");
        u1.setName("Ananya Sharma");
        u1.setEmail("ananya.sharma@demo.skillswap.com");
        u1.setLocation("Ahmedabad, Gujarat, India");
        u1.setOfferedSkills(Arrays.asList("Python", "Excel"));
        u1.setWantedSkills(Arrays.asList("Web Design"));
        u1.setAvailability("Weekday Evenings");
        u1.setPublic(true);
        u1.setSkillCoins(100);
        u1.setAboutMe("Passionate python developer looking to learn modern web design.");
        demoUsers.add(u1);

        // Demo User 2: Rohan Mehta
        FirestoreUser u2 = new FirestoreUser();
        u2.setUid("demo_rohan_mehta");
        u2.setFirstName("Rohan");
        u2.setLastName("Mehta");
        u2.setName("Rohan Mehta");
        u2.setEmail("rohan.mehta@demo.skillswap.com");
        u2.setLocation("Mumbai, Maharashtra, India");
        u2.setOfferedSkills(Arrays.asList("Web Design", "HTML"));
        u2.setWantedSkills(Arrays.asList("Python"));
        u2.setAvailability("Weekends");
        u2.setPublic(true);
        u2.setSkillCoins(120);
        u2.setAboutMe("UI/UX Designer who wants to master backend with Python.");
        demoUsers.add(u2);

        // Demo User 3: Manya Patel
        FirestoreUser u3 = new FirestoreUser();
        u3.setUid("demo_manya_patel");
        u3.setFirstName("Manya");
        u3.setLastName("Patel");
        u3.setName("Manya Patel");
        u3.setEmail("manya.patel@demo.skillswap.com");
        u3.setLocation("Surat, Gujarat, India");
        u3.setOfferedSkills(Arrays.asList("Photoshop", "UI Design"));
        u3.setWantedSkills(Arrays.asList("Excel"));
        u3.setAvailability("Weekend Evenings");
        u3.setPublic(true);
        u3.setSkillCoins(90);
        u3.setAboutMe("Graphic artist wanting to organize data with Excel.");
        demoUsers.add(u3);

        // Demo User 4: Arjun Shah
        FirestoreUser u4 = new FirestoreUser();
        u4.setUid("demo_arjun_shah");
        u4.setFirstName("Arjun");
        u4.setLastName("Shah");
        u4.setName("Arjun Shah");
        u4.setEmail("arjun.shah@demo.skillswap.com");
        u4.setLocation("Pune, Maharashtra, India");
        u4.setOfferedSkills(Arrays.asList("Guitar"));
        u4.setWantedSkills(Arrays.asList("Python"));
        u4.setAvailability("Evenings");
        u4.setPublic(true);
        u4.setSkillCoins(110);
        u4.setAboutMe("Music enthusiast exploring programming!");
        demoUsers.add(u4);

        WriteBatch batch = db.batch();
        for (FirestoreUser du : demoUsers) {
            DocumentReference docRef = db.collection(COLLECTION_USERS).document(du.getUid());
            Map<String, Object> map = new HashMap<>();
            map.put("uid", du.getUid());
            map.put("firstName", du.getFirstName());
            map.put("lastName", du.getLastName());
            map.put("name", du.getName());
            map.put("email", du.getEmail());
            map.put("location", du.getLocation());
            map.put("offeredSkills", du.getOfferedSkills());
            map.put("wantedSkills", du.getWantedSkills());
            map.put("availability", du.getAvailability());
            map.put("isPublic", du.isPublic());
            map.put("public", du.isPublic());
            map.put("skillCoins", du.getSkillCoins());
            map.put("aboutMe", du.getAboutMe());
            batch.set(docRef, map, SetOptions.merge());
        }

        return batch.commit();
    }

    /**
     * Listens in real-time to users in Users collection excluding current user.
     * Loads all users so both public and private profiles appear in recommendations and search.
     */
    public ListenerRegistration listenToPublicUsers(String excludeUserId, OnPublicUsersListener listener) {
        if (db == null) {
            if (listener != null) listener.onError(new IllegalStateException("Firestore unavailable"));
            return () -> {};
        }
        seedDemoProfilesIfNotExist();
        return db.collection(COLLECTION_USERS)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to users", error);
                        if (listener != null) listener.onError(error);
                        return;
                    }
                    List<FirestoreUser> list = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (excludeUserId != null && excludeUserId.equals(doc.getId())) {
                                continue;
                            }
                            FirestoreUser u = doc.toObject(FirestoreUser.class);
                            if (u != null) {
                                u.setUid(doc.getId());
                                u.setPublic(parseUserPrivacy(doc));
                                list.add(u);
                            }
                        }
                    }
                    if (listener != null) listener.onUsersLoaded(list);
                });
    }

    // =========================================================================
    // 3. SWAPS COLLECTION (Swaps/{swapId})
    // =========================================================================

    public Task<DocumentReference> createSwapRequest(FirestoreSwap swap) {
        return createSwap(swap);
    }

    public Task<Void> updateSwapStatus(String swapId, String newStatus) {
        return updateSwapStatus(swapId, newStatus, null, null);
    }

    public Task<Void> deleteSwap(String swapId) {
        if (db == null || swapId == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid swap ID"));
        }
        return db.collection(COLLECTION_SWAPS).document(swapId).delete();
    }

    /**
     * Real-time query for Swaps where current user is requesterId or receiverId AND status is "ACCEPTED".
     */
    public ListenerRegistration listenToAcceptedSwaps(String currentUserId, OnSwapsLoadedListener listener) {
        if (db == null) {
            if (listener != null) listener.onError(new IllegalStateException("Firestore unavailable"));
            return () -> {};
        }
        Query q1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", currentUserId)
                .whereEqualTo("status", SWAP_STATUS_ACCEPTED);

        Query q2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", SWAP_STATUS_ACCEPTED);

        final List<FirestoreSwap>[] lists = new List[]{new ArrayList<>(), new ArrayList<>()};

        ListenerRegistration r1 = q1.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                if (listener != null) listener.onError(error);
                return;
            }
            List<FirestoreSwap> l = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    FirestoreSwap s = doc.toObject(FirestoreSwap.class);
                    if (s != null) {
                        s.setId(doc.getId());
                        l.add(s);
                    }
                }
            }
            lists[0] = l;
            dispatchAcceptedSwaps(currentUserId, lists, listener);
        });

        ListenerRegistration r2 = q2.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                if (listener != null) listener.onError(error);
                return;
            }
            List<FirestoreSwap> l = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    FirestoreSwap s = doc.toObject(FirestoreSwap.class);
                    if (s != null) {
                        s.setId(doc.getId());
                        l.add(s);
                    }
                }
            }
            lists[1] = l;
            dispatchAcceptedSwaps(currentUserId, lists, listener);
        });

        return () -> {
            r1.remove();
            r2.remove();
        };
    }

    // Alias for compatibility
    public ListenerRegistration listenToFriendsOnlySwaps(String currentUserId, OnSwapsLoadedListener listener) {
        return listenToAcceptedSwaps(currentUserId, listener);
    }

    private void dispatchAcceptedSwaps(String currentUserId, List<FirestoreSwap>[] lists, OnSwapsLoadedListener listener) {
        if (listener == null) return;
        Map<String, FirestoreSwap> unique = new HashMap<>();
        Set<String> friends = new HashSet<>();

        for (List<FirestoreSwap> l : lists) {
            if (l != null) {
                for (FirestoreSwap s : l) {
                    unique.put(s.getId(), s);
                    if (currentUserId.equals(s.getRequesterId())) {
                        friends.add(s.getReceiverId());
                    } else {
                        friends.add(s.getRequesterId());
                    }
                }
            }
        }
        listener.onSwapsLoaded(new ArrayList<>(unique.values()), new ArrayList<>(friends));
    }

    /**
     * Checks/listens to swap status between currentUserId and friendId.
     */
    public ListenerRegistration listenToSwapStatus(String currentUserId, String friendId, OnSwapStatusListener listener) {
        Query q1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", currentUserId)
                .whereEqualTo("receiverId", friendId);

        Query q2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", friendId)
                .whereEqualTo("receiverId", currentUserId);

        final String[] status1 = new String[]{null};
        final String[] status2 = new String[]{null};

        ListenerRegistration r1 = q1.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                if (listener != null) listener.onError(error);
                return;
            }
            if (snapshot != null && !snapshot.isEmpty()) {
                status1[0] = snapshot.getDocuments().get(0).getString("status");
            } else {
                status1[0] = null;
            }
            evaluateSwapStatus(status1[0], status2[0], listener);
        });

        ListenerRegistration r2 = q2.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                if (listener != null) listener.onError(error);
                return;
            }
            if (snapshot != null && !snapshot.isEmpty()) {
                status2[0] = snapshot.getDocuments().get(0).getString("status");
            } else {
                status2[0] = null;
            }
            evaluateSwapStatus(status1[0], status2[0], listener);
        });

        return () -> {
            r1.remove();
            r2.remove();
        };
    }

    private void evaluateSwapStatus(String s1, String s2, OnSwapStatusListener listener) {
        if (listener == null) return;
        String status = s1 != null ? s1 : s2;
        boolean accepted = SWAP_STATUS_ACCEPTED.equalsIgnoreCase(status);
        listener.onStatusChanged(accepted, status != null ? status : "NONE");
    }

    // =========================================================================
    // 4. CHATS COLLECTION (Chats/{chatId}) & MESSAGES SUBCOLLECTION
    // =========================================================================

    /**
     * Real-time query for Chats containing current user in participants array.
     */
    public ListenerRegistration listenToUserChats(String currentUserId, OnChatsLoadedListener listener) {
        return db.collection(COLLECTION_CHATS)
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to user chats", error);
                        if (listener != null) listener.onError(error);
                        return;
                    }
                    List<FirestoreChat> chats = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                FirestoreChat c = doc.toObject(FirestoreChat.class);
                                if (c != null) {
                                    c.setChatId(doc.getId());
                                    chats.add(c);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error deserializing chat doc " + doc.getId(), e);
                            }
                        }
                    }
                    if (listener != null) listener.onChatsLoaded(chats);
                });
    }

    /**
     * Real-time SnapshotListener on Chats/{chatId}/Messages ordered by timestamp ASC.
     */
    public ListenerRegistration listenToMessages(String chatId, OnMessagesLoadedListener listener) {
        return db.collection(COLLECTION_CHATS)
                .document(chatId)
                .collection(SUBCOLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages", error);
                        if (listener != null) listener.onError(error);
                        return;
                    }
                    List<FirestoreMessage> list = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                FirestoreMessage msg = doc.toObject(FirestoreMessage.class);
                                if (msg != null) {
                                    msg.setMessageId(doc.getId());
                                    list.add(msg);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error deserializing message doc " + doc.getId(), e);
                            }
                        }
                    }
                    if (listener != null) listener.onMessagesLoaded(list);
                });
    }

    /**
     * Sends message to Messages subcollection and updates parent Chats document using WriteBatch.
     */
    public Task<Void> sendMessage(String chatId, FirestoreMessage message) {
        if (db == null) return Tasks.forException(new IllegalStateException("Firestore unavailable"));
        if (message == null || message.getText() == null || message.getText().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Message cannot be empty."));
        }
        if (chatId == null || message.getSenderId() == null || (!chatId.contains(message.getSenderId()))) {
            return Tasks.forException(new IllegalStateException("You are not authorized to send messages in this chat."));
        }
        WriteBatch batch = db.batch();

        DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
        DocumentReference messageRef = chatRef.collection(SUBCOLLECTION_MESSAGES).document();

        // 1. Write message document to subcollection
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("senderId", message.getSenderId());
        msgMap.put("receiverId", message.getReceiverId());
        msgMap.put("text", message.getText());
        msgMap.put("timestamp", FieldValue.serverTimestamp());

        batch.set(messageRef, msgMap);

        // 2. Update parent Chats document
        Map<String, Object> chatMap = new HashMap<>();
        chatMap.put("chatId", chatId);
        chatMap.put("participants", Arrays.asList(message.getSenderId(), message.getReceiverId()));
        chatMap.put("lastMessageText", message.getText());
        chatMap.put("lastMessageTimestamp", FieldValue.serverTimestamp());
        chatMap.put("lastSenderId", message.getSenderId());
        chatMap.put("unreadBy", message.getReceiverId());
        chatMap.put("unread", true);

        batch.set(chatRef, chatMap, SetOptions.merge());

        return batch.commit().continueWithTask(t -> {
            if (t.isSuccessful() && message.getReceiverId() != null) {
                getUserProfile(message.getSenderId()).addOnSuccessListener(sender -> {
                    String senderName = sender != null && sender.getName() != null ? sender.getName() : "A member";
                    FirestoreNotification notification = new FirestoreNotification(
                            "msg_" + messageRef.getId(),
                            message.getReceiverId(),
                            message.getSenderId(),
                            senderName,
                            sender != null ? sender.getProfilePhotoUrl() : "",
                            FirestoreNotification.TYPE_NEW_MESSAGE,
                            "New Message",
                            senderName + " sent you a message."
                    );
                    notification.setRelatedChatId(chatId);
                    createNotification(notification);
                });
            }
            return t;
        });
    }

    /**
     * Marks a chat as read by the current user.
     * Clears unreadBy if current user is the receiver.
     */
    public Task<Void> markChatAsRead(String chatId, String currentUserId) {
        if (db == null || chatId == null || currentUserId == null) {
            return Tasks.forResult(null);
        }
        DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
        return chatRef.get().continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                String unreadBy = task.getResult().getString("unreadBy");
                if (currentUserId.equals(unreadBy)) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("unreadBy", null);
                    updates.put("unread", false);
                    return chatRef.update(updates);
                }
            }
            return Tasks.forResult(null);
        });
    }

    // =========================================================================
    // 4. SKILL SWAPS MANAGEMENT & GATEWAY
    // =========================================================================

    /**
     * Creates a new Swap request in Swaps collection with status PENDING
     * after validating against self-requests, missing skills, duplicate pending requests, and profile privacy.
     */
    public Task<DocumentReference> createSwap(FirestoreSwap swap) {
        if (swap == null) {
            return Tasks.forException(new IllegalArgumentException("Swap details cannot be empty."));
        }
        if (db == null) {
            return Tasks.forException(new IllegalStateException("Firestore is not available"));
        }

        // 1. Prevent self-swap
        if (swap.getRequesterId() != null && swap.getRequesterId().equals(swap.getReceiverId())) {
            return Tasks.forException(new IllegalArgumentException("You cannot send a swap request to yourself."));
        }

        // 2. Validate required skill details
        if (swap.getOfferedSkillName() == null || swap.getOfferedSkillName().trim().isEmpty() ||
                swap.getRequestedSkillName() == null || swap.getRequestedSkillName().trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Please select both a skill to offer and a skill to learn."));
        }

        // 3. Query existing swaps between these two users to prevent duplicate pending or active requests
        return db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", swap.getRequesterId())
                .whereEqualTo("receiverId", swap.getReceiverId())
                .get()
                .continueWithTask(sentTask -> {
                    if (sentTask.isSuccessful() && sentTask.getResult() != null) {
                        for (DocumentSnapshot doc : sentTask.getResult().getDocuments()) {
                            String status = doc.getString("status");
                            if ("PENDING".equalsIgnoreCase(status)) {
                                return Tasks.forException(new IllegalStateException("A swap request with this user is already pending."));
                            } else if ("ACCEPTED".equalsIgnoreCase(status)) {
                                return Tasks.forException(new IllegalStateException("You already have an active swap with this user."));
                            }
                        }
                    }

                    // Also check if the other user already sent a pending request
                    return db.collection(COLLECTION_SWAPS)
                            .whereEqualTo("requesterId", swap.getReceiverId())
                            .whereEqualTo("receiverId", swap.getRequesterId())
                            .get()
                            .continueWithTask(recvTask -> {
                                if (recvTask.isSuccessful() && recvTask.getResult() != null) {
                                    for (DocumentSnapshot doc : recvTask.getResult().getDocuments()) {
                                        String status = doc.getString("status");
                                        if ("PENDING".equalsIgnoreCase(status)) {
                                            return Tasks.forException(new IllegalStateException("This user has already sent you a swap request. Please check your received requests."));
                                        } else if ("ACCEPTED".equalsIgnoreCase(status)) {
                                            return Tasks.forException(new IllegalStateException("You already have an active swap with this user."));
                                        }
                                    }
                                }

                                DocumentReference newDocRef = db.collection(COLLECTION_SWAPS).document();
                                swap.setId(newDocRef.getId());

                                return newDocRef.set(swap).continueWith(task -> {
                                    if (!task.isSuccessful()) {
                                        throw task.getException() != null ? task.getException() : new RuntimeException("Failed to send swap request.");
                                    }
                                    // Trigger in-app notification & FCM push data for receiver
                                    String requesterName = swap.getRequesterName() != null ? swap.getRequesterName() : "A member";
                                    FirestoreNotification notification = new FirestoreNotification(
                                            "swap_req_" + newDocRef.getId(),
                                            swap.getReceiverId(),
                                            swap.getRequesterId(),
                                            requesterName,
                                            swap.getRequesterPhoto(),
                                            FirestoreNotification.TYPE_SWAP_REQUEST,
                                            "New Swap Request",
                                            requesterName + " sent you a swap request."
                                    );
                                    notification.setRelatedSwapId(newDocRef.getId());
                                    createNotification(notification);
                                    return newDocRef;
                                });
                            });
                });
    }

    /**
     * Updates swap status ("ACCEPTED" or "REJECTED").
     * When ACCEPTED, automatically initializes the 1:1 chat room.
     */
    public Task<Void> updateSwapStatus(String swapId, String status, String requesterId, String receiverId) {
        if (db == null) return Tasks.forException(new IllegalStateException("Firestore is not available"));
        
        WriteBatch batch = db.batch();
        DocumentReference swapRef = db.collection(COLLECTION_SWAPS).document(swapId);
        batch.update(swapRef, "status", status);
        batch.update(swapRef, "timestamp", FieldValue.serverTimestamp());

        // When a swap is ACCEPTED, clean up any stale "ended" messages from previous swap sessions
        // so that neither user sees old "Skill swap was ended" text in chat or chat list.
        if ("ACCEPTED".equalsIgnoreCase(status) && requesterId != null && receiverId != null) {
            String chatId = getChatId(requesterId, receiverId);
            DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);

            chatRef.get().addOnSuccessListener(chatDoc -> {
                if (chatDoc != null && chatDoc.exists()) {
                    String lastMsg = chatDoc.getString("lastMessageText");
                    if (lastMsg != null && lastMsg.toLowerCase().contains("ended")) {
                        Map<String, Object> clearMap = new HashMap<>();
                        clearMap.put("lastMessageText", null);
                        clearMap.put("unreadBy", null);
                        clearMap.put("unread", false);
                        chatRef.update(clearMap);
                    }
                }
            });

            chatRef.collection(SUBCOLLECTION_MESSAGES).get().addOnSuccessListener(msgSnapshot -> {
                if (msgSnapshot != null && !msgSnapshot.isEmpty()) {
                    WriteBatch delBatch = db.batch();
                    boolean hasDeletions = false;
                    for (DocumentSnapshot m : msgSnapshot.getDocuments()) {
                        String text = m.getString("text");
                        if (text != null && text.toLowerCase().contains("ended")) {
                            delBatch.delete(m.getReference());
                            hasDeletions = true;
                        }
                    }
                    if (hasDeletions) {
                        delBatch.commit();
                    }
                }
            });
        }

        return batch.commit().continueWithTask(t -> {
            if (t.isSuccessful() && "ACCEPTED".equalsIgnoreCase(status) && requesterId != null) {
                String currentUid = getCurrentUserId();
                getUserProfile(currentUid).addOnSuccessListener(me -> {
                    String myName = me != null && me.getName() != null ? me.getName() : "Your partner";
                    FirestoreNotification notification = new FirestoreNotification(
                            "swap_acc_" + swapId,
                            requesterId,
                            currentUid,
                            myName,
                            me != null ? me.getProfilePhotoUrl() : "",
                            FirestoreNotification.TYPE_SWAP_ACCEPTED,
                            "Swap Request Accepted",
                            myName + " accepted your swap request."
                    );
                    notification.setRelatedSwapId(swapId);
                    createNotification(notification);
                });
            }
            return t;
        });
    }

    /**
     * Real-time listener for all swaps where user is either requester or receiver.
     * Excludes swaps with status "ENDED" if ended by the current user,
     * but includes ENDED swaps if ended by partner so partner can be notified via Home screen icon.
     */
    public ListenerRegistration listenToUserSwaps(String userId, OnUserSwapsListener listener) {
        if (db == null || userId == null) {
            if (listener != null) listener.onError(new IllegalArgumentException("Invalid state"));
            return () -> {};
        }

        Query qReceived = db.collection(COLLECTION_SWAPS).whereEqualTo("receiverId", userId);
        Query qSent = db.collection(COLLECTION_SWAPS).whereEqualTo("requesterId", userId);

        final List<FirestoreSwap>[] lists = new List[]{new ArrayList<>(), new ArrayList<>()};

        Runnable dispatch = () -> {
            if (listener == null) return;
            List<FirestoreSwap> received = new ArrayList<>();
            List<FirestoreSwap> sent = new ArrayList<>();

            if (lists[0] != null) {
                for (FirestoreSwap s : lists[0]) {
                    if ("ENDED".equalsIgnoreCase(s.getStatus()) && userId.equals(s.getEndedBy())) {
                        continue;
                    }
                    received.add(s);
                }
            }
            if (lists[1] != null) {
                for (FirestoreSwap s : lists[1]) {
                    if ("ENDED".equalsIgnoreCase(s.getStatus()) && userId.equals(s.getEndedBy())) {
                        continue;
                    }
                    sent.add(s);
                }
            }
            listener.onSwapsLoaded(received, sent);
        };

        ListenerRegistration r1 = qReceived.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening to received swaps", error);
                if (listener != null) listener.onError(error);
                return;
            }
            List<FirestoreSwap> list = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    FirestoreSwap s = doc.toObject(FirestoreSwap.class);
                    if (s != null) {
                        s.setId(doc.getId());
                        list.add(s);
                    }
                }
            }
            lists[0] = list;
            dispatch.run();
        });

        ListenerRegistration r2 = qSent.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening to sent swaps", error);
                if (listener != null) listener.onError(error);
                return;
            }
            List<FirestoreSwap> list = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    FirestoreSwap s = doc.toObject(FirestoreSwap.class);
                    if (s != null) {
                        s.setId(doc.getId());
                        list.add(s);
                    }
                }
            }
            lists[1] = list;
            dispatch.run();
        });

        return () -> {
            r1.remove();
            r2.remove();
        };
    }

    public interface OnActiveSwapListener {
        void onResult(FirestoreSwap swap);
    }

    public void getActiveSwap(String uid1, String uid2, OnActiveSwapListener listener) {
        if (db == null || uid1 == null || uid2 == null) {
            if (listener != null) listener.onResult(null);
            return;
        }

        Task<QuerySnapshot> t1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid1)
                .whereEqualTo("receiverId", uid2)
                .get();

        Task<QuerySnapshot> t2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid2)
                .whereEqualTo("receiverId", uid1)
                .get();

        Tasks.whenAllSuccess(t1, t2).addOnSuccessListener(results -> {
            FirestoreSwap foundSwap = null;
            if (results != null) {
                for (Object res : results) {
                    if (res instanceof QuerySnapshot) {
                        for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                            if ("ACCEPTED".equalsIgnoreCase(doc.getString("status"))) {
                                foundSwap = doc.toObject(FirestoreSwap.class);
                                foundSwap.setId(doc.getId());
                                break;
                            }
                        }
                    }
                    if (foundSwap != null) break;
                }
            }
            if (listener != null) listener.onResult(foundSwap);
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onResult(null);
        });
    }

    /**
     * Checks if there is an ACCEPTED swap between uid1 and uid2.
     */
    public void checkAcceptedSwap(String uid1, String uid2, OnSwapCheckListener listener) {
        if (db == null || uid1 == null || uid2 == null) {
            if (listener != null) listener.onResult(false);
            return;
        }

        Task<QuerySnapshot> t1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid1)
                .whereEqualTo("receiverId", uid2)
                .get();

        Task<QuerySnapshot> t2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid2)
                .whereEqualTo("receiverId", uid1)
                .get();

        Tasks.whenAllSuccess(t1, t2).addOnSuccessListener(results -> {
            boolean found = false;
            if (results != null) {
                for (Object res : results) {
                    if (res instanceof QuerySnapshot) {
                        for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                            if ("ACCEPTED".equalsIgnoreCase(doc.getString("status"))) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) break;
                }
            }
            if (listener != null) listener.onResult(found);
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e);
        });
    }

    /**
     * Ends all swaps (ACCEPTED or PENDING) between uid1 and uid2 by updating status to "ENDED"
     * and setting endedBy to uid1. Does not write to chat so it does not trigger the chat badge.
     */
    public Task<Void> endSwap(String uid1, String uid2) {
        if (db == null || uid1 == null || uid2 == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid arguments"));
        }

        Task<QuerySnapshot> t1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid1)
                .whereEqualTo("receiverId", uid2)
                .get();

        Task<QuerySnapshot> t2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid2)
                .whereEqualTo("receiverId", uid1)
                .get();

        return Tasks.whenAllSuccess(t1, t2).continueWithTask(task -> {
            WriteBatch batch = db.batch();
            List<Object> results = task.getResult();
            if (results != null) {
                for (Object res : results) {
                    if (res instanceof QuerySnapshot) {
                        for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                            String status = doc.getString("status");
                            if ("ACCEPTED".equalsIgnoreCase(status)) {
                                batch.update(doc.getReference(),
                                        "status", "ENDED",
                                        "endedBy", uid1,
                                        "timestamp", FieldValue.serverTimestamp());
                            }
                        }
                    }
                }
            }
            return batch.commit();
        });
    }

    /**
     * Checks if uid1 and uid2 have ever concluded a swap (status is ENDED or COMPLETED).
     * Used to determine eligibility for rating & feedback (only users who have swapped can rate).
     */
    public void checkHasSwappedWith(String uid1, String uid2, OnSwapCheckListener listener) {
        if (db == null || uid1 == null || uid2 == null) {
            if (listener != null) listener.onResult(false);
            return;
        }
        if (uid1.equals(uid2)) {
            if (listener != null) listener.onResult(true);
            return;
        }

        Task<QuerySnapshot> t1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid1)
                .whereEqualTo("receiverId", uid2)
                .get();

        Task<QuerySnapshot> t2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid2)
                .whereEqualTo("receiverId", uid1)
                .get();

        Tasks.whenAllSuccess(t1, t2).addOnSuccessListener(results -> {
            boolean found = false;
            if (results != null) {
                for (Object res : results) {
                    if (res instanceof QuerySnapshot) {
                        for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                            String st = doc.getString("status");
                            if ("ACCEPTED".equalsIgnoreCase(st) || "ENDED".equalsIgnoreCase(st) || "COMPLETED".equalsIgnoreCase(st)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) break;
                }
            }
            if (listener != null) listener.onResult(found);
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e);
        });
    }

    /**
     * Checks if viewer (uid1) can view the private profile of target (uid2).
     * Allowed if viewer is target or they have any active or past swap (ACCEPTED, ENDED, or COMPLETED).
     */
    public void checkCanViewPrivateProfile(String uid1, String uid2, OnSwapCheckListener listener) {
        if (db == null || uid1 == null || uid2 == null) {
            if (listener != null) listener.onResult(false);
            return;
        }
        if (uid1.equals(uid2)) {
            if (listener != null) listener.onResult(true);
            return;
        }

        Task<QuerySnapshot> t1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid1)
                .whereEqualTo("receiverId", uid2)
                .get();

        Task<QuerySnapshot> t2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", uid2)
                .whereEqualTo("receiverId", uid1)
                .get();

        Tasks.whenAllSuccess(t1, t2).addOnSuccessListener(results -> {
            boolean found = false;
            if (results != null) {
                for (Object res : results) {
                    if (res instanceof QuerySnapshot) {
                        for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                            String st = doc.getString("status");
                            Log.d("SWAP_DEBUG", "checkCanViewPrivateProfile found swap doc: " + doc.getId() + ", status=" + st);
                            // Only an active ACCEPTED swap unlocks the private profile
                            if (SWAP_STATUS_ACCEPTED.equalsIgnoreCase(st)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) break;
                }
            }
            Log.d("SWAP_DEBUG", "checkCanViewPrivateProfile final result for (" + uid1 + ", " + uid2 + "): " + found);
            if (listener != null) listener.onResult(found);
        }).addOnFailureListener(e -> {
            Log.e("SWAP_DEBUG", "checkCanViewPrivateProfile failed", e);
            if (listener != null) listener.onError(e);
        });
    }

    // =========================================================================
    // 5. REVIEWS & RATINGS (Resilient Storage in Swaps and Reviews)
    // =========================================================================

    /**
     * Checks if requesterId has a PENDING swap request sent to receiverId.
     */
    public void checkPendingSwap(String requesterId, String receiverId, OnSwapCheckListener listener) {
        if (db == null || requesterId == null || receiverId == null) {
            if (listener != null) listener.onResult(false);
            return;
        }
        db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", requesterId)
                .whereEqualTo("receiverId", receiverId)
                .whereEqualTo("status", SWAP_STATUS_PENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean pending = snapshot != null && !snapshot.isEmpty();
                    if (listener != null) listener.onResult(pending);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e);
                });
    }

    /**
     * Submits or updates a rating & feedback review for a swapped user.
     * Enforces that a swap (ACCEPTED, ENDED, or COMPLETED) exists between reviewer and target.
     * Writes to the Swap document (guaranteed permission) and also attempts Reviews collection.
     */
    public Task<Void> addReview(FirestoreReview review) {
        if (db == null || review == null || review.getTargetUserId() == null || review.getReviewerId() == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid review data"));
        }
        String reviewerId = review.getReviewerId();
        String targetUserId = review.getTargetUserId();

        String docId = review.getReviewId();
        if (docId == null || docId.trim().isEmpty()) {
            docId = reviewerId + "_" + targetUserId;
            review.setReviewId(docId);
        }

        final String finalDocId = docId;
        final Map<String, Object> data = new HashMap<>();
        data.put("reviewId", finalDocId);
        data.put("targetUserId", targetUserId);
        data.put("reviewerId", reviewerId);
        data.put("reviewerName", review.getReviewerName() != null ? review.getReviewerName() : "Swap Partner");
        data.put("reviewerPhotoUrl", review.getReviewerPhotoUrl() != null ? review.getReviewerPhotoUrl() : "");
        data.put("rating", (double) review.getRating());
        data.put("feedback", review.getFeedback() != null ? review.getFeedback() : "");
        data.put("timestamp", FieldValue.serverTimestamp());

        // Verify that an ACCEPTED, ENDED, or COMPLETED swap exists between these users (only swapped users can rate)
        Task<QuerySnapshot> t1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", reviewerId)
                .whereEqualTo("receiverId", targetUserId)
                .get();

        Task<QuerySnapshot> t2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", targetUserId)
                .whereEqualTo("receiverId", reviewerId)
                .get();

        return Tasks.whenAllSuccess(t1, t2).continueWithTask(task -> {
            DocumentSnapshot targetSwapDoc = null;
            List<Object> results = task.getResult();
            if (results != null) {
                for (Object res : results) {
                    if (res instanceof QuerySnapshot) {
                        for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                            String st = doc.getString("status");
                            if ("ACCEPTED".equalsIgnoreCase(st) || "ENDED".equalsIgnoreCase(st) || "COMPLETED".equalsIgnoreCase(st)) {
                                targetSwapDoc = doc;
                                break;
                            }
                        }
                    }
                    if (targetSwapDoc != null) break;
                }
            }

            if (targetSwapDoc == null) {
                return Tasks.forException(new IllegalStateException("You can only rate and give feedback to users you have swapped with."));
            }

            // Write to the Swap document using merge (creates reviews map if not existing, never fails with NOT_FOUND)
            Map<String, Object> updateWrapper = new HashMap<>();
            Map<String, Object> revMap = new HashMap<>();
            revMap.put(reviewerId, data);
            updateWrapper.put("reviews", revMap);
            Task<Void> swapUpdateTask = targetSwapDoc.getReference().set(updateWrapper, SetOptions.merge());

            // Also attempt to write to Reviews collection
            Task<Void> reviewDocTask = db.collection(COLLECTION_REVIEWS).document(finalDocId).set(data, SetOptions.merge());

            return Tasks.whenAllComplete(swapUpdateTask, reviewDocTask).continueWithTask(t -> {
                boolean success = swapUpdateTask.isSuccessful() || (reviewDocTask != null && reviewDocTask.isSuccessful());
                if (success) {
                    if (review.getRating() >= 5.0) {
                        award5StarReviewRewardIfEligible(targetUserId, finalDocId);
                    }
                    String reviewerName = review.getReviewerName() != null ? review.getReviewerName() : "A partner";
                    String ratingStr = String.format(java.util.Locale.US, "%.1f", review.getRating());
                    FirestoreNotification notification = new FirestoreNotification(
                            "review_" + finalDocId,
                            targetUserId,
                            reviewerId,
                            reviewerName,
                            review.getReviewerPhotoUrl() != null ? review.getReviewerPhotoUrl() : "",
                            FirestoreNotification.TYPE_NEW_FEEDBACK,
                            "New Feedback",
                            reviewerName + " rated your swap ⭐" + ratingStr + " and left feedback."
                    );
                    notification.setRelatedReviewId(finalDocId);
                    createNotification(notification);
                    return Tasks.forResult(null);
                } else if (swapUpdateTask.getException() != null) {
                    return Tasks.forException(swapUpdateTask.getException());
                } else {
                    return Tasks.forResult(null);
                }
            });
        });
    }

    /**
     * Deletes a review from both the Swaps document and Reviews collection.
     */
    public Task<Void> deleteReview(String reviewId, String reviewerId, String targetUserId) {
        if (db == null) {
            return Tasks.forException(new IllegalArgumentException("Database not initialized"));
        }

        if (reviewId != null && !reviewId.isEmpty()) {
            try {
                db.collection(COLLECTION_REVIEWS).document(reviewId).delete();
            } catch (Exception ignored) {}
        }

        if (reviewerId == null || targetUserId == null) {
            return Tasks.forResult(null);
        }

        Task<QuerySnapshot> t1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", reviewerId)
                .whereEqualTo("receiverId", targetUserId)
                .get();

        Task<QuerySnapshot> t2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", targetUserId)
                .whereEqualTo("receiverId", reviewerId)
                .get();

        return Tasks.whenAllSuccess(t1, t2).continueWithTask(task -> {
            WriteBatch batch = db.batch();
            boolean found = false;
            List<Object> results = task.getResult();
            if (results != null) {
                for (Object res : results) {
                    if (res instanceof QuerySnapshot) {
                        for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                            batch.update(doc.getReference(), "reviews." + reviewerId, FieldValue.delete());
                            found = true;
                        }
                    }
                }
            }
            if (found) {
                return batch.commit();
            }
            return Tasks.forResult(null);
        });
    }

    public Task<Void> deleteReview(String reviewId) {
        return deleteReview(reviewId, null, null);
    }

    /**
     * Real-time listener for reviews submitted for a target user.
     * Listens to both Swaps and Reviews collections so reviews load regardless of security rule configuration.
     */
    public ListenerRegistration listenToUserReviews(String targetUserId, OnReviewsLoadedListener listener) {
        if (db == null || targetUserId == null) {
            if (listener != null) listener.onError(new IllegalArgumentException("Invalid target user ID"));
            return () -> {};
        }

        final Map<String, Map<String, FirestoreReview>> swapToReviewsMap = new HashMap<>();
        final Map<String, FirestoreReview> reviewsCollectionMap = new HashMap<>();

        Runnable dispatch = () -> {
            Map<String, FirestoreReview> allReviews = new HashMap<>();
            for (Map<String, FirestoreReview> inner : swapToReviewsMap.values()) {
                allReviews.putAll(inner);
            }
            allReviews.putAll(reviewsCollectionMap);

            List<FirestoreReview> list = new ArrayList<>(allReviews.values());
            Collections.sort(list, (r1, r2) -> {
                if (r1.getTimestamp() == null && r2.getTimestamp() == null) return 0;
                if (r1.getTimestamp() == null) return 1;
                if (r2.getTimestamp() == null) return -1;
                return r2.getTimestamp().compareTo(r1.getTimestamp());
            });

            if (listener != null) {
                listener.onReviewsLoaded(list);
            }
        };

        ListenerRegistration reg1 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("receiverId", targetUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Map<String, FirestoreReview> parsed = parseReviewsFromSwapDoc(doc, targetUserId);
                            swapToReviewsMap.put(doc.getId(), parsed);
                        }
                        dispatch.run();
                    }
                });

        ListenerRegistration reg2 = db.collection(COLLECTION_SWAPS)
                .whereEqualTo("requesterId", targetUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Map<String, FirestoreReview> parsed = parseReviewsFromSwapDoc(doc, targetUserId);
                            swapToReviewsMap.put(doc.getId(), parsed);
                        }
                        dispatch.run();
                    }
                });

        ListenerRegistration regReviews = db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("targetUserId", targetUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (snapshot != null) {
                        reviewsCollectionMap.clear();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                FirestoreReview r = doc.toObject(FirestoreReview.class);
                                if (r != null) {
                                    r.setReviewId(doc.getId());
                                    reviewsCollectionMap.put(r.getReviewId(), r);
                                }
                            } catch (Exception ignored) {}
                        }
                        dispatch.run();
                    }
                });

        return () -> {
            reg1.remove();
            reg2.remove();
            regReviews.remove();
        };
    }

    private Map<String, FirestoreReview> parseReviewsFromSwapDoc(DocumentSnapshot doc, String targetUserId) {
        Map<String, FirestoreReview> result = new HashMap<>();
        if (doc == null || !doc.exists()) return result;
        Object rawReviews = doc.get("reviews");
        if (rawReviews instanceof Map) {
            Map<?, ?> reviewsMap = (Map<?, ?>) rawReviews;
            for (Map.Entry<?, ?> entry : reviewsMap.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<?, ?> rData = (Map<?, ?>) entry.getValue();
                    String tUid = (String) rData.get("targetUserId");
                    if (targetUserId.equals(tUid)) {
                        FirestoreReview r = new FirestoreReview();
                        String rId = (String) rData.get("reviewId");
                        String reviewerId = (String) rData.get("reviewerId");
                        r.setReviewId(rId != null ? rId : (reviewerId + "_" + tUid));
                        r.setTargetUserId(tUid);
                        r.setReviewerId(reviewerId);
                        r.setReviewerName((String) rData.get("reviewerName"));
                        r.setReviewerPhotoUrl((String) rData.get("reviewerPhotoUrl"));
                        Object ratingObj = rData.get("rating");
                        if (ratingObj instanceof Number) {
                            r.setRating(((Number) ratingObj).doubleValue());
                        }
                        r.setFeedback((String) rData.get("feedback"));
                        Object tsObj = rData.get("timestamp");
                        if (tsObj instanceof com.google.firebase.Timestamp) {
                            r.setTimestamp(((com.google.firebase.Timestamp) tsObj).toDate());
                        } else if (tsObj instanceof Date) {
                            r.setTimestamp((Date) tsObj);
                        }
                        result.put(r.getReviewId(), r);
                    }
                }
            }
        }
        return result;
    }

    // =========================================================================
    // 6. SKILL COINS ECONOMY (Users/{userId}/CoinTransactions)
    // =========================================================================

    /**
     * Checks and awards the one-time Welcome Starter Bonus (5 Skill Coins) if not yet claimed.
     * Guaranteed idempotent using the deterministic document ID "tx_welcome_{userId}".
     */
    public Task<Void> awardWelcomeBonusIfEligible(String userId) {
        return com.google.android.gms.tasks.Tasks.forResult(null);

    }

    public interface OnDailyRewardListener {
        void onRewardGranted(int dayCount);
        void onAlreadyClaimedOrError();
    }

    public void claimDailyActiveReward(String userId, OnDailyRewardListener listener) {
        if (db == null || userId == null || userId.isEmpty()) {
            if (listener != null) listener.onAlreadyClaimedOrError();
            return;
        }

        String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new Date());
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(userId);

        db.runTransaction((Transaction.Function<Integer>) transaction -> {
            DocumentSnapshot userSnap = transaction.get(userRef);
            if (!userSnap.exists()) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("User document does not exist", 
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND);
            }

            FirestoreUser user = userSnap.toObject(FirestoreUser.class);
            if (user == null) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("User serialization failed", 
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }

            String lastRewardDate = user.getLastActiveRewardDate();
            if (todayStr.equals(lastRewardDate)) {
                // Already claimed today
                return null;
            }

            int currentDays = user.getActiveDaysCount();
            int newDays = currentDays + 1;
            
            int currentCoins = user.getSkillCoins();
            int currentTotal = user.getTotalCoinsEarned();
            
            // Update user document
            transaction.update(userRef, 
                    "lastActiveRewardDate", todayStr,
                    "activeDaysCount", newDays,
                    "skillCoins", currentCoins + 1,
                    "totalCoinsEarned", currentTotal + 1);

            // Create coin transaction
            String txId = "tx_daily_" + todayStr + "_" + userId;
            DocumentReference txRef = userRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).document(txId);
            
            CoinTransaction tx = new CoinTransaction(
                    txId, userId, 1,
                    CoinTransaction.TYPE_DAILY_ACTIVE_REWARD,
                    "Daily Active Day Reward", null,
                    currentCoins, currentCoins + 1
            );
            // set day info in relatedSwapId for display, or we can use reason
            tx.setReason("Daily Active Day Reward (Day " + newDays + ")");
            
            transaction.set(txRef, tx);

            return newDays;
        }).addOnSuccessListener(dayCount -> {
            if (listener != null) {
                if (dayCount != null) {
                    listener.onRewardGranted(dayCount);
                } else {
                    listener.onAlreadyClaimedOrError();
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to claim daily reward", e);
            if (listener != null) listener.onAlreadyClaimedOrError();
        });
    }

    /**
     * Real-time listener on Users/{userId}/CoinTransactions ordered by timestamp descending.
     */
    public ListenerRegistration listenToCoinTransactions(String userId, OnTransactionsLoadedListener listener) {
        if (db == null || userId == null || userId.isEmpty()) {
            if (listener != null) listener.onError(new IllegalArgumentException("Invalid user ID"));
            return () -> {};
        }

        return db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_COIN_TRANSACTIONS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Error listening to coin transactions with orderBy: " + error.getMessage() + ". Attempting fallback query...");
                        // Fallback: Query collection without orderBy and sort in memory
                        db.collection(COLLECTION_USERS)
                                .document(userId)
                                .collection(SUBCOLLECTION_COIN_TRANSACTIONS)
                                .get()
                                .addOnSuccessListener(snap -> {
                                    List<CoinTransaction> fallbackList = new ArrayList<>();
                                    if (snap != null) {
                                        for (DocumentSnapshot doc : snap.getDocuments()) {
                                            try {
                                                CoinTransaction tx = doc.toObject(CoinTransaction.class);
                                                if (tx != null) {
                                                    tx.setTransactionId(doc.getId());
                                                    fallbackList.add(tx);
                                                }
                                            } catch (Exception ignored) {}
                                        }
                                    }
                                    Collections.sort(fallbackList, (a, b) -> {
                                        if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                                        return b.getTimestamp().compareTo(a.getTimestamp());
                                    });
                                    if (listener != null) listener.onTransactionsLoaded(fallbackList);
                                })
                                .addOnFailureListener(fallbackErr -> {
                                    Log.w(TAG, "Fallback coin transactions query error: " + fallbackErr.getMessage());
                                    if (listener != null) listener.onError(fallbackErr);
                                });
                        return;
                    }
                    List<CoinTransaction> list = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                CoinTransaction tx = doc.toObject(CoinTransaction.class);
                                if (tx != null) {
                                    tx.setTransactionId(doc.getId());
                                    list.add(tx);
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error deserializing coin transaction: " + doc.getId(), e);
                            }
                        }
                    }
                    if (listener != null) listener.onTransactionsLoaded(list);
                });
    }

    /**
     * Atomically awards +1 Skill Coin for receiving a 5-star review.
     * Prevents duplicate rewards using deterministic transaction ID "tx_review_{reviewId}".
     */
    public Task<Void> award5StarReviewRewardIfEligible(String targetUserId, String reviewId) {
        if (db == null || targetUserId == null || reviewId == null) {
            return Tasks.forResult(null);
        }

        String txId = "tx_review_" + reviewId;
        DocumentReference userRef = db.collection(COLLECTION_USERS).document(targetUserId);
        DocumentReference txRef = userRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).document(txId);

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot txSnap = transaction.get(txRef);
            if (txSnap.exists()) {
                return null; // Already rewarded!
            }

            DocumentSnapshot userSnap = transaction.get(userRef);
            if (!userSnap.exists()) {
                return null;
            }

            Long coins = userSnap.getLong("skillCoins");
            Long earned = userSnap.getLong("totalCoinsEarned");
            int curBalance = coins != null ? coins.intValue() : 0;
            int curEarned = earned != null ? earned.intValue() : curBalance;

            int newBalance = curBalance + 1;
            int newEarned = curEarned + 1;

            transaction.update(userRef, "skillCoins", newBalance, "totalCoinsEarned", newEarned);

            CoinTransaction tx = new CoinTransaction(
                    txId,
                    targetUserId,
                    1,
                    CoinTransaction.TYPE_FIVE_STAR_REVIEW,
                    "Received 5★ Review Reward",
                    null,
                    curBalance,
                    newBalance
            );
            transaction.set(txRef, tx);
            return null;
        });
    }

    /**
     * Concludes a swap with teaching session rewards:
     * - Learner spends coins according to skill level (1, 2, or 3 coins).
     * - Teacher earns teaching bonus (+1 for 30m, +2 for 1h) and completed swap bonus (+2 coins).
     * - Marked ENDED and completedRewardClaimed = true.
     * - Fully protected against negative balance and duplicate rewards.
     */
    public Task<Void> completeSwapWithTeachingReward(String swapId, String currentUserId, String partnerId, int durationMinutes) {
        if (db == null || swapId == null || currentUserId == null || partnerId == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid swap completion arguments"));
        }

        DocumentReference swapRef = db.collection(COLLECTION_SWAPS).document(swapId);

        return db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot swapSnap = transaction.get(swapRef);
            if (!swapSnap.exists()) {
                throw new FirebaseFirestoreException("Swap document not found", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            Boolean alreadyClaimed = swapSnap.getBoolean("completedRewardClaimed");
            if (alreadyClaimed != null && alreadyClaimed) {
                return null; // Already processed!
            }

            String requesterId = swapSnap.getString("requesterId");
            String receiverId = swapSnap.getString("receiverId");
            Long coinCostVal = swapSnap.getLong("coinCost");
            int cost = (coinCostVal != null && coinCostVal > 0) ? coinCostVal.intValue() : 1;

            // Requester is the learner who requested the receiver's skill
            // Receiver is the teacher
            String learnerId = requesterId;
            String teacherId = receiverId;

            int teachingReward = durationMinutes >= 60 ? 2 : 1;
            String teachingType = durationMinutes >= 60 ? CoinTransaction.TYPE_TEACH_1_HOUR : CoinTransaction.TYPE_TEACH_30_MIN;
            String teachingReason = durationMinutes >= 60 ? "Taught for 1 Hour" : "Taught for 30 Minutes";
            int completedSwapReward = 2;

            DocumentReference learnerRef = (learnerId != null) ? db.collection(COLLECTION_USERS).document(learnerId) : null;
            DocumentReference teacherRef = (teacherId != null) ? db.collection(COLLECTION_USERS).document(teacherId) : null;

            DocumentSnapshot learnerSnap = (learnerRef != null) ? transaction.get(learnerRef) : null;
            DocumentSnapshot teacherSnap = (teacherRef != null) ? transaction.get(teacherRef) : null;

            // 1. Process Learner deduction (prevent negative balance)
            if (learnerRef != null && learnerSnap != null && learnerSnap.exists()) {
                Long lCoins = learnerSnap.getLong("skillCoins");
                int curL = lCoins != null ? lCoins.intValue() : 0;
                int deductAmount = Math.min(cost, Math.max(0, curL));
                int newL = curL - deductAmount;

                transaction.update(learnerRef, "skillCoins", newL);

                String learnerTxId = "tx_learn_" + swapId + "_" + learnerId;
                DocumentReference lTxRef = learnerRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).document(learnerTxId);
                String skillName = swapSnap.getString("requestedSkillName");
                CoinTransaction lTx = new CoinTransaction(
                        learnerTxId,
                        learnerId,
                        -deductAmount,
                        CoinTransaction.TYPE_LEARNING_SESSION_PAYMENT,
                        "Learned " + (skillName != null ? skillName : "Skill"),
                        swapId,
                        curL,
                        newL
                );
                transaction.set(lTxRef, lTx);
            }

            // 2. Process Teacher earnings (+teachingReward and +completedSwapReward)
            if (teacherRef != null && teacherSnap != null && teacherSnap.exists()) {
                Long tCoins = teacherSnap.getLong("skillCoins");
                Long tEarned = teacherSnap.getLong("totalCoinsEarned");
                int curT = tCoins != null ? tCoins.intValue() : 0;
                int curTEarned = tEarned != null ? tEarned.intValue() : curT;

                int totalTeacherGain = teachingReward + completedSwapReward;
                int newT = curT + totalTeacherGain;
                int newTEarned = curTEarned + totalTeacherGain;

                transaction.update(teacherRef, "skillCoins", newT, "totalCoinsEarned", newTEarned);

                // Teaching Reward Transaction
                String teachTxId = "tx_teach_" + swapId + "_" + teacherId;
                DocumentReference tTxRef = teacherRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).document(teachTxId);
                CoinTransaction tTx = new CoinTransaction(
                        teachTxId,
                        teacherId,
                        teachingReward,
                        teachingType,
                        teachingReason,
                        swapId,
                        curT,
                        curT + teachingReward
                );
                transaction.set(tTxRef, tTx);

                // Successful Swap Reward Transaction
                String swapCompleteTxId = "tx_complete_" + swapId + "_" + teacherId;
                DocumentReference cTxRef = teacherRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).document(swapCompleteTxId);
                CoinTransaction cTx = new CoinTransaction(
                        swapCompleteTxId,
                        teacherId,
                        completedSwapReward,
                        CoinTransaction.TYPE_SUCCESSFUL_SWAP,
                        "Successful Swap Completed",
                        swapId,
                        curT + teachingReward,
                        newT
                );
                transaction.set(cTxRef, cTx);
            }

            // 3. Mark swap ended and reward claimed
            Map<String, Object> swapUpdates = new HashMap<>();
            swapUpdates.put("status", "ENDED");
            swapUpdates.put("endedBy", currentUserId);
            swapUpdates.put("completedRewardClaimed", true);
            swapUpdates.put("timestamp", FieldValue.serverTimestamp());
            transaction.update(swapRef, swapUpdates);

            return null;
        });
    }

    // =========================================================================
    // 7. SESSIONS & DUAL CONFIRMATION (Sessions/{sessionId})
    // =========================================================================

    /**
     * Saves a scheduled learning session to Sessions/{sessionId} in Firestore
     * and automatically dispatches the session details chat message to partner.
     */
    public Task<Void> createSession(FirestoreSession session) {
        if (db == null || session == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid session data"));
        }

        if (session.getSessionId() == null || session.getSessionId().trim().isEmpty()) {
            session.setSessionId("sess_" + System.currentTimeMillis());
        }

        if (session.getMeetingLink() == null || session.getMeetingLink().isEmpty()) {
            if (session.getMeetLink() != null && !session.getMeetLink().isEmpty()) {
                session.setMeetingLink(session.getMeetLink());
            } else {
                String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
                String meetUrl = "https://meet.jit.si/SkillSwap-Session-" + uniqueId;
                session.setMeetingLink(meetUrl);
                session.setMeetLink(meetUrl);
            }
        }
        if (session.getMeetingProvider() == null || session.getMeetingProvider().isEmpty()) {
            session.setMeetingProvider("Jitsi Meet");
        }

        DocumentReference sessionRef = db.collection(COLLECTION_SESSIONS).document(session.getSessionId());

        return sessionRef.set(session).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Direct write to Sessions collection failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"), task.getException());
                // Fallback: If swapId is present, persist session under Swaps/{swapId}
                if (session.getSwapId() != null && !session.getSwapId().isEmpty()) {
                    Map<String, Object> swapSessionUpdates = new HashMap<>();
                    swapSessionUpdates.put("meetLink", session.getMeetLink());
                    swapSessionUpdates.put("meetingLink", session.getMeetingLink());
                    swapSessionUpdates.put("scheduledDate", session.getScheduledDate());
                    swapSessionUpdates.put("startTime", session.getStartTime());
                    swapSessionUpdates.put("activeSessionId", session.getSessionId());
                    db.collection(COLLECTION_SWAPS).document(session.getSwapId()).set(swapSessionUpdates, SetOptions.merge());
                }
                return Tasks.forException(task.getException() != null ? task.getException() : new Exception("Failed to save session to Firestore"));
            }

            // Always dispatch notification to partner
            String currentUserId = getCurrentUserId();
            String targetUserId = currentUserId != null && currentUserId.equals(session.getLearnerId())
                    ? session.getTeacherId()
                    : session.getLearnerId();

            if (targetUserId != null) {
                getUserProfile(currentUserId).addOnSuccessListener(me -> {
                    String myName = me != null && me.getName() != null ? me.getName() : "Your partner";
                    FirestoreNotification notification = new FirestoreNotification(
                            "session_sched_" + session.getSessionId(),
                            targetUserId,
                            currentUserId,
                            myName,
                            me != null ? me.getProfilePhotoUrl() : "",
                            FirestoreNotification.TYPE_SESSION_SCHEDULED,
                            "Session Scheduled",
                            "Your SkillSwap session with " + myName + " has been scheduled."
                    );
                    notification.setRelatedSessionId(session.getSessionId());
                    createNotification(notification);
                });
            }

            return Tasks.forResult(null);
        });
    }

    /**
     * Automatically dispatches a formatted session summary chat message into Chats/{chatId}/Messages.
     * Guaranteed idempotent using the deterministic message document ID "msg_session_scheduled_{sessionId}".
     */
    public Task<Void> sendAutomaticSessionChatMessage(FirestoreSession session) {
        if (db == null || session == null) return Tasks.forResult(null);

        String learnerId = session.getLearnerId();
        String teacherId = session.getTeacherId();
        if (learnerId == null || teacherId == null) return Tasks.forResult(null);

        String chatId = getChatId(learnerId, teacherId);

        String msgText = "📅 Session Scheduled\n" +
                session.getSkillName() + " session has been scheduled.\n\n" +
                "📆 Date: " + (session.getScheduledDate() != null ? session.getScheduledDate() : "Upcoming") + "\n" +
                "🕕 Time: " + (session.getStartTime() != null ? session.getStartTime() : "") + "\n" +
                "⏱ Duration: " + (session.getDurationMinutes() == 60 ? "1 hour" : "30 mins") + "\n" +
                "📊 Level: " + session.getSkillLevel() + " (" + session.getCoinCost() + " " + (session.getCoinCost() == 1 ? "Coin" : "Coins") + ")\n\n" +
                "🎥 Jitsi Meet:\n" + (session.getMeetLink() != null ? session.getMeetLink() : "Link Attached") + "\n\n" +
                "See you there!";

        String msgId = "msg_session_scheduled_" + session.getSessionId();
        FirestoreMessage msg = new FirestoreMessage(learnerId, teacherId, msgText);
        msg.setMessageId(msgId);

        return sendMessage(chatId, msg);
    }

    /**
     * Real-time listener on a single session document Sessions/{sessionId}.
     */
    public ListenerRegistration listenToSession(String sessionId, OnSingleSessionListener listener) {
        if (db == null || sessionId == null || sessionId.isEmpty()) {
            if (listener != null) listener.onError(new IllegalArgumentException("Invalid sessionId"));
            return () -> {};
        }

        return db.collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to session " + sessionId, error);
                        if (listener != null) listener.onError(error);
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        FirestoreSession s = snapshot.toObject(FirestoreSession.class);
                        if (s != null) {
                            s.setSessionId(snapshot.getId());
                            if (listener != null) listener.onSessionLoaded(s);
                        }
                    } else if (listener != null) {
                        listener.onSessionLoaded(null);
                    }
                });
    }

    /**
     * Real-time listener on all sessions for a specific swapId ordered by creation time DESC.
     */
    public ListenerRegistration listenToSwapSessions(String swapId, OnSessionsLoadedListener listener) {
        if (db == null || swapId == null || swapId.isEmpty()) {
            if (listener != null) listener.onError(new IllegalArgumentException("Invalid swapId"));
            return () -> {};
        }

        return db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("swapId", swapId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to swap sessions", error);
                        if (listener != null) listener.onError(error);
                        return;
                    }
                    List<FirestoreSession> list = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            FirestoreSession s = doc.toObject(FirestoreSession.class);
                            if (s != null) {
                                s.setSessionId(doc.getId());
                                list.add(s);
                            }
                        }
                    }
                    if (listener != null) listener.onSessionsLoaded(list);
                });
    }

    /**
     * Real-time listener for all sessions where current user is learner or teacher.
     */
    public ListenerRegistration listenToUserSessions(String userId, OnSessionsLoadedListener listener) {
        if (db == null || userId == null || userId.isEmpty()) {
            if (listener != null) listener.onError(new IllegalArgumentException("Invalid userId"));
            return () -> {};
        }

        Query q1 = db.collection(COLLECTION_SESSIONS).whereEqualTo("learnerId", userId);
        Query q2 = db.collection(COLLECTION_SESSIONS).whereEqualTo("teacherId", userId);

        final List<FirestoreSession>[] lists = new List[]{new ArrayList<>(), new ArrayList<>()};

        Runnable dispatch = () -> {
            if (listener == null) return;
            Map<String, FirestoreSession> map = new HashMap<>();
            if (lists[0] != null) {
                for (FirestoreSession s : lists[0]) map.put(s.getSessionId(), s);
            }
            if (lists[1] != null) {
                for (FirestoreSession s : lists[1]) map.put(s.getSessionId(), s);
            }
            List<FirestoreSession> result = new ArrayList<>(map.values());
            Collections.sort(result, (s1, s2) -> Long.compare(s2.getScheduledTimestamp(), s1.getScheduledTimestamp()));
            listener.onSessionsLoaded(result);
        };

        ListenerRegistration r1 = q1.addSnapshotListener((snapshot, error) -> {
            List<FirestoreSession> list = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    FirestoreSession s = doc.toObject(FirestoreSession.class);
                    if (s != null) {
                        s.setSessionId(doc.getId());
                        list.add(s);
                    }
                }
            }
            lists[0] = list;
            dispatch.run();
        });

        ListenerRegistration r2 = q2.addSnapshotListener((snapshot, error) -> {
            List<FirestoreSession> list = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    FirestoreSession s = doc.toObject(FirestoreSession.class);
                    if (s != null) {
                        s.setSessionId(doc.getId());
                        list.add(s);
                    }
                }
            }
            lists[1] = list;
            dispatch.run();
        });

        return () -> {
            r1.remove();
            r2.remove();
        };
    }

    /**
     * DUAL-CONFIRMATION ATOMIC FIRESTORE TRANSACTION:
     * Tapped when a participant confirms session completion.
     * - Learner tap -> learnerConfirmed = true
     * - Teacher tap -> teacherConfirmed = true
     * - First confirmation -> updates status to "PENDING_CONFIRMATION". NO coins released yet.
     * - Second confirmation -> BOTH confirmed! Atomically verifies learner balance >= coinCost,
     *   deducts learner coins, awards teacher teaching + swap completed rewards, creates CoinTransaction records,
     *   sets coinsReleased = true, and marks session COMPLETED.
     */
    public Task<Void> confirmSessionCompletion(String sessionId, String currentUserId) {
        if (db == null || sessionId == null || currentUserId == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid confirmation parameters"));
        }

        DocumentReference sessionRef = db.collection(COLLECTION_SESSIONS).document(sessionId);

        return sessionRef.get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Log.e(TAG, "Session not found for completion: " + sessionId, task.getException());
                return Tasks.forException(new FirebaseFirestoreException("Session not found", FirebaseFirestoreException.Code.NOT_FOUND));
            }

            DocumentSnapshot sessionSnap = task.getResult();
            Boolean alreadyReleased = sessionSnap.getBoolean("coinsReleased");
            Boolean rewardClaimed = sessionSnap.getBoolean("rewardClaimed");
            String status = sessionSnap.getString("status");
            if (Boolean.TRUE.equals(alreadyReleased) || Boolean.TRUE.equals(rewardClaimed) || FirestoreSession.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                return Tasks.forResult(null); // Already completed & coins released! Idempotent guard.
            }

            if (FirestoreSession.STATUS_CANCELLED.equalsIgnoreCase(status)) {
                return Tasks.forException(new FirebaseFirestoreException("Cannot confirm a cancelled session", FirebaseFirestoreException.Code.ABORTED));
            }

            Boolean isDisputed = sessionSnap.getBoolean("disputed");
            if (Boolean.TRUE.equals(isDisputed) || FirestoreSession.STATUS_NEEDS_REVIEW.equalsIgnoreCase(status) || FirestoreSession.STATUS_DISPUTED.equalsIgnoreCase(status)) {
                return Tasks.forException(new FirebaseFirestoreException("This session has a reported problem and needs review before completion", FirebaseFirestoreException.Code.ABORTED));
            }

            String learnerId = sessionSnap.getString("learnerId");
            String teacherId = sessionSnap.getString("teacherId");
            String learnerName = sessionSnap.getString("learnerName");
            String teacherName = sessionSnap.getString("teacherName");
            if (learnerName == null || learnerName.isEmpty()) learnerName = "Learner";
            if (teacherName == null || teacherName.isEmpty()) teacherName = "Teacher";

            if (learnerId == null || teacherId == null) {
                return Tasks.forException(new FirebaseFirestoreException("Invalid session participant IDs", FirebaseFirestoreException.Code.INVALID_ARGUMENT));
            }

            boolean isLearner = currentUserId.equals(learnerId);
            boolean isTeacher = currentUserId.equals(teacherId);
            if (!isLearner && !isTeacher) {
                return Tasks.forException(new FirebaseFirestoreException("User is not a participant of this session", FirebaseFirestoreException.Code.PERMISSION_DENIED));
            }

            Boolean learnerConfirmed = sessionSnap.getBoolean("learnerConfirmed");
            Boolean teacherConfirmed = sessionSnap.getBoolean("teacherConfirmed");

            boolean finalLearnerConfirmed = isLearner || Boolean.TRUE.equals(learnerConfirmed);
            boolean finalTeacherConfirmed = isTeacher || Boolean.TRUE.equals(teacherConfirmed);

            Map<String, Object> sessionUpdates = new HashMap<>();
            sessionUpdates.put("learnerConfirmed", finalLearnerConfirmed);
            sessionUpdates.put("teacherConfirmed", finalTeacherConfirmed);

            if (isLearner) {
                sessionUpdates.put("learnerConfirmedAt", FieldValue.serverTimestamp());
            }
            if (isTeacher) {
                sessionUpdates.put("teacherConfirmedAt", FieldValue.serverTimestamp());
            }

            boolean bothConfirmed = finalLearnerConfirmed && finalTeacherConfirmed;

            if (!bothConfirmed) {
                // First confirmation recorded! Update status to PENDING_CONFIRMATION; do NOT transfer coins yet.
                sessionUpdates.put("status", FirestoreSession.STATUS_PENDING_CONFIRMATION);
                return sessionRef.set(sessionUpdates, SetOptions.merge())
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to record first confirmation on session " + sessionId + ": " + e.getMessage(), e));
            }

            // BOTH confirmed! Mark Session COMPLETED and record coin release
            sessionUpdates.put("status", FirestoreSession.STATUS_COMPLETED);
            sessionUpdates.put("coinsReleased", true);
            sessionUpdates.put("rewardClaimed", true);
            sessionUpdates.put("completedAt", FieldValue.serverTimestamp());

            final Long coinCostLong = sessionSnap.getLong("coinCost");
            final int coinCost = coinCostLong != null && coinCostLong > 0 ? coinCostLong.intValue() : 1;
            final String skillName = sessionSnap.getString("skillName") != null ? sessionSnap.getString("skillName") : "Skill";
            final String finalTeacherName = teacherName;
            final String finalLearnerName = learnerName;

            return sessionRef.set(sessionUpdates, SetOptions.merge()).continueWithTask(updateTask -> {
                if (!updateTask.isSuccessful()) {
                    Log.e(TAG, "Failed to update session to COMPLETED: " + sessionId, updateTask.getException());
                    return Tasks.forException(updateTask.getException() != null ? updateTask.getException() : new Exception("Failed to complete session"));
                }

                // Both confirmed and session marked completed. Now process coin transfer
                return db.runTransaction(tx -> {
                    DocumentReference learnerRef = db.collection(COLLECTION_USERS).document(learnerId);
                    DocumentReference teacherRef = db.collection(COLLECTION_USERS).document(teacherId);

                    DocumentSnapshot learnerSnap = tx.get(learnerRef);
                    DocumentSnapshot teacherSnap = tx.get(teacherRef);

                    // 1. Deduct coins from Learner
                    if (learnerSnap.exists()) {
                        Long lCoins = learnerSnap.getLong("skillCoins");
                        Long lSpent = learnerSnap.getLong("totalCoinsSpent");
                        int curL = lCoins != null ? lCoins.intValue() : 0;
                        int curLSpent = lSpent != null ? lSpent.intValue() : 0;

                        int newL = Math.max(0, curL - coinCost);
                        int newLSpent = curLSpent + coinCost;

                        tx.update(learnerRef, "skillCoins", newL, "totalCoinsSpent", newLSpent);

                        String learnTxId = "tx_learn_session_" + sessionId;
                        DocumentReference lTxRef = learnerRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).document(learnTxId);
                        CoinTransaction lTx = new CoinTransaction(
                                learnTxId,
                                learnerId,
                                -coinCost,
                                CoinTransaction.TYPE_LEARNING_SESSION_PAYMENT,
                                "Completed " + skillName + " session with " + finalTeacherName,
                                sessionId,
                                curL,
                                newL
                        );
                        lTx.setSessionId(sessionId);
                        tx.set(lTxRef, lTx);
                    }

                    // 2. Credit coins to Teacher
                    if (teacherSnap.exists()) {
                        Long tCoins = teacherSnap.getLong("skillCoins");
                        Long tEarned = teacherSnap.getLong("totalCoinsEarned");
                        int curT = tCoins != null ? tCoins.intValue() : 0;
                        int curTEarned = tEarned != null ? tEarned.intValue() : 0;

                        int newT = curT + coinCost;
                        int newTEarned = curTEarned + coinCost;

                        tx.update(teacherRef, "skillCoins", newT, "totalCoinsEarned", newTEarned);

                        String teachTxId = "tx_teach_session_" + sessionId;
                        DocumentReference tTxRef = teacherRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).document(teachTxId);
                        CoinTransaction tTx = new CoinTransaction(
                                teachTxId,
                                teacherId,
                                coinCost,
                                CoinTransaction.TYPE_TEACHING_SESSION_COMPLETED,
                                "Completed teaching " + skillName + " to " + finalLearnerName,
                                sessionId,
                                curT,
                                newT
                        );
                        tTx.setSessionId(sessionId);
                        tx.set(tTxRef, tTx);
                    }
                    return null;
                }).continueWith(coinTask -> {
                    if (!coinTask.isSuccessful()) {
                        Log.w(TAG, "Coin transfer transaction failed (security rules may restrict cross-user writes): " +
                                (coinTask.getException() != null ? coinTask.getException().getMessage() : "Unknown"), coinTask.getException());
                        // Fallback: update current user's own wallet in isolation so their transaction is recorded
                        DocumentReference myRef = db.collection(COLLECTION_USERS).document(currentUserId);
                        myRef.get().addOnSuccessListener(mySnap -> {
                            if (mySnap.exists()) {
                                Long myCoinsLong = mySnap.getLong("skillCoins");
                                int myCurCoins = myCoinsLong != null ? myCoinsLong.intValue() : 0;
                                if (isTeacher) {
                                    int newCoins = myCurCoins + coinCost;
                                    Long earnedLong = mySnap.getLong("totalCoinsEarned");
                                    int newEarned = (earnedLong != null ? earnedLong.intValue() : 0) + coinCost;
                                    Map<String, Object> myUpdates = new HashMap<>();
                                    myUpdates.put("skillCoins", newCoins);
                                    myUpdates.put("totalCoinsEarned", newEarned);
                                    myRef.set(myUpdates, SetOptions.merge());

                                    String teachTxId = "tx_teach_session_" + sessionId;
                                    DocumentReference tTxRef = myRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).document(teachTxId);
                                    CoinTransaction tTx = new CoinTransaction(
                                            teachTxId, currentUserId, coinCost,
                                            CoinTransaction.TYPE_TEACHING_SESSION_COMPLETED,
                                            "Completed teaching " + skillName + " to " + finalLearnerName,
                                            sessionId, myCurCoins, newCoins
                                    );
                                    tTx.setSessionId(sessionId);
                                    tTxRef.set(tTx, SetOptions.merge());
                                } else if (isLearner) {
                                    int newCoins = Math.max(0, myCurCoins - coinCost);
                                    Long spentLong = mySnap.getLong("totalCoinsSpent");
                                    int newSpent = (spentLong != null ? spentLong.intValue() : 0) + coinCost;
                                    Map<String, Object> myUpdates = new HashMap<>();
                                    myUpdates.put("skillCoins", newCoins);
                                    myUpdates.put("totalCoinsSpent", newSpent);
                                    myRef.set(myUpdates, SetOptions.merge());

                                    String learnTxId = "tx_learn_session_" + sessionId;
                                    DocumentReference lTxRef = myRef.collection(SUBCOLLECTION_COIN_TRANSACTIONS).document(learnTxId);
                                    CoinTransaction lTx = new CoinTransaction(
                                            learnTxId, currentUserId, -coinCost,
                                            CoinTransaction.TYPE_LEARNING_SESSION_PAYMENT,
                                            "Completed " + skillName + " session with " + finalTeacherName,
                                            sessionId, myCurCoins, newCoins
                                    );
                                    lTx.setSessionId(sessionId);
                                    lTxRef.set(lTx, SetOptions.merge());
                                }
                            }
                        });
                    }
                    return null;
                });
            });
        });
    }

    /**
     * Reports a problem with a session, marking it as NEEDS_REVIEW / DISPUTED to prevent automatic coin release.
     */
    public Task<Void> reportSessionProblem(String sessionId, String reportedBy, String reason) {
        if (db == null || sessionId == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid sessionId"));
        }
        Map<String, Object> map = new HashMap<>();
        map.put("status", FirestoreSession.STATUS_NEEDS_REVIEW);
        map.put("disputed", true);
        map.put("disputedBy", reportedBy);
        map.put("disputeReason", reason != null && !reason.trim().isEmpty() ? reason.trim() : "Problem reported by participant");
        map.put("disputedAt", FieldValue.serverTimestamp());

        return db.collection(COLLECTION_SESSIONS).document(sessionId).update(map);
    }

    /**
     * Creates or reuses a Jitsi Meet link for a chat between two users,
     * attaching it to any active session and sending the meet card into the chat.
     */
    public Task<String> getOrCreateMeetLinkForChat(String chatId, String currentUserId, String partnerId, String partnerName, String skillName) {
        if (db == null) return Tasks.forException(new IllegalStateException("Firestore unavailable"));

        return db.collection(COLLECTION_SESSIONS)
                .whereIn("status", Arrays.asList(
                        FirestoreSession.STATUS_REQUESTED,
                        FirestoreSession.STATUS_ACCEPTED,
                        FirestoreSession.STATUS_SCHEDULED,
                        FirestoreSession.STATUS_IN_PROGRESS,
                        FirestoreSession.STATUS_WAITING_CONFIRMATION,
                        FirestoreSession.STATUS_PENDING_CONFIRMATION
                ))
                .get()
                .continueWithTask(task -> {
                    String existingMeeting = null;
                    DocumentReference targetSessionDoc = null;

                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String lId = doc.getString("learnerId");
                            String tId = doc.getString("teacherId");
                            if ((currentUserId.equals(lId) && partnerId.equals(tId)) ||
                                (currentUserId.equals(tId) && partnerId.equals(lId))) {
                                targetSessionDoc = doc.getReference();
                                String link = doc.getString("meetLink");
                                if (link == null || link.isEmpty()) link = doc.getString("meetingLink");
                                if (link != null && !link.trim().isEmpty()) {
                                    existingMeeting = link.trim();
                                    break;
                                }
                            }
                        }
                    }

                    if (existingMeeting != null) {
                        return Tasks.forResult(existingMeeting);
                    }

                    // Generate a new clean Jitsi Meet URL
                    String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
                    String meetUrl = "https://meet.jit.si/SkillSwap-Session-" + uniqueId;

                    if (targetSessionDoc != null) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("meetLink", meetUrl);
                        updates.put("meetingLink", meetUrl);
                        updates.put("meetingProvider", "Jitsi Meet");
                        updates.put("meetingCreatedAt", FieldValue.serverTimestamp());
                        targetSessionDoc.update(updates);
                    }

                    // Automatically post meeting invitation message to chat
                    String msgText = "🎥 Jitsi Meet\n" +
                            (skillName != null && !skillName.isEmpty() ? skillName + " Skill Session\n\n" : "Skill Swap Session\n\n") +
                            "Tap below to join the video session:\n" +
                            meetUrl;

                    FirestoreMessage msg = new FirestoreMessage(currentUserId, partnerId, msgText);
                    msg.setMessageId("msg_meet_" + System.currentTimeMillis());
                    sendMessage(chatId, msg);

                    return Tasks.forResult(meetUrl);
                });
    }

    public interface OnMeetLinkListener {
        void onResult(String meetLink);
    }

    public void findActiveMeetLink(String currentUserId, String partnerId, OnMeetLinkListener listener) {
        if (db == null || currentUserId == null || partnerId == null) {
            if (listener != null) listener.onResult(null);
            return;
        }
        db.collection(COLLECTION_SESSIONS)
                .whereIn("status", Arrays.asList(
                        FirestoreSession.STATUS_REQUESTED,
                        FirestoreSession.STATUS_ACCEPTED,
                        FirestoreSession.STATUS_SCHEDULED,
                        FirestoreSession.STATUS_IN_PROGRESS,
                        FirestoreSession.STATUS_WAITING_CONFIRMATION,
                        FirestoreSession.STATUS_PENDING_CONFIRMATION
                ))
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String lId = doc.getString("learnerId");
                            String tId = doc.getString("teacherId");
                            if ((currentUserId.equals(lId) && partnerId.equals(tId)) ||
                                (currentUserId.equals(tId) && partnerId.equals(lId))) {
                                String link = doc.getString("meetLink");
                                if (link == null || link.isEmpty()) link = doc.getString("meetingLink");
                                if (link != null && !link.isEmpty()) {
                                    if (listener != null) listener.onResult(link);
                                    return;
                                }
                            }
                        }
                    }
                    if (listener != null) listener.onResult(null);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onResult(null);
                });
    }

    public interface OnActiveSessionFoundListener {
        void onResult(String sessionId, String meetLink);
    }

    public void findActiveSession(String currentUserId, String partnerId, OnActiveSessionFoundListener listener) {
        if (db == null || currentUserId == null || partnerId == null) {
            if (listener != null) listener.onResult(null, null);
            return;
        }

        // Query 1: current user is teacher, partner is learner
        db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("teacherId", currentUserId)
                .whereEqualTo("learnerId", partnerId)
                .get()
                .addOnSuccessListener(snap1 -> {
                    DocumentSnapshot activeDoc = findMostRelevantSession(snap1);
                    if (activeDoc != null) {
                        String link = activeDoc.getString("meetLink");
                        if (link == null || link.isEmpty()) link = activeDoc.getString("meetingLink");
                        if (listener != null) listener.onResult(activeDoc.getId(), link);
                        return;
                    }

                    // Query 2: current user is learner, partner is teacher
                    db.collection(COLLECTION_SESSIONS)
                            .whereEqualTo("learnerId", currentUserId)
                            .whereEqualTo("teacherId", partnerId)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                DocumentSnapshot activeDoc2 = findMostRelevantSession(snap2);
                                if (activeDoc2 != null) {
                                    String link = activeDoc2.getString("meetLink");
                                    if (link == null || link.isEmpty()) link = activeDoc2.getString("meetingLink");
                                    if (listener != null) listener.onResult(activeDoc2.getId(), link);
                                    return;
                                }
                                if (listener != null) listener.onResult(null, null);
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) listener.onResult(null, null);
                            });
                })
                .addOnFailureListener(e -> {
                    // Fallback to query 2
                    db.collection(COLLECTION_SESSIONS)
                            .whereEqualTo("learnerId", currentUserId)
                            .whereEqualTo("teacherId", partnerId)
                            .get()
                            .addOnSuccessListener(snap2 -> {
                                DocumentSnapshot activeDoc2 = findMostRelevantSession(snap2);
                                if (activeDoc2 != null) {
                                    String link = activeDoc2.getString("meetLink");
                                    if (link == null || link.isEmpty()) link = activeDoc2.getString("meetingLink");
                                    if (listener != null) listener.onResult(activeDoc2.getId(), link);
                                    return;
                                }
                                if (listener != null) listener.onResult(null, null);
                            })
                            .addOnFailureListener(err -> {
                                if (listener != null) listener.onResult(null, null);
                            });
                });
    }

    private DocumentSnapshot findMostRelevantSession(QuerySnapshot snap) {
        if (snap == null || snap.isEmpty()) return null;
        List<String> activeStatuses = Arrays.asList(
                FirestoreSession.STATUS_SCHEDULED,
                FirestoreSession.STATUS_IN_PROGRESS,
                FirestoreSession.STATUS_WAITING_CONFIRMATION,
                FirestoreSession.STATUS_PENDING_CONFIRMATION,
                FirestoreSession.STATUS_REQUESTED,
                FirestoreSession.STATUS_ACCEPTED
        );
        DocumentSnapshot fallback = null;
        for (DocumentSnapshot doc : snap.getDocuments()) {
            String status = doc.getString("status");
            if (status != null && activeStatuses.contains(status)) {
                return doc;
            }
            if (fallback == null) fallback = doc;
        }
        return fallback;
    }

    /**
     * Cancels a scheduled session before completion.
     */
    public Task<Void> cancelSession(String sessionId, String cancelledBy, String reason) {
        if (db == null || sessionId == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid sessionId"));
        }
        Map<String, Object> map = new HashMap<>();
        map.put("status", FirestoreSession.STATUS_CANCELLED);
        map.put("cancelledBy", cancelledBy);
        map.put("cancellationReason", reason != null ? reason : "Cancelled by user");
        map.put("cancelledAt", FieldValue.serverTimestamp());

        return db.collection(COLLECTION_SESSIONS).document(sessionId)
                .set(map, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Error cancelling session " + sessionId + ": " + e.getMessage(), e));
    }

    /**
     * Deletes a session document completely from Firestore.
     */
    public Task<Void> deleteSession(String sessionId) {
        if (db == null || sessionId == null) {
            return Tasks.forException(new IllegalArgumentException("Invalid sessionId"));
        }
        return db.collection(COLLECTION_SESSIONS).document(sessionId).delete();
    }

    /**
     * Checks if there is a COMPLETED session with coinsReleased == true between uid1 and uid2.
     * Used to unlock the rating/review action button on UserProfileActivity.
     */
    public void checkHasCompletedSessionWith(String uid1, String uid2, OnSwapCheckListener listener) {
        if (db == null || uid1 == null || uid2 == null) {
            if (listener != null) listener.onResult(false);
            return;
        }

        Task<QuerySnapshot> t1 = db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("learnerId", uid1)
                .whereEqualTo("teacherId", uid2)
                .get();

        Task<QuerySnapshot> t2 = db.collection(COLLECTION_SESSIONS)
                .whereEqualTo("learnerId", uid2)
                .whereEqualTo("teacherId", uid1)
                .get();

        Tasks.whenAllSuccess(t1, t2).addOnSuccessListener(results -> {
            boolean found = false;
            if (results != null) {
                for (Object res : results) {
                    if (res instanceof QuerySnapshot) {
                        for (DocumentSnapshot doc : ((QuerySnapshot) res).getDocuments()) {
                            Boolean released = doc.getBoolean("coinsReleased");
                            String status = doc.getString("status");
                            if (Boolean.TRUE.equals(released) || FirestoreSession.STATUS_COMPLETED.equalsIgnoreCase(status)) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) break;
                }
            }
            if (listener != null) listener.onResult(found);
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e);
        });
    }

    // ==========================================
    // NOTIFICATION & FCM TOKEN REPOSITORY METHODS
    // ==========================================

    public interface OnNotificationsLoadedListener {
        void onNotificationsLoaded(List<FirestoreNotification> notifications);
        void onError(Exception e);
    }

    public Task<Void> updateFcmToken(String uid, String token) {
        if (db == null || uid == null || uid.isEmpty() || token == null) {
            return Tasks.forResult(null);
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        return db.collection(COLLECTION_USERS).document(uid).set(updates, SetOptions.merge());
    }

    public Task<Void> createNotification(FirestoreNotification notification) {
        if (db == null || notification == null || notification.getRecipientUserId() == null) {
            return Tasks.forResult(null);
        }
        String docId = notification.getNotificationId();
        if (docId == null || docId.isEmpty()) {
            docId = db.collection(COLLECTION_NOTIFICATIONS).document().getId();
            notification.setNotificationId(docId);
        }
        return db.collection(COLLECTION_NOTIFICATIONS).document(docId).set(notification, SetOptions.merge());
    }

    public ListenerRegistration listenToUserNotifications(String recipientUserId, OnNotificationsLoadedListener listener) {
        if (db == null || recipientUserId == null || recipientUserId.isEmpty()) {
            if (listener != null) listener.onNotificationsLoaded(new ArrayList<>());
            return null;
        }
        return db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientUserId", recipientUserId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        if (listener != null) listener.onError(e);
                        return;
                    }
                    List<FirestoreNotification> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            FirestoreNotification n = doc.toObject(FirestoreNotification.class);
                            if (n != null) {
                                if (n.getNotificationId() == null) n.setNotificationId(doc.getId());
                                list.add(n);
                            }
                        }
                    }
                    // Sort by timestamp DESCENDING so newest notification is at the top
                    Collections.sort(list, (n1, n2) -> {
                        if (n1.getTimestamp() == null && n2.getTimestamp() == null) return 0;
                        if (n1.getTimestamp() == null) return 1;
                        if (n2.getTimestamp() == null) return -1;
                        return n2.getTimestamp().compareTo(n1.getTimestamp());
                    });
                    if (listener != null) listener.onNotificationsLoaded(list);
                });
    }

    public Task<Void> markNotificationAsRead(String notificationId) {
        if (db == null || notificationId == null || notificationId.isEmpty()) {
            return Tasks.forResult(null);
        }
        Map<String, Object> update = new HashMap<>();
        update.put("read", true);
        return db.collection(COLLECTION_NOTIFICATIONS).document(notificationId).set(update, SetOptions.merge());
    }

    public Task<Void> markAllNotificationsAsRead(String recipientUserId) {
        if (db == null || recipientUserId == null || recipientUserId.isEmpty()) {
            return Tasks.forResult(null);
        }
        return db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientUserId", recipientUserId)
                .whereEqualTo("read", false)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return Tasks.forResult(null);
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    return batch.commit();
                });
    }

    public Task<Void> clearAllNotifications(String recipientUserId) {
        if (db == null || recipientUserId == null || recipientUserId.isEmpty()) {
            return Tasks.forResult(null);
        }
        return db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientUserId", recipientUserId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Exception error = task.getException() != null ? task.getException() : new Exception("Failed to query notifications");
                        return Tasks.forException(error);
                    }
                    List<DocumentSnapshot> docs = task.getResult().getDocuments();
                    if (docs.isEmpty()) {
                        return Tasks.forResult(null);
                    }
                    List<Task<Void>> commitTasks = new ArrayList<>();
                    WriteBatch batch = db.batch();
                    int count = 0;
                    for (DocumentSnapshot doc : docs) {
                        batch.delete(doc.getReference());
                        count++;
                        if (count % 450 == 0) {
                            commitTasks.add(batch.commit());
                            batch = db.batch();
                        }
                    }
                    if (count % 450 != 0) {
                        commitTasks.add(batch.commit());
                    }
                    return Tasks.whenAll(commitTasks);
                });
    }
}

