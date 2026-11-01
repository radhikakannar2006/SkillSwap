package com.example.skillswapproject.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

public class CoinTransaction implements Serializable {

    // Standard Transaction Types
    public static final String TYPE_WELCOME_BONUS = "WELCOME_BONUS";
    public static final String TYPE_TEACH_30_MIN = "TEACH_30_MIN";
    public static final String TYPE_TEACH_1_HOUR = "TEACH_1_HOUR";
    public static final String TYPE_SUCCESSFUL_SWAP = "SUCCESSFUL_SWAP";
    public static final String TYPE_FIVE_STAR_REVIEW = "FIVE_STAR_REVIEW";
    public static final String TYPE_LEARNING_SESSION_PAYMENT = "LEARNING_SESSION_PAYMENT";
    public static final String TYPE_TEACHING_SESSION_COMPLETED = "TEACHING_SESSION_COMPLETED";
    public static final String TYPE_DAILY_ACTIVE_REWARD = "DAILY_ACTIVE_REWARD";

    private String transactionId;
    private String userId;
    private int amount; // e.g. +5, +2, +1, -1, -2, -3
    private String type; // e.g. WELCOME_BONUS
    private String reason; // e.g. "Welcome Starter Bonus"
    private String relatedSwapId; // null or swapId
    private String sessionId; // null or sessionId
    private int balanceBefore;
    private int balanceAfter;

    @ServerTimestamp
    private Date timestamp;

    public CoinTransaction() {
    }

    public CoinTransaction(String transactionId, String userId, int amount, String type,
                           String reason, String relatedSwapId, int balanceBefore, int balanceAfter) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.reason = reason;
        this.relatedSwapId = relatedSwapId;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.timestamp = new Date();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReason() {
        return reason != null ? reason : "";
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRelatedSwapId() {
        return relatedSwapId;
    }

    public void setRelatedSwapId(String relatedSwapId) {
        this.relatedSwapId = relatedSwapId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(int balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(int balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isCredit() {
        return amount > 0;
    }
}
