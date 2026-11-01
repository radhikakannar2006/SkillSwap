package com.example.skillswapproject.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.skillswapproject.data.model.FirestoreMessage;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the 1:1 chat screen.
 * Listens in real-time to messages in Chats/{chatId}/Messages.
 * Sends new messages using a WriteBatch (message + chat metadata update).
 * No swap requirement — any two users can chat directly (WhatsApp-style).
 */
public class FirestoreChatDetailViewModel extends AndroidViewModel {

    private final FirestoreRepository firestoreRepository;

    private final MutableLiveData<List<FirestoreMessage>> messagesLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isSwapActiveLiveData = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isSendingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> messageSentSuccessLiveData = new MutableLiveData<>();

    private ListenerRegistration messagesRegistration;

    private String currentUserId;
    private String friendUserId;
    private String chatId;

    public FirestoreChatDetailViewModel(@NonNull Application application) {
        super(application);
        this.firestoreRepository = FirestoreRepository.getInstance();
    }

    public LiveData<List<FirestoreMessage>> getMessagesLiveData() {
        return messagesLiveData;
    }

    public LiveData<Boolean> getIsSwapActiveLiveData() {
        return isSwapActiveLiveData;
    }

    public LiveData<Boolean> getIsSendingLiveData() {
        return isSendingLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getMessageSentSuccessLiveData() {
        return messageSentSuccessLiveData;
    }

    /**
     * Sets up the real-time messages listener for the given chat.
     * chatId is always uid1_uid2 (sorted alphabetically) — guaranteed unique per pair.
     */
    public void initialize(String chatId, String currentUserId, String friendUserId) {
        this.currentUserId = currentUserId;
        this.friendUserId = friendUserId;

        // Always compute deterministic chatId — never empty
        if (chatId != null && !chatId.isEmpty()) {
            this.chatId = chatId;
        } else {
            this.chatId = FirestoreRepository.getChatId(currentUserId, friendUserId);
        }

        cleanupListeners();

        // Real-time listener for Chats/{chatId}/Messages ordered by timestamp ASC
        messagesRegistration = firestoreRepository.listenToMessages(this.chatId, new FirestoreRepository.OnMessagesLoadedListener() {
            @Override
            public void onMessagesLoaded(List<FirestoreMessage> messages) {
                messagesLiveData.postValue(messages != null ? messages : new ArrayList<>());
            }

            @Override
            public void onError(Exception e) {
                errorLiveData.postValue("Could not load messages: " + (e != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    /**
     * Sends a message. Writes to Chats/{chatId}/Messages and updates Chats/{chatId} metadata
     * using an atomic WriteBatch. Message is identified by sender's UID — no conflicts possible.
     */
    public void sendMessage(String messageText) {
        if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(getApplication())) {
            errorLiveData.setValue("Please check your internet connection.");
            return;
        }

        if (messageText == null || messageText.trim().isEmpty()) {
            errorLiveData.setValue("Message cannot be empty.");
            return;
        }
        if (currentUserId == null || friendUserId == null || chatId == null) {
            errorLiveData.setValue("Chat session is not ready. Please go back and try again.");
            return;
        }
        if (!chatId.contains(currentUserId)) {
            errorLiveData.setValue("You are not authorized to send messages in this chat.");
            return;
        }

        isSendingLiveData.setValue(true);
        FirestoreMessage message = new FirestoreMessage(currentUserId, friendUserId, messageText.trim());

        firestoreRepository.sendMessage(chatId, message)
                .addOnSuccessListener(aVoid -> {
                    isSendingLiveData.postValue(false);
                    messageSentSuccessLiveData.postValue(true);
                })
                .addOnFailureListener(e -> {
                    isSendingLiveData.postValue(false);
                    String err = (e != null && e.getMessage() != null && !e.getMessage().isEmpty())
                            ? e.getMessage()
                            : "Failed to send message. Please try again.";
                    errorLiveData.postValue(err);
                });
    }

    private void cleanupListeners() {
        if (messagesRegistration != null) {
            messagesRegistration.remove();
            messagesRegistration = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cleanupListeners();
    }
}
