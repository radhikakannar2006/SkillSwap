package com.example.skillswapproject.ui.activity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import com.example.skillswapproject.R;
import com.example.skillswapproject.data.model.FirestoreSession;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.ActivityChatDetailBinding;
import com.example.skillswapproject.databinding.DialogCreateMeetingSessionBinding;
import com.example.skillswapproject.ui.adapter.MessageAdapter;
import com.example.skillswapproject.ui.viewmodel.FirestoreChatDetailViewModel;
import com.example.skillswapproject.util.GoogleCalendarMeetHelper;
import com.example.skillswapproject.util.ImageLoaderHelper;

import android.widget.ArrayAdapter;
import com.example.skillswapproject.util.NotificationBannerHelper;
import com.example.skillswapproject.util.SkillConstants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ChatDetailActivity extends AppCompatActivity {

    public static volatile String sActiveChatId = null;

    private ActivityChatDetailBinding binding;
    private FirestoreChatDetailViewModel viewModel;
    private MessageAdapter adapter;

    private String currentUserId;
    private String friendUserId;
    private String friendName;
    private String chatId;
    private boolean isSwapAccepted = false;
    private String activeMeetUrl = null;
    private String activeSessionId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Always get current user from Firebase Auth
        currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        extractIntentData();
        setupToolbar();
        setupToolbarVideoCall();
        setupRecyclerView();
        setupViewModel();
        setupInputListeners();
        setupStarterListeners();
        checkSwapStatus();
        loadFriendPhoto();
        setupNotificationBanner();
    }

    private void setupNotificationBanner() {
        if (binding.bannerNotificationPermission != null) {
            NotificationBannerHelper.setupBanner(
                    this,
                    binding.bannerNotificationPermission.getRoot(),
                    "Get message notifications",
                    "Make sure you know when you have new messages."
            );
        }
    }

    private void extractIntentData() {
        friendUserId = getIntent().getStringExtra("EXTRA_FRIEND_USER_ID");
        friendName = getIntent().getStringExtra("EXTRA_FRIEND_NAME");
        if (friendName == null || friendName.isEmpty()) friendName = "User";
        chatId = getIntent().getStringExtra("EXTRA_CHAT_ID");

        // If chatId not provided, derive it deterministically — same result always
        if (chatId == null || chatId.isEmpty()) {
            chatId = FirestoreRepository.getChatId(currentUserId, friendUserId);
        }
    }

    private void setupToolbar() {
        binding.tvToolbarName.setText(friendName);
        binding.tvToolbarAvatarInitial.setText(
                friendName.isEmpty() ? "U" : String.valueOf(friendName.charAt(0)).toUpperCase());
        binding.tvToolbarStatus.setText("Skill Swap Member");
        binding.btnBack.setOnClickListener(v -> finish());

        View.OnClickListener profileClick = v -> openFriendProfile();
        binding.toolbarAvatarCard.setOnClickListener(profileClick);
        binding.tvToolbarName.setOnClickListener(profileClick);
    }

    private void setupToolbarVideoCall() {
        if (binding.btnToolbarVideoCall == null) return;

        // Query active meeting link and session ID if exists
        FirestoreRepository.getInstance().findActiveSession(currentUserId, friendUserId, (sessionId, meetLink) -> {
            if (isFinishing() || isDestroyed()) return;
            if (meetLink != null && !meetLink.trim().isEmpty()) {
                activeMeetUrl = meetLink.trim();
                activeSessionId = sessionId;
            }
        });

        binding.btnToolbarVideoCall.setOnClickListener(v -> {
            if (activeMeetUrl != null && !activeMeetUrl.isEmpty()) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Video Call & Session 🎥")
                        .setMessage("A session is currently active with " + friendName + ".")
                        .setPositiveButton("🎥 Join Call", (dialog, which) -> {
                            GoogleCalendarMeetHelper.launchGoogleMeet(this, activeMeetUrl);
                        })
                        .setNeutralButton("📋 View Details", (dialog, which) -> {
                            if (activeSessionId != null && !activeSessionId.isEmpty()) {
                                android.content.Intent intent = new android.content.Intent(this, SessionDetailActivity.class);
                                intent.putExtra(SessionDetailActivity.EXTRA_SESSION_ID, activeSessionId);
                                intent.putExtra(SessionDetailActivity.EXTRA_PARTNER_USER_ID, friendUserId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Session details loading...", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Schedule New", (dialog, which) -> {
                            showCreateMeetingDialog();
                        })
                        .show();
            } else {
                showCreateMeetingDialog();
            }
        });
    }

    private void showCreateMeetingDialog() {
        FirestoreRepository.getInstance().getActiveSwap(currentUserId, friendUserId, swap -> {
            if (isFinishing() || isDestroyed()) return;
            if (swap == null) {
                Toast.makeText(this, "Active swap not found for scheduling.", Toast.LENGTH_SHORT).show();
                return;
            }
            showCreateMeetingDialogWithSwap(swap);
        });
    }

    private void showCreateMeetingDialogWithSwap(com.example.skillswapproject.data.model.FirestoreSwap swap) {
        DialogCreateMeetingSessionBinding dialogBinding =
                DialogCreateMeetingSessionBinding.inflate(getLayoutInflater());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 30);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        final String[] selectedDate = {""};
        final String[] selectedTime = {""};

        dialogBinding.tvSelectedMeetDate.setText("Select Date");
        dialogBinding.tvSelectedMeetTime.setText("Select Time");

        // Populate "Skill to Teach" dropdown with only the skills in the swap
        List<String> swapSkills = new ArrayList<>();
        if (swap.getOfferedSkillName() != null && !swap.getOfferedSkillName().isEmpty()) {
            swapSkills.add(swap.getOfferedSkillName());
        }
        if (swap.getRequestedSkillName() != null && !swap.getRequestedSkillName().isEmpty()) {
            if (!swapSkills.contains(swap.getRequestedSkillName())) {
                swapSkills.add(swap.getRequestedSkillName());
            }
        }
        
        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item_dark, swapSkills);
        dialogBinding.etMeetSkill.setAdapter(skillAdapter);
        dialogBinding.etMeetSkill.setOnClickListener(v -> dialogBinding.etMeetSkill.showDropDown());
        dialogBinding.etMeetSkill.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) dialogBinding.etMeetSkill.showDropDown();
        });
        
        // Auto-select first skill if available
        if (!swapSkills.isEmpty()) {
            dialogBinding.etMeetSkill.setText(swapSkills.get(0), false);
        }

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.getRoot())
                .setCancelable(true)
                .create();

        dialogBinding.btnSelectMeetDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        selectedDate[0] = dateFormat.format(calendar.getTime());
                        dialogBinding.tvSelectedMeetDate.setText(selectedDate[0]);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePicker.show();
        });

        dialogBinding.btnSelectMeetTime.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        selectedTime[0] = timeFormat.format(calendar.getTime());
                        dialogBinding.tvSelectedMeetTime.setText(selectedTime[0]);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            );
            timePicker.show();
        });

        String myName = swap.getRequesterId().equals(currentUserId) ? swap.getRequesterName() : swap.getReceiverName();
        String otherName = swap.getRequesterId().equals(currentUserId) ? swap.getReceiverName() : swap.getRequesterName();
        if (myName == null || myName.isEmpty()) myName = "Current User";
        if (otherName == null || otherName.isEmpty()) otherName = friendName;

        List<String> roleOptions = new ArrayList<>();
        roleOptions.add(myName);
        roleOptions.add(otherName);
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item_dark, roleOptions);
        dialogBinding.etMeetTeacher.setAdapter(roleAdapter);
        dialogBinding.etMeetLearner.setAdapter(roleAdapter);
        
        dialogBinding.etMeetTeacher.setOnClickListener(v -> dialogBinding.etMeetTeacher.showDropDown());
        dialogBinding.etMeetLearner.setOnClickListener(v -> dialogBinding.etMeetLearner.showDropDown());

        // Update Teacher/Learner automatically when skill changes
        dialogBinding.etMeetSkill.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSkill = (String) parent.getItemAtPosition(position);
            if (selectedSkill.equals(swap.getOfferedSkillName())) {
                dialogBinding.etMeetTeacher.setText(swap.getRequesterName(), false);
                dialogBinding.etMeetLearner.setText(swap.getReceiverName(), false);
            } else if (selectedSkill.equals(swap.getRequestedSkillName())) {
                dialogBinding.etMeetTeacher.setText(swap.getReceiverName(), false);
                dialogBinding.etMeetLearner.setText(swap.getRequesterName(), false);
            }
        });
        
        // Initial setup for Teacher/Learner if skill is auto-selected
        if (!swapSkills.isEmpty()) {
            String initialSkill = swapSkills.get(0);
            if (initialSkill.equals(swap.getOfferedSkillName())) {
                dialogBinding.etMeetTeacher.setText(swap.getRequesterName(), false);
                dialogBinding.etMeetLearner.setText(swap.getReceiverName(), false);
            } else if (initialSkill.equals(swap.getRequestedSkillName())) {
                dialogBinding.etMeetTeacher.setText(swap.getReceiverName(), false);
                dialogBinding.etMeetLearner.setText(swap.getRequesterName(), false);
            }
        }

        dialogBinding.btnCancelMeetDialog.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnGenerateMeetLink.setOnClickListener(v -> {
            String skill = dialogBinding.etMeetSkill.getText() != null
                    ? dialogBinding.etMeetSkill.getText().toString().trim()
                    : "";

            if (skill.isEmpty()) {
                dialogBinding.tilMeetSkill.setError("Please select a skill to teach");
                return;
            } else {
                dialogBinding.tilMeetSkill.setError(null);
            }

            String teacherSelection = dialogBinding.etMeetTeacher.getText() != null ? dialogBinding.etMeetTeacher.getText().toString().trim() : "";
            String learnerSelection = dialogBinding.etMeetLearner.getText() != null ? dialogBinding.etMeetLearner.getText().toString().trim() : "";

            if (teacherSelection.isEmpty()) {
                dialogBinding.tilMeetTeacher.setError("Please select a teacher");
                return;
            } else { dialogBinding.tilMeetTeacher.setError(null); }
            
            if (learnerSelection.isEmpty()) {
                dialogBinding.tilMeetLearner.setError("Please select a learner");
                return;
            } else { dialogBinding.tilMeetLearner.setError(null); }

            if (teacherSelection.equals(learnerSelection)) {
                dialogBinding.tilMeetLearner.setError("Teacher and Learner cannot be the same person");
                return;
            } else { dialogBinding.tilMeetLearner.setError(null); }

            if (selectedDate[0] == null || selectedDate[0].isEmpty()) {
                Toast.makeText(this, "Please select a date for the session.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedTime[0] == null || selectedTime[0].isEmpty()) {
                Toast.makeText(this, "Please select a time for the session.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (calendar.getTimeInMillis() < (System.currentTimeMillis() - 60000)) {
                Toast.makeText(this, "Selected session date and time cannot be in the past. Please choose a future time.", Toast.LENGTH_LONG).show();
                return;
            }

            int checkedDurationId = dialogBinding.rgMeetDuration.getCheckedRadioButtonId();
            if (checkedDurationId != R.id.rbDuration30 && checkedDurationId != R.id.rbDuration60) {
                Toast.makeText(this, "Please select a duration", Toast.LENGTH_SHORT).show();
                return;
            }

            final int duration = dialogBinding.rbDuration30.isChecked() ? 30 : 60;
            int coinCost = duration == 60 ? 2 : 1;
            final String finalDate = selectedDate[0];
            final String finalTime = selectedTime[0];
            
            // Auto-generate Jitsi Meet URL
            String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
            final String finalMeetUrl = "https://meet.jit.si/SkillSwap-Session-" + uniqueId;

            dialog.dismiss();

            String newSessionId = "sess_" + System.currentTimeMillis();
            activeSessionId = newSessionId;
            activeMeetUrl = finalMeetUrl;
            
            String selectedTeacherId = teacherSelection.equals(swap.getRequesterName()) ? swap.getRequesterId() : swap.getReceiverId();
            String selectedLearnerId = learnerSelection.equals(swap.getRequesterName()) ? swap.getRequesterId() : swap.getReceiverId();

            // Format message with session details and [SessionID:...]
            String msgText = "📅 Session Scheduled\n" +
                    "Skill: " + skill + "\n" +
                    "Teacher: " + teacherSelection + "\n" +
                    "Learner: " + learnerSelection + "\n" +
                    "📆 Date: " + finalDate + "\n" +
                    "🕕 Time: " + finalTime + "\n" +
                    "⏳ Duration: " + (duration == 60 ? "1 hr" : "30 mins") + "\n" +
                    "Status: Scheduled\n" +
                    "🔗 Jitsi Meet: " + finalMeetUrl + "\n" +
                    "[SessionID:" + newSessionId + "]";

            viewModel.sendMessage(msgText);
            Toast.makeText(this, "Session created & link sent to chat!", Toast.LENGTH_SHORT).show();

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Session Ready 🎥")
                    .setMessage("Session scheduled and link sent to " + friendName + ".\n\n" +
                            "Date: " + finalDate + " at " + finalTime + "\n" +
                            "Duration: " + (duration == 60 ? "1 hr" : "30 mins") + "\n\n" +
                            "What would you like to do?")
                    .setPositiveButton("Join Call", (d, w) -> {
                        // Open Jitsi Meet link in browser or app
                        android.content.Intent browserIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(finalMeetUrl));
                        startActivity(browserIntent);
                    })
                    .setNeutralButton("View Details", (d, w) -> {
                        android.content.Intent intent = new android.content.Intent(this, SessionDetailActivity.class);
                        intent.putExtra(SessionDetailActivity.EXTRA_SESSION_ID, newSessionId);
                        intent.putExtra(SessionDetailActivity.EXTRA_PARTNER_USER_ID, friendUserId);
                        startActivity(intent);
                    })
                    .setNegativeButton("Later", null)
                    .show();

            FirestoreSession session = new FirestoreSession();
            session.setSessionId(newSessionId);
            session.setLearnerId(selectedLearnerId);
            session.setLearnerName(learnerSelection);
            session.setTeacherId(selectedTeacherId);
            session.setTeacherName(teacherSelection);
            session.setSkillName(skill);
            session.setScheduledDate(finalDate);
            session.setStartTime(finalTime);
            session.setDurationMinutes(duration);
            session.setCoinCost(coinCost);
            session.setMeetLink(finalMeetUrl);
            session.setMeetingLink(finalMeetUrl);
            session.setMeetingProvider("Jitsi Meet");
            session.setStatus(FirestoreSession.STATUS_SCHEDULED);

            FirestoreRepository.getInstance().createSession(session)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("ChatDetailActivity", "Session created successfully in Firestore: " + newSessionId);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("ChatDetailActivity", "Failed to create session in Firestore: " + (e != null ? e.getMessage() : "Unknown"), e);
                        Toast.makeText(this, "Notice: Session sync pending (" + (e != null ? e.getMessage() : "syncing") + ")", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void openFriendProfile() {
        if (friendUserId == null || friendUserId.isEmpty()) return;
        android.content.Intent intent = new android.content.Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_TARGET_USER_ID, friendUserId);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sActiveChatId = chatId;
        markChatRead();
        if (binding != null && binding.bannerNotificationPermission != null) {
            NotificationBannerHelper.updateBannerVisibility(this, binding.bannerNotificationPermission.getRoot());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (chatId != null && chatId.equals(sActiveChatId)) {
            sActiveChatId = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatId != null && chatId.equals(sActiveChatId)) {
            sActiveChatId = null;
        }
    }

    private void markChatRead() {
        if (chatId != null && currentUserId != null) {
            com.example.skillswapproject.util.ChatReadStatusManager.markChatRead(this, chatId, System.currentTimeMillis());
            FirestoreRepository.getInstance().markChatAsRead(chatId, currentUserId);
        }
    }

    private void loadFriendPhoto() {
        if (friendUserId == null) return;
        FirestoreRepository.getInstance().getUser(friendUserId).addOnSuccessListener(user -> {
            if (user != null && user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
                if (binding.ivToolbarAvatar != null) {
                    binding.ivToolbarAvatar.setVisibility(View.VISIBLE);
                    ImageLoaderHelper.loadProfileImage(binding.ivToolbarAvatar, user.getProfilePhotoUrl());
                    binding.tvToolbarAvatarInitial.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(currentUserId);
        adapter.setOnSessionActionListener(new MessageAdapter.OnSessionActionListener() {
            @Override
            public void onViewDetails(String sessionId) {
                String targetSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : activeSessionId;
                if (targetSessionId != null && !targetSessionId.isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(ChatDetailActivity.this, SessionDetailActivity.class);
                    intent.putExtra(SessionDetailActivity.EXTRA_SESSION_ID, targetSessionId);
                    intent.putExtra(SessionDetailActivity.EXTRA_PARTNER_USER_ID, friendUserId);
                    startActivity(intent);
                } else {
                    FirestoreRepository.getInstance().findActiveSession(currentUserId, friendUserId, (foundId, link) -> {
                        if (foundId != null && !foundId.isEmpty()) {
                            android.content.Intent intent = new android.content.Intent(ChatDetailActivity.this, SessionDetailActivity.class);
                            intent.putExtra(SessionDetailActivity.EXTRA_SESSION_ID, foundId);
                            intent.putExtra(SessionDetailActivity.EXTRA_PARTNER_USER_ID, friendUserId);
                            startActivity(intent);
                        } else {
                            Toast.makeText(ChatDetailActivity.this, "Session details not found", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onDeleteSession(String sessionId) {
                String targetSessionId = (sessionId != null && !sessionId.isEmpty()) ? sessionId : activeSessionId;
                if (targetSessionId != null && !targetSessionId.isEmpty()) {
                    showCancelSessionDialog(targetSessionId);
                } else {
                    FirestoreRepository.getInstance().findActiveSession(currentUserId, friendUserId, (foundId, link) -> {
                        if (foundId != null && !foundId.isEmpty()) {
                            showCancelSessionDialog(foundId);
                        } else {
                            Toast.makeText(ChatDetailActivity.this, "No active session found to cancel", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onJoinMeeting(String meetUrl) {
                if (meetUrl != null && !meetUrl.isEmpty()) {
                    GoogleCalendarMeetHelper.launchGoogleMeet(ChatDetailActivity.this, meetUrl);
                }
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);
    }

    private void showCancelSessionDialog(String targetSessionId) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Session")
                .setMessage("Are you sure you want to cancel this scheduled session? No Skill Coins will be transferred.")
                .setPositiveButton("Confirm Cancellation", (dialog, which) -> {
                    FirestoreRepository.getInstance().cancelSession(targetSessionId, currentUserId, "Cancelled by user")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Session cancelled successfully.", Toast.LENGTH_SHORT).show();
                                if (targetSessionId.equals(activeSessionId)) {
                                    activeSessionId = null;
                                    activeMeetUrl = null;
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to cancel: " + (e != null ? e.getMessage() : "Try again"), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Keep Session", null)
                .show();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(FirestoreChatDetailViewModel.class);

        // Real-time messages listener
        viewModel.getMessagesLiveData().observe(this, messages -> {
            adapter.setMessages(messages);
            updateChatVisibility(messages);
        });

        viewModel.getErrorLiveData().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize chat with chatId and receiverId
        viewModel.initialize(chatId, currentUserId, friendUserId);
    }

    private void updateChatVisibility(java.util.List<com.example.skillswapproject.data.model.FirestoreMessage> messages) {
        boolean hasMessages = messages != null && !messages.isEmpty();
        if (hasMessages) {
            binding.layoutConversationStarters.setVisibility(View.GONE);
            binding.rvMessages.setVisibility(View.VISIBLE);
            binding.rvMessages.smoothScrollToPosition(messages.size() - 1);
            markChatRead();
        } else {
            if (isSwapAccepted) {
                binding.rvMessages.setVisibility(View.GONE);
                binding.layoutConversationStarters.setVisibility(View.VISIBLE);
            } else {
                binding.layoutConversationStarters.setVisibility(View.GONE);
                binding.rvMessages.setVisibility(View.VISIBLE);
            }
        }
    }

    private void checkSwapStatus() {
        if (friendUserId == null) return;
        FirestoreRepository.getInstance().checkAcceptedSwap(currentUserId, friendUserId, new FirestoreRepository.OnSwapCheckListener() {
            @Override
            public void onResult(boolean accepted) {
                isSwapAccepted = accepted;
                if (accepted) {
                    binding.bannerSwapCancelled.setVisibility(View.GONE);
                    binding.etMessageInput.setEnabled(true);
                    binding.btnSend.setEnabled(true);
                    binding.btnSend.setAlpha(1.0f);
                } else {
                    binding.bannerSwapCancelled.setVisibility(View.VISIBLE);
                    binding.tvSwapCancelledNotice.setText("Skill swap must be active and accepted before you can send messages.");
                    binding.etMessageInput.setEnabled(false);
                    binding.btnSend.setEnabled(false);
                    binding.btnSend.setAlpha(0.4f);
                }
                if (viewModel != null) {
                    updateChatVisibility(viewModel.getMessagesLiveData().getValue());
                }
            }

            @Override
            public void onError(Exception e) {
                // If error checking, keep enabled
            }
        });
    }

    private void setupStarterListeners() {
        binding.btnStarter1.setOnClickListener(v -> sendStarterMessage(binding.tvStarter1.getText().toString()));
        binding.btnStarter2.setOnClickListener(v -> sendStarterMessage(binding.tvStarter2.getText().toString()));
        binding.btnStarter3.setOnClickListener(v -> sendStarterMessage(binding.tvStarter3.getText().toString()));
        binding.btnStarter4.setOnClickListener(v -> sendStarterMessage(binding.tvStarter4.getText().toString()));
        binding.btnStarter5.setOnClickListener(v -> sendStarterMessage(binding.tvStarter5.getText().toString()));

        binding.btnWriteOwnMessage.setOnClickListener(v -> {
            binding.etMessageInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(binding.etMessageInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void sendStarterMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;
        viewModel.sendMessage(text.trim());
    }

    private void setupInputListeners() {
        binding.btnSend.setOnClickListener(v -> sendMessage());

        binding.etMessageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void sendMessage() {
        if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        String rawText = binding.etMessageInput.getText() != null
                ? binding.etMessageInput.getText().toString()
                : "";

        String validationError = com.example.skillswapproject.util.ValidationUtils.validateChatMessage(rawText);
        if (validationError != null) {
            Toast.makeText(this, validationError, Toast.LENGTH_SHORT).show();
            return;
        }

        binding.etMessageInput.setText("");
        viewModel.sendMessage(rawText.trim());
    }
}
