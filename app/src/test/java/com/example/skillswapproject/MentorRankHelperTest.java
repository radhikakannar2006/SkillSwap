package com.example.skillswapproject;

import com.example.skillswapproject.util.MentorRankHelper;

import org.junit.Test;

import static org.junit.Assert.*;

public class MentorRankHelperTest {

    @Test
    public void testRankTitles() {
        assertEquals(MentorRankHelper.RANK_BEGINNER, MentorRankHelper.getRankTitle(0));
        assertEquals(MentorRankHelper.RANK_BEGINNER, MentorRankHelper.getRankTitle(5));
        assertEquals(MentorRankHelper.RANK_CONTRIBUTOR, MentorRankHelper.getRankTitle(6));
        assertEquals(MentorRankHelper.RANK_CONTRIBUTOR, MentorRankHelper.getRankTitle(20));
        assertEquals(MentorRankHelper.RANK_MENTOR, MentorRankHelper.getRankTitle(21));
        assertEquals(MentorRankHelper.RANK_MENTOR, MentorRankHelper.getRankTitle(50));
        assertEquals(MentorRankHelper.RANK_MASTER, MentorRankHelper.getRankTitle(51));
        assertEquals(MentorRankHelper.RANK_MASTER, MentorRankHelper.getRankTitle(100));
    }

    @Test
    public void testNextRankTitles() {
        assertEquals(MentorRankHelper.RANK_CONTRIBUTOR, MentorRankHelper.getNextRankTitle(0));
        assertEquals(MentorRankHelper.RANK_CONTRIBUTOR, MentorRankHelper.getNextRankTitle(5));
        assertEquals(MentorRankHelper.RANK_MENTOR, MentorRankHelper.getNextRankTitle(6));
        assertEquals(MentorRankHelper.RANK_MENTOR, MentorRankHelper.getNextRankTitle(20));
        assertEquals(MentorRankHelper.RANK_MASTER, MentorRankHelper.getNextRankTitle(21));
        assertEquals(MentorRankHelper.RANK_MASTER, MentorRankHelper.getNextRankTitle(50));
        assertEquals("Max Rank Reached", MentorRankHelper.getNextRankTitle(51));
    }

    @Test
    public void testCoinsToNextRank() {
        assertEquals(6, MentorRankHelper.getCoinsToNextRank(0));
        assertEquals(1, MentorRankHelper.getCoinsToNextRank(5));
        assertEquals(15, MentorRankHelper.getCoinsToNextRank(6));
        assertEquals(1, MentorRankHelper.getCoinsToNextRank(20));
        assertEquals(30, MentorRankHelper.getCoinsToNextRank(21));
        assertEquals(1, MentorRankHelper.getCoinsToNextRank(50));
        assertEquals(0, MentorRankHelper.getCoinsToNextRank(51));
    }

    @Test
    public void testProgressPercentageClamping() {
        assertTrue(MentorRankHelper.getProgressPercentage(0) >= 0);
        assertTrue(MentorRankHelper.getProgressPercentage(5) <= 100);
        assertTrue(MentorRankHelper.getProgressPercentage(6) >= 0);
        assertTrue(MentorRankHelper.getProgressPercentage(20) <= 100);
        assertTrue(MentorRankHelper.getProgressPercentage(21) >= 0);
        assertTrue(MentorRankHelper.getProgressPercentage(50) <= 100);
        assertEquals(100, MentorRankHelper.getProgressPercentage(51));
        assertEquals(100, MentorRankHelper.getProgressPercentage(200));
    }

    @Test
    public void testRankDescriptionNotNull() {
        assertNotNull(MentorRankHelper.getRankDescription(0));
        assertNotNull(MentorRankHelper.getRankDescription(10));
        assertNotNull(MentorRankHelper.getRankDescription(30));
        assertNotNull(MentorRankHelper.getRankDescription(80));
    }
}
