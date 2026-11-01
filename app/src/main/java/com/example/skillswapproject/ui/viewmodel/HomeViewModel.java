package com.example.skillswapproject.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.model.Skill;
import com.example.skillswapproject.data.model.User;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final FirestoreRepository firestoreRepository;

    private final MutableLiveData<List<Skill>> popularSkillsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<User>> recommendedUsersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> pendingSwapsCountLiveData = new MutableLiveData<>(0);
    private ListenerRegistration publicUsersRegistration;
    private ListenerRegistration pendingSwapsRegistration;

    private final List<User> allUsersList = new ArrayList<>();
    private String selectedSkill = "All";

    private com.google.firebase.auth.FirebaseAuth.AuthStateListener authStateListener;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.firestoreRepository = FirestoreRepository.getInstance();

        loadPopularSkills();
        listenToUsersFeed();
        listenToPendingSwaps();

        if (firestoreRepository.getAuth() != null) {
            authStateListener = auth -> {
                String uid = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
                if (uid != null) {
                    if (pendingSwapsRegistration == null) {
                        listenToPendingSwaps();
                    }
                    if (publicUsersRegistration == null) {
                        listenToUsersFeed();
                    }
                }
            };
            firestoreRepository.getAuth().addAuthStateListener(authStateListener);
        }
    }

    public LiveData<Integer> getPendingSwapsCount() {
        return pendingSwapsCountLiveData;
    }

    public LiveData<List<Skill>> getPopularSkills() {
        return popularSkillsLiveData;
    }

    public LiveData<List<User>> getRecommendedUsers() {
        return recommendedUsersLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public void filterBySkill(String skillName) {
        this.selectedSkill = (skillName != null) ? skillName : "All";
        applyFilter();
    }

    public String getSelectedSkill() {
        return selectedSkill;
    }

    private final java.util.Set<String> activeSwappedUserIds = new java.util.HashSet<>();

    private void applyFilter() {
        boolean isAll = (selectedSkill == null || selectedSkill.equalsIgnoreCase("All") || selectedSkill.trim().isEmpty());
        String filterLower = isAll ? "" : selectedSkill.toLowerCase().trim();

        List<User> filtered = new ArrayList<>();
        for (User u : allUsersList) {
            // Do not show people I have already swapped with (active ACCEPTED swaps)
            if (u.getId() != null && activeSwappedUserIds.contains(u.getId())) {
                continue;
            }

            if (isAll) {
                filtered.add(u);
                continue;
            }

            boolean matches = false;
            if (u.getOfferedSkills() != null) {
                for (Skill s : u.getOfferedSkills()) {
                    if (s.getName() != null && s.getName().toLowerCase().contains(filterLower)) {
                        matches = true;
                        break;
                    }
                }
            }
            if (!matches && u.getWantedSkills() != null) {
                for (Skill s : u.getWantedSkills()) {
                    if (s.getName() != null && s.getName().toLowerCase().contains(filterLower)) {
                        matches = true;
                        break;
                    }
                }
            }
            if (matches) {
                filtered.add(u);
            }
        }
        // Sort by AI Compatibility Score DESCENDING so top matches appear first
        java.util.Collections.sort(filtered, (u1, u2) -> Integer.compare(u2.getCompatibilityScore(), u1.getCompatibilityScore()));
        if (filtered.size() > 5) {
            filtered = new ArrayList<>(filtered.subList(0, 5));
        }
        recommendedUsersLiveData.postValue(filtered);
    }

    private void loadPopularSkills() {
        List<Skill> skills = new ArrayList<>();
        skills.add(new Skill("0", "All", "All"));
        for (int i = 0; i < com.example.skillswapproject.util.SkillConstants.PREDEFINED_SKILLS.length; i++) {
            String name = com.example.skillswapproject.util.SkillConstants.PREDEFINED_SKILLS[i];
            skills.add(new Skill(String.valueOf(i + 1), name, "General"));
        }
        popularSkillsLiveData.setValue(skills);
    }

    private void listenToUsersFeed() {
        isLoadingLiveData.setValue(true);
        String currentUserId = firestoreRepository.getCurrentUserId();

        firestoreRepository.getUserProfile(currentUserId).addOnSuccessListener(myProfile -> {
            publicUsersRegistration = firestoreRepository.listenToPublicUsers(currentUserId, new FirestoreRepository.OnPublicUsersListener() {
                @Override
                public void onUsersLoaded(List<FirestoreUser> fUsers) {
                    isLoadingLiveData.postValue(false);
                    allUsersList.clear();
                    if (fUsers != null) {
                        for (FirestoreUser fu : fUsers) {
                            com.example.skillswapproject.util.CompatibilityMatcher.MatchResult matchRes =
                                    com.example.skillswapproject.util.CompatibilityMatcher.calculateMatch(myProfile, fu);
                            fu.setCompatibilityScore(matchRes.getOverallScore());

                            User user = convertFirestoreUser(fu);
                            user.setCompatibilityScore(matchRes.getOverallScore());
                            allUsersList.add(user);
                        }
                    }
                    applyFilter();
                }

                @Override
                public void onError(Exception e) {
                    isLoadingLiveData.postValue(false);
                    allUsersList.clear();
                    recommendedUsersLiveData.postValue(new ArrayList<>());
                }
            });
        }).addOnFailureListener(e -> {
            publicUsersRegistration = firestoreRepository.listenToPublicUsers(currentUserId, new FirestoreRepository.OnPublicUsersListener() {
                @Override
                public void onUsersLoaded(List<FirestoreUser> fUsers) {
                    isLoadingLiveData.postValue(false);
                    allUsersList.clear();
                    if (fUsers != null) {
                        for (FirestoreUser fu : fUsers) {
                            allUsersList.add(convertFirestoreUser(fu));
                        }
                    }
                    applyFilter();
                }

                @Override
                public void onError(Exception ex) {
                    isLoadingLiveData.postValue(false);
                    allUsersList.clear();
                    recommendedUsersLiveData.postValue(new ArrayList<>());
                }
            });
        });
    }

    private User convertFirestoreUser(FirestoreUser fu) {
        List<Skill> offered = new ArrayList<>();
        if (fu.getOfferedSkills() != null) {
            for (String s : fu.getOfferedSkills()) {
                offered.add(new Skill("s_" + s.hashCode(), s, "General"));
            }
        }
        List<Skill> wanted = new ArrayList<>();
        if (fu.getWantedSkills() != null) {
            for (String s : fu.getWantedSkills()) {
                wanted.add(new Skill("w_" + s.hashCode(), s, "General"));
            }
        }
        return new User(
                fu.getUid(),
                fu.getName(),
                fu.getEmail(),
                "",
                fu.getLocation() != null ? fu.getLocation() : "Nearby",
                fu.getProfilePhotoUrl(),
                fu.isPublic(),
                5.0,
                3,
                2,
                10,
                com.example.skillswapproject.data.model.Role.STANDARD,
                offered,
                wanted,
                fu.getAvailability() != null ? fu.getAvailability() : "Open to Swaps",
                "Passionate about skill swapping!"
        );
    }

    private List<com.example.skillswapproject.data.model.FirestoreSwap> lastReceivedSwaps = new ArrayList<>();
    private List<com.example.skillswapproject.data.model.FirestoreSwap> lastSentSwaps = new ArrayList<>();

    private void listenToPendingSwaps() {
        String currentUserId = firestoreRepository.getCurrentUserId();
        if (currentUserId == null) return;

        pendingSwapsRegistration = firestoreRepository.listenToUserSwaps(currentUserId, new FirestoreRepository.OnUserSwapsListener() {
            @Override
            public void onSwapsLoaded(List<com.example.skillswapproject.data.model.FirestoreSwap> receivedSwaps,
                                      List<com.example.skillswapproject.data.model.FirestoreSwap> sentSwaps) {
                lastReceivedSwaps = (receivedSwaps != null) ? receivedSwaps : new ArrayList<>();
                lastSentSwaps = (sentSwaps != null) ? sentSwaps : new ArrayList<>();
                int count = com.example.skillswapproject.util.SwapNotificationManager.calculateHomeNotificationCount(
                        getApplication(), currentUserId, lastReceivedSwaps, lastSentSwaps
                );
                pendingSwapsCountLiveData.postValue(count);

                // Populate active swapped partner IDs to filter from recommendations
                activeSwappedUserIds.clear();
                for (com.example.skillswapproject.data.model.FirestoreSwap s : lastReceivedSwaps) {
                    if ("ACCEPTED".equalsIgnoreCase(s.getStatus()) && s.getRequesterId() != null) {
                        activeSwappedUserIds.add(s.getRequesterId());
                    }
                }
                for (com.example.skillswapproject.data.model.FirestoreSwap s : lastSentSwaps) {
                    if ("ACCEPTED".equalsIgnoreCase(s.getStatus()) && s.getReceiverId() != null) {
                        activeSwappedUserIds.add(s.getReceiverId());
                    }
                }
                applyFilter();
            }

            @Override
            public void onError(Exception e) {
                // Ignore
            }
        });
    }

    public void refreshNotificationBadge() {
        String currentUserId = firestoreRepository.getCurrentUserId();
        if (currentUserId == null) return;
        if (pendingSwapsRegistration == null) {
            listenToPendingSwaps();
        } else {
            int count = com.example.skillswapproject.util.SwapNotificationManager.calculateHomeNotificationCount(
                    getApplication(), currentUserId, lastReceivedSwaps, lastSentSwaps
            );
            pendingSwapsCountLiveData.postValue(count);
        }
    }

    public void markNotificationsSeen() {
        String currentUserId = firestoreRepository.getCurrentUserId();
        if (currentUserId == null) return;
        com.example.skillswapproject.util.SwapNotificationManager.markNotificationsSeen(
                getApplication(), lastReceivedSwaps, lastSentSwaps
        );
        int count = com.example.skillswapproject.util.SwapNotificationManager.calculateHomeNotificationCount(
                getApplication(), currentUserId, lastReceivedSwaps, lastSentSwaps
        );
        pendingSwapsCountLiveData.postValue(count);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (authStateListener != null && firestoreRepository.getAuth() != null) {
            firestoreRepository.getAuth().removeAuthStateListener(authStateListener);
            authStateListener = null;
        }
        if (publicUsersRegistration != null) {
            publicUsersRegistration.remove();
            publicUsersRegistration = null;
        }
        if (pendingSwapsRegistration != null) {
            pendingSwapsRegistration.remove();
            pendingSwapsRegistration = null;
        }
    }
}
