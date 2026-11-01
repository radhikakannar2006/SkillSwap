package com.example.skillswapproject.data.model;

import java.io.Serializable;

public class ChatConversation implements Serializable {
    private User otherUser;
    private String lastMessage;
    private String lastMessageTime;
    private int unreadCount;

    public ChatConversation(User otherUser, String lastMessage, String lastMessageTime, int unreadCount) {
        this.otherUser = otherUser;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
    }

    public User getOtherUser() {
        return otherUser;
    }

    public void setOtherUser(User otherUser) {
        this.otherUser = otherUser;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(String lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
