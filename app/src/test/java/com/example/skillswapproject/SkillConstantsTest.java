package com.example.skillswapproject;

import com.example.skillswapproject.util.SkillConstants;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SkillConstantsTest {

    @Test
    public void testPredefinedSkillsNotEmpty() {
        assertNotNull(SkillConstants.PREDEFINED_SKILLS);
        assertTrue(SkillConstants.PREDEFINED_SKILLS.length > 10);
        List<String> list = SkillConstants.getSkillList();
        assertNotNull(list);
        assertEquals(SkillConstants.PREDEFINED_SKILLS.length, list.size());
        assertTrue(list.contains("Android Development"));
        assertTrue(list.contains("Python Programming"));
        assertTrue(list.contains("UI/UX Design"));
    }

    @Test
    public void testAvailabilityOptionsNotEmpty() {
        assertNotNull(SkillConstants.AVAILABILITY_OPTIONS);
        assertTrue(SkillConstants.AVAILABILITY_OPTIONS.length > 0);
    }
}
