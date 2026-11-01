package com.example.skillswapproject.ui.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.skillswapproject.data.model.FirestoreSession;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.ActivityScheduleSessionBinding;
import com.example.skillswapproject.util.GoogleCalendarMeetHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ScheduleSessionActivity extends AppCompatActivity {

    public static final String EXTRA_SWAP_ID = "EXTRA_SWAP_ID";
    public static final String EXTRA_TARGET_USER_ID = "EXTRA_TARGET_USER_ID";
    public static final String EXTRA_TARGET_USER_NAME = "EXTRA_TARGET_USER_NAME";
    public static final String EXTRA_SKILL_NAME = "EXTRA_SKILL_NAME";

    private ActivityScheduleSessionBinding binding;

    private String swapId;
    private String targetUserId;
    private String targetUserName = "Partner";
    private String skillName = "Skill Swap";
    private String currentUserId;
    private String currentUserName = "User";

    private String selectedSkillLevel = "Beginner";
    private int selectedCoinCost = 1;
    private int selectedDurationMinutes = 60;
    private Calendar selectedCalendar = Calendar.getInstance();

    private boolean isDatePicked = false;
    private boolean isTimePicked = false;
    private int learnerAvailableCoins = 5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScheduleSessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        extractIntentData();
        setupUI();
        loadLearnerWalletBalance();
    }

    private void extractIntentData() {
        swapId = getIntent().getStringExtra(EXTRA_SWAP_ID);
        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        targetUserName = getIntent().getStringExtra(EXTRA_TARGET_USER_NAME);
        if (targetUserName == null || targetUserName.isEmpty()) targetUserName = "Partner";
        skillName = getIntent().getStringExtra(EXTRA_SKILL_NAME);
        if (skillName == null || skillName.isEmpty()) skillName = "General Knowledge";
    }

    private void setupUI() {
        binding.btnBackSchedule.setOnClickListener(v -> finish());

        binding.tvPartnerNameHeader.setText("Learning with " + targetUserName);
        binding.tvSkillNameSubtitle.setText("Skill: " + skillName);

        // Skill Level Chips
        binding.chipGroupScheduleLevel.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(binding.chipSchedAdvanced.getId())) {
                selectedSkillLevel = "Advanced";
                selectedCoinCost = 3;
            } else if (checkedIds.contains(binding.chipSchedIntermediate.getId())) {
                selectedSkillLevel = "Intermediate";
                selectedCoinCost = 2;
            } else {
                selectedSkillLevel = "Beginner";
                selectedCoinCost = 1;
            }
            updateWalletValidationUi();
        });

        // Duration Chips
        binding.chipGroupDuration.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(binding.chipDur30.getId())) {
                selectedDurationMinutes = 30;
            } else {
                selectedDurationMinutes = 60;
            }
        });

        // Date Picker Button
        binding.btnSelectDate.setOnClickListener(v -> showDatePicker());

        // Time Picker Button
        binding.btnSelectTime.setOnClickListener(v -> showTimePicker());

        // Submit Button
        binding.btnConfirmScheduleSession.setOnClickListener(v -> executeScheduleSession());
    }

    private void loadLearnerWalletBalance() {
        FirestoreRepository.getInstance().getUser(currentUserId).addOnSuccessListener(user -> {
            if (user != null) {
                currentUserName = user.getName() != null ? user.getName() : "User";
                learnerAvailableCoins = user.getSkillCoins();
                updateWalletValidationUi();
            }
        });
    }

    private void updateWalletValidationUi() {
        if (binding == null) return;
        binding.tvSchedWalletBalance.setText(
                "Your Balance: " + learnerAvailableCoins + " " + (learnerAvailableCoins == 1 ? "Coin" : "Coins") +
                " (Cost: " + selectedCoinCost + " " + (selectedCoinCost == 1 ? "Coin" : "Coins") + ")"
        );

        if (learnerAvailableCoins < selectedCoinCost) {
            binding.tvSchedInsufficientWarning.setVisibility(View.VISIBLE);
            binding.btnConfirmScheduleSession.setEnabled(false);
            binding.btnConfirmScheduleSession.setAlpha(0.6f);
        } else {
            binding.tvSchedInsufficientWarning.setVisibility(View.GONE);
            binding.btnConfirmScheduleSession.setEnabled(true);
            binding.btnConfirmScheduleSession.setAlpha(1.0f);
        }
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    isDatePicked = true;

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
                    binding.btnSelectDate.setText(dateFormat.format(selectedCalendar.getTime()));
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(now.getTimeInMillis());
        dialog.show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minute);
                    isTimePicked = true;

                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                    binding.btnSelectTime.setText(timeFormat.format(selectedCalendar.getTime()));
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                false
        );
        dialog.show();
    }

    private void executeScheduleSession() {
        if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        String dtErr = com.example.skillswapproject.util.ValidationUtils.validateSessionDateTime(selectedCalendar, isDatePicked, isTimePicked);
        if (dtErr != null) {
            Toast.makeText(this, dtErr, Toast.LENGTH_LONG).show();
            return;
        }

        if (learnerAvailableCoins < selectedCoinCost) {
            Toast.makeText(this, "You don't have enough Skill Coins to schedule this session level.", Toast.LENGTH_LONG).show();
            return;
        }

        binding.btnConfirmScheduleSession.setEnabled(false);
        binding.btnConfirmScheduleSession.setText("Creating Session...");

        long startTimestamp = selectedCalendar.getTimeInMillis();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.US);
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US);
        String scheduledDate = dateFormat.format(selectedCalendar.getTime());
        String startTime = timeFormat.format(selectedCalendar.getTime());

        java.util.Calendar endCal = (java.util.Calendar) selectedCalendar.clone();
        endCal.add(java.util.Calendar.MINUTE, selectedDurationMinutes);
        String endTime = timeFormat.format(endCal.getTime());

        String uniqueId = java.util.UUID.randomUUID().toString().substring(0, 8);
        final String meetLink = "https://meet.jit.si/SkillSwap-Session-" + uniqueId;
        String eventId = "jitsi_evt_" + uniqueId;

        saveSessionToFirestore(eventId, meetLink, scheduledDate, startTime, endTime, startTimestamp);
    }

    private void saveSessionToFirestore(String eventId, String meetLink, String dateStr, String startTimeStr, String endTimeStr, long startTimestamp) {
        String sessionId = "sess_" + (swapId != null ? swapId : "gen") + "_" + System.currentTimeMillis();

        FirestoreSession session = new FirestoreSession();
        session.setSessionId(sessionId);
        session.setSwapId(swapId != null ? swapId : "swap_default");
        // The user who creates/schedules the session = Teacher
        session.setTeacherId(currentUserId);
        session.setTeacherName(currentUserName != null && !currentUserName.isEmpty() ? currentUserName : "Teacher");
        // The user who receives the session invitation = Learner
        session.setLearnerId(targetUserId != null ? targetUserId : "user_partner");
        session.setLearnerName(targetUserName != null && !targetUserName.isEmpty() ? targetUserName : "Learner");
        session.setSkillName(skillName);
        session.setSkillLevel(selectedSkillLevel);
        session.setCoinCost(selectedCoinCost);
        session.setDurationMinutes(selectedDurationMinutes);
        session.setScheduledDate(dateStr);
        session.setStartTime(startTimeStr);
        session.setEndTime(endTimeStr);
        session.setScheduledTimestamp(startTimestamp);
        session.setGoogleCalendarEventId(eventId);
        session.setMeetLink(meetLink);
        session.setMeetingLink(meetLink);
        session.setMeetingProvider("Jitsi Meet");
        session.setStatus(FirestoreSession.STATUS_SCHEDULED);

        FirestoreRepository.getInstance().createSession(session)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ScheduleSessionActivity.this, "Session Scheduled! Jitsi Meet created.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(ScheduleSessionActivity.this, SessionDetailActivity.class);
                    intent.putExtra(SessionDetailActivity.EXTRA_SESSION_ID, sessionId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        binding.btnConfirmScheduleSession.setEnabled(true);
                        binding.btnConfirmScheduleSession.setText("Schedule & Create Jitsi Meet");
                    }
                    Toast.makeText(ScheduleSessionActivity.this, "Failed to save session: " + (e != null ? e.getMessage() : "Try again"), Toast.LENGTH_SHORT).show();
                });
    }
}
