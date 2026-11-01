package com.example.skillswapproject.ui.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.skillswapproject.data.model.FirestoreSession;
import com.example.skillswapproject.data.repository.FirestoreRepository;
import com.example.skillswapproject.databinding.ActivitySessionDetailBinding;
import com.example.skillswapproject.util.JitsiMeetHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SessionDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "EXTRA_SESSION_ID";
    public static final String EXTRA_PARTNER_USER_ID = "EXTRA_PARTNER_USER_ID";
    private static final String TAG = "SessionDetailActivity";

    private ActivitySessionDetailBinding binding;
    private ListenerRegistration sessionRegistration;

    private String sessionId;
    private String partnerUserId;
    private String currentUserId;
    private FirestoreSession currentSession;
    private boolean isRetryingFallback = false;
    private CountDownTimer sessionTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySessionDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirestoreRepository.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        partnerUserId = getIntent().getStringExtra(EXTRA_PARTNER_USER_ID);
        if (partnerUserId == null) {
            partnerUserId = getIntent().getStringExtra("EXTRA_PARTNER_USER_ID");
        }
        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, "Session ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        listenToSessionData();
    }

    private void setupUI() {
        binding.btnBackSessionDetail.setOnClickListener(v -> finish());

        binding.btnJoinGoogleMeet.setOnClickListener(v -> {
            if (currentSession != null && currentSession.getMeetLink() != null) {
                JitsiMeetHelper.launchJitsiMeet(this, currentSession.getMeetLink());
            } else {
                Toast.makeText(this, "Google Meet link unavailable", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnConfirmSessionCompleted.setOnClickListener(v -> handleConfirmCompletion());

        binding.btnRatePartnerSession.setOnClickListener(v -> {
            if (currentSession == null) return;
            String partnerId = currentUserId.equals(currentSession.getLearnerId())
                    ? currentSession.getTeacherId()
                    : currentSession.getLearnerId();

            Intent intent = new Intent(SessionDetailActivity.this, UserProfileActivity.class);
            intent.putExtra(UserProfileActivity.EXTRA_TARGET_USER_ID, partnerId);
            startActivity(intent);
        });

        binding.btnCancelSession.setOnClickListener(v -> confirmCancelSession());

    }

    private void listenToSessionData() {
        if (sessionRegistration != null) {
            sessionRegistration.remove();
            sessionRegistration = null;
        }
        sessionRegistration = FirestoreRepository.getInstance().listenToSession(sessionId, new FirestoreRepository.OnSingleSessionListener() {
            @Override
            public void onSessionLoaded(FirestoreSession session) {
                if (isFinishing() || isDestroyed()) return;
                if (session == null) {
                    Log.w(TAG, "Session document null for id: " + sessionId);
                    if (currentSession != null) {
                        return; // Keep existing data if already bound
                    }
                    if (partnerUserId != null && !partnerUserId.isEmpty() && !isRetryingFallback) {
                        isRetryingFallback = true;
                        FirestoreRepository.getInstance().findActiveSession(currentUserId, partnerUserId, (foundId, link) -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (foundId != null && !foundId.isEmpty() && !foundId.equals(sessionId)) {
                                sessionId = foundId;
                                listenToSessionData();
                            } else {
                                Toast.makeText(SessionDetailActivity.this, "Session data is syncing. Please wait...", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(SessionDetailActivity.this, "Session data is syncing. Please wait...", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                currentSession = session;
                bindSessionData(session);
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                Log.e(TAG, "Error loading session details: " + (e != null ? e.getMessage() : "Unknown"), e);
                
                if (e != null && e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                    Toast.makeText(SessionDetailActivity.this, "Firebase Permissions Denied! Please update your Firestore Security Rules.", Toast.LENGTH_LONG).show();
                    if (binding != null) {
                        binding.tvDetailStatusChip.setText("Permission Denied");
                        binding.tvDetailStatusChip.setTextColor(android.graphics.Color.parseColor("#991B1B"));
                        binding.tvDetailStatusChip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEE2E2")));
                        binding.tvConfirmationStatusDesc.setText("⚠️ Cannot load session due to Firebase Security Rules. Please update them in the Firebase Console to allow access to the Sessions collection.");
                        binding.tvConfirmationStatusDesc.setTextColor(android.graphics.Color.parseColor("#DC2626"));
                        binding.btnConfirmSessionCompleted.setEnabled(false);
                        binding.btnCancelSession.setEnabled(false);
                    }
                } else {
                    Toast.makeText(SessionDetailActivity.this, "Could not load session details: " + (e != null ? e.getMessage() : "Error"), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindSessionData(FirestoreSession s) {
        binding.tvDetailSkillTitle.setText(s.getSkillName() + " Session");
        binding.tvDetailParticipants.setText("👨‍🏫 Teacher: " + s.getTeacherName() + "   |   🧑‍🎓 Learner: " + s.getLearnerName());

        String timeText = "Scheduled: " + (s.getScheduledDate() != null ? s.getScheduledDate() : "") +
                (s.getStartTime() != null ? ", " + s.getStartTime() : "") +
                " (" + (s.getDurationMinutes() == 60 ? "1 Hour" : "30 Mins") + ")";
        binding.tvDetailScheduleTime.setText(timeText);

        binding.tvDetailCoinCost.setText("Level: " + s.getSkillLevel() + " (Cost: " + s.getCoinCost() + " " + (s.getCoinCost() == 1 ? "Skill Coin" : "Skill Coins") + ")");

        binding.tvDetailMeetLink.setText(s.getMeetLink() != null ? s.getMeetLink() : "Link Pending");


        // Status Chip
        String status = s.getStatus();
        binding.tvDetailStatusChip.setText(status);

        if (FirestoreSession.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            binding.tvDetailStatusChip.setText("Completed");
            binding.tvDetailStatusChip.setTextColor(Color.parseColor("#166534"));
            binding.tvDetailStatusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
        } else if (FirestoreSession.STATUS_WAITING_CONFIRMATION.equalsIgnoreCase(status) || FirestoreSession.STATUS_PENDING_CONFIRMATION.equalsIgnoreCase(status)) {
            binding.tvDetailStatusChip.setText("Waiting for Confirmation");
            binding.tvDetailStatusChip.setTextColor(Color.parseColor("#9A3412"));
            binding.tvDetailStatusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEDD5")));
        } else if (FirestoreSession.STATUS_NEEDS_REVIEW.equalsIgnoreCase(status) || FirestoreSession.STATUS_DISPUTED.equalsIgnoreCase(status)) {
            binding.tvDetailStatusChip.setText("Needs Review");
            binding.tvDetailStatusChip.setTextColor(Color.parseColor("#991B1B"));
            binding.tvDetailStatusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
        } else if (FirestoreSession.STATUS_CANCELLED.equalsIgnoreCase(status)) {
            binding.tvDetailStatusChip.setText("Cancelled");
            binding.tvDetailStatusChip.setTextColor(Color.parseColor("#64748B"));
            binding.tvDetailStatusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
        } else {
            binding.tvDetailStatusChip.setText("Scheduled");
            binding.tvDetailStatusChip.setTextColor(Color.parseColor("#1E40AF"));
            binding.tvDetailStatusChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
        }

        // Dual Confirmation State Rendering
        boolean isLearner = currentUserId.equals(s.getLearnerId());
        boolean isTeacher = currentUserId.equals(s.getTeacherId());

        boolean learnerDone = s.isLearnerConfirmed();
        boolean teacherDone = s.isTeacherConfirmed();

        binding.tvStatusLearnerConfirm.setText("Learner (" + s.getLearnerName() + "): " + (learnerDone ? "Confirmed ✓" : "Pending ⏳"));
        binding.tvStatusLearnerConfirm.setTextColor(Color.parseColor(learnerDone ? "#166534" : "#475569"));
        binding.tvStatusLearnerConfirm.setBackgroundColor(Color.parseColor(learnerDone ? "#DCFCE7" : "#F1F5F9"));

        binding.tvStatusTeacherConfirm.setText("Teacher (" + s.getTeacherName() + "): " + (teacherDone ? "Confirmed ✓" : "Pending ⏳"));
        binding.tvStatusTeacherConfirm.setTextColor(Color.parseColor(teacherDone ? "#166534" : "#475569"));
        binding.tvStatusTeacherConfirm.setBackgroundColor(Color.parseColor(teacherDone ? "#DCFCE7" : "#F1F5F9"));

        boolean myConfirmationDone = (isLearner && learnerDone) || (isTeacher && teacherDone);
        boolean coinsReleased = s.isCoinsReleased();
        boolean isDisputed = s.isDisputed() || FirestoreSession.STATUS_NEEDS_REVIEW.equalsIgnoreCase(status) || FirestoreSession.STATUS_DISPUTED.equalsIgnoreCase(status);

        // Role-specific confirmation prompt text
        String partnerName = isTeacher ? s.getLearnerName() : s.getTeacherName();
        binding.tvCompletionQuestion.setText(isTeacher
                ? "Did you complete teaching this session to " + s.getLearnerName() + "?"
                : "Did you complete learning this session with " + s.getTeacherName() + "?");

        if (isDisputed) {
            binding.tvConfirmationStatusDesc.setText("⚠️ This session has been reported for review. SkillCoin distribution is on hold.");
            binding.tvConfirmationStatusDesc.setTextColor(Color.parseColor("#DC2626"));

            binding.layoutConfirmationActions.setVisibility(View.GONE);
            binding.layoutSessionCompletedBanner.setVisibility(View.GONE);
            binding.btnRatePartnerSession.setVisibility(View.GONE);
        } else if (coinsReleased || FirestoreSession.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            binding.tvConfirmationStatusDesc.setText("Session Verified & Completed! SkillCoins have been transferred.");
            binding.tvConfirmationStatusDesc.setTextColor(Color.parseColor("#166534"));

            binding.layoutSessionCompletedBanner.setVisibility(View.VISIBLE);
            if (isTeacher) {
                binding.tvCompletedHeadline.setText("🎉 Session Completed! You earned " + s.getCoinCost() + " SkillCoins.");
                binding.tvCompletedRewardDetail.setText("Congratulations on completing your teaching session! The coins are in your wallet.");
            } else {
                binding.tvCompletedHeadline.setText("🎉 Session Completed!");
                binding.tvCompletedRewardDetail.setText("Your session has been recorded as completed. " + s.getCoinCost() + " SkillCoins transferred to " + s.getTeacherName() + ".");
            }

            binding.layoutConfirmationActions.setVisibility(View.GONE);
            binding.btnRatePartnerSession.setVisibility(View.VISIBLE);
        } else if (FirestoreSession.STATUS_CANCELLED.equalsIgnoreCase(status)) {
            binding.tvConfirmationStatusDesc.setText("This session was cancelled. No SkillCoins were transferred.");
            binding.tvConfirmationStatusDesc.setTextColor(Color.parseColor("#991B1B"));

            binding.layoutSessionCompletedBanner.setVisibility(View.GONE);
            binding.layoutConfirmationActions.setVisibility(View.GONE);
            binding.btnRatePartnerSession.setVisibility(View.GONE);
        } else if (myConfirmationDone) {
            binding.tvConfirmationStatusDesc.setText("You have confirmed completion. Waiting for " + partnerName + " to confirm...");
            binding.tvConfirmationStatusDesc.setTextColor(Color.parseColor("#C2410C"));

            binding.layoutSessionCompletedBanner.setVisibility(View.GONE);
            binding.layoutConfirmationActions.setVisibility(View.VISIBLE);
            binding.btnConfirmSessionCompleted.setEnabled(false);
            binding.btnConfirmSessionCompleted.setText("Waiting for " + (isTeacher ? "Learner" : "Teacher") + " to confirm...");
            binding.btnCancelSession.setVisibility(View.VISIBLE);
            binding.btnRatePartnerSession.setVisibility(View.GONE);
        } else {
            binding.tvConfirmationStatusDesc.setText("Both participants must confirm completion after the session ends.");
            binding.tvConfirmationStatusDesc.setTextColor(Color.parseColor("#64748B"));

            binding.layoutSessionCompletedBanner.setVisibility(View.GONE);
            binding.layoutConfirmationActions.setVisibility(View.VISIBLE);
            binding.btnConfirmSessionCompleted.setEnabled(false);
            binding.btnConfirmSessionCompleted.setText("Verifying time...");
            binding.btnCancelSession.setVisibility(View.VISIBLE);
            binding.btnRatePartnerSession.setVisibility(View.GONE);
        }
        
        setupSessionTimer(s);
    }

    private void setupSessionTimer(FirestoreSession s) {
        if (sessionTimer != null) {
            sessionTimer.cancel();
            sessionTimer = null;
        }

        binding.tvSessionTimer.setVisibility(View.GONE);

        String status = s.getStatus();
        boolean coinsReleased = s.isCoinsReleased();
        boolean isDisputed = s.isDisputed() || FirestoreSession.STATUS_NEEDS_REVIEW.equalsIgnoreCase(status) || FirestoreSession.STATUS_DISPUTED.equalsIgnoreCase(status);
        
        if (coinsReleased || FirestoreSession.STATUS_COMPLETED.equalsIgnoreCase(status) || 
            FirestoreSession.STATUS_CANCELLED.equalsIgnoreCase(status) || isDisputed) {
            return; 
        }

        String scheduledDateStr = s.getScheduledDate();
        String startTimeStr = s.getStartTime();
        
        if (scheduledDateStr == null || scheduledDateStr.isEmpty() || startTimeStr == null || startTimeStr.isEmpty()) {
            return;
        }

        try {
            String combinedDate = scheduledDateStr + " " + startTimeStr;
            // Fix common locale issue where "Sept" is generated instead of "Sep"
            if (combinedDate.contains("Sept ")) {
                combinedDate = combinedDate.replace("Sept ", "Sep ");
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            Date startDate = null;
            try {
                startDate = sdf.parse(combinedDate);
            } catch (ParseException e) {
                // Try US locale as fallback
                SimpleDateFormat sdfUS = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US);
                try {
                    startDate = sdfUS.parse(combinedDate);
                } catch (ParseException e2) {
                    // Try without comma
                    combinedDate = combinedDate.replace(",", "");
                    SimpleDateFormat sdfNoComma = new SimpleDateFormat("MMM dd yyyy hh:mm a", Locale.US);
                    startDate = sdfNoComma.parse(combinedDate);
                }
            }
            
            if (startDate == null) return;

            long startMillis = startDate.getTime();
            int durationMins = s.getDurationMinutes() > 0 ? s.getDurationMinutes() : 60;
            long endMillis = startMillis + (durationMins * 60 * 1000L);

            startTimerUpdateLoop(startMillis, endMillis);

        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse session schedule time: " + scheduledDateStr + " " + startTimeStr, e);
        }
    }

    private void startTimerUpdateLoop(long startMillis, long endMillis) {
        long nowInitial = System.currentTimeMillis();
        if (nowInitial >= endMillis) {
            binding.tvSessionTimer.setVisibility(View.GONE);
            if (currentSession != null) {
                boolean isLearner = currentUserId != null && currentUserId.equals(currentSession.getLearnerId());
                boolean isTeacher = currentUserId != null && currentUserId.equals(currentSession.getTeacherId());
                boolean myConfirmationDone = (isLearner && currentSession.isLearnerConfirmed()) || 
                                             (isTeacher && currentSession.isTeacherConfirmed());
                
                if (!myConfirmationDone) {
                    binding.btnConfirmSessionCompleted.setEnabled(true);
                    binding.btnConfirmSessionCompleted.setText("✅ Yes, I Completed It");
                }
            }
            return;
        }

        sessionTimer = new CountDownTimer(endMillis - nowInitial, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long now = System.currentTimeMillis();
                if (now < startMillis) {
                    binding.tvSessionTimer.setVisibility(View.VISIBLE);
                    binding.tvSessionTimer.setBackgroundColor(Color.parseColor("#FEF08A"));
                    binding.tvSessionTimer.setTextColor(Color.parseColor("#854D0E"));
                    long diff = startMillis - now;
                    long hours = (diff / (1000 * 60 * 60));
                    long mins = (diff / (1000 * 60)) % 60;
                    binding.tvSessionTimer.setText("Session starts in " + hours + "h " + mins + "m");
                    
                    if (binding.btnConfirmSessionCompleted.isEnabled()) {
                        binding.btnConfirmSessionCompleted.setEnabled(false);
                        binding.btnConfirmSessionCompleted.setText("Cannot confirm before session ends");
                    }
                } else if (now >= startMillis && now < endMillis) {
                    binding.tvSessionTimer.setVisibility(View.VISIBLE);
                    binding.tvSessionTimer.setBackgroundColor(Color.parseColor("#BBF7D0"));
                    binding.tvSessionTimer.setTextColor(Color.parseColor("#166534"));
                    long diff = endMillis - now;
                    long mins = (diff / (1000 * 60)) % 60;
                    long secs = (diff / 1000) % 60;
                    binding.tvSessionTimer.setText("Session in progress. Ends in " + mins + "m " + secs + "s");
                    
                    if (binding.btnConfirmSessionCompleted.isEnabled()) {
                        binding.btnConfirmSessionCompleted.setEnabled(false);
                        binding.btnConfirmSessionCompleted.setText("Cannot confirm before session ends");
                    }
                } else {
                    onFinish();
                    cancel();
                }
            }

            @Override
            public void onFinish() {
                binding.tvSessionTimer.setVisibility(View.GONE);
                if (currentSession != null) {
                    boolean isLearner = currentUserId != null && currentUserId.equals(currentSession.getLearnerId());
                    boolean isTeacher = currentUserId != null && currentUserId.equals(currentSession.getTeacherId());
                    boolean myConfirmationDone = (isLearner && currentSession.isLearnerConfirmed()) || 
                                                 (isTeacher && currentSession.isTeacherConfirmed());
                    
                    if (!myConfirmationDone) {
                        binding.btnConfirmSessionCompleted.setEnabled(true);
                        binding.btnConfirmSessionCompleted.setText("✅ Yes, I Completed It");
                    }
                }
            }
        }.start();
    }

    private void handleConfirmCompletion() {
        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, "Session ID missing", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.btnConfirmSessionCompleted.setEnabled(false);
        binding.btnConfirmSessionCompleted.setText("Processing Confirmation...");

        FirestoreRepository.getInstance().confirmSessionCompletion(sessionId, currentUserId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SessionDetailActivity.this, "Confirmation recorded!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to confirm completion: " + (e != null ? e.getMessage() : "Unknown"), e);
                    if (binding != null) {
                        binding.btnConfirmSessionCompleted.setEnabled(true);
                        binding.btnConfirmSessionCompleted.setText("✅ Yes, I Completed It");
                    }
                    String err = e != null && e.getMessage() != null ? e.getMessage() : "Try again";
                    Toast.makeText(SessionDetailActivity.this, "Failed to confirm: " + err, Toast.LENGTH_LONG).show();
                });
    }

    private void confirmCancelSession() {
        if (!com.example.skillswapproject.util.ValidationUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(this, "Session ID missing", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Session")
                .setMessage("Are you sure you want to cancel this scheduled session? No Skill Coins will be transferred.")
                .setPositiveButton("Confirm Cancellation", (dialog, which) -> {
                    FirestoreRepository.getInstance().cancelSession(sessionId, currentUserId, "Cancelled by user")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Session cancelled successfully.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to cancel session: " + (e != null ? e.getMessage() : "Unknown"), e);
                                String err = e != null && e.getMessage() != null && !e.getMessage().isEmpty() ? e.getMessage() : "Please try again.";
                                Toast.makeText(this, "Failed to cancel session: " + err, Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionTimer != null) {
            sessionTimer.cancel();
            sessionTimer = null;
        }
        if (sessionRegistration != null) {
            sessionRegistration.remove();
            sessionRegistration = null;
        }
        binding = null;
    }
}
