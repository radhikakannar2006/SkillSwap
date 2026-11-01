package com.example.skillswapproject;

import com.example.skillswapproject.data.model.FirestoreUser;
import com.example.skillswapproject.util.CompatibilityMatcher;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class CompatibilityMatcherTest {

    @Test
    public void testNullUserHandling() {
        CompatibilityMatcher.MatchResult res1 = CompatibilityMatcher.calculateMatch(null, null);
        assertNotNull(res1);
        assertTrue(res1.getOverallScore() >= 0 && res1.getOverallScore() <= 100);
        assertNotNull(res1.getMatchedSkillsSummary());
        assertNotNull(res1.getAiSummary());

        FirestoreUser user1 = new FirestoreUser();
        CompatibilityMatcher.MatchResult res2 = CompatibilityMatcher.calculateMatch(user1, null);
        assertNotNull(res2);
        assertTrue(res2.getOverallScore() >= 0 && res2.getOverallScore() <= 100);

        CompatibilityMatcher.MatchResult res3 = CompatibilityMatcher.calculateMatch(null, user1);
        assertNotNull(res3);
        assertTrue(res3.getOverallScore() >= 0 && res3.getOverallScore() <= 100);
    }

    @Test
    public void testTwoWayDirectMatch() {
        FirestoreUser me = new FirestoreUser();
        me.setOfferedSkills(Collections.singletonList("Python Programming"));
        me.setWantedSkills(Collections.singletonList("Spanish Conversation"));
        me.setSkillCoins(15);
        me.setLocation("New York, USA");
        me.setAvailability("Weekends");

        FirestoreUser partner = new FirestoreUser();
        partner.setName("Maria");
        partner.setOfferedSkills(Collections.singletonList("Spanish Conversation"));
        partner.setWantedSkills(Collections.singletonList("Python Programming"));
        partner.setSkillCoins(25);
        partner.setLocation("New York, USA");
        partner.setAvailability("Weekends");

        CompatibilityMatcher.MatchResult result = CompatibilityMatcher.calculateMatch(me, partner);
        assertNotNull(result);
        assertEquals(40, result.getSkillScore());
        assertTrue(result.getOverallScore() >= 70);
        assertTrue(result.getMatchedSkillsSummary().contains("Direct 2-Way Match"));
        assertTrue(result.getAiSummary().contains("Maria"));
    }

    @Test
    public void testOneWayLearnMatch() {
        FirestoreUser me = new FirestoreUser();
        me.setOfferedSkills(Collections.singletonList("Graphic Design"));
        me.setWantedSkills(Collections.singletonList("Kotlin"));

        FirestoreUser partner = new FirestoreUser();
        partner.setName("Alex");
        partner.setOfferedSkills(Collections.singletonList("Kotlin"));
        partner.setWantedSkills(Collections.singletonList("Guitar"));

        CompatibilityMatcher.MatchResult result = CompatibilityMatcher.calculateMatch(me, partner);
        assertNotNull(result);
        assertEquals(25, result.getSkillScore());
        assertTrue(result.getMatchedSkillsSummary().contains("Teaches"));
    }

    @Test
    public void testNoSkillOverlap() {
        FirestoreUser me = new FirestoreUser();
        me.setOfferedSkills(Collections.singletonList("Cooking"));
        me.setWantedSkills(Collections.singletonList("Surfing"));

        FirestoreUser partner = new FirestoreUser();
        partner.setName("Sam");
        partner.setOfferedSkills(Collections.singletonList("Piano"));
        partner.setWantedSkills(Collections.singletonList("Yoga"));

        CompatibilityMatcher.MatchResult result = CompatibilityMatcher.calculateMatch(me, partner);
        assertNotNull(result);
        assertEquals(0, result.getSkillScore());
        assertEquals("No direct skill overlap", result.getMatchedSkillsSummary());
        assertTrue(result.getOverallScore() >= 5); // baseline minimum
    }
}
