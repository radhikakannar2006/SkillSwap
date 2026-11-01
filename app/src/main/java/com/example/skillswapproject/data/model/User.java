package com.example.skillswapproject.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private String id;
    private String name;
    private String email;
    private String password;
    private String location;
    private String profilePhotoUrl;
    private boolean isPublic;
    private double rating;
    private int reviewsCount;
    private int swapsCount;
    private int followingCount;
    private Role role;
    private List<Skill> offeredSkills;
    private List<Skill> wantedSkills;
    private String firstName;
    private String lastName;
    private String phone;
    private String availability;
    private String aboutMe;

    public User() {
        this.offeredSkills = new ArrayList<>();
        this.wantedSkills = new ArrayList<>();
        this.isPublic = true;
        this.role = Role.STANDARD;
        this.rating = 5.0;
        this.swapsCount = 0;
        this.reviewsCount = 0;
        this.followingCount = 0;
    }

    public User(String id, String name, String location, String profilePhotoUrl,
                boolean isPublic, double rating, Role role,
                List<Skill> offeredSkills, List<Skill> wantedSkills, String availability) {
        this(id, name, "", "", location, profilePhotoUrl, isPublic, rating, 0, 0, 0,
             role, offeredSkills, wantedSkills, availability, "");
    }

    public User(String id, String name, String email, String password, String location,
                String profilePhotoUrl, boolean isPublic, double rating, int reviewsCount,
                int swapsCount, int followingCount, Role role, List<Skill> offeredSkills,
                List<Skill> wantedSkills, String availability, String aboutMe) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.location = location;
        this.profilePhotoUrl = profilePhotoUrl;
        this.isPublic = isPublic;
        this.rating = rating;
        this.reviewsCount = reviewsCount;
        this.swapsCount = swapsCount;
        this.followingCount = followingCount;
        this.role = role != null ? role : Role.STANDARD;
        this.offeredSkills = offeredSkills != null ? new ArrayList<>(offeredSkills) : new ArrayList<>();
        this.wantedSkills = wantedSkills != null ? new ArrayList<>(wantedSkills) : new ArrayList<>();
        this.availability = availability;
        this.aboutMe = aboutMe;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

    public int getSwapsCount() {
        return swapsCount;
    }

    public void setSwapsCount(int swapsCount) {
        this.swapsCount = swapsCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Skill> getOfferedSkills() {
        return offeredSkills;
    }

    public void setOfferedSkills(List<Skill> offeredSkills) {
        this.offeredSkills = offeredSkills != null ? new ArrayList<>(offeredSkills) : new ArrayList<>();
    }

    public List<Skill> getWantedSkills() {
        return wantedSkills;
    }

    public void setWantedSkills(List<Skill> wantedSkills) {
        this.wantedSkills = wantedSkills != null ? new ArrayList<>(wantedSkills) : new ArrayList<>();
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getAboutMe() {
        return aboutMe;
    }

    public void setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
    }

    private int compatibilityScore = 0;

    public int getCompatibilityScore() {
        return compatibilityScore;
    }

    public void setCompatibilityScore(int compatibilityScore) {
        this.compatibilityScore = compatibilityScore;
    }
}
