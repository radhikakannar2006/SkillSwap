package com.example.skillswapproject.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.skillswapproject.data.model.User;
import com.example.skillswapproject.data.repository.SkillSwapRepository;
import com.example.skillswapproject.ui.base.BaseViewModel;

public class SwapViewModel extends BaseViewModel {

    private final SkillSwapRepository repository = SkillSwapRepository.getInstance();
    private final MutableLiveData<Boolean> requestSentSuccess = new MutableLiveData<>(false);

    public LiveData<Boolean> getRequestSentSuccess() {
        return requestSentSuccess;
    }

    public LiveData<User> getCurrentUser() {
        return repository.getCurrentUser();
    }

    public User getTargetUser(String userId) {
        return repository.getUserById(userId);
    }

    public void submitSwapRequest(String targetUserId, String offeredSkill, String requestedSkill, String message) {
        isLoading.setValue(true);
        repository.sendSwapRequest(targetUserId, offeredSkill, requestedSkill, message);
        isLoading.setValue(false);
        requestSentSuccess.setValue(true);
    }
}
