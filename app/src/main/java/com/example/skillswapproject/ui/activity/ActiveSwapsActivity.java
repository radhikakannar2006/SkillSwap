package com.example.skillswapproject.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.widget.ArrayAdapter;

import com.example.skillswapproject.R;
import com.example.skillswapproject.data.model.FirestoreSession;
import com.example.skillswapproject.data.model.FirestoreSwap;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.ActivityActiveSwapsBinding;
import com.example.skillswapproject.databinding.DialogCreateMeetingSessionBinding;
import com.example.skillswapproject.ui.adapter.ActiveSwapsAdapter;
import com.example.skillswapproject.util.JitsiMeetHelper;
import com.example.skillswapproject.util.NotificationBannerHelper;
import com.example.skillswapproject.util.SkillConstants;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActiveSwapsActivity extends AppCompatActivity implements ActiveSwapsAdapter.OnActiveSwapActionListener {

    private ActivityActiveSwapsBinding binding;
    private ActiveSwapsAdapter adapter;
    private ListenerRegistration swapsRegistration;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityActiveSwapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Please log in to view active swaps", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadActiveSwaps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bannerNotificationPermission != null) {
            NotificationBannerHelper.updateBannerVisibility(
                    this,
                    binding.bannerNotificationPermission.getRoot()
            );
        }
    }

    private void setupUI() {
        binding.btnBackActiveSwaps.setOnClickListener(v -> finish());

        if (binding.bannerNotificationPermission != null) {
            NotificationBannerHelper.setupBanner(
                    this,
                    binding.bannerNotificationPermission.getRoot(),
                    "Get swap & session notifications",
                    "Never miss active swap updates, scheduled sessions, and Google Meet reminders."
            );
        }

        adapter = new ActiveSwapsAdapter(currentUserId, this);
        binding.rvActiveSwaps.setAdapter(adapter);

        // Search text watcher
        binding.etSearchActiveSwaps.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = (s != null) ? s.toString().trim() : "";
                binding.btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.filter(query);
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etSearchActiveSwaps.setText("");
        });
    }

    private void loadActiveSwaps() {
        binding.progressBarActiveSwaps.setVisibility(View.VISIBLE);

        swapsRegistration = FirestoreRepository.getInstance().listenToUserSwaps(currentUserId, new FirestoreRepository.OnUserSwapsListener() {
            @Override
            public void onSwapsLoaded(List<FirestoreSwap> receivedSwaps, List<FirestoreSwap> sentSwaps) {
                binding.progressBarActiveSwaps.setVisibility(View.GONE);

                // Filter for ACCEPTED swaps and deduplicate by partner ID
                Map<String, FirestoreSwap> activePartnersMap = new HashMap<>();

                if (receivedSwaps != null) {
                    for (FirestoreSwap s : receivedSwaps) {
                        if ("ACCEPTED".equalsIgnoreCase(s.getStatus()) && s.getRequesterId() != null) {
                            activePartnersMap.put(s.getRequesterId(), s);
                        }
                    }
                }

                if (sentSwaps != null) {
                    for (FirestoreSwap s : sentSwaps) {
                        if ("ACCEPTED".equalsIgnoreCase(s.getStatus()) && s.getReceiverId() != null) {
                            activePartnersMap.put(s.getReceiverId(), s);
                        }
                    }
                }

                List<FirestoreSwap> activeList = new ArrayList<>(activePartnersMap.values());
                adapter.setSwaps(activeList);

                int count = activeList.size();
                binding.tvActiveSwapsCountSubtitle.setText(
                        count + " " + (count == 1 ? "partner" : "partners")
                );

                updateEmptyState();
            }

            @Override
            public void onError(Exception e) {
                binding.progressBarActiveSwaps.setVisibility(View.GONE);
                Toast.makeText(ActiveSwapsActivity.this, "Could not load swaps: " + (e != null ? e.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        if (adapter.getFilteredCount() == 0) {
            binding.layoutEmptyActiveSwaps.setVisibility(View.VISIBLE);
            binding.rvActiveSwaps.setVisibility(View.GONE);
            if (adapter.getTotalCount() == 0) {
                binding.tvEmptyActiveSwapsTitle.setText("No Active Swaps");
                binding.tvEmptyActiveSwapsDesc.setText("When a skill swap is accepted, your partners will appear here. You can chat or end swaps anytime.");
            } else {
                binding.tvEmptyActiveSwapsTitle.setText("No Matches Found");
                binding.tvEmptyActiveSwapsDesc.setText("No active swap partners match your search query.");
            }
        } else {
            binding.layoutEmptyActiveSwaps.setVisibility(View.GONE);
            binding.rvActiveSwaps.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onProfileClick(String partnerId) {
        if (partnerId == null || partnerId.isEmpty()) return;
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_TARGET_USER_ID, partnerId);
        startActivity(intent);
    }

    @Override
    public void onMessageClick(String partnerId, String partnerName, String partnerPhoto) {
        if (partnerId == null || partnerId.isEmpty()) return;
        String chatId = FirestoreRepository.getChatId(currentUserId, partnerId);

        Intent intent = new Intent(this, ChatDetailActivity.class);
        intent.putExtra("EXTRA_CHAT_ID", chatId);
        intent.putExtra("EXTRA_FRIEND_USER_ID", partnerId);
        intent.putExtra("EXTRA_FRIEND_NAME", partnerName != null ? partnerName : "User");
        startActivity(intent);
    }

    @Override
    public void onScheduleClick(FirestoreSwap swap, String partnerName, String partnerId) {
        if (partnerId == null || partnerId.isEmpty()) return;

        FirestoreRepository.getInstance().findActiveSession(currentUserId, partnerId, (sessionId, meetLink) -> {
            if (sessionId != null && !sessionId.isEmpty()) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Session Scheduled 📅")
                        .setMessage("You already have an active session scheduled with " + partnerName + ".\n\nWould you like to view session details or schedule a new one?")
                        .setPositiveButton("📋 View Details", (dialog, which) -> {
                            Intent intent = new Intent(this, SessionDetailActivity.class);
                            intent.putExtra(SessionDetailActivity.EXTRA_SESSION_ID, sessionId);
                            intent.putExtra(SessionDetailActivity.EXTRA_PARTNER_USER_ID, partnerId);
                            startActivity(intent);
                        })
                        .setNegativeButton("Schedule New", (dialog, which) -> {
                            showScheduleMeetingDialog(swap, partnerName, partnerId);
                        })
                        .setNeutralButton("Cancel", null)
                        .show();
            } else {
                showScheduleMeetingDialog(swap, partnerName, partnerId);
            }
        });
    }

    private void showScheduleMeetingDialog(FirestoreSwap swap, String partnerName, String partnerId) {
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

        String initialSkill = "";
        if (swap != null) {
            if (currentUserId.equals(swap.getRequesterId())) {
                initialSkill = swap.getRequestedSkillName() != null ? swap.getRequestedSkillName() : swap.getOfferedSkillName();
            } else {
                initialSkill = swap.getOfferedSkillName() != null ? swap.getOfferedSkillName() : swap.getRequestedSkillName();
            }
        }

        List<String> skillOptions = new ArrayList<>();
        if (initialSkill != null && !initialSkill.trim().isEmpty()) {
            skillOptions.add(initialSkill.trim());
        }
        for (String pre : SkillConstants.PREDEFINED_SKILLS) {
            if (!skillOptions.contains(pre)) {
                skillOptions.add(pre);
            }
        }

        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(this,
                R.layout.dropdown_item_dark, skillOptions);
        dialogBinding.etMeetSkill.setAdapter(skillAdapter);
        if (initialSkill != null && !initialSkill.trim().isEmpty()) {
            dialogBinding.etMeetSkill.setText(initialSkill, false);
        }
        dialogBinding.etMeetSkill.setOnClickListener(v -> dialogBinding.etMeetSkill.showDropDown());
        dialogBinding.etMeetSkill.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) dialogBinding.etMeetSkill.showDropDown();
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
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

        String myName = "Current User";
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            String dName = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (dName != null && !dName.isEmpty()) {
                myName = dName;
            }
        }

        final String finalMyName = myName;

        List<String> roleOptions = new ArrayList<>();
        roleOptions.add(finalMyName);
        roleOptions.add(partnerName);
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item_dark, roleOptions);
        dialogBinding.etMeetTeacher.setAdapter(roleAdapter);
        dialogBinding.etMeetLearner.setAdapter(roleAdapter);
        
        dialogBinding.etMeetTeacher.setOnClickListener(v -> dialogBinding.etMeetTeacher.showDropDown());
        dialogBinding.etMeetLearner.setOnClickListener(v -> dialogBinding.etMeetLearner.showDropDown());

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
                Toast.makeText(this, "Please select a date for the session", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedTime[0] == null || selectedTime[0].isEmpty()) {
                Toast.makeText(this, "Please select a time for the session", Toast.LENGTH_SHORT).show();
                return;
            }

            int checkedDurationId = dialogBinding.rgMeetDuration.getCheckedRadioButtonId();
            if (checkedDurationId != R.id.rbDuration30 && checkedDurationId != R.id.rbDuration60) {
                Toast.makeText(this, "Please select a session duration", Toast.LENGTH_SHORT).show();
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

            String selectedTeacherId = teacherSelection.equals(finalMyName) ? currentUserId : partnerId;
            String selectedLearnerId = learnerSelection.equals(finalMyName) ? currentUserId : partnerId;

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

            FirestoreRepository.getInstance().createSession(session).addOnSuccessListener(aVoid -> {
                Toast.makeText(ActiveSwapsActivity.this, "Session scheduled successfully!", Toast.LENGTH_SHORT).show();

                // Post session details message into the chat
                String chatId = FirestoreRepository.getChatId(currentUserId, partnerId);
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

                com.example.skillswapproject.data.model.FirestoreMessage message =
                        new com.example.skillswapproject.data.model.FirestoreMessage();
                message.setSenderId(currentUserId);
                message.setReceiverId(partnerId);
                message.setText(msgText);
                FirestoreRepository.getInstance().sendMessage(chatId, message);

                new MaterialAlertDialogBuilder(ActiveSwapsActivity.this)
                        .setTitle("Session Ready 🎥")
                        .setMessage("Session scheduled and link created with " + partnerName + ".\n\n" +
                                "Date: " + finalDate + " at " + finalTime + "\n" +
                                "Duration: " + (duration == 60 ? "1 hr" : "30 mins") + "\n\n" +
                                "What would you like to do?")
                        .setPositiveButton("Join Call", (d, w) -> {
                            JitsiMeetHelper.launchJitsiMeet(ActiveSwapsActivity.this, finalMeetUrl);
                        })
                        .setNeutralButton("View Details", (d, w) -> {
                            Intent intent = new Intent(ActiveSwapsActivity.this, SessionDetailActivity.class);
                            intent.putExtra(SessionDetailActivity.EXTRA_SESSION_ID, newSessionId);
                            intent.putExtra(SessionDetailActivity.EXTRA_PARTNER_USER_ID, partnerId);
                            startActivity(intent);
                        })
                        .setNegativeButton("Later", null)
                        .show();
            }).addOnFailureListener(e -> {
                Toast.makeText(ActiveSwapsActivity.this, "Failed to schedule session: " + (e != null ? e.getMessage() : "Error"), Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }

    @Override
    public void onEndSwapClick(FirestoreSwap swap, String partnerName, String partnerId) {
        if (partnerId == null || partnerId.isEmpty()) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("End Skill Swap with " + partnerName)
                .setMessage("Are you sure you want to end this active skill swap? Active sessions will be concluded.")
                .setPositiveButton("End Swap", (dialog, which) -> endSwapDirectly(partnerName, partnerId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void endSwapDirectly(String partnerName, String partnerId) {
        FirestoreRepository.getInstance().endSwap(currentUserId, partnerId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ActiveSwapsActivity.this, "Skill swap ended with " + partnerName, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ActiveSwapsActivity.this, "Failed to end swap: " + (e != null ? e.getMessage() : "Try again"), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (swapsRegistration != null) {
            swapsRegistration.remove();
            swapsRegistration = null;
        }
        binding = null;
    }
}
