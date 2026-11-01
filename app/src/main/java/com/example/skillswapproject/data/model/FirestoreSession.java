package com.example.skillswapproject.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.Date;

public class FirestoreSession implements Serializable {

    public static final String STATUS_REQUESTED = "REQUESTED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_WAITING_CONFIRMATION = "WAITING_CONFIRMATION";
    public static final String STATUS_PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_NEEDS_REVIEW = "NEEDS_REVIEW";
    public static final String STATUS_DISPUTED = "DISPUTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final int TEACHING_SESSION_REWARD_COINS = 2;

    private String sessionId;
    private String swapId;
    private String learnerId;
    private String learnerName;
    private String teacherId;
    private String teacherName;
    private String skillName;
    private String skillLevel;
    private int coinCost;
    private int durationMinutes;
    private String scheduledDate;
    private String startTime;
    private String endTime;
    private long scheduledTimestamp;
    private String googleCalendarEventId;
    private String meetLink;
    private String meetingLink;
    private String meetingProvider = "Jitsi Meet";
    private Date meetingCreatedAt;
    private String status;
    private boolean learnerConfirmed;
    private boolean teacherConfirmed;
    private Date learnerConfirmedAt;
    private Date teacherConfirmedAt;
    private boolean coinsReleased;
    private boolean rewardClaimed;
    private int rewardCoins = TEACHING_SESSION_REWARD_COINS;

    private boolean disputed;
    private String disputedBy;
    private String disputeReason;
    private Date disputedAt;

    @ServerTimestamp
    private Date createdAt;
    private Date completedAt;
    private Date cancelledAt;
    private String cancelledBy;
    private String cancellationReason;

    public FirestoreSession() {
        this.status = STATUS_SCHEDULED;
        this.coinCost = 1;
        this.durationMinutes = 60;
        this.learnerConfirmed = false;
        this.teacherConfirmed = false;
        this.coinsReleased = false;
        this.rewardClaimed = false;
        this.disputed = false;
        this.meetingProvider = "Jitsi Meet";
        this.rewardCoins = TEACHING_SESSION_REWARD_COINS;
    }

    public FirestoreSession(String sessionId, String swapId, String learnerId, String learnerName,
                            String teacherId, String teacherName, String skillName, String skillLevel,
                            int coinCost, int durationMinutes, String scheduledDate, String startTime,
                            String endTime, long scheduledTimestamp, String googleCalendarEventId,
                            String meetLink) {
        this.sessionId = sessionId;
        this.swapId = swapId;
        this.learnerId = learnerId;
        this.learnerName = learnerName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.skillName = skillName;
        this.skillLevel = skillLevel;
        this.coinCost = coinCost;
        this.durationMinutes = durationMinutes;
        this.scheduledDate = scheduledDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduledTimestamp = scheduledTimestamp;
        this.googleCalendarEventId = googleCalendarEventId;
        this.meetLink = meetLink;
        this.status = STATUS_SCHEDULED;
        this.learnerConfirmed = false;
        this.teacherConfirmed = false;
        this.coinsReleased = false;
        this.createdAt = new Date();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSwapId() {
        return swapId;
    }

    public void setSwapId(String swapId) {
        this.swapId = swapId;
    }

    public String getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(String learnerId) {
        this.learnerId = learnerId;
    }

    public String getLearnerName() {
        return learnerName != null ? learnerName : "Learner";
    }

    public void setLearnerName(String learnerName) {
        this.learnerName = learnerName;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName != null ? teacherName : "Teacher";
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getSkillName() {
        return skillName != null ? skillName : "Skill Swap";
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
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

    public int getDurationMinutes() {
        return durationMinutes > 0 ? durationMinutes : 60;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(String scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getScheduledTimestamp() {
        return scheduledTimestamp;
    }

    public void setScheduledTimestamp(long scheduledTimestamp) {
        this.scheduledTimestamp = scheduledTimestamp;
    }

    public String getGoogleCalendarEventId() {
        return googleCalendarEventId;
    }

    public void setGoogleCalendarEventId(String googleCalendarEventId) {
        this.googleCalendarEventId = googleCalendarEventId;
    }

    public String getMeetLink() {
        return meetLink;
    }

    public void setMeetLink(String meetLink) {
        this.meetLink = meetLink;
    }

    public String getStatus() {
        return status != null ? status : STATUS_SCHEDULED;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isLearnerConfirmed() {
        return learnerConfirmed;
    }

    public void setLearnerConfirmed(boolean learnerConfirmed) {
        this.learnerConfirmed = learnerConfirmed;
    }

    public boolean isTeacherConfirmed() {
        return teacherConfirmed;
    }

    public void setTeacherConfirmed(boolean teacherConfirmed) {
        this.teacherConfirmed = teacherConfirmed;
    }

    public Date getLearnerConfirmedAt() {
        return learnerConfirmedAt;
    }

    public void setLearnerConfirmedAt(Date learnerConfirmedAt) {
        this.learnerConfirmedAt = learnerConfirmedAt;
    }

    public Date getTeacherConfirmedAt() {
        return teacherConfirmedAt;
    }

    public void setTeacherConfirmedAt(Date teacherConfirmedAt) {
        this.teacherConfirmedAt = teacherConfirmedAt;
    }

    public boolean isCoinsReleased() {
        return coinsReleased;
    }

    public void setCoinsReleased(boolean coinsReleased) {
        this.coinsReleased = coinsReleased;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getMeetingLink() {
        return meetingLink != null && !meetingLink.isEmpty() ? meetingLink : meetLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
        if (this.meetLink == null || this.meetLink.isEmpty()) {
            this.meetLink = meetingLink;
        }
    }

    public String getMeetingProvider() {
        return meetingProvider != null ? meetingProvider : "Jitsi Meet";
    }

    public void setMeetingProvider(String meetingProvider) {
        this.meetingProvider = meetingProvider;
    }

    public Date getMeetingCreatedAt() {
        return meetingCreatedAt;
    }

    public void setMeetingCreatedAt(Date meetingCreatedAt) {
        this.meetingCreatedAt = meetingCreatedAt;
    }

    public boolean isDisputed() {
        return disputed;
    }

    public void setDisputed(boolean disputed) {
        this.disputed = disputed;
    }

    public String getDisputedBy() {
        return disputedBy;
    }

    public void setDisputedBy(String disputedBy) {
        this.disputedBy = disputedBy;
    }

    public String getDisputeReason() {
        return disputeReason;
    }

    public void setDisputeReason(String disputeReason) {
        this.disputeReason = disputeReason;
    }

    public Date getDisputedAt() {
        return disputedAt;
    }

    public void setDisputedAt(Date disputedAt) {
        this.disputedAt = disputedAt;
    }

    public int getRewardCoins() {
        return rewardCoins > 0 ? rewardCoins : TEACHING_SESSION_REWARD_COINS;
    }

    public void setRewardCoins(int rewardCoins) {
        this.rewardCoins = rewardCoins;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }
}
