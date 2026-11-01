package com.example.skillswapproject.data.model;

import java.io.Serializable;
import java.util.Date;

public class ChatListItem implements Serializable {
    private String chatId;
    private String friendUserId;
    private String friendName;
    private String friendProfilePhotoUrl;
    private String lastMessageText;
    private Date lastMessageTime;
    private boolean isSwapActive;
    private boolean isUnread;
    private String lastSenderId;

    public ChatListItem() {}

    public ChatListItem(String chatId, String friendUserId, String friendName, String friendProfilePhotoUrl,
                        String lastMessageText, Date lastMessageTime, boolean isSwapActive) {
        this(chatId, friendUserId, friendName, friendProfilePhotoUrl, lastMessageText, lastMessageTime, isSwapActive, false, null);
    }

    public ChatListItem(String chatId, String friendUserId, String friendName, String friendProfilePhotoUrl,
                        String lastMessageText, Date lastMessageTime, boolean isSwapActive, boolean isUnread, String lastSenderId) {
        this.chatId = chatId;
        this.friendUserId = friendUserId;
        this.friendName = friendName;
        this.friendProfilePhotoUrl = friendProfilePhotoUrl;
        this.lastMessageText = lastMessageText;
        this.lastMessageTime = lastMessageTime;
        this.isSwapActive = isSwapActive;
        this.isUnread = isUnread;
        this.lastSenderId = lastSenderId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getFriendUserId() {
        return friendUserId;
    }

    public void setFriendUserId(String friendUserId) {
        this.friendUserId = friendUserId;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public String getFriendProfilePhotoUrl() {
        return friendProfilePhotoUrl;
    }

    public String getFriendProfilePhoto() {
        return friendProfilePhotoUrl;
    }

    public void setFriendProfilePhotoUrl(String friendProfilePhotoUrl) {
        this.friendProfilePhotoUrl = friendProfilePhotoUrl;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isSwapActive() {
        return isSwapActive;
    }

    public void setSwapActive(boolean swapActive) {
        isSwapActive = swapActive;
    }

    public boolean isUnread() {
        return isUnread;
    }

    public void setUnread(boolean unread) {
        isUnread = unread;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }
}
