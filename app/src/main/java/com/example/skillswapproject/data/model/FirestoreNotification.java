package com.example.skillswapproject.data.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

public class FirestoreNotification implements Serializable {

    // Notification Type Constants
    public static final String TYPE_SWAP_REQUEST = "SWAP_REQUEST";
    public static final String TYPE_SWAP_ACCEPTED = "SWAP_ACCEPTED";
    public static final String TYPE_NEW_MESSAGE = "NEW_MESSAGE";
    public static final String TYPE_SESSION_SCHEDULED = "SESSION_SCHEDULED";
    public static final String TYPE_SESSION_REMINDER = "SESSION_REMINDER";
    public static final String TYPE_NEW_FEEDBACK = "NEW_FEEDBACK";

    private String notificationId;
    private String recipientUserId;
    private String senderUserId;
    private String senderName;
    private String senderAvatarUrl;
    private String type;
    private String title;
    private String message;
    private @ServerTimestamp Date timestamp;
    private boolean read;
    private String relatedSwapId;
    private String relatedSessionId;
    private String relatedChatId;
    private String relatedReviewId;

    public FirestoreNotification() {
        this.read = false;
    }

    public FirestoreNotification(String notificationId, String recipientUserId, String senderUserId,
                                 String senderName, String senderAvatarUrl, String type,
                                 String title, String message) {
        this.notificationId = notificationId;
        this.recipientUserId = recipientUserId;
        this.senderUserId = senderUserId;
        this.senderName = senderName;
        this.senderAvatarUrl = senderAvatarUrl;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = false;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatarUrl() {
        return senderAvatarUrl;
    }

    public void setSenderAvatarUrl(String senderAvatarUrl) {
        this.senderAvatarUrl = senderAvatarUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getRelatedSwapId() {
        return relatedSwapId;
    }

    public void setRelatedSwapId(String relatedSwapId) {
        this.relatedSwapId = relatedSwapId;
    }

    public String getRelatedSessionId() {
        return relatedSessionId;
    }

    public void setRelatedSessionId(String relatedSessionId) {
        this.relatedSessionId = relatedSessionId;
    }

    public String getRelatedChatId() {
        return relatedChatId;
    }

    public void setRelatedChatId(String relatedChatId) {
        this.relatedChatId = relatedChatId;
    }

    public String getRelatedReviewId() {
        return relatedReviewId;
    }

    public void setRelatedReviewId(String relatedReviewId) {
        this.relatedReviewId = relatedReviewId;
    }
}
