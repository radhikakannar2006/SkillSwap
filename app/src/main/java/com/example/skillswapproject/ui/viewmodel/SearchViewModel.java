package com.example.skillswapproject.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel that manages real-time user search from Firestore.
 * Listens to ALL public users once, then filters locally by name query.
 * This avoids per-keystroke Firestore reads.
 */
public class SearchViewModel extends AndroidViewModel {

    private final FirestoreRepository repository;

    // Full list of all users fetched from Firestore
    private final List<FirestoreUser> allUsers = new ArrayList<>();

    // The currently filtered (search result) list
    private final MutableLiveData<List<FirestoreUser>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private ListenerRegistration usersRegistration;
    private String currentQuery = "";

    public SearchViewModel(@NonNull Application application) {
        super(application);
        this.repository = FirestoreRepository.getInstance();
        loadAllUsers();
    }

    public LiveData<List<FirestoreUser>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Called whenever the search query changes.
     * Filters the already-fetched user list locally — no extra Firestore reads.
     */
    public void search(String query) {
        this.currentQuery = query == null ? "" : query.trim().toLowerCase();
        applyFilter();
    }

    /**
     * Listens in real-time to ALL public users in Firestore (excluding self).
     * Called once on init. Results are cached in allUsers list.
     */
    private void loadAllUsers() {
        isLoading.setValue(true);
        String currentUserId = repository.getCurrentUserId();

        repository.getUserProfile(currentUserId).addOnSuccessListener(myProfile -> {
            usersRegistration = repository.listenToPublicUsers(currentUserId, new FirestoreRepository.OnPublicUsersListener() {
                @Override
                public void onUsersLoaded(List<FirestoreUser> users) {
                    allUsers.clear();
                    if (users != null) {
                        for (FirestoreUser fu : users) {
                            com.example.skillswapproject.util.CompatibilityMatcher.MatchResult matchRes =
                                    com.example.skillswapproject.util.CompatibilityMatcher.calculateMatch(myProfile, fu);
                            fu.setCompatibilityScore(matchRes.getOverallScore());
                            allUsers.add(fu);
                        }
                    }
                    isLoading.postValue(false);
                    applyFilter();
                }

                @Override
                public void onError(Exception e) {
                    isLoading.postValue(false);
                }
            });
        }).addOnFailureListener(e -> {
            usersRegistration = repository.listenToPublicUsers(currentUserId, new FirestoreRepository.OnPublicUsersListener() {
                @Override
                public void onUsersLoaded(List<FirestoreUser> users) {
                    allUsers.clear();
                    if (users != null) {
                        allUsers.addAll(users);
                    }
                    isLoading.postValue(false);
                    applyFilter();
                }

                @Override
                public void onError(Exception ex) {
                    isLoading.postValue(false);
                }
            });
        });
    }

    /**
     * Filters allUsers list by matching First Name, Last Name, Full Name, Offered Skills,
     * Wanted Skills, or Location against the current query.
     * If query is empty, returns all users.
     */
    private void applyFilter() {
        List<FirestoreUser> filtered = new ArrayList<>();
        if (currentQuery.isEmpty()) {
            filtered.addAll(allUsers);
        } else {
            for (FirestoreUser user : allUsers) {
                if (matchesQuery(user, currentQuery)) {
                    filtered.add(user);
                }
            }
        }
        // Sort search results by AI Compatibility Score DESCENDING
        java.util.Collections.sort(filtered, (u1, u2) -> Integer.compare(u2.getCompatibilityScore(), u1.getCompatibilityScore()));
        searchResults.postValue(filtered);
    }

    private boolean matchesQuery(FirestoreUser user, String query) {
        if (user == null || query == null || query.trim().isEmpty()) return false;
        String q = query.trim().toLowerCase();

        // 1. First Name
        if (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(q)) {
            return true;
        }
        // 2. Last Name
        if (user.getLastName() != null && user.getLastName().toLowerCase().contains(q)) {
            return true;
        }
        // 3. Full Name
        if (user.getName() != null && user.getName().toLowerCase().contains(q)) {
            return true;
        }
        // 4. Location
        if (user.getLocation() != null && user.getLocation().toLowerCase().contains(q)) {
            return true;
        }
        // 5. Skills I Can Teach
        if (user.getOfferedSkills() != null) {
            for (String skill : user.getOfferedSkills()) {
                if (skill != null && skill.toLowerCase().contains(q)) {
                    return true;
                }
            }
        }
        // 6. Skills I Want to Learn
        if (user.getWantedSkills() != null) {
            for (String skill : user.getWantedSkills()) {
                if (skill != null && skill.toLowerCase().contains(q)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (usersRegistration != null) {
            usersRegistration.remove();
        }
    }
}
