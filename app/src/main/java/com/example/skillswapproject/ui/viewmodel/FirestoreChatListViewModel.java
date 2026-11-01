package com.example.skillswapproject.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.skillswapproject.data.model.ChatListItem;
import com.example.skillswapproject.data.model.FirestoreChat;
import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ViewModel for the Chat List screen.
 * Displays only people with whom the logged-in user has an actual chat conversation.
 */
public class FirestoreChatListViewModel extends AndroidViewModel {

    private static final String TAG = "ChatListViewModel";

    private final FirestoreRepository firestoreRepository;

    private final MutableLiveData<List<ChatListItem>> chatListLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private ListenerRegistration chatsRegistration;

    private String currentUserId;
    private final Map<String, FirestoreUser> userCache = new HashMap<>();
    private List<FirestoreChat> latestChats = new ArrayList<>();

    public FirestoreChatListViewModel(@NonNull Application application) {
        super(application);
        this.firestoreRepository = FirestoreRepository.getInstance();
    }

    public LiveData<List<ChatListItem>> getChatListLiveData() {
        return chatListLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void initialize(String userId) {
        if (userId == null || userId.isEmpty()) {
            errorLiveData.setValue("Not authenticated.");
            chatListLiveData.setValue(new ArrayList<>());
            return;
        }

        this.currentUserId = userId;
        cleanupListeners();
        isLoadingLiveData.setValue(true);

        // Listen to actual user chats
        chatsRegistration = firestoreRepository.listenToUserChats(currentUserId, new FirestoreRepository.OnChatsLoadedListener() {
            @Override
            public void onChatsLoaded(List<FirestoreChat> chats) {
                latestChats = (chats != null) ? chats : new ArrayList<>();
                refreshChatList();
            }

            @Override
            public void onError(Exception e) {
                isLoadingLiveData.postValue(false);
                errorLiveData.postValue("Could not load chats: " + (e != null ? e.getMessage() : "Unknown"));
            }
        });
    }

    public void refresh() {
        if (currentUserId != null) {
            refreshChatList();
        }
    }

    private void refreshChatList() {
        if (latestChats == null || latestChats.isEmpty()) {
            isLoadingLiveData.postValue(false);
            chatListLiveData.postValue(new ArrayList<>());
            return;
        }

        try {
            Set<String> partnerIds = new HashSet<>();
            for (FirestoreChat chat : latestChats) {
                if (chat != null && chat.getLastMessageText() != null && !chat.getLastMessageText().trim().isEmpty()) {
                    String partnerId = getOtherParticipant(chat);
                    if (partnerId != null && !partnerId.isEmpty()) {
                        partnerIds.add(partnerId);
                    }
                }
            }

            if (partnerIds.isEmpty()) {
                isLoadingLiveData.postValue(false);
                chatListLiveData.postValue(new ArrayList<>());
                return;
            }

            Set<String> uncachedIds = new HashSet<>();
            for (String partnerId : partnerIds) {
                if (!userCache.containsKey(partnerId)) {
                    uncachedIds.add(partnerId);
                }
            }

            if (uncachedIds.isEmpty()) {
                buildChatList();
                return;
            }

            List<Task<FirestoreUser>> tasks = new ArrayList<>();
            for (String uid : uncachedIds) {
                tasks.add(firestoreRepository.getUser(uid));
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
                try {
                    for (Task<FirestoreUser> task : tasks) {
                        if (task.isSuccessful()) {
                            FirestoreUser u = task.getResult();
                            if (u != null && u.getUid() != null) {
                                userCache.put(u.getUid(), u);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error resolving users", e);
                }
                buildChatList();
            });
        } catch (Exception e) {
            Log.e(TAG, "refreshChatList failed safely", e);
            buildChatList();
        }
    }

    private void buildChatList() {
        List<ChatListItem> items = new ArrayList<>();

        if (latestChats == null || latestChats.isEmpty()) {
            isLoadingLiveData.postValue(false);
            chatListLiveData.postValue(items);
            return;
        }

        try {
            Set<String> processedChatIds = new HashSet<>();

            for (FirestoreChat chat : latestChats) {
                if (chat == null) continue;

                // Display only people with whom an actual chat conversation exists
                String lastMsg = chat.getLastMessageText();
                if (lastMsg == null || lastMsg.trim().isEmpty()) {
                    continue;
                }

                String partnerId = getOtherParticipant(chat);
                if (partnerId == null || partnerId.isEmpty()) {
                    continue;
                }

                String chatId = chat.getChatId();
                if (chatId == null || chatId.isEmpty()) {
                    chatId = FirestoreRepository.getChatId(currentUserId, partnerId);
                }

                if (processedChatIds.contains(chatId)) {
                    continue;
                }
                processedChatIds.add(chatId);

                String friendName = "User";
                String profilePhoto = null;

                FirestoreUser fUser = userCache.get(partnerId);
                if (fUser != null) {
                    if (fUser.getName() != null && !fUser.getName().isEmpty()) {
                        friendName = fUser.getName();
                    }
                    profilePhoto = fUser.getProfilePhotoUrl();
                }

                Date lastTime = chat.getLastMessageTime() != null ? chat.getLastMessageTime() : new Date(0);
                String lastSenderId = chat.getLastSenderId();
                boolean isUnread = com.example.skillswapproject.util.ChatReadStatusManager.isChatUnread(
                        getApplication(), currentUserId, chat);

                items.add(new ChatListItem(
                        chatId, partnerId, friendName, profilePhoto, lastMsg, lastTime, true, isUnread, lastSenderId));
            }

            // Sort by most recent message first
            Collections.sort(items, (a, b) -> {
                if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                if (a.getLastMessageTime() == null) return 1;
                if (b.getLastMessageTime() == null) return -1;
                return b.getLastMessageTime().compareTo(a.getLastMessageTime());
            });
        } catch (Exception e) {
            Log.e(TAG, "buildChatList error", e);
        }

        isLoadingLiveData.postValue(false);
        chatListLiveData.postValue(items);
    }

    private String getOtherParticipant(FirestoreChat chat) {
        if (chat == null) return null;
        if (chat.getParticipants() != null) {
            for (String uid : chat.getParticipants()) {
                if (uid != null && !uid.equals(currentUserId)) return uid;
            }
        }
        if (chat.getChatId() != null && chat.getChatId().contains("_")) {
            String[] parts = chat.getChatId().split("_");
            if (parts.length == 2) {
                if (parts[0].equals(currentUserId)) return parts[1];
                if (parts[1].equals(currentUserId)) return parts[0];
            }
        }
        return null;
    }

    private void cleanupListeners() {
        if (chatsRegistration != null) {
            chatsRegistration.remove();
            chatsRegistration = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cleanupListeners();
    }
}
