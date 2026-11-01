package com.example.skillswapproject.data.model;

import java.io.Serializable;

public class SwapRequest implements Serializable {
    private String id;
    private String requesterId;
    private String receiverId;
    private String offeredSkillId;
    private String requestedSkillId;
    private SwapStatus status;
    private String message;

    public SwapRequest() {
        this.status = SwapStatus.PENDING;
    }

    public SwapRequest(String id, String requesterId, String receiverId,
                       String offeredSkillId, String requestedSkillId,
                       SwapStatus status, String message) {
        this.id = id;
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.offeredSkillId = offeredSkillId;
        this.requestedSkillId = requestedSkillId;
        this.status = status != null ? status : SwapStatus.PENDING;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public SwapStatus getStatus() {
        return status;
    }

    public void setStatus(SwapStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
