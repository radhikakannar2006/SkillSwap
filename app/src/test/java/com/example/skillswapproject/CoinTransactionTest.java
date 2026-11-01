package com.example.skillswapproject;

import com.example.skillswapproject.data.model.CoinTransaction;

import org.junit.Test;

import static org.junit.Assert.*;

public class CoinTransactionTest {

    @Test
    public void testCreditDebitDetermination() {
        CoinTransaction creditTx = new CoinTransaction(
                "tx_1", "user_123", 5, CoinTransaction.TYPE_WELCOME_BONUS,
                "Welcome Bonus", null, 0, 5
        );
        assertTrue(creditTx.isCredit());
        assertEquals(5, creditTx.getAmount());
        assertEquals("user_123", creditTx.getUserId());
        assertEquals(0, creditTx.getBalanceBefore());
        assertEquals(5, creditTx.getBalanceAfter());

        CoinTransaction debitTx = new CoinTransaction(
                "tx_2", "user_123", -2, CoinTransaction.TYPE_LEARNING_SESSION_PAYMENT,
                "Session Payment", null, 5, 3
        );
        assertFalse(debitTx.isCredit());
        assertEquals(-2, debitTx.getAmount());
        assertEquals(5, debitTx.getBalanceBefore());
        assertEquals(3, debitTx.getBalanceAfter());
    }

    @Test
    public void testTransactionTypesNonNull() {
        assertNotNull(CoinTransaction.TYPE_WELCOME_BONUS);
        assertNotNull(CoinTransaction.TYPE_TEACH_30_MIN);
        assertNotNull(CoinTransaction.TYPE_TEACH_1_HOUR);
        assertNotNull(CoinTransaction.TYPE_SUCCESSFUL_SWAP);
        assertNotNull(CoinTransaction.TYPE_FIVE_STAR_REVIEW);
        assertNotNull(CoinTransaction.TYPE_LEARNING_SESSION_PAYMENT);
        assertNotNull(CoinTransaction.TYPE_TEACHING_SESSION_COMPLETED);
        assertNotNull(CoinTransaction.TYPE_DAILY_ACTIVE_REWARD);
    }
}
