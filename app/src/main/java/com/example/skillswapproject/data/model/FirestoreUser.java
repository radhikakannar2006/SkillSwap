package com.example.skillswapproject.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FirestoreUser implements Serializable {
    private String uid;
    private String name;
    private String email;
    private String location;
    private String profilePhotoUrl;
    private boolean isPublic;
    private List<String> offeredSkills;
    private List<String> wantedSkills;
    private String firstName;
    private String lastName;
    private String phone;
    private String availability;
    private String aboutMe;
    private int skillCoins;
    private int totalCoinsEarned;
    private boolean welcomeBonusClaimed;
    private String fcmToken;
    private int activeDaysCount;
    private String lastActiveRewardDate;

    public FirestoreUser() {
        this.offeredSkills = new ArrayList<>();
        this.wantedSkills = new ArrayList<>();
        this.isPublic = true;
        this.skillCoins = 5;
        this.totalCoinsEarned = 5;
        this.welcomeBonusClaimed = false;
        this.activeDaysCount = 0;
    }

    public FirestoreUser(String uid, String name, String email, String location,
                         String profilePhotoUrl, boolean isPublic,
                         List<String> offeredSkills, List<String> wantedSkills,
                         String availability) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.location = location;
        this.profilePhotoUrl = profilePhotoUrl;
        this.isPublic = isPublic;
        this.offeredSkills = offeredSkills != null ? offeredSkills : new ArrayList<>();
        this.wantedSkills = wantedSkills != null ? wantedSkills : new ArrayList<>();
        this.availability = availability;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    // Alias for compatibility
    @Exclude
    public String getId() {
        return uid;
    }

    @Exclude
    public void setId(String id) {
        this.uid = id;
    }

    public String getName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        if (firstName != null || lastName != null) {
            String f = firstName != null ? firstName.trim() : "";
            String l = lastName != null ? lastName.trim() : "";
            return (f + " " + l).trim();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    @PropertyName("isPublic")
    public boolean isPublic() {
        return isPublic;
    }

    @PropertyName("isPublic")
    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    @PropertyName("public")
    public boolean getPublic() {
        return isPublic;
    }

    @PropertyName("public")
    public void setPublicField(boolean aPublic) {
        isPublic = aPublic;
    }

    public List<String> getOfferedSkills() {
        return offeredSkills;
    }

    public void setOfferedSkills(List<String> offeredSkills) {
        this.offeredSkills = offeredSkills != null ? offeredSkills : new ArrayList<>();
    }

    public List<String> getWantedSkills() {
        return wantedSkills;
    }

    public void setWantedSkills(List<String> wantedSkills) {
        this.wantedSkills = wantedSkills != null ? wantedSkills : new ArrayList<>();
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getAboutMe() {
        return aboutMe != null ? aboutMe : "";
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    public int getSkillCoins() {
        return skillCoins;
    }

    public void setSkillCoins(int skillCoins) {
        this.skillCoins = skillCoins;
    }

    public int getTotalCoinsEarned() {
        return totalCoinsEarned;
    }

    public void setTotalCoinsEarned(int totalCoinsEarned) {
        this.totalCoinsEarned = totalCoinsEarned;
    }

    public boolean isWelcomeBonusClaimed() {
        return welcomeBonusClaimed;
    }

    public void setWelcomeBonusClaimed(boolean welcomeBonusClaimed) {
        this.welcomeBonusClaimed = welcomeBonusClaimed;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    private int compatibilityScore = 0;
    private String matchReason = "";

    @Exclude
    public int getCompatibilityScore() {
        return compatibilityScore;
    }

    @Exclude
    public void setCompatibilityScore(int compatibilityScore) {
        this.compatibilityScore = compatibilityScore;
    }

    @Exclude
    public String getMatchReason() {
        return matchReason != null ? matchReason : "";
    }

    @Exclude
    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public int getActiveDaysCount() {
        return activeDaysCount;
    }

    public void setActiveDaysCount(int activeDaysCount) {
        this.activeDaysCount = activeDaysCount;
    }

    public String getLastActiveRewardDate() {
        return lastActiveRewardDate;
    }

    public void setLastActiveRewardDate(String lastActiveRewardDate) {
        this.lastActiveRewardDate = lastActiveRewardDate;
    }
}
