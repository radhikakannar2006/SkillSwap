package com.example.skillswapproject.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

public class FirestoreSwap implements Serializable {
    private String id;
    private String requesterId;
    private String receiverId;
    private String requesterName;
    private String receiverName;
    private String requesterPhoto;
    private String receiverPhoto;
    private String offeredSkillId;
    private String requestedSkillId;
    private String offeredSkillName;
    private String requestedSkillName;
    private String status; // "PENDING", "ACCEPTED", "REJECTED", "ENDED"
    private String endedBy;
    private String skillLevel; // "Beginner", "Intermediate", "Advanced"
    private int coinCost; // 1, 2, or 3
    private boolean completedRewardClaimed;

    private Object timestamp;

    public FirestoreSwap() {
        this.status = "PENDING";
        this.skillLevel = "Beginner";
        this.coinCost = 1;
        this.completedRewardClaimed = false;
    }

    public FirestoreSwap(String requesterId, String receiverId, String offeredSkillId, String requestedSkillId, String status) {
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.offeredSkillId = offeredSkillId;
        this.requestedSkillId = requestedSkillId;
        this.offeredSkillName = offeredSkillId;
        this.requestedSkillName = requestedSkillId;
        this.status = status != null ? status : "PENDING";
        this.timestamp = com.google.firebase.firestore.FieldValue.serverTimestamp();
    }

    public FirestoreSwap(String id, String requesterId, String receiverId, String offeredSkillId, String requestedSkillId, String status) {
        this.id = id;
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.offeredSkillId = offeredSkillId;
        this.requestedSkillId = requestedSkillId;
        this.offeredSkillName = offeredSkillId;
        this.requestedSkillName = requestedSkillId;
        this.status = status != null ? status : "PENDING";
        this.timestamp = com.google.firebase.firestore.FieldValue.serverTimestamp();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSwapId() {
        return id;
    }

    public void setSwapId(String swapId) {
        this.id = swapId;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getRequesterName() {
        return requesterName != null ? requesterName : "User";
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getReceiverName() {
        return receiverName != null ? receiverName : "User";
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getRequesterPhoto() {
        return requesterPhoto;
    }

    public void setRequesterPhoto(String requesterPhoto) {
        this.requesterPhoto = requesterPhoto;
    }

    public String getReceiverPhoto() {
        return receiverPhoto;
    }

    public void setReceiverPhoto(String receiverPhoto) {
        this.receiverPhoto = receiverPhoto;
    }

    public String getOfferedSkillId() {
        return offeredSkillId;
    }

    public void setOfferedSkillId(String offeredSkillId) {
        this.offeredSkillId = offeredSkillId;
    }

    public String getRequestedSkillId() {
        return requestedSkillId;
    }

    public void setRequestedSkillId(String requestedSkillId) {
        this.requestedSkillId = requestedSkillId;
    }

    public String getOfferedSkillName() {
        return offeredSkillName != null ? offeredSkillName : offeredSkillId;
    }

    public void setOfferedSkillName(String offeredSkillName) {
        this.offeredSkillName = offeredSkillName;
    }

    public String getRequestedSkillName() {
        return requestedSkillName != null ? requestedSkillName : requestedSkillId;
    }

    public void setRequestedSkillName(String requestedSkillName) {
        this.requestedSkillName = requestedSkillName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEndedBy() {
        return endedBy;
    }

    public void setEndedBy(String endedBy) {
        this.endedBy = endedBy;
    }

    public Date getTimestamp() {
        if (timestamp instanceof Date) {
            return (Date) timestamp;
        } else if (timestamp instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) timestamp).toDate();
        } else if (timestamp instanceof Long) {
            return new Date((Long) timestamp);
        }
        return new Date();
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    public String getSkillLevel() {
        return skillLevel != null ? skillLevel : "Beginner";
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }

    public int getCoinCost() {
        return coinCost > 0 ? coinCost : 1;
    }

    public void setCoinCost(int coinCost) {
        this.coinCost = coinCost;
    }

    public boolean isCompletedRewardClaimed() {
        return completedRewardClaimed;
    }

    public void setCompletedRewardClaimed(boolean completedRewardClaimed) {
        this.completedRewardClaimed = completedRewardClaimed;
    }
}
