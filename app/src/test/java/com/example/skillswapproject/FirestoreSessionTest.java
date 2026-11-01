package com.example.skillswapproject;

import com.example.skillswapproject.data.model.FirestoreSession;

import org.junit.Test;

import static org.junit.Assert.*;

public class FirestoreSessionTest {

    @Test
    public void testStatusConstantsNotNull() {
        assertNotNull(FirestoreSession.STATUS_REQUESTED);
        assertNotNull(FirestoreSession.STATUS_ACCEPTED);
        assertNotNull(FirestoreSession.STATUS_SCHEDULED);
        assertNotNull(FirestoreSession.STATUS_IN_PROGRESS);
        assertNotNull(FirestoreSession.STATUS_COMPLETED);
        assertNotNull(FirestoreSession.STATUS_CANCELLED);
    }

    @Test
    public void testSessionDefaults() {
        FirestoreSession session = new FirestoreSession();
        assertEquals("Jitsi Meet", session.getMeetingProvider());
        assertFalse(session.isLearnerConfirmed());
        assertFalse(session.isTeacherConfirmed());
        assertFalse(session.isCoinsReleased());
        assertEquals(FirestoreSession.TEACHING_SESSION_REWARD_COINS, session.getRewardCoins());
    }

    @Test
    public void testSessionGettersAndSetters() {
        FirestoreSession session = new FirestoreSession();
        session.setSessionId("sess_101");
        session.setStatus(FirestoreSession.STATUS_SCHEDULED);
        session.setLearnerId("user_1");
        session.setTeacherId("user_2");
        session.setSkillName("Android Development");
        session.setDurationMinutes(60);
        session.setMeetLink("https://meet.jit.si/SkillSwap_101");

        assertEquals("sess_101", session.getSessionId());
        assertEquals(FirestoreSession.STATUS_SCHEDULED, session.getStatus());
        assertEquals("user_1", session.getLearnerId());
        assertEquals("user_2", session.getTeacherId());
        assertEquals("Android Development", session.getSkillName());
        assertEquals(60, session.getDurationMinutes());
        assertEquals("https://meet.jit.si/SkillSwap_101", session.getMeetLink());
    }
}
