package com.example.skillswapproject.util;

import com.example.skillswapproject.data.model.FirestoreUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import java.util.Locale;

public class CompatibilityMatcher {

    public static class MatchResult implements Serializable {
        private final int overallScore; // e.g. 95 (for 95%)
        private final int skillScore;   // out of 40
        private final int mentorScore;  // out of 20
        private final int synergyScore; // out of 20
        private final int trustScore;   // out of 20
        private final String matchedSkillsSummary;
        private final String aiSummary;

        public MatchResult(int overallScore, int skillScore, int mentorScore, int synergyScore, int trustScore,
                           String matchedSkillsSummary, String aiSummary) {
            this.overallScore = overallScore;
            this.skillScore = skillScore;
            this.mentorScore = mentorScore;
            this.synergyScore = synergyScore;
            this.trustScore = trustScore;
            this.matchedSkillsSummary = matchedSkillsSummary;
            this.aiSummary = aiSummary;
        }

        public int getOverallScore() {
            return overallScore;
        }

        public int getSkillScore() {
            return skillScore;
        }

        public int getMentorScore() {
            return mentorScore;
        }

        public int getSynergyScore() {
            return synergyScore;
        }

        public int getTrustScore() {
            return trustScore;
        }

        public String getMatchedSkillsSummary() {
            return matchedSkillsSummary != null ? matchedSkillsSummary : "Skill Swap Match";
        }

        public String getAiSummary() {
            return aiSummary != null ? aiSummary : "Great compatibility for skill exchange!";
        }
    }

