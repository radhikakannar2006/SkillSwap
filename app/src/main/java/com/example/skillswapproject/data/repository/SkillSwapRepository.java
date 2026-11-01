package com.example.skillswapproject.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.skillswapproject.data.model.ChatConversation;
import com.example.skillswapproject.data.model.ChatMessage;
import com.example.skillswapproject.data.model.Role;
import com.example.skillswapproject.data.model.Skill;
import com.example.skillswapproject.data.model.SwapRequest;
import com.example.skillswapproject.data.model.SwapStatus;
import com.example.skillswapproject.data.model.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SkillSwapRepository {

    private static volatile SkillSwapRepository instance;

    // Reactive State Holders
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<List<User>> usersList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<SwapRequest>> swapRequests = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ChatConversation>> conversations = new MutableLiveData<>(new ArrayList<>());
    private final Map<String, List<ChatMessage>> chatHistory = new HashMap<>();
    private final MutableLiveData<Map<String, List<ChatMessage>>> chatMessagesLiveData = new MutableLiveData<>(new HashMap<>());

    private SkillSwapRepository() {
        seedInitialMockDatabase();
    }

    public static SkillSwapRepository getInstance() {
        if (instance == null) {
            synchronized (SkillSwapRepository.class) {
                if (instance == null) {
                    instance = new SkillSwapRepository();
                }
            }
        }
        return instance;
    }

    // --- LiveData Getters ---

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser.postValue(user);
    }

    public LiveData<List<User>> getUsersList() {
        return usersList;
    }

    public LiveData<List<SwapRequest>> getSwapRequests() {
        return swapRequests;
    }

    public LiveData<List<ChatConversation>> getConversations() {
        return conversations;
    }

    public LiveData<Map<String, List<ChatMessage>>> getChatMessagesLiveData() {
        return chatMessagesLiveData;
    }

    // --- Authentication & User Actions ---

    public boolean login(String emailOrPhone, String password) {
        List<User> list = usersList.getValue();
        if (list != null) {
            for (User user : list) {
                if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(emailOrPhone)) {
                    currentUser.setValue(user);
                    refreshConversations();
                    return true;
                }
            }
        }

        // Default login fallback for demo
        User defaultUser = (list != null && !list.isEmpty()) ? list.get(0) : createDefaultUser();
        currentUser.setValue(defaultUser);
        refreshConversations();
        return true;
    }

    public User signUp(String name, String email, String password, List<Skill> teachSkills, List<Skill> learnSkills) {
        String newId = "user_" + System.currentTimeMillis();
        User newUser = new User(
                newId, name, email, password, "San Francisco, CA", null,
                true, 5.0, 0, 0, 0, Role.STANDARD,
                teachSkills, learnSkills, "Weekends & Evenings",
                "Hi! I'm " + name + ", excited to learn and teach new skills on SkillSwap!"
        );

        List<User> currentUsers = usersList.getValue();
        if (currentUsers == null) currentUsers = new ArrayList<>();
        List<User> updatedUsers = new ArrayList<>(currentUsers);
        updatedUsers.add(newUser);
        usersList.setValue(updatedUsers);

        currentUser.setValue(newUser);
        refreshConversations();
        return newUser;
    }

    public void updateCurrentUserProfile(String name, String location, String availability,
                                         String aboutMe, List<Skill> teachSkills,
                                         List<Skill> learnSkills, boolean isPublic) {
        User user = currentUser.getValue();
        if (user != null) {
            user.setName(name);
            user.setLocation(location);
            user.setAvailability(availability);
            user.setAboutMe(aboutMe);
            user.setOfferedSkills(teachSkills);
            user.setWantedSkills(learnSkills);
            user.setPublic(isPublic);

            // Trigger LiveData update
            currentUser.setValue(user);

            // Update user in the global usersList
            List<User> currentUsers = usersList.getValue();
            if (currentUsers != null) {
                List<User> updatedUsers = new ArrayList<>();
                for (User u : currentUsers) {
                    if (u.getId().equals(user.getId())) {
                        updatedUsers.add(user);
                    } else {
                        updatedUsers.add(u);
                    }
                }
                usersList.setValue(updatedUsers);
            }
        }
    }

    // --- Swap Requests & Friend Connections ---

    public void sendSwapRequest(String receiverId, String offeredSkill, String requestedSkill, String message) {
        User current = currentUser.getValue();
        String senderId = current != null ? current.getId() : "u_self";

        SwapRequest request = new SwapRequest(
                "req_" + System.currentTimeMillis(),
                senderId,
                receiverId,
                offeredSkill,
                requestedSkill,
                SwapStatus.ACCEPTED, // Auto-accepted for demo interaction & instant chat unlock!
                message
        );

        List<SwapRequest> currentRequests = swapRequests.getValue();
        if (currentRequests == null) currentRequests = new ArrayList<>();
        List<SwapRequest> updatedRequests = new ArrayList<>(currentRequests);
        updatedRequests.add(0, request);
        swapRequests.setValue(updatedRequests);

        // Add an initial greeting message in the chat thread
        sendMessage(receiverId, "Hi! I just sent a swap request for " + requestedSkill + ". Looking forward to collaborating!");
        refreshConversations();
    }

    // --- Chat & Messaging ---

    public void sendMessage(String receiverId, String text) {
        User current = currentUser.getValue();
        String senderId = current != null ? current.getId() : "u_self";

        ChatMessage msg = new ChatMessage(
                "msg_" + System.currentTimeMillis(),
                senderId,
                receiverId,
                text,
                System.currentTimeMillis()
        );

        List<ChatMessage> thread = chatHistory.get(receiverId);
        if (thread == null) {
            thread = new ArrayList<>();
            chatHistory.put(receiverId, thread);
        }
        thread.add(msg);

        // Notify LiveData observers
        chatMessagesLiveData.setValue(new HashMap<>(chatHistory));
        refreshConversations();
    }

    public List<ChatMessage> getMessagesForUser(String userId) {
        List<ChatMessage> list = chatHistory.get(userId);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public User getUserById(String userId) {
        List<User> list = usersList.getValue();
        if (list != null) {
            for (User u : list) {
                if (u.getId().equals(userId)) return u;
            }
        }
        return null;
    }

    public void refreshConversations() {
        User current = currentUser.getValue();
        String currentId = current != null ? current.getId() : "u_self";
        List<User> allUsers = usersList.getValue();
        List<ChatConversation> conversationList = new ArrayList<>();

        if (allUsers != null) {
            for (User user : allUsers) {
                if (!user.getId().equals(currentId)) {
                    List<ChatMessage> messages = chatHistory.get(user.getId());
                    String lastMsg = "Tap to start chatting and swap skills!";
                    String timeStr = "Now";

                    if (messages != null && !messages.isEmpty()) {
                        ChatMessage last = messages.get(messages.size() - 1);
                        lastMsg = last.getMessage();
                        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                        timeStr = sdf.format(new Date(last.getTimestamp()));
                    }

                    conversationList.add(new ChatConversation(user, lastMsg, timeStr, 0));
                }
            }
        }

        conversations.setValue(conversationList);
    }

    // --- Seed Initial Mock Database ---

    private void seedInitialMockDatabase() {
        Skill webDesign = new Skill("s1", "Web Design", "Technology");
        Skill figma = new Skill("s2", "UI/UX & Figma", "Design");
        Skill htmlCss = new Skill("s3", "HTML/CSS", "Technology");
        Skill python = new Skill("s4", "Python", "Technology");
        Skill dataAnalysis = new Skill("s5", "Data Analysis", "Technology");
        Skill guitar = new Skill("s6", "Guitar", "Music");
        Skill excel = new Skill("s7", "Excel & Finance", "Business");
        Skill photography = new Skill("s8", "Photography", "Arts");
        Skill english = new Skill("s9", "English", "Languages");

        // Primary Logged-in User
        User self = new User(
                "u_self", "Maanasya Jain", "maanasya@skillswap.com", "password123",
                "San Francisco, CA", null, true, 4.8, 8, 12, 3, Role.STANDARD,
                Arrays.asList(webDesign, figma, htmlCss),
                Arrays.asList(python, dataAnalysis, guitar),
                "Weekends & Evenings",
                "I love creating digital designs and helping others learn."
        );
        currentUser.setValue(null);

        // Community Recommended Users
        User ananya = new User(
                "u_ananya", "Ananya S.", "ananya@skillswap.com", "password123",
                "San Francisco, CA", null, true, 4.8, 24, 18, 5, Role.STANDARD,
                Arrays.asList(webDesign, figma),
                Arrays.asList(python, dataAnalysis),
                "Flexible weekday evenings",
                "Learner & Teacher · Computer Science Student passionate about UI design and Python."
        );

        User rohan = new User(
                "u_rohan", "Rohan M.", "rohan@skillswap.com", "password123",
                "Austin, TX", null, true, 4.6, 18, 14, 4, Role.STANDARD,
                Arrays.asList(guitar, new Skill("s10", "Music Theory", "Music")),
                Arrays.asList(excel, new Skill("s11", "Financial Modeling", "Business")),
                "Saturday afternoons",
                "Guitarist for 7 years! Looking to master financial analysis in Excel."
        );

        User sneha = new User(
                "u_sneha", "Sneha K.", "sneha@skillswap.com", "password123",
                "Seattle, WA", null, true, 4.9, 22, 20, 6, Role.STANDARD,
                Arrays.asList(photography, new Skill("s12", "Lightroom", "Arts")),
                Arrays.asList(english, new Skill("s13", "Public Speaking", "Languages")),
                "Sunday mornings",
                "Portrait photographer looking to hone public speaking and communication skills."
        );

        List<User> users = new ArrayList<>(Arrays.asList(self, ananya, rohan, sneha));
        usersList.setValue(users);

        // Seed initial WhatsApp style chats
        List<ChatMessage> ananyaChat = new ArrayList<>();
        ananyaChat.add(new ChatMessage("m1", "u_ananya", "u_self", "Hey Maanasya! Saw your Web Design profile, would love to swap for Python!", System.currentTimeMillis() - 3600000));
        ananyaChat.add(new ChatMessage("m2", "u_self", "u_ananya", "Sounds great! Let's schedule a session this weekend.", System.currentTimeMillis() - 1800000));
        chatHistory.put("u_ananya", ananyaChat);

        List<ChatMessage> rohanChat = new ArrayList<>();
        rohanChat.add(new ChatMessage("m3", "u_rohan", "u_self", "Hi! Thanks for accepting my swap request.", System.currentTimeMillis() - 86400000));
        chatHistory.put("u_rohan", rohanChat);

        chatMessagesLiveData.setValue(new HashMap<>(chatHistory));
        refreshConversations();
    }

    private User createDefaultUser() {
        return new User(
                "u_default", "New Swapper", "user@skillswap.com", "password",
                "Global", null, true, 5.0, 0, 0, 0, Role.STANDARD,
                new ArrayList<>(), new ArrayList<>(), "Flexible", "Welcome to SkillSwap!"
        );
    }
}
