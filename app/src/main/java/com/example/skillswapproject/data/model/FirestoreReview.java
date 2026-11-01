package com.example.skillswapproject.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

public class FirestoreReview implements Serializable {
    private String reviewId;
    private String targetUserId;
    private String reviewerId;
    private String reviewerName;
    private String reviewerPhotoUrl;
    private double rating;
    private String feedback;
    @ServerTimestamp
    private Date timestamp;

    public FirestoreReview() {
    }

    public FirestoreReview(String targetUserId, String reviewerId, String reviewerName,
                           String reviewerPhotoUrl, double rating, String feedback) {
        this.targetUserId = targetUserId;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName != null ? reviewerName : "User";
        this.reviewerPhotoUrl = reviewerPhotoUrl != null ? reviewerPhotoUrl : "";
        this.rating = rating;
        this.feedback = feedback != null ? feedback : "";
        this.timestamp = new Date();
    }

    public FirestoreReview(String targetUserId, String reviewerId, String reviewerName,
                           String reviewerPhotoUrl, float rating, String feedback) {
        this(targetUserId, reviewerId, reviewerName, reviewerPhotoUrl, (double) rating, feedback);
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName != null && !reviewerName.isEmpty() ? reviewerName : "Swap Partner";
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getReviewerPhotoUrl() {
        return reviewerPhotoUrl;
    }

    public void setReviewerPhotoUrl(String reviewerPhotoUrl) {
        this.reviewerPhotoUrl = reviewerPhotoUrl;
    }

    public double getRating() {
        return rating;
    }

    public float getRatingAsFloat() {
        return (float) rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getFeedback() {
        return feedback != null ? feedback : "";
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
