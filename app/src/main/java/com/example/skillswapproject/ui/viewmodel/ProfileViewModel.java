package com.example.skillswapproject.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.model.Role;
import com.example.skillswapproject.data.model.Skill;
import com.example.skillswapproject.data.model.User;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.data.repository.SkillSwapRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends AndroidViewModel {

    private final FirestoreRepository firestoreRepository;
    private final SkillSwapRepository localRepository;

    private final MutableLiveData<FirestoreUser> firestoreUserLiveData = new MutableLiveData<>();
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccessLiveData = new MutableLiveData<>();

    private ListenerRegistration userRegistration;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.firestoreRepository = FirestoreRepository.getInstance();
        this.localRepository = SkillSwapRepository.getInstance();

        initializeRealtimeProfileListener();
    }

    public LiveData<FirestoreUser> getFirestoreUser() {
        return firestoreUserLiveData;
    }

    public LiveData<User> getCurrentUser() {
        return userLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccessLiveData;
    }

    public void initializeRealtimeProfileListener() {
        String uid = firestoreRepository.getCurrentUserId();
        if (uid == null || uid.isEmpty()) {
            firestoreUserLiveData.postValue(null);
            userLiveData.postValue(null);
            return;
        }

        userRegistration = firestoreRepository.listenToUserProfile(uid, new FirestoreRepository.OnUserProfileListener() {
            @Override
            public void onUserProfileLoaded(FirestoreUser fUser) {
                if (fUser != null) {
                    if (!fUser.isWelcomeBonusClaimed()) {
                        firestoreRepository.awardWelcomeBonusIfEligible(fUser.getUid());
                    }
                    firestoreUserLiveData.postValue(fUser);
                    User user = convertToUser(fUser);
                    userLiveData.postValue(user);
                    localRepository.setCurrentUser(user);
                } else {
                    firestoreUserLiveData.postValue(null);
                    userLiveData.postValue(null);
                }
            }

            @Override
            public void onError(Exception e) {
                errorLiveData.postValue("Profile load issue: " + (e != null ? e.getMessage() : ""));
                firestoreUserLiveData.postValue(null);
                userLiveData.postValue(null);
            }
        });
    }

    public void updateProfile(String name, String location, String availability,
                               String aboutMe, String profilePhotoUrl,
                               List<String> offeredSkillNames, List<String> wantedSkillNames,
                               boolean isPublic) {
        String uid = firestoreRepository.getCurrentUserId();
        if (uid == null || uid.isEmpty()) {
            errorLiveData.setValue("Not authenticated");
            return;
        }

        isLoadingLiveData.setValue(true);

        FirestoreUser updatedUser = new FirestoreUser();
        updatedUser.setUid(uid);
        updatedUser.setName(name);
        updatedUser.setLocation(location);
        updatedUser.setAvailability(availability);
        updatedUser.setAboutMe(aboutMe);
        updatedUser.setProfilePhotoUrl(profilePhotoUrl);
        updatedUser.setOfferedSkills(offeredSkillNames != null ? offeredSkillNames : new ArrayList<>());
        updatedUser.setWantedSkills(wantedSkillNames != null ? wantedSkillNames : new ArrayList<>());
        updatedUser.setPublic(isPublic);

        firestoreRepository.updateUserProfile(updatedUser)
                .addOnSuccessListener(aVoid -> {
                    isLoadingLiveData.postValue(false);
                    saveSuccessLiveData.postValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoadingLiveData.postValue(false);
                    errorLiveData.postValue("Failed to save changes to Firestore: " + (e != null ? e.getMessage() : ""));
                });
    }

    public void updateProfile(String name, String location, String availability,
                               List<String> offeredSkillNames, List<String> wantedSkillNames,
                               boolean isPublic) {
        String currentPhoto = "";
        String currentAbout = "";
        FirestoreUser current = firestoreUserLiveData.getValue();
        if (current != null) {
            currentPhoto = current.getProfilePhotoUrl();
            currentAbout = current.getAboutMe();
        }
        updateProfile(name, location, availability, currentAbout, currentPhoto, offeredSkillNames, wantedSkillNames, isPublic);
    }

    public void updateProfile(String name, String location, String availability,
                               String aboutMe, List<Skill> teachSkills,
                               List<Skill> learnSkills, boolean isPublic) {
        List<String> offered = new ArrayList<>();
        if (teachSkills != null) {
            for (Skill s : teachSkills) {
                offered.add(s.getName());
            }
        }
        List<String> wanted = new ArrayList<>();
        if (learnSkills != null) {
            for (Skill s : learnSkills) {
                wanted.add(s.getName());
            }
        }
        String currentPhoto = "";
        FirestoreUser current = firestoreUserLiveData.getValue();
        if (current != null) {
            currentPhoto = current.getProfilePhotoUrl();
        }
        updateProfile(name, location, availability, aboutMe, currentPhoto, offered, wanted, isPublic);
    }

    private User convertToUser(FirestoreUser fu) {
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
        String displayName = fu.getName();
        if (displayName == null || displayName.trim().isEmpty()) {
            String f = fu.getFirstName() != null ? fu.getFirstName().trim() : "";
            String l = fu.getLastName() != null ? fu.getLastName().trim() : "";
            displayName = (f + " " + l).trim();
        }
        if (displayName.isEmpty()) {
            displayName = "User";
        }

        return new User(
                fu.getUid(),
                displayName,
                fu.getEmail() != null ? fu.getEmail() : "",
                "",
                fu.getLocation() != null ? fu.getLocation() : "Worldwide",
                fu.getProfilePhotoUrl(),
                fu.isPublic(),
                0.0,
                0,
                0,
                0,
                Role.STANDARD,
                offered,
                wanted,
                fu.getAvailability() != null ? fu.getAvailability() : "Flexible",
                fu.getAboutMe() != null ? fu.getAboutMe() : ""
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (userRegistration != null) {
            userRegistration.remove();
            userRegistration = null;
        }
    }
}
