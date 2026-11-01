package com.example.skillswapproject.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillswapproject.R;
import com.example.skillswapproject.data.model.FirestoreMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_SESSION = 3;

    public interface OnSessionActionListener {
        void onViewDetails(String sessionId);
        void onDeleteSession(String sessionId);
        void onJoinMeeting(String meetUrl);
    }

    private final String currentUserId;
    private final List<FirestoreMessage> messageList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private OnSessionActionListener sessionActionListener;

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setOnSessionActionListener(OnSessionActionListener listener) {
        this.sessionActionListener = listener;
    }

    public void setMessages(List<FirestoreMessage> messages) {
        messageList.clear();
        if (messages != null) {
            messageList.addAll(messages);
        }
        notifyDataSetChanged();
    }

    private boolean isSessionMessage(FirestoreMessage msg) {
        if (msg == null || msg.getMessageText() == null) return false;
        String text = msg.getMessageText();
        return text.contains("📅 Session Scheduled") || text.contains("[SessionID:");
    }

    @Override
    public int getItemViewType(int position) {
        FirestoreMessage msg = messageList.get(position);
        if (isSessionMessage(msg)) {
            return VIEW_TYPE_SESSION;
        }
        if (msg.getSenderId() != null && msg.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SESSION) {
            View view = inflater.inflate(R.layout.item_message_session, parent, false);
            return new SessionViewHolder(view);
        } else if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FirestoreMessage message = messageList.get(position);
        if (holder instanceof SessionViewHolder) {
            ((SessionViewHolder) holder).bind(message, timeFormat, sessionActionListener, currentUserId);
        } else if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message, timeFormat);
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).bind(message, timeFormat);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSentMessage;
        private final TextView tvSentTime;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentTime = itemView.findViewById(R.id.tvSentTime);
        }

        public void bind(FirestoreMessage msg, SimpleDateFormat format) {
            String text = msg.getMessageText();
            tvSentMessage.setText(text);
            if (text != null && (text.contains("meet.google.com") || text.contains("meet.jit.si") || text.contains("http://") || text.contains("https://"))) {
                android.text.util.Linkify.addLinks(tvSentMessage, android.text.util.Linkify.WEB_URLS);
                tvSentMessage.setOnClickListener(v -> {
                    String url = extractMeetUrl(text);
                    if (url != null) {
                        com.example.skillswapproject.util.JitsiMeetHelper.launchJitsiMeet(v.getContext(), url);
                    }
                });
            } else {
                tvSentMessage.setOnClickListener(null);
            }

            if (msg.getTimestamp() != null) {
                tvSentTime.setText(format.format(msg.getTimestamp()));
            } else {
                tvSentTime.setText("Sending...");
            }
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvReceivedMessage;
        private final TextView tvReceivedTime;

        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);
        }

        public void bind(FirestoreMessage msg, SimpleDateFormat format) {
            String text = msg.getMessageText();
            tvReceivedMessage.setText(text);
            if (text != null && (text.contains("meet.google.com") || text.contains("meet.jit.si") || text.contains("http://") || text.contains("https://"))) {
                android.text.util.Linkify.addLinks(tvReceivedMessage, android.text.util.Linkify.WEB_URLS);
                tvReceivedMessage.setOnClickListener(v -> {
                    String url = extractMeetUrl(text);
                    if (url != null) {
                        com.example.skillswapproject.util.JitsiMeetHelper.launchJitsiMeet(v.getContext(), url);
                    }
                });
            } else {
                tvReceivedMessage.setOnClickListener(null);
            }

            if (msg.getTimestamp() != null) {
                tvReceivedTime.setText(format.format(msg.getTimestamp()));
            } else {
                tvReceivedTime.setText("");
            }
        }
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSessionSkillName;
        private final TextView tvSessionDateTime;
        private final TextView tvSessionDuration;
        private final TextView tvSessionMeetUrl;
        private final TextView tvSessionStatusBadge;
        private final TextView tvSessionMsgTime;
        private final View cardSessionMeetLink;
        private final View btnViewSessionDetails;
        private final View btnDeleteSession;
        private final View cardSessionBubble;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSessionSkillName = itemView.findViewById(R.id.tvSessionSkillName);
            tvSessionDateTime = itemView.findViewById(R.id.tvSessionDateTime);
            tvSessionDuration = itemView.findViewById(R.id.tvSessionDuration);
            tvSessionMeetUrl = itemView.findViewById(R.id.tvSessionMeetUrl);
            tvSessionStatusBadge = itemView.findViewById(R.id.tvSessionStatusBadge);
            tvSessionMsgTime = itemView.findViewById(R.id.tvSessionMsgTime);
            cardSessionMeetLink = itemView.findViewById(R.id.cardSessionMeetLink);
            btnViewSessionDetails = itemView.findViewById(R.id.btnViewSessionDetails);
            btnDeleteSession = itemView.findViewById(R.id.btnDeleteSession);
            cardSessionBubble = itemView.findViewById(R.id.cardSessionBubble);
        }

        public void bind(FirestoreMessage msg, java.text.SimpleDateFormat format, OnSessionActionListener listener, String currentUserId) {
            String text = msg.getMessageText();
            if (text == null) text = "";

            String skill = "";
            String date = "";
            String time = "";
            String duration = "";
            String status = "Scheduled";
            String meetUrl = "";
            String sessionId = "";

            String[] lines = text.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("Skill:")) {
                    skill = trimmed.substring("Skill:".length()).trim();
                } else if (trimmed.startsWith("📆 Date:") || trimmed.startsWith("Date:")) {
                    date = trimmed.replace("📆 Date:", "").replace("Date:", "").trim();
                } else if (trimmed.startsWith("🕕 Time:") || trimmed.startsWith("Time:")) {
                    time = trimmed.replace("🕕 Time:", "").replace("Time:", "").trim();
                } else if (trimmed.startsWith("⏳ Duration:") || trimmed.startsWith("Duration:")) {
                    duration = trimmed.replace("⏳ Duration:", "").replace("Duration:", "").trim();
                } else if (trimmed.startsWith("Status:")) {
                    status = trimmed.substring("Status:".length()).trim();
                } else if (trimmed.contains("[SessionID:")) {
                    int startIdx = trimmed.indexOf("[SessionID:");
                    int endIdx = trimmed.indexOf("]", startIdx);
                    if (endIdx != -1) {
                        sessionId = trimmed.substring(startIdx + "[SessionID:".length(), endIdx).trim();
                    }
                } else if (trimmed.startsWith("https://meet.google.com/") || trimmed.startsWith("https://meet.jit.si/")
                        || trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
                    meetUrl = trimmed;
                } else if (trimmed.startsWith("🔗 Google Meet:") || trimmed.startsWith("🔗 Link:")) {
                    meetUrl = trimmed.substring(trimmed.indexOf(":") + 1).trim();
                }
            }

            if (meetUrl.isEmpty()) {
                meetUrl = extractMeetUrl(text);
            }

            if (tvSessionSkillName != null) {
                tvSessionSkillName.setText(!skill.isEmpty() ? skill : "Skill Session");
            }

            StringBuilder dateTimeBuilder = new StringBuilder();
            if (!date.isEmpty()) {
                dateTimeBuilder.append("📅 ").append(date);
            }
            if (!time.isEmpty()) {
                if (dateTimeBuilder.length() > 0) dateTimeBuilder.append("  •  ");
                dateTimeBuilder.append("🕕 ").append(time);
            }
            if (tvSessionDateTime != null) {
                tvSessionDateTime.setText(dateTimeBuilder.length() > 0 ? dateTimeBuilder.toString() : "Scheduled Session");
            }

            if (tvSessionDuration != null) {
                if (!duration.isEmpty()) {
                    tvSessionDuration.setText("⏳ Duration: " + duration);
                    tvSessionDuration.setVisibility(View.VISIBLE);
                } else {
                    tvSessionDuration.setVisibility(View.GONE);
                }
            }

            if (tvSessionStatusBadge != null) {
                tvSessionStatusBadge.setText(status);
            }
            if (tvSessionMeetUrl != null) {
                tvSessionMeetUrl.setText(meetUrl != null && !meetUrl.isEmpty() ? meetUrl : "Tap to open Google Meet");
            }

            final String finalMeetUrl = meetUrl;
            final String finalSessionId = sessionId;

            if (cardSessionMeetLink != null) {
                cardSessionMeetLink.setOnClickListener(v -> {
                    if (listener != null && finalMeetUrl != null && !finalMeetUrl.isEmpty()) {
                        listener.onJoinMeeting(finalMeetUrl);
                    } else if (finalMeetUrl != null && !finalMeetUrl.isEmpty()) {
                        com.example.skillswapproject.util.JitsiMeetHelper.launchJitsiMeet(v.getContext(), finalMeetUrl);
                    }
                });
            }

            if (btnViewSessionDetails != null) {
                btnViewSessionDetails.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onViewDetails(finalSessionId);
                    }
                });
            }

            if (btnDeleteSession != null) {
                btnDeleteSession.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteSession(finalSessionId);
                    }
                });
            }

            if (tvSessionMsgTime != null) {
                if (msg.getTimestamp() != null) {
                    tvSessionMsgTime.setText(format.format(msg.getTimestamp()));
                } else {
                    tvSessionMsgTime.setText("");
                }
            }

            if (cardSessionBubble != null) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) cardSessionBubble.getLayoutParams();
                if (msg.getSenderId() != null && msg.getSenderId().equals(currentUserId)) {
                    params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                } else {
                    params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                }
                cardSessionBubble.setLayoutParams(params);
            }
        }
    }

    private static String extractMeetUrl(String text) {
        if (text == null) return null;
        for (String word : text.split("\\s+")) {
            if (word.startsWith("https://meet.google.com/") || word.startsWith("https://meet.jit.si/")
                    || word.startsWith("https://") || word.startsWith("http://")) {
                return word.trim();
            }
        }
        return null;
    }
}
