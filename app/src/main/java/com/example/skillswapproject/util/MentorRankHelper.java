package com.example.skillswapproject.util;

import android.graphics.Color;

public class MentorRankHelper {

    public static final String RANK_BEGINNER = "Beginner";
    public static final String RANK_CONTRIBUTOR = "Contributor";
    public static final String RANK_MENTOR = "Mentor";
    public static final String RANK_MASTER = "Master";

    /**
     * Calculates the mentor rank dynamically from the user's current Skill Coin balance.
     * Beginner: 0–5 Coins
     * Contributor: 6–20 Coins
     * Mentor: 21–50 Coins
     * Master: 51+ Coins
     */
    public static String getRankTitle(int coins) {
        if (coins <= 5) {
            return RANK_BEGINNER;
        } else if (coins <= 20) {
            return RANK_CONTRIBUTOR;
        } else if (coins <= 50) {
            return RANK_MENTOR;
        } else {
            return RANK_MASTER;
        }
    }

    public static int getRankBadgeColor(int coins) {
        if (coins <= 5) {
            return Color.parseColor("#6B7280"); // Muted Gray
        } else if (coins <= 20) {
            return Color.parseColor("#2E7D5B"); // SkillSwap Green
        } else if (coins <= 50) {
            return Color.parseColor("#2563EB"); // Royal Blue
        } else {
            return Color.parseColor("#F59E0B"); // Gold
        }
    }

    public static int getRankBgColor(int coins) {
        if (coins <= 5) {
            return Color.parseColor("#F3F4F6");
        } else if (coins <= 20) {
            return Color.parseColor("#E8F5E9");
        } else if (coins <= 50) {
            return Color.parseColor("#EFF6FF");
        } else {
            return Color.parseColor("#FEF3C7");
        }
    }

    public static String getNextRankTitle(int coins) {
        if (coins <= 5) {
            return RANK_CONTRIBUTOR;
        } else if (coins <= 20) {
            return RANK_MENTOR;
        } else if (coins <= 50) {
            return RANK_MASTER;
        } else {
            return "Max Rank Reached";
        }
    }

    public static int getCoinsToNextRank(int coins) {
        if (coins <= 5) {
            return 6 - coins;
        } else if (coins <= 20) {
            return 21 - coins;
        } else if (coins <= 50) {
            return 51 - coins;
        } else {
            return 0;
        }
    }

    public static int getProgressPercentage(int coins) {
        if (coins <= 5) {
            return Math.min(100, Math.max(0, (coins * 100) / 6));
        } else if (coins <= 20) {
            int range = 21 - 6;
            int current = coins - 6;
            return Math.min(100, Math.max(0, (current * 100) / range));
        } else if (coins <= 50) {
            int range = 51 - 21;
            int current = coins - 21;
            return Math.min(100, Math.max(0, (current * 100) / range));
        } else {
            return 100;
        }
    }

    public static String getRankDescription(int coins) {
        if (coins <= 5) {
            return "Getting started on SkillSwap. Learn and teach skills to grow your balance!";
        } else if (coins <= 20) {
            return "Active community member regularly sharing and learning new skills.";
        } else if (coins <= 50) {
            return "Experienced knowledge swapper trusted by the community.";
        } else {
            return "Elite master teacher with extensive contributions to SkillSwap!";
        }
    }
}
