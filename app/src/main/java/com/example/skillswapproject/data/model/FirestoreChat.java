package com.example.skillswapproject.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FirestoreChat implements Serializable {
    private String chatId;
    private List<String> participants;
    private String lastMessageText;
    @ServerTimestamp
    private Date lastMessageTimestamp;

    public FirestoreChat() {
        this.participants = new ArrayList<>();
    }

    public FirestoreChat(String chatId, List<String> participants, String lastMessageText, Date lastMessageTimestamp) {
        this.chatId = chatId;
        this.participants = participants != null ? participants : new ArrayList<>();
        this.lastMessageText = lastMessageText;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    public Date getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Date lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    private String lastSenderId;
    private String unreadBy;
    private boolean unread;

    // Alias for compatibility
    public Date getLastMessageTime() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTimestamp = lastMessageTime;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public String getUnreadBy() {
        return unreadBy;
    }

    public void setUnreadBy(String unreadBy) {
        this.unreadBy = unreadBy;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }
}