    /**
     * Calculates the AI Compatibility Score (0%–100%) and sub-breakdown between two users.
     */
    public static MatchResult calculateMatch(FirestoreUser me, FirestoreUser target) {
        if (me == null || target == null) {
            return new MatchResult(75, 25, 15, 15, 20, "General Skill Match", "Potential learning match.");
        }

        List<String> myWanted = me.getWantedSkills() != null ? me.getWantedSkills() : new ArrayList<>();
        List<String> myOffered = me.getOfferedSkills() != null ? me.getOfferedSkills() : new ArrayList<>();

        List<String> targetWanted = target.getWantedSkills() != null ? target.getWantedSkills() : new ArrayList<>();
        List<String> targetOffered = target.getOfferedSkills() != null ? target.getOfferedSkills() : new ArrayList<>();

        // 1. Skill Complementarity Score (Max 40 Pts)
        boolean ICanLearnFromTarget = hasSkillOverlap(myWanted, targetOffered);
        boolean targetCanLearnFromMe = hasSkillOverlap(targetWanted, myOffered);

        String matchedSkillMe = getFirstMatchingSkill(myWanted, targetOffered);
        String matchedSkillTarget = getFirstMatchingSkill(targetWanted, myOffered);

        int skillScore = 0;
        String matchedSummary;

        if (ICanLearnFromTarget && targetCanLearnFromMe) {
            skillScore = 40;
            matchedSummary = "Direct 2-Way Match: " + matchedSkillMe + " ⇄ " + matchedSkillTarget;
        } else if (ICanLearnFromTarget) {
            skillScore = 25;
            matchedSummary = "Teaches: " + matchedSkillMe;
        } else if (targetCanLearnFromMe) {
            skillScore = 20;
            matchedSummary = "Wants to learn: " + matchedSkillTarget;
        } else {
            skillScore = 0;
            matchedSummary = "No direct skill overlap";
        }

        // 2. Mentor Rank & Experience Alignment (Max 20 Pts)
        int targetCoins = target.getSkillCoins();
        String targetRank = MentorRankHelper.getRankTitle(targetCoins);
        int mentorScore;

        if (MentorRankHelper.RANK_MASTER.equalsIgnoreCase(targetRank)) {
            mentorScore = 20;
        } else if (MentorRankHelper.RANK_MENTOR.equalsIgnoreCase(targetRank)) {
            mentorScore = 16;
        } else if (MentorRankHelper.RANK_CONTRIBUTOR.equalsIgnoreCase(targetRank)) {
            mentorScore = 12;
        } else {
            mentorScore = 6;
        }

        // 3. Availability & Location Synergy (Max 20 Pts)
        int synergyScore = 0;
        String myLoc = me.getLocation() != null ? me.getLocation().toLowerCase(Locale.ROOT).trim() : "";
        String targetLoc = target.getLocation() != null ? target.getLocation().toLowerCase(Locale.ROOT).trim() : "";

        // Location synergy (Max 8 pts)
        if (!myLoc.isEmpty() && !targetLoc.isEmpty()) {
            if (myLoc.equalsIgnoreCase(targetLoc) || myLoc.contains(targetLoc) || targetLoc.contains(myLoc)) {
                synergyScore += 8;
            } else {
                String[] myParts = myLoc.split(",");
                String[] targetParts = targetLoc.split(",");
                if (myParts.length >= 2 && targetParts.length >= 2 &&
                        myParts[myParts.length - 1].trim().equalsIgnoreCase(targetParts[targetParts.length - 1].trim())) {
                    synergyScore += 4; // Same country / broad region
                }
            }
        }

        // Availability synergy (Max 12 pts)
        String myAvail = me.getAvailability() != null ? me.getAvailability().toLowerCase(Locale.ROOT).trim() : "";
        String targetAvail = target.getAvailability() != null ? target.getAvailability().toLowerCase(Locale.ROOT).trim() : "";

        if (!myAvail.isEmpty() && !targetAvail.isEmpty()) {
            if (myAvail.equalsIgnoreCase(targetAvail)) {
                synergyScore += 12;
            } else if (myAvail.contains("flexible") || targetAvail.contains("flexible") ||
                       myAvail.contains("anytime") || targetAvail.contains("anytime")) {
                synergyScore += 8;
            } else if (myAvail.contains("weekend") && targetAvail.contains("weekend")) {
                synergyScore += 10;
            } else if (myAvail.contains("weekday") && targetAvail.contains("weekday")) {
                synergyScore += 10;
            }
        } else if (!myAvail.isEmpty() || !targetAvail.isEmpty()) {
            synergyScore += 4;
        }

        // 4. Community Trust & Activity (Max 20 Pts)
        int trustScore = 8; // Verified account baseline
        int coinsEarned = target.getTotalCoinsEarned();
        boolean hasAbout = target.getAboutMe() != null && !target.getAboutMe().trim().isEmpty();
        boolean hasPhoto = target.getProfilePhotoUrl() != null && !target.getProfilePhotoUrl().trim().isEmpty();

        if (coinsEarned >= 20) {
            trustScore = 18;
        } else if (coinsEarned >= 10) {
            trustScore = 15;
        } else if (coinsEarned >= 5) {
            trustScore = 12;
        }

        if (hasAbout && trustScore < 20) trustScore += 1;
        if (hasPhoto && trustScore < 20) trustScore += 1;
        trustScore = Math.min(20, trustScore);

        // Calculate Overall Compatibility Score (Direct true sum: 0 - 100%)
        int totalRaw = skillScore + mentorScore + synergyScore + trustScore;
        int overallScore = Math.max(5, Math.min(100, totalRaw));

        // Generate dynamic AI Summary sentence
        String targetName = target.getName() != null ? target.getName() : "Partner";
        String aiSummary;
        if (overallScore >= 85) {
            aiSummary = targetName + " is an exceptional match for your learning goals with strong skill reciprocity and high community trust.";
        } else if (overallScore >= 60) {
            aiSummary = targetName + " has good compatibility with your profile. " +
                    (skillScore > 0 ? "You share complementary skills in " + matchedSkillMe + "." : "Connect to explore cross-domain skill exchange.");
        } else if (overallScore >= 35) {
            aiSummary = targetName + " offers varied skills with potential mutual interests. Review availability to coordinate a session.";
        } else {
            aiSummary = targetName + " has limited skill or schedule overlap currently, but you can message them to discuss custom topics.";
        }

        return new MatchResult(overallScore, skillScore, mentorScore, synergyScore, trustScore, matchedSummary, aiSummary);
    }

    private static boolean hasSkillOverlap(List<String> list1, List<String> list2) {
        if (list1 == null || list2 == null) return false;
        for (String s1 : list1) {
            for (String s2 : list2) {
                if (s1 != null && s2 != null && (s1.equalsIgnoreCase(s2) || s1.toLowerCase(Locale.ROOT).contains(s2.toLowerCase(Locale.ROOT)) || s2.toLowerCase(Locale.ROOT).contains(s1.toLowerCase(Locale.ROOT)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getFirstMatchingSkill(List<String> list1, List<String> list2) {
        if (list1 == null || list2 == null) return "Skill";
        for (String s1 : list1) {
            for (String s2 : list2) {
                if (s1 != null && s2 != null && (s1.equalsIgnoreCase(s2) || s1.toLowerCase(Locale.ROOT).contains(s2.toLowerCase(Locale.ROOT)) || s2.toLowerCase(Locale.ROOT).contains(s1.toLowerCase(Locale.ROOT)))) {
                    return s2;
                }
            }
        }
        return !list2.isEmpty() ? list2.get(0) : (!list1.isEmpty() ? list1.get(0) : "Skill");
    }
}
